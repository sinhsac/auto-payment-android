package com.hotroso.autopay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotroso.autopay.data.DashboardStats
import com.hotroso.autopay.data.PaymentRecord
import com.hotroso.autopay.data.SyncStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandNavy = Color(0xFF0F172A)
private val BrandBlue = Color(0xFF1D4ED8)
private val BrandEmerald = Color(0xFF059669)
private val BrandAmber = Color(0xFFD97706)
private val BrandRed = Color(0xFFDC2626)
private val SurfaceBg = Color(0xFFF1F5F9)

private val AutoPayColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = BrandNavy,
    secondary = BrandEmerald,
    onSecondary = Color.White,
    background = SurfaceBg,
    surface = Color.White,
    onSurface = BrandNavy,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

data class AppUiState(
    val records: List<PaymentRecord> = emptyList(),
    val stats: DashboardStats = DashboardStats(),
    val selectedFilter: SyncStatus? = null,
    val searchQuery: String = "",
    val isListenerGranted: Boolean = false,
    val isBatteryExempt: Boolean = false,
    val webhookUrl: String = "",
    val webhookSecret: String = "",
    val orderPrefix: String = "AP",
    val onlyKnownBanks: Boolean = false,
    val listeningEnabled: Boolean = true,
    val isSyncing: Boolean = false
)

