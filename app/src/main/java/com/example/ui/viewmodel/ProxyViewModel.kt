package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.ProxyEntity
import com.example.data.repository.ProxyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.random.Random

sealed interface ConnectionState {
    object Disconnected : ConnectionState
    object Connecting : ConnectionState
    data class Connected(val server: ProxyEntity) : ConnectionState
}

sealed interface ScrapeState {
    object Idle : ScrapeState
    data class Scraping(val currentIndex: Int, val total: Int, val workingCount: Int) : ScrapeState
    data class Success(val addedCount: Int) : ScrapeState
    data class Error(val message: String) : ScrapeState
}

class ProxyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProxyRepository
    
    val allProxies: StateFlow<List<ProxyEntity>>
    val activeProxies: StateFlow<List<ProxyEntity>>

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _selectedProxy = MutableStateFlow<ProxyEntity?>(null)
    val selectedProxy: StateFlow<ProxyEntity?> = _selectedProxy.asStateFlow()

    private val _autoConnectEnabled = MutableStateFlow(true)
    val autoConnectEnabled: StateFlow<Boolean> = _autoConnectEnabled.asStateFlow()

    private val _scrapeState = MutableStateFlow<ScrapeState>(ScrapeState.Idle)
    val scrapeState: StateFlow<ScrapeState> = _scrapeState.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    // Traffic stats
    private val _kbDownloaded = MutableStateFlow(0.0)
    val kbDownloaded: StateFlow<Double> = _kbDownloaded.asStateFlow()

    private val _kbUploaded = MutableStateFlow(0.0)
    val kbUploaded: StateFlow<Double> = _kbUploaded.asStateFlow()

    private val _connectionDurationSec = MutableStateFlow(0L)
    val connectionDurationSec: StateFlow<Long> = _connectionDurationSec.asStateFlow()

    private val _latenciesList = MutableStateFlow<List<Long>>(emptyList())
    val latenciesList: StateFlow<List<Long>> = _latenciesList.asStateFlow()

    private var trafficJob: Job? = null
    private var timerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ProxyRepository(database.proxyDao())
        
        allProxies = repository.allProxies.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        activeProxies = repository.activeProxies.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed database immediately if empty with high grade XRAY bypass routes
        viewModelScope.launch(Dispatchers.IO) {
            val count = database.proxyDao().getCount()
            if (count == 0) {
                val preloaded = com.example.data.network.ProxyScraper.fetchCandidateProxies()
                database.proxyDao().insertProxies(preloaded)
            }
        }

        // Automatically default pick the first proxy or fastest if available
        viewModelScope.launch {
            allProxies.collect { list ->
                if (_selectedProxy.value == null && list.isNotEmpty()) {
                    _selectedProxy.value = list.firstOrNull { it.isActive } ?: list.first()
                }
            }
        }
    }

    fun toggleAutoConnect(enabled: Boolean) {
        _autoConnectEnabled.value = enabled
        if (enabled) {
            autoSelectFastest()
        }
    }

    fun selectProxy(proxy: ProxyEntity) {
        _selectedProxy.value = proxy
        _autoConnectEnabled.value = false // turn off auto connect if manually selected
        
        // If already connected, reconnect to the new server
        if (_connectionState.value is ConnectionState.Connected) {
            connectToServer(proxy)
        }
    }

    fun requestScrapePermission() {
        _showPermissionDialog.value = true
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }

    fun confirmScrape() {
        _showPermissionDialog.value = false
        triggerServerSearchAndFilter()
    }

    /**
     * Finds and fetches servers from public internet directories.
     * Screens and filters servers, deleting offline ones and keeping online ones.
     */
    fun triggerServerSearchAndFilter() {
        viewModelScope.launch {
            _scrapeState.value = ScrapeState.Scraping(0, 0, 0)
            try {
                val added = repository.refreshServers { index, total, working ->
                    _scrapeState.value = ScrapeState.Scraping(index, total, working)
                }
                _scrapeState.value = ScrapeState.Success(added)
                
                // If auto-connect is on, reconnect to the newly scraped fastest node
                if (_autoConnectEnabled.value) {
                    autoSelectFastest()
                }
                
                delay(3000)
                _scrapeState.value = ScrapeState.Idle
            } catch (e: Exception) {
                _scrapeState.value = ScrapeState.Error("Ошибка обновления: ${e.localizedMessage}")
                delay(3000)
                _scrapeState.value = ScrapeState.Idle
            }
        }
    }

    private fun autoSelectFastest() {
        val activeList = allProxies.value.filter { it.isActive && it.latencyMs > 0 }
        if (activeList.isNotEmpty()) {
            val fastest = activeList.minByOrNull { it.latencyMs }
            if (fastest != null) {
                _selectedProxy.value = fastest
                if (_connectionState.value is ConnectionState.Connected) {
                    connectToServer(fastest)
                }
            }
        }
    }

    /**
     * Toggles connection state.
     */
    fun toggleConnection() {
        val current = _connectionState.value
        if (current is ConnectionState.Connected) {
            disconnect()
        } else {
            // Find proxy to connect
            val target = _selectedProxy.value
            if (target != null) {
                connectToServer(target)
            } else {
                // If no chosen server, trigger scraping or grab first
                val list = allProxies.value
                val activeList = list.filter { it.isActive }
                val fallback = activeList.firstOrNull() ?: list.firstOrNull()
                if (fallback != null) {
                    _selectedProxy.value = fallback
                    connectToServer(fallback)
                } else {
                    // Ask user permission to search for servers
                    requestScrapePermission()
                }
            }
        }
    }

    private fun connectToServer(proxy: ProxyEntity) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            
            // Perform rapid socket test before official connection establishing
            val tested = repository.testSingleProxy(proxy)
            
            if (tested.isActive) {
                _connectionState.value = ConnectionState.Connected(tested)
                _selectedProxy.value = tested
                startTrafficSimulation()
                startTimer()
            } else {
                // Connection failed (server offline). Prompt removal and reconnect to next best.
                _connectionState.value = ConnectionState.Disconnected
                
                // Re-calculate or trigger notification
                autoSelectFastest()
            }
        }
    }

    fun testCurrentServerConnection() {
        val proxy = _selectedProxy.value ?: return
        viewModelScope.launch {
            val tested = repository.testSingleProxy(proxy)
            _selectedProxy.value = tested
            
            // If we are currently connected and and the ping failed, disconnect
            if (!tested.isActive && _connectionState.value is ConnectionState.Connected) {
                disconnect()
            }
            
            // Add to latency histories for dashboard visualization
            if (tested.isActive) {
                val currentList = _latenciesList.value.toMutableList()
                if (currentList.size > 8) currentList.removeAt(0)
                currentList.add(tested.latencyMs)
                _latenciesList.value = currentList
            }
        }
    }

    private fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
        stopTrafficSimulation()
        stopTimer()
    }

    private fun startTrafficSimulation() {
        trafficJob?.cancel()
        trafficJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_connectionState.value is ConnectionState.Connected) {
                    val addDown = Random.nextDouble(10.0, 150.0)
                    val addUp = Random.nextDouble(2.0, 35.0)
                    _kbDownloaded.value += addDown
                    _kbUploaded.value += addUp
                }
            }
        }
    }

    private fun stopTrafficSimulation() {
        trafficJob?.cancel()
        _kbDownloaded.value = 0.0
        _kbUploaded.value = 0.0
    }

    private fun startTimer() {
        timerJob?.cancel()
        _connectionDurationSec.value = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _connectionDurationSec.value += 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _connectionDurationSec.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        stopTrafficSimulation()
        stopTimer()
    }
}
