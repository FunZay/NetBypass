package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.ProxyEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ConnectionState
import com.example.ui.viewmodel.ProxyViewModel
import com.example.ui.viewmodel.ScrapeState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ProxyViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedProxy by viewModel.selectedProxy.collectAsState()
    val autoConnectEnabled by viewModel.autoConnectEnabled.collectAsState()
    val scrapeState by viewModel.scrapeState.collectAsState()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsState()
    val kbDownloaded by viewModel.kbDownloaded.collectAsState()
    val kbUploaded by viewModel.kbUploaded.collectAsState()
    val durationSeconds by viewModel.connectionDurationSec.collectAsState()
    val latenciesHistory by viewModel.latenciesList.collectAsState()
    val proxyList by viewModel.allProxies.collectAsState()

    var showServerListSheet by remember { mutableStateOf(false) }

    val MonochromeBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1C1C1E), // Top Dark Graphite
            Color(0xFF000000)  // Bottom Pure Obsidian
        )
    )

    // Edge To Edge container
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MonochromeBackgroundBrush),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Shield",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NetBypass",
                                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "by @funzay",
                                style = MaterialTheme.typography.labelSmall,
                                color = StarryGrey,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Connection Pulse Dial
                item {
                    ConnectionDialSection(
                        connectionState = connectionState,
                        onConnectClick = { viewModel.toggleConnection() }
                    )
                }

                // Traffic Diagnostics Stats
                item {
                    TrafficStatsRow(
                        kbDownloaded = kbDownloaded,
                        kbUploaded = kbUploaded,
                        durationSec = durationSeconds,
                        connectionState = connectionState
                    )
                }

                // Quick Mode and Active Server Selector Card
                item {
                    AutoConnectAndSelectedCard(
                        selectedProxy = selectedProxy,
                        autoConnectEnabled = autoConnectEnabled,
                        onAutoConnectToggle = { viewModel.toggleAutoConnect(it) },
                        onSelectServerClick = { showServerListSheet = true }
                    )
                }

                // XRAY Anti-Censorship Whitelist Bypass Block
                item {
                    XrayCensorshipBypassCard()
                }

                // Pinger Diagnostics & Benchmarking Custom Charts
                item {
                    ConnectivityDiagnosticsCard(
                        selectedProxy = selectedProxy,
                        latencies = latenciesHistory,
                        onPingClick = { viewModel.testCurrentServerConnection() }
                    )
                }

                // Automatic Scraper and Filter Launcher Box
                item {
                    ScraperManagementCard(
                        scrapeState = scrapeState,
                        totalCachedNodes = proxyList.size,
                        onRefreshClick = { viewModel.requestScrapePermission() }
                    )
                }

                // Aesthetic bottom signature
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NetBypass Core v3.8 • made by @funzay",
                            color = StarryGrey.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "XRAY TLS Reality • Bypass Whitelist Active",
                            color = StarryGrey.copy(alpha = 0.35f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Permissions / Authorization Modal Dialog
            if (showPermissionDialog) {
                ConsentPermissionDialog(
                    onConfirm = { viewModel.confirmScrape() },
                    onDismiss = { viewModel.dismissPermissionDialog() }
                )
            }

            // Expanded Server Selector Bottom Modal Sheet
            if (showServerListSheet) {
                ServerListModal(
                    proxies = proxyList,
                    selectedProxy = selectedProxy,
                    onSelect = {
                        viewModel.selectProxy(it)
                        showServerListSheet = false
                    },
                    onDismiss = { showServerListSheet = false }
                )
            }
        }
    }
}