enum class AppTab {
    DASHBOARD,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoPaymentApp(
    state: AppUiState,
    snackbarHostState: SnackbarHostState,
    onFilterChange: (SyncStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSyncAllPending: () -> Unit,
    onRetrySingle: (PaymentRecord) -> Unit,
    onDeleteRecord: (PaymentRecord) -> Unit,
    onSaveConfig: (url: String, secret: String, prefix: String, onlyKnownBanks: Boolean, listeningEnabled: Boolean) -> Unit,
    onTestWebhook: (url: String, secret: String, prefix: String) -> Unit,
    onClearSynced: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestAutoStart: () -> Unit,
    onRequestLockScreenNotifications: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var inspectedRecord by remember { mutableStateOf<PaymentRecord?>(null) }

    MaterialTheme(colorScheme = AutoPayColorScheme) {
        Scaffold(
            containerColor = SurfaceBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandNavy,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    title = {
                        Column {
                            Text(
                                text = if (activeTab == AppTab.DASHBOARD) "Auto Payment" else "Cấu hình hệ thống",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Hỗ trợ giải pháp số • Đồng bộ đơn tự động",
                                fontSize = 11.sp,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    },
                    actions = {
                        if (activeTab == AppTab.DASHBOARD) {
                            IconButton(onClick = onSyncAllPending) {
                                Icon(Icons.Default.Refresh, contentDescription = "Đồng bộ ngay")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                    NavigationBarItem(
                        selected = activeTab == AppTab.DASHBOARD,
                        onClick = { activeTab = AppTab.DASHBOARD },
                        icon = { Icon(Icons.Default.List, contentDescription = "Danh sách đơn") },
                        label = { Text("Danh sách đơn") }
                    )
                    NavigationBarItem(
                        selected = activeTab == AppTab.SETTINGS,
                        onClick = { activeTab = AppTab.SETTINGS },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Cấu hình") },
                        label = { Text("Cấu hình") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    AppTab.DASHBOARD -> DashboardScreen(
                        state = state,
                        onFilterChange = onFilterChange,
                        onSearchChange = onSearchChange,
                        onSyncAllPending = onSyncAllPending,
                        onRefresh = onRefresh,
                        onOpenSettings = { activeTab = AppTab.SETTINGS },
                        onInspectRecord = { inspectedRecord = it },
                        onRetrySingle = onRetrySingle
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        state = state,
                        onSaveConfig = onSaveConfig,
                        onTestWebhook = onTestWebhook,
                        onSyncAllPending = onSyncAllPending,
                        onClearSynced = onClearSynced,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onRequestBatteryExemption = onRequestBatteryExemption,
                        onRequestAutoStart = onRequestAutoStart,
                        onRequestLockScreenNotifications = onRequestLockScreenNotifications
                    )
                }
            }
        }

        inspectedRecord?.let { record ->
            OrderDetailDialog(
                record = record,
                onDismiss = { inspectedRecord = null },
                onRetry = {
                    onRetrySingle(record)
                    inspectedRecord = null
                },
                onDelete = {
                    onDeleteRecord(record)
                    inspectedRecord = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardScreen(
    state: AppUiState,
    onFilterChange: (SyncStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onSyncAllPending: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onInspectRecord: (PaymentRecord) -> Unit,
    onRetrySingle: (PaymentRecord) -> Unit
) {
    val unsyncedCount = state.stats.pendingCount + state.stats.failedCount

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Status & Quick Sync Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(BrandNavy, BrandBlue))
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.isListenerGranted && state.listeningEnabled) Color(0xFF34D399)
                                        else Color(0xFFF87171)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    !state.isListenerGranted -> "Chưa cấp quyền đọc thông báo"
                                    !state.listeningEnabled -> "Đang tạm dừng lắng nghe"
                                    else -> "Đang lắng nghe mã ${state.orderPrefix}xxxxxx"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        if (!state.isListenerGranted) {
                            TextButton(
                                onClick = onOpenSettings,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFDE047))
                            ) {
                                Text("Cấp quyền →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KpiMini("Tổng tiền đã bắt", "${formatVnd(state.stats.totalAmountVnd)} ₫")
                        KpiMini("Đã đồng bộ", "${state.stats.syncedCount}/${state.stats.totalCount}")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onSyncAllPending,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (unsyncedCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (unsyncedCount > 0) "Đồng bộ $unsyncedCount đơn chờ" else "Đồng bộ lại",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Làm mới", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Search input
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                placeholder = { Text("Tìm mã đơn (AP...), ngân hàng, nội dung...", fontSize = 13.sp) }
            )
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text("Tất cả (${state.stats.totalCount})") }
                )
                FilterChip(
                    selected = state.selectedFilter == SyncStatus.SYNCED,
                    onClick = { onFilterChange(SyncStatus.SYNCED) },
                    label = { Text("Đã đồng bộ (${state.stats.syncedCount})") }
                )
                FilterChip(
                    selected = state.selectedFilter == SyncStatus.PENDING,
                    onClick = { onFilterChange(SyncStatus.PENDING) },
                    label = { Text("Chờ gửi (${state.stats.pendingCount})") }
                )
                FilterChip(
                    selected = state.selectedFilter == SyncStatus.FAILED,
                    onClick = { onFilterChange(SyncStatus.FAILED) },
                    label = { Text("Lỗi (${state.stats.failedCount})") }
                )
            }
        }

        // Empty state or order list
        if (state.records.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Chưa có giao dịch đơn hàng nào",
                            fontWeight = FontWeight.SemiBold,
                            color = BrandNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Khi có thông báo ngân hàng chứa mã ${state.orderPrefix}xxxxxx, đơn sẽ tự lưu vào SQLite và đồng bộ lên server ngay.",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(state.records, key = { it.id }) { record ->
                PaymentOrderCard(
                    record = record,
                    onClick = { onInspectRecord(record) },
                    onRetry = { onRetrySingle(record) }
                )
            }
        }

        // Subtle Brand Footer
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Auto Payment v1.1.0 • Phát triển bởi Hỗ trợ giải pháp số",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun KpiMini(label: String, value: String) {
    Column {
        Text(text = label, color = Color(0xFFBFDBFE), fontSize = 11.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun PaymentOrderCard(
    record: PaymentRecord,
    onClick: () -> Unit,
    onRetry: () -> Unit
) {
    val (badgeBg, badgeFg, badgeText) = when (record.syncStatus) {
        SyncStatus.SYNCED -> Triple(Color(0xFFD1FAE5), BrandEmerald, "ĐÃ ĐỒNG BỘ")
        SyncStatus.PENDING -> Triple(Color(0xFFFEF3C7), BrandAmber, "CHỜ ĐỒNG BỘ")
        SyncStatus.FAILED -> Triple(Color(0xFFFEE2E2), BrandRed, "LỖI GỬI (${record.retryCount})")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "#${record.orderCode}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandNavy
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "+${formatVnd(record.amount)} ₫",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = BrandEmerald
                )
                Text(
                    text = "${record.bankName} • ${formatTime(record.postTimeMs)}",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = record.rawMessage,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!record.serverOutcome.isNullOrBlank() && record.syncStatus == SyncStatus.SYNCED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Server: ${record.serverOutcome} (HTTP ${record.httpCode ?: 200})",
                    fontSize = 11.sp,
                    color = BrandEmerald
                )
            }

            if (record.syncStatus != SyncStatus.SYNCED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = record.lastError ?: "Đang đợi kết nối server...",
                        fontSize = 11.sp,
                        color = BrandRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gửi lại", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    state: AppUiState,
    onSaveConfig: (url: String, secret: String, prefix: String, onlyKnownBanks: Boolean, listeningEnabled: Boolean) -> Unit,
    onTestWebhook: (url: String, secret: String, prefix: String) -> Unit,
    onSyncAllPending: () -> Unit,
    onClearSynced: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onRequestAutoStart: () -> Unit,
    onRequestLockScreenNotifications: () -> Unit
) {
    var urlInput by remember(state.webhookUrl) { mutableStateOf(state.webhookUrl) }
    var secretInput by remember(state.webhookSecret) { mutableStateOf(state.webhookSecret) }
    var prefixInput by remember(state.orderPrefix) { mutableStateOf(state.orderPrefix) }
    var onlyKnownBanks by remember(state.onlyKnownBanks) { mutableStateOf(state.onlyKnownBanks) }
    var listeningEnabled by remember(state.listeningEnabled) { mutableStateOf(state.listeningEnabled) }
    var showSecret by remember { mutableStateOf(false) }
    var confirmClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Server Webhook Configuration Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Cấu hình Server Webhook", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động lắng nghe & gửi đơn", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("Bật để app tự lưu SQLite và gọi webhook", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = listeningEnabled, onCheckedChange = { listeningEnabled = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chỉ đọc app ngân hàng VN", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("VCB, TCB, MB, VPB, BIDV, ACB, TPB, MoMo...", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(checked = onlyKnownBanks, onCheckedChange = { onlyKnownBanks = it })
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Webhook URL") },
                    placeholder = { Text("https://domain/api/webhooks/bank-notify") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                OutlinedTextField(
                    value = secretInput,
                    onValueChange = { secretInput = it },
                    label = { Text("Webhook Secret (X-Webhook-Secret)") },
                    placeholder = { Text("Khớp NUXT_WEBHOOK_SECRET (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showSecret = !showSecret }) {
                            Icon(Icons.Default.Lock, contentDescription = "Ẩn/hiện mật khẩu")
                        }
                    }
                )

                OutlinedTextField(
                    value = prefixInput,
                    onValueChange = { prefixInput = it.uppercase(Locale.ROOT) },
                    label = { Text("Tiền tố mã đơn (Prefix)") },
                    placeholder = { Text("AP") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveConfig(urlInput, secretInput, prefixInput, onlyKnownBanks, listeningEnabled)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lưu cấu hình", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            onSaveConfig(urlInput, secretInput, prefixInput, onlyKnownBanks, listeningEnabled)
                            onTestWebhook(urlInput, secretInput, prefixInput)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi thử")
                    }
                }
            }
        }

        // 2. Permissions & Keep-Alive Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quyền hệ thống & Chạy nền ổn định", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)

                PermissionActionRow(
                    title = "Quyền đọc thông báo (Bắt buộc)",
                    subtitle = if (state.isListenerGranted) "Đã cấp quyền ✓" else "Chưa cấp — nhấn để mở cài đặt",
                    granted = state.isListenerGranted,
                    onClick = onRequestNotificationPermission
                )
                Divider(color = Color(0xFFE2E8F0))
                PermissionActionRow(
                    title = "Tắt tối ưu pin (Chống tắt app nền)",
                    subtitle = if (state.isBatteryExempt) "Đã miễn trừ tối ưu pin ✓" else "Khuyên bật để không bị Android đóng app",
                    granted = state.isBatteryExempt,
                    onClick = onRequestBatteryExemption
                )
                Divider(color = Color(0xFFE2E8F0))
                PermissionActionRow(
                    title = "Quyền Tự khởi động (Xiaomi/Oppo/Vivo/Samsung)",
                    subtitle = "Cho phép tự chạy lại sau khi khởi động máy",
                    granted = null,
                    onClick = onRequestAutoStart
                )
                Divider(color = Color(0xFFE2E8F0))
                PermissionActionRow(
                    title = "Quyền thông báo Android 13+",
                    subtitle = "Hiển thị trạng thái dịch vụ chạy nền",
                    granted = null,
                    onClick = onRequestLockScreenNotifications
                )
            }
        }

        // 3. Database & Sync Maintenance Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Cơ sở dữ liệu & Đồng bộ (SQLite)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandNavy)
                Text(
                    text = "Mọi đơn khớp mã đều được lưu vào SQLite cục bộ trước khi gửi. Nếu server bảo trì hoặc mất mạng, app tự đồng bộ lại mỗi 15 phút hoặc khi bạn bấm nút dưới đây.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onSyncAllPending,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đồng bộ chờ", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { confirmClearDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dọn đơn đã gửi", fontSize = 13.sp)
                    }
                }
            }
        }

        // 4. Brand Card — Hỗ trợ giải pháp số
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hỗ trợ giải pháp số",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Auto Payment Android v1.1.0 — Giải pháp xác nhận chuyển khoản ngân hàng tự động không qua cổng trung gian, phát triển bởi Hỗ trợ giải pháp số.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mã nguồn mở (MIT) • github.com/sinhsac/auto-payment",
                    color = Color(0xFF93C5FD),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            title = { Text("Xóa lịch sử đã đồng bộ?") },
            text = { Text("Chỉ xóa các đơn đã đồng bộ thành công (SYNCED). Các đơn đang chờ hoặc bị lỗi vẫn được giữ nguyên.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearDialog = false
                    onClearSynced()
                }) {
                    Text("Xóa", color = BrandRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    subtitle: String,
    granted: Boolean?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (granted) {
                true -> Icons.Default.CheckCircle
                false -> Icons.Default.Warning
                null -> Icons.Default.Build
            },
            contentDescription = null,
            tint = when (granted) {
                true -> BrandEmerald
                false -> BrandRed
                null -> BrandBlue
            },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = BrandNavy)
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun OrderDetailDialog(
    record: PaymentRecord,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chi tiết đơn #${record.orderCode}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Số tiền: +${formatVnd(record.amount)} ₫", fontWeight = FontWeight.Bold, color = BrandEmerald)
                Text("Ngân hàng: ${record.bankName} (${record.packageName})", fontSize = 13.sp)
                Text("Thời gian: ${record.strPostTime}", fontSize = 13.sp)
                Text("Trạng thái: ${record.syncStatus.name}", fontSize = 13.sp)
                if (!record.serverOutcome.isNullOrBlank()) {
                    Text("Phản hồi server: ${record.serverOutcome} (HTTP ${record.httpCode ?: "-"})", fontSize = 13.sp)
                }
                if (!record.lastError.isNullOrBlank()) {
                    Text("Lỗi gần nhất: ${record.lastError}", fontSize = 12.sp, color = BrandRed)
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Nội dung thông báo gốc:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Text(record.rawMessage, fontSize = 12.sp, color = Color(0xFF334155))
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (record.syncStatus != SyncStatus.SYNCED) {
                    TextButton(onClick = onRetry) {
                        Text("Gửi lại ngay", color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Đóng")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Xóa", color = BrandRed)
            }
        }
    )
}

private fun formatVnd(amount: Long): String {
    return NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount)
}

private fun formatTime(epochMs: Long): String {
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(epochMs))
}
