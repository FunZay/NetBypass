package com.example.data.repository

import com.example.data.database.ProxyDao
import com.example.data.database.ProxyEntity
import com.example.data.network.ProxyScraper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ProxyRepository(private val proxyDao: ProxyDao) {

    val allProxies: Flow<List<ProxyEntity>> = proxyDao.getAllProxies()
    val activeProxies: Flow<List<ProxyEntity>> = proxyDao.getActiveProxies()

    /**
     * Downloads alternative fresh servers from online repositories, pings them,
     * deletes defunct ones, and persists working ones in the local DB.
     */
    suspend fun refreshServers(
        onProgress: (index: Int, total: Int, working: Int) -> Unit
    ): Int {
        // 1. Fetch potential candidates from the internet
        val candidates = ProxyScraper.fetchCandidateProxies()
        if (candidates.isEmpty()) return 0

        // 2. Test connectivity in batch
        val tested = ProxyScraper.testBatch(candidates, onProgress)

        // 3. Clear existing inactive, then save the tested set
        // (This deletes non-working and adds working ones to the client)
        proxyDao.deleteInactiveProxies()
        
        // Filter out online proxies & insert
        val activeOnly = tested.filter { it.isActive }
        if (activeOnly.isNotEmpty()) {
            proxyDao.insertProxies(activeOnly)
        }
        
        return activeOnly.size
    }

    suspend fun testSingleProxy(proxy: ProxyEntity): ProxyEntity {
        val updated = ProxyScraper.testProxyConnectivity(proxy)
        proxyDao.insertProxy(updated)
        if (!updated.isActive) {
            // Delete if dead as requested: "нерабочие сервера удаляются а рабочие добавляются в клиент"
            proxyDao.deleteProxyById(proxy.id)
        }
        return updated
    }

    suspend fun insertProxy(proxy: ProxyEntity) {
        proxyDao.insertProxy(proxy)
    }

    suspend fun deleteProxy(proxy: ProxyEntity) {
        proxyDao.deleteProxy(proxy)
    }

    suspend fun deleteProxyById(id: String) {
        proxyDao.deleteProxyById(id)
    }

    suspend fun clearAll() {
        proxyDao.clearAllProxies()
    }
}