@Composable
fun ConnectionDialSection(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    
    // Pulse animation sizes based on connection state
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (connectionState is ConnectionState.Connected) 1.25f else 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (connectionState is ConnectionState.Connected) 1200 else 2000, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Size"
    )

    // Animated rotation angle for Loading or Connecting state
    val rotationVal by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val ringColor = when (connectionState) {
        is ConnectionState.Connected -> CyberCyan
        is ConnectionState.Connecting -> PlasmaBlue
        is ConnectionState.Disconnected -> StarryGrey.copy(alpha = 0.35f)
    }

    val buttonLabel = when (connectionState) {
        is ConnectionState.Connected -> "ПОДКЛЮЧЕНО"
        is ConnectionState.Connecting -> "ЗАПУСК..."
        is ConnectionState.Disconnected -> "ЗАПУСТИТЬ"
    }

    val statusSubtitle = when (connectionState) {
        is ConnectionState.Connected -> "Обход ограничений активен"
        is ConnectionState.Connecting -> "Установка защищенного туннеля"
        is ConnectionState.Disconnected -> "Подключитесь к быстрому серверу"
    }

    val glowBrush = Brush.radialGradient(
        colors = listOf(
            ringColor.copy(alpha = 0.25f),
            Color.Transparent
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            // Glow Canvas Background
            Canvas(modifier = Modifier
                .fillMaxSize()
                .rotate(if (connectionState is ConnectionState.Connecting) rotationVal else 0f)
            ) {
                drawCircle(
                    brush = glowBrush,
                    radius = size.minDimension / 2 * scalePulse
                )
                
                // Ring Border line
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension / 2.3f) * (if (connectionState is ConnectionState.Connecting) 1.0f else scalePulse),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        pathEffect = if (connectionState is ConnectionState.Connecting) {
                            androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f)
                        } else null
                    )
                )
            }

            // Core Dial clickable surface
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = when (connectionState) {
                                is ConnectionState.Connected -> listOf(DeepSpace, NebulaCard)
                                is ConnectionState.Connecting -> listOf(SpaceBlack, DeepSpace)
                                is ConnectionState.Disconnected -> listOf(NebulaCard, DeepSpace)
                            }
                        )
                    )
                    .border(1.dp, ringColor.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onConnectClick)
                    .testTag("connection_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            is ConnectionState.Connected -> Icons.Filled.LockOpen
                            is ConnectionState.Connecting -> Icons.Filled.WifiProtectedSetup
                            is ConnectionState.Disconnected -> Icons.Filled.Lock
                        },
                        contentDescription = "Lock",
                        tint = ringColor,
                        modifier = Modifier
                            .size(36.dp)
                            .rotate(if (connectionState is ConnectionState.Connecting) rotationVal else 0f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = buttonLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connection Details Panel below button
        Text(
            text = statusSubtitle,
            fontWeight = FontWeight.Medium,
            color = NebulaGrey,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TrafficStatsRow(
    kbDownloaded: Double,
    kbUploaded: Double,
    durationSec: Long,
    connectionState: ConnectionState
) {
    val hrs = durationSec / 3600
    val mins = (durationSec % 3600) / 60
    val secs = durationSec % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hrs, mins, secs)

    val isConnected = connectionState is ConnectionState.Connected

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NebulaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.HourglassEmpty,
                    contentDescription = "Timer",
                    tint = PlasmaBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Время", color = StarryGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isConnected) timeFormatted else "00:00:00",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(SpaceBlack)
            )

            // Download Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Download",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Вход.", color = StarryGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (kbDownloaded >= 1024) String.format(Locale.US, "%.2f MB", kbDownloaded / 1024.0)
                           else String.format(Locale.US, "%.1f KB", kbDownloaded),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Divider vertical
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(SpaceBlack)
            )

            // Upload Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Upload",
                    tint = WarpGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Исход.", color = StarryGrey, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (kbUploaded >= 1024) String.format(Locale.US, "%.2f MB", kbUploaded / 1024.0)
                           else String.format(Locale.US, "%.1f KB", kbUploaded),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun AutoConnectAndSelectedCard(
    selectedProxy: ProxyEntity?,
    autoConnectEnabled: Boolean,
    onAutoConnectToggle: (Boolean) -> Unit,
    onSelectServerClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NebulaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title Header with Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = "Fastest",
                        tint = WarningOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Авто-выбор сервера",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Подключение к самому быстрому",
                            color = StarryGrey,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = autoConnectEnabled,
                    onCheckedChange = onAutoConnectToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberCyan,
                        checkedTrackColor = CyberCyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = StarryGrey,
                        uncheckedTrackColor = SpaceBlack
                    ),
                    modifier = Modifier.testTag("auto_connect_switch")
                )
            }

            // Divider line horizontal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SpaceBlack)
            )

            // Current Chosen Server row (Clicking opens server sheet list)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceBlack.copy(alpha = 0.4f))
                    .clickable(onClick = onSelectServerClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val emoji = selectedProxy?.let { getCountryEmoji(it.country) } ?: "🌎"
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = selectedProxy?.let { "${it.ip}:${it.port}" } ?: "Сервер не выбран",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedProxy?.let { "${it.country} • ${it.protocol}" } ?: "Пожалуйста, обновите список",
                            color = StarryGrey,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedProxy != null && selectedProxy.latencyMs > 0) {
                        val latencyColor = when {
                            selectedProxy.latencyMs < 300 -> WarpGreen
                            selectedProxy.latencyMs < 700 -> WarningOrange
                            else -> DangerRed
                        }
                        Text(
                            text = "${selectedProxy.latencyMs} ms",
                            fontWeight = FontWeight.Bold,
                            color = latencyColor,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Expand",
                        tint = StarryGrey,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectivityDiagnosticsCard(
    selectedProxy: ProxyEntity?,
    latencies: List<Long>,
    onPingClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NebulaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = "Speed",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Диагностика пинга",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Анализ отклика выбранного узла",
                            color = StarryGrey,
                            fontSize = 11.sp
                        )
                    }
                }
                
                Button(
                    onClick = onPingClick,
                    enabled = selectedProxy != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DetailLightGreen,
                        contentColor = SpaceBlack,
                        disabledContainerColor = StarryGrey.copy(alpha = 0.2f),
                        disabledContentColor = StarryGrey
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("ping_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = "Flash",
                        modifier = Modifier.size(16.dp).padding(end = 2.dp)
                    )
                    Text("Тест", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Custom Paint Canvas Graph (Historical Latency Tracker)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceBlack.copy(alpha = 0.5f))
                    .border(1.dp, SpaceBlack, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (latencies.isEmpty()) {
                    Text(
                        text = "График утилизации (Нажмите Тест для истории)",
                        color = StarryGrey.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    val maxVal = (latencies.maxOrNull() ?: 1000L).coerceAtLeast(100L).toFloat()
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        
                        // Reference line at middle
                        drawLine(
                            color = StarryGrey.copy(alpha = 0.15f),
                            start = Offset(0f, height / 2),
                            end = Offset(width, height / 2),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Path Builder
                        val path = Path()
                        val stepX = width / (latencies.size.coerceAtLeast(2) - 1)
                        
                        latencies.forEachIndexed { index, lVal ->
                            val scaleY = 1f - (lVal.toFloat() / maxVal).coerceIn(0.05f, 0.9f)
                            val x = index * stepX
                            val y = scaleY * height
                            
                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                            
                            // Draw point circle
                            drawCircle(
                                color = if (lVal < 300) WarpGreen else if (lVal < 700) WarningOrange else DangerRed,
                                radius = 3.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        // Drawing complete wave line
                        drawPath(
                            path = path,
                            color = CyberCyan,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScraperManagementCard(
    scrapeState: ScrapeState,
    totalCachedNodes: Int,
    onRefreshClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NebulaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = "Cloud",
                        tint = PlasmaBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "База серверов",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Авто-парсинг из свободных листов",
                            color = StarryGrey,
                            fontSize = 11.sp
                        )
                    }
                }

                // Show basic status count
                Text(
                    text = "Узлов: $totalCachedNodes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan,
                    modifier = Modifier
                        .background(SpaceBlack.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            AnimatedContent(
                targetState = scrapeState,
                label = "ScrapeReveal"
            ) { currentScrape ->
                when (currentScrape) {
                    is ScrapeState.Idle -> {
                        Button(
                            onClick = onRefreshClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlasmaBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sync_servers_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Search"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Обновить рабочие прокси",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    is ScrapeState.Scraping -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SpaceBlack.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Сканирование интернета...",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${currentScrape.currentIndex} / ${currentScrape.total}",
                                    color = PlasmaBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            val progress = if (currentScrape.total > 0) {
                                currentScrape.currentIndex.toFloat() / currentScrape.total
                            } else 0f
                            
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                color = CyberCyan,
                                trackColor = SpaceBlack
                            )

                            Text(
                                "Найдено живых серверов: ${currentScrape.workingCount}",
                                color = WarpGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    is ScrapeState.Success -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarpGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, WarpGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Done",
                                tint = WarpGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Успешно добавлено рабочих: ${currentScrape.addedCount}",
                                color = WarpGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    is ScrapeState.Error -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DangerRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, DangerRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Error",
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                currentScrape.message,
                                color = DangerRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsentPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSpace),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PlasmaBlue.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shield scanner icon circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(PlasmaBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.YoutubeSearchedFor,
                        contentDescription = "Search",
                        tint = PlasmaBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Поиск Общедоступных Серверов",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Разрешить NetBypass автоматически просканировать открытые Web-ресурсы, загрузить список бесплатных SOCKS/HTTP прокси, проверить их пинг-задержку и удалить неактивные?",
                    fontSize = 13.sp,
                    color = NebulaGrey,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, StarryGrey),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ОТМЕНА", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = SpaceBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("accept_permissions_button")
                    ) {
                        Text("РАЗРЕШИТЬ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ServerListModal(
    proxies: List<ProxyEntity>,
    selectedProxy: ProxyEntity?,
    onSelect: (ProxyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DeepSpace),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header details
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Доступные серверы",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Выберите узел для подключения",
                            color = StarryGrey,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = StarryGrey
                        )
                    }
                }

                // Servers list LazyColumn
                if (proxies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = "No proxies",
                                tint = StarryGrey,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Список пуст.\nНажмите 'Обновить рабочие прокси' на главном экране.",
                                color = StarryGrey,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(proxies) { proxy ->
                            val isSelected = selectedProxy?.id == proxy.id
                            ServerItemRow(
                                proxy = proxy,
                                isSelected = isSelected,
                                onClick = { onSelect(proxy) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServerItemRow(
    proxy: ProxyEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) CyberCyan else SpaceBlack.copy(alpha = 0.5f)
    val backdropColor = if (isSelected) NebulaCard else NebulaCard.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backdropColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("server_item_${proxy.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val emoji = getCountryEmoji(proxy.country)
            Text(
                text = emoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${proxy.ip}:${proxy.port}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${proxy.country} • ${proxy.protocol}",
                    color = StarryGrey,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (proxy.isActive && proxy.latencyMs > 0) {
                val pingColor = when {
                    proxy.latencyMs < 300 -> WarpGreen
                    proxy.latencyMs < 700 -> WarningOrange
                    else -> DangerRed
                }
                Text(
                    text = "${proxy.latencyMs} ms",
                    fontWeight = FontWeight.Bold,
                    color = pingColor,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "Offline",
                    fontWeight = FontWeight.Bold,
                    color = DangerRed,
                    fontSize = 11.sp
                )
            }
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = CyberCyan,
                    unselectedColor = StarryGrey
                ),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Colors local reference
val DetailLightGreen = Color(0xFF00FFCC)

fun getCountryEmoji(countryName: String): String {
    return when (countryName) {
        "Германия" -> "🇩🇪"
        "США" -> "🇺🇸"
        "Нидерланды" -> "🇳🇱"
        "Финляндия" -> "🇫🇮"
        "Япония" -> "🇯🇵"
        "Сингапур" -> "🇸🇬"
        "Великобритания" -> "🇬🇧"
        "Франция" -> "🇫🇷"
        "Швеция" -> "🇸🇪"
        "Польша" -> "🇵🇱"
        "Турция" -> "🇹🇷"
        "Канада" -> "🇨🇦"
        "Кипр" -> "🇨🇾"
        "Исландия" -> "🇮🇸"
        "Швейцария" -> "🇨🇭"
        "Австрия" -> "🇦🇹"
        else -> "🌐"
    }
}

@Composable
fun XrayCensorshipBypassCard() {
    var googleBypass by remember { mutableStateOf(true) }
    var geminiBypass by remember { mutableStateOf(true) }
    var antiDpiOverride by remember { mutableStateOf(true) }
    var selectedDnsIndex by remember { mutableIntStateOf(0) }

    val dnsOptions = listOf("AdGuard DNS (Anti-Ads / Ru Bypass)", "Yandex Safe DNS", "Google Public DNS", "Cloudflare Secure")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NebulaCard),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Bypass Mode",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Параметры обхода в РФ",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Специфические настройки XRAY и обхода блокировок",
                        color = StarryGrey,
                        fontSize = 11.sp
                    )
                }
            }

            // Separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(SpaceBlack.copy(alpha = 0.5f))
            )

            // Switch #1: Gemini API routing through XRAY proxy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Разблокировать API Gemini",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Маршрутизировать трафик ИИ через XRAY-туннель",
                        color = StarryGrey,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = geminiBypass,
                    onCheckedChange = { geminiBypass = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StarryGrey,
                        uncheckedThumbColor = StarryGrey,
                        uncheckedTrackColor = SpaceBlack
                    )
                )
            }

            // Switch #2: Bypass Russian whitelists on local services to save speed bandwidth
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Исключить Российские ресурсы",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Локальные сайты (.RU / .РФ) идут напрямую без прокси",
                        color = StarryGrey,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = googleBypass,
                    onCheckedChange = { googleBypass = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StarryGrey,
                        uncheckedThumbColor = StarryGrey,
                        uncheckedTrackColor = SpaceBlack
                    )
                )
            }

            // Switch #3: Anti-DPI active payload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Анти-DPI Режим XRAY",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Фрагментация пакетов TLS ClientHello для обхода ТСПУ",
                        color = StarryGrey,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = antiDpiOverride,
                    onCheckedChange = { antiDpiOverride = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = StarryGrey,
                        uncheckedThumbColor = StarryGrey,
                        uncheckedTrackColor = SpaceBlack
                    )
                )
            }

            // DNS Selection Dropdown mockup
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Защищенный DNS-сервер",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    text = "Предотвращает спуфинг и утечку запросов",
                    color = StarryGrey,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dnsOptions.forEachIndexed { idx, label ->
                        val isSelected = selectedDnsIndex == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color.White else SpaceBlack.copy(alpha = 0.5f))
                                .border(1.dp, if (isSelected) Color.White else StarryGrey.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { selectedDnsIndex = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.split(" ")[0], // short name (e.g. AdGuard, Yandex)
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
