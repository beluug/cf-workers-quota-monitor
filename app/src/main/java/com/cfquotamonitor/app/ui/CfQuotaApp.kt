package com.cfquotamonitor.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfquotamonitor.app.model.AccountUiState
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.ui.theme.CfOrange
import com.cfquotamonitor.app.ui.theme.DangerRed
import com.cfquotamonitor.app.ui.theme.SafeGreen
import com.cfquotamonitor.app.ui.theme.WarningAmber
import com.cfquotamonitor.app.util.formatCount
import com.cfquotamonitor.app.util.formatFetchedTime
import com.cfquotamonitor.app.util.resetCountdown
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CfQuotaApp(viewModel: MainViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CfAccount?>(null) }
    var deletingAccount by remember { mutableStateOf<CfAccount?>(null) }

    if (showHelp) {
        HelpScreen(onBack = { showHelp = false })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingAccount = null
                    showAccountDialog = true
                },
                containerColor = CfOrange,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding(),
            ) {
                Text("＋", fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AppHeader(
                    refreshing = refreshing,
                    onRefresh = viewModel::refreshAll,
                    onHelp = { showHelp = true },
                )
            }
            item { SummaryCard(accounts) }
            if (accounts.isEmpty()) {
                item {
                    EmptyState(
                        onAdd = {
                            editingAccount = null
                            showAccountDialog = true
                        },
                        onHelp = { showHelp = true },
                    )
                }
            } else {
                item {
                    Text(
                        text = "账号用量",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(accounts, key = { it.account.localId }) { state ->
                    AccountCard(
                        state = state,
                        onEdit = {
                            editingAccount = state.account
                            showAccountDialog = true
                        },
                        onDelete = { deletingAccount = state.account },
                    )
                }
            }
            item { PrivacyNote() }
        }
    }

    if (showAccountDialog) {
        AccountDialog(
            existing = editingAccount,
            onDismiss = { showAccountDialog = false },
            onOpenHelp = {
                showAccountDialog = false
                showHelp = true
            },
            onSave = { name, accountId, limit, token ->
                val error = viewModel.saveAccount(editingAccount, name, accountId, limit, token)
                if (error == null) showAccountDialog = false
                error
            },
        )
    }

    deletingAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            title = { Text("删除账号？") },
            text = { Text("将从本机删除“${account.name}”及其加密 Token，此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(account.localId)
                        deletingAccount = null
                    }
                ) { Text("删除", color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun AppHeader(refreshing: Boolean, onRefresh: () -> Unit, onHelp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "CF额度监控",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                "Workers 免费额度 · 近实时估算",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RoundAction(label = "?", description = "配置教程", onClick = onHelp)
        Spacer(Modifier.width(8.dp))
        RoundAction(
            label = if (refreshing) "…" else "↻",
            description = "刷新",
            enabled = !refreshing,
            onClick = onRefresh,
        )
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .shadow(8.dp, RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFFF9B4A), CfOrange)),
                RoundedCornerShape(15.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            drawCircle(Color.White, radius = size.minDimension * .19f, center = Offset(size.width * .37f, size.height * .55f))
            drawCircle(Color.White, radius = size.minDimension * .25f, center = Offset(size.width * .56f, size.height * .44f))
            drawCircle(Color.White, radius = size.minDimension * .17f, center = Offset(size.width * .75f, size.height * .57f))
            drawRect(Color.White, topLeft = Offset(size.width * .28f, size.height * .55f), size = Size(size.width * .55f, size.height * .19f))
            drawLine(
                color = CfOrange,
                start = Offset(size.width * .52f, size.height * .65f),
                end = Offset(size.width * .72f, size.height * .49f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun RoundAction(
    label: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun SummaryCard(accounts: List<AccountUiState>) {
    var countdown by remember { mutableStateOf(resetCountdown()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            countdown = resetCountdown()
        }
    }
    val riskCount = accounts.count { it.usage != null && it.progress >= .85f }
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF242A35), Color(0xFF15181F)),
                        start = Offset.Zero,
                        end = Offset(900f, 600f),
                    )
                )
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("今日监控", color = Color(0xFFBEC4CF), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (accounts.isEmpty()) "等待添加账号" else "${accounts.size} 个 Cloudflare 账号",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Surface(
                    color = if (riskCount > 0) DangerRed.copy(alpha = .2f) else SafeGreen.copy(alpha = .2f),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        if (riskCount > 0) "$riskCount 个需注意" else "状态正常",
                        color = if (riskCount > 0) Color(0xFFFF8B8B) else Color(0xFF67D7AE),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .1f))
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("UTC 00:00", color = Color(0xFFFFAC6B), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(9.dp))
                Text("$countdown · 香港时间约 08:00", color = Color(0xFFBEC4CF), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun AccountCard(state: AccountUiState, onEdit: () -> Unit, onDelete: () -> Unit) {
    val percent = if (state.usage == null) 0 else
        (state.usage.used * 100.0 / state.account.dailyLimit).roundToInt().coerceAtLeast(0)
    val accent = when {
        state.progress >= .95f -> DangerRed
        state.progress >= .85f -> WarningAmber
        else -> SafeGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = .13f), shape = RoundedCornerShape(13.dp)) {
                    Text(
                        state.account.name.take(1).uppercase(),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.account.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "ID ·••••${state.account.accountId.takeLast(6)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(25.dp), strokeWidth = 2.5.dp, color = CfOrange)
                } else {
                    Text("$percent%", color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = accent.copy(alpha = .13f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(15.dp))

            Row {
                Metric(
                    title = "今日已用",
                    value = state.usage?.let { formatCount(it.used) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    title = "估算剩余",
                    value = state.usage?.let { formatCount(state.remaining) } ?: "—",
                    valueColor = accent,
                    modifier = Modifier.weight(1f),
                )
                Metric(
                    title = "每日额度",
                    value = formatCount(state.account.dailyLimit),
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedVisibility(visible = state.error != null) {
                Text(
                    text = state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = .08f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                )
            }
            Spacer(Modifier.height(11.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.usage?.let { "最后更新 ${formatFetchedTime(it.fetchedAtEpochMillis)} · 数据可能延迟数分钟" }
                        ?: "尚未获得用量数据",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun Metric(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, onHelp: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("☁", fontSize = 54.sp)
            Text("添加第一个 Cloudflare 账号", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "只需 Account ID 和只读 API Token。凭证只会加密保存在这台手机中。",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CfOrange)) {
                Text("添加账号")
            }
            TextButton(onClick = onHelp) { Text("先看配置教程") }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("🔒", fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "本应用不在后台运行，不含广告与统计 SDK。Token 使用 Android Keystore 加密，仅直接发送给 Cloudflare 官方 API。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountDialog(
    existing: CfAccount?,
    onDismiss: () -> Unit,
    onOpenHelp: () -> Unit,
    onSave: (String, String, String, String) -> String?,
) {
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var accountId by rememberSaveable { mutableStateOf(existing?.accountId.orEmpty()) }
    var limit by rememberSaveable { mutableStateOf((existing?.dailyLimit ?: 100_000L).toString()) }
    var token by rememberSaveable { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "添加账号" else "编辑账号") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "请使用仅含 Account Analytics Read 权限的 Token。不要填写 Global API Key。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账号备注") },
                    placeholder = { Text("例如：主账号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = accountId,
                    onValueChange = { accountId = it.trim() },
                    label = { Text("Account ID") },
                    supportingText = { Text("32 位，可在 Cloudflare 控制台找到") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text(if (existing == null) "API Token" else "新 API Token（可留空）") },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showToken = !showToken }) {
                            Text(if (showToken) "隐藏" else "显示")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = limit,
                    onValueChange = { limit = it.filter(Char::isDigit) },
                    label = { Text("每日额度") },
                    supportingText = { Text("Workers Free 默认 100000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onOpenHelp, modifier = Modifier.align(Alignment.Start)) {
                    Text("不知道去哪里找？查看完整教程")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { error = onSave(name, accountId, limit, token) },
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
            ) { Text("保存并查询") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun LockScreen(message: String?, onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF101218), Color(0xFF252A35)),
                    start = Offset.Zero,
                    end = Offset(1000f, 1600f),
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Surface(color = CfOrange, shape = RoundedCornerShape(25.dp)) {
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                    Text("☁", fontSize = 50.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(25.dp))
            Text("CF额度监控", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("账号凭证已锁定", color = Color(0xFFBFC4CE))
            message?.let {
                Spacer(Modifier.height(18.dp))
                Text(it, color = Color(0xFFFF9292), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(26.dp))
            Button(
                onClick = onUnlock,
                colors = ButtonDefaults.buttonColors(containerColor = CfOrange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("验证身份并进入", modifier = Modifier.padding(vertical = 5.dp)) }
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text("‹ 返回", fontSize = 17.sp) }
                Text("零基础配置教程", style = MaterialTheme.typography.titleLarge)
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxHeight(),
            ) {
                item {
                    HelpIntro()
                }
                item {
                    HelpStep(
                        number = "1",
                        title = "登录 Cloudflare",
                        body = "用浏览器打开 dash.cloudflare.com 并登录。选择需要监控的账号。不要把密码告诉本应用或任何人。",
                    )
                }
                item {
                    HelpStep(
                        number = "2",
                        title = "复制 Account ID",
                        body = "进入 Workers & Pages 页面。在页面右侧或账号概览中找到 Account ID，它通常是 32 位字母和数字。复制后粘贴到本应用。",
                    )
                }
                item {
                    HelpStep(
                        number = "3",
                        title = "创建只读 API Token",
                        body = "打开个人资料 → API Tokens → Create Token → Create Custom Token。权限选择 Account → Account Analytics → Read；账号资源只选择需要监控的账号。其余权限不要添加。",
                    )
                }
                item {
                    HelpStep(
                        number = "4",
                        title = "复制并保存 Token",
                        body = "Cloudflare 只会完整显示 Token 一次。复制后立即回到本应用添加账号。不要使用 Global API Key，也不要把 Token 提交到 GitHub。",
                    )
                }
                item {
                    HelpStep(
                        number = "5",
                        title = "查看结果",
                        body = "保存后应用会立即查询今天 UTC 00:00 至当前时间的 Workers 请求数。免费额度默认每天 100,000 次，香港时间约上午 08:00 重置。",
                    )
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Column(Modifier.padding(17.dp)) {
                            Text("为什么写着“估算”？", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Cloudflare Analytics 可能延迟几分钟，并且不是官方计费计数器。因此临近 100% 时请预留余量。应用只在打开或手动点击刷新时联网，退出后不会后台运行。",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }
}

@Composable
private fun HelpIntro() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            Modifier
                .background(Brush.linearGradient(listOf(Color(0xFFFFA45B), CfOrange)))
                .padding(21.dp)
        ) {
            Text("大约 3 分钟即可完成", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(5.dp))
            Text("每个 Cloudflare 账号分别创建一个最小权限 Token，安全且容易撤销。", color = Color.White.copy(alpha = .9f))
        }
    }
}

@Composable
private fun HelpStep(number: String, title: String, body: String) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Surface(color = CfOrange, shape = CircleShape) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                    Text(number, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(5.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
