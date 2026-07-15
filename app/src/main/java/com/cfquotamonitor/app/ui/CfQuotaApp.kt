package com.cfquotamonitor.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cfquotamonitor.app.R
import com.cfquotamonitor.app.model.AccountUiState
import com.cfquotamonitor.app.model.CfAccount
import com.cfquotamonitor.app.settings.AppSettings
import com.cfquotamonitor.app.settings.supportedLanguages
import com.cfquotamonitor.app.ui.theme.CfOrange
import com.cfquotamonitor.app.ui.theme.DangerRed
import com.cfquotamonitor.app.ui.theme.SafeGreen
import com.cfquotamonitor.app.ui.theme.WarningAmber
import com.cfquotamonitor.app.util.formatCount
import com.cfquotamonitor.app.util.formatFetchedTime
import com.cfquotamonitor.app.util.resetCountdownParts
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val refreshIntervals = listOf(15L, 30L, 60L, 180L, 360L, 720L, 1440L)

@Composable
fun CfQuotaApp(viewModel: MainViewModel, onLanguageChanged: () -> Unit) {
    val accounts by viewModel.accounts.collectAsState()
    val refreshing by viewModel.isRefreshing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<CfAccount?>(null) }
    var deletingAccount by remember { mutableStateOf<CfAccount?>(null) }

    when {
        showHelp -> {
            HelpScreen(onBack = { showHelp = false })
            return
        }
        showSettings -> {
            SettingsScreen(
                settings = settings,
                onBack = { showSettings = false },
                onLockChanged = viewModel::setLockEnabled,
                onLanguageChanged = { tag ->
                    viewModel.setLanguage(tag)
                    onLanguageChanged()
                },
                onBackgroundChanged = viewModel::setBackgroundRefresh,
                onIntervalChanged = viewModel::setRefreshInterval,
            )
            return
        }
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
            ) { Text("＋", fontSize = 28.sp, fontWeight = FontWeight.Light) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                AppHeader(
                    refreshing = refreshing,
                    onRefresh = viewModel::refreshAll,
                    onHelp = { showHelp = true },
                    onSettings = { showSettings = true },
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
                item { Text(stringResource(R.string.section_accounts), style = MaterialTheme.typography.titleLarge) }
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
            item { PrivacyNote(settings.backgroundRefreshEnabled) }
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
                viewModel.saveAccount(editingAccount, name, accountId, limit, token)
                    .also { if (it == null) showAccountDialog = false }
            },
        )
    }

    deletingAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, account.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAccount(account.localId)
                    deletingAccount = null
                }) { Text(stringResource(R.string.action_delete), color = DangerRed) }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun AppHeader(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        RoundAction("?", stringResource(R.string.action_help), onClick = onHelp)
        Spacer(Modifier.width(6.dp))
        RoundAction("⚙", stringResource(R.string.action_settings), onClick = onSettings)
        Spacer(Modifier.width(6.dp))
        RoundAction(
            if (refreshing) "…" else "↻",
            stringResource(R.string.action_refresh),
            enabled = !refreshing,
            onClick = onRefresh,
        )
    }
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier.size(44.dp).shadow(8.dp, RoundedCornerShape(14.dp)).background(
            Brush.linearGradient(listOf(Color(0xFFFF9B4A), CfOrange)), RoundedCornerShape(14.dp)
        ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(27.dp)) {
            drawCircle(Color.White, size.minDimension * .19f, Offset(size.width * .37f, size.height * .56f))
            drawCircle(Color.White, size.minDimension * .25f, Offset(size.width * .57f, size.height * .46f))
            drawCircle(Color.White, size.minDimension * .17f, Offset(size.width * .72f, size.height * .59f))
        }
    }
}

@Composable
private fun RoundAction(label: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(38.dp).semantics { contentDescription = description }
            .clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 18.sp, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
        }
    }
}

@Composable
private fun SummaryCard(accounts: List<AccountUiState>) {
    var countdown by remember { mutableStateOf(resetCountdownParts()) }
    LaunchedEffect(Unit) {
        while (true) {
            countdown = resetCountdownParts()
            delay(60_000)
        }
    }
    val used = accounts.sumOf { it.usage?.used ?: 0L }
    val limit = accounts.sumOf { it.account.dailyLimit }
    val remaining = (limit - used).coerceAtLeast(0L)
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(Color(0xFF1B1F2A), Color(0xFF303746)))).padding(22.dp)
        ) {
            Text(stringResource(R.string.daily_overview), color = Color.White.copy(.72f))
            Spacer(Modifier.height(7.dp))
            Text(
                stringResource(R.string.summary_usage_format, formatCount(used), formatCount(limit)),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(18.dp))
            Row {
                SummaryMetric(stringResource(R.string.total_remaining), formatCount(remaining), Modifier.weight(1f))
                SummaryMetric(stringResource(R.string.account_count), accounts.size.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(15.dp))
            Text(
                stringResource(R.string.reset_in, countdown.first, countdown.second),
                color = Color.White.copy(.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SummaryMetric(title: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(title, color = Color.White.copy(.63f), style = MaterialTheme.typography.bodySmall)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    }
}

@Composable
private fun AccountCard(state: AccountUiState, onEdit: () -> Unit, onDelete: () -> Unit) {
    val percent = (state.progress * 100).roundToInt()
    val accent = when {
        state.progress >= .9f -> DangerRed
        state.progress >= .7f -> WarningAmber
        else -> SafeGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.account.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        state.account.accountId.take(8) + "…" + state.account.accountId.takeLast(5),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isLoading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.percentage_used, percent), color = accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                color = accent,
                trackColor = accent.copy(alpha = .13f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(15.dp))
            Row {
                Metric(stringResource(R.string.metric_used_today), state.usage?.let { formatCount(it.used) } ?: "—", Modifier.weight(1f))
                Metric(stringResource(R.string.metric_remaining), state.usage?.let { formatCount(state.remaining) } ?: "—", Modifier.weight(1f), accent)
                Metric(stringResource(R.string.metric_daily_limit), formatCount(state.account.dailyLimit), Modifier.weight(1f))
            }
            AnimatedVisibility(state.error != null) {
                Text(
                    state.error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                        .background(MaterialTheme.colorScheme.error.copy(.08f), RoundedCornerShape(12.dp)).padding(12.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.usage?.let { stringResource(R.string.last_updated, formatFetchedTime(it.fetchedAtEpochMillis)) }
                        ?: stringResource(R.string.no_usage),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onEdit) { Text(stringResource(R.string.action_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = DangerRed) }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit, onHelp: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("☁", fontSize = 54.sp)
            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.empty_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CfOrange)) {
                Text(stringResource(R.string.action_add_account))
            }
            TextButton(onClick = onHelp) { Text(stringResource(R.string.action_view_guide)) }
        }
    }
}

@Composable
private fun PrivacyNote(backgroundEnabled: Boolean) {
    Row(Modifier.fillMaxWidth().padding(4.dp, 8.dp), verticalAlignment = Alignment.Top) {
        Text("🔒", fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(if (backgroundEnabled) R.string.privacy_note_background else R.string.privacy_note_manual),
            style = MaterialTheme.typography.bodySmall,
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
        title = { Text(stringResource(if (existing == null) R.string.add_account_title else R.string.edit_account_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.token_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.account_name_label)) },
                    placeholder = { Text(stringResource(R.string.account_name_placeholder)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = accountId, onValueChange = { accountId = it.trim() },
                    label = { Text("Account ID") },
                    supportingText = { Text(stringResource(R.string.account_id_support)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it.trim() },
                    label = { Text(stringResource(if (existing == null) R.string.api_token_label else R.string.api_token_replace_label)) },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showToken = !showToken }) {
                            Text(stringResource(if (showToken) R.string.action_hide else R.string.action_show))
                        }
                    },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = limit, onValueChange = { limit = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.daily_limit_label)) },
                    supportingText = { Text(stringResource(R.string.daily_limit_support)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onOpenHelp) { Text(stringResource(R.string.guide_prompt)) }
            }
        },
        confirmButton = {
            Button(onClick = { error = onSave(name, accountId, limit, token) }, colors = ButtonDefaults.buttonColors(containerColor = CfOrange)) {
                Text(stringResource(R.string.action_save_query))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun LockScreen(message: String?, onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(listOf(Color(0xFF101218), Color(0xFF252A35)), Offset.Zero, Offset(1000f, 1600f))
        ).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Surface(color = CfOrange, shape = RoundedCornerShape(25.dp)) {
                Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) { Text("☁", fontSize = 50.sp, color = Color.White) }
            }
            Spacer(Modifier.height(25.dp))
            Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.lock_credentials_locked), color = Color(0xFFBFC4CE))
            message?.let {
                Spacer(Modifier.height(18.dp))
                Text(it, color = Color(0xFFFF9292), textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(26.dp))
            Button(onClick = onUnlock, colors = ButtonDefaults.buttonColors(containerColor = CfOrange), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_unlock), modifier = Modifier.padding(vertical = 5.dp))
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onLockChanged: (Boolean) -> Unit,
    onLanguageChanged: (String) -> Unit,
    onBackgroundChanged: (Boolean) -> Unit,
    onIntervalChanged: (Long) -> Unit,
) {
    BackHandler(onBack = onBack)
    var languageDialog by remember { mutableStateOf(false) }
    var frequencyDialog by remember { mutableStateOf(false) }
    val currentLanguage = supportedLanguages.firstOrNull { it.tag == settings.languageTag } ?: supportedLanguages.first()
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader(stringResource(R.string.settings_title), onBack)
            SettingsCard(stringResource(R.string.settings_security)) {
                SettingToggle(
                    stringResource(R.string.settings_lock_title),
                    stringResource(R.string.settings_lock_desc),
                    settings.lockEnabled,
                    onLockChanged,
                )
            }
            SettingsCard(stringResource(R.string.settings_language)) {
                SettingChoice(
                    stringResource(R.string.settings_language_title),
                    stringResource(currentLanguage.labelRes),
                    stringResource(R.string.settings_language_desc),
                ) { languageDialog = true }
            }
            SettingsCard(stringResource(R.string.settings_background)) {
                SettingToggle(
                    stringResource(R.string.settings_background_title),
                    stringResource(R.string.settings_background_desc),
                    settings.backgroundRefreshEnabled,
                    onBackgroundChanged,
                )
                Spacer(Modifier.height(12.dp))
                SettingChoice(
                    stringResource(R.string.settings_frequency),
                    intervalLabel(settings.refreshIntervalMinutes),
                    stringResource(R.string.settings_frequency_desc),
                    enabled = settings.backgroundRefreshEnabled,
                ) { frequencyDialog = true }
            }
            Text(
                stringResource(R.string.settings_schedule_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(6.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (languageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.language_dialog_title),
            values = supportedLanguages.map { it.tag to stringResource(it.labelRes) },
            selected = settings.languageTag,
            onDismiss = { languageDialog = false },
            onSelected = {
                languageDialog = false
                onLanguageChanged(it)
            },
        )
    }
    if (frequencyDialog) {
        ChoiceDialog(
            title = stringResource(R.string.frequency_dialog_title),
            values = refreshIntervals.map { it.toString() to intervalLabel(it) },
            selected = settings.refreshIntervalMinutes.toString(),
            onDismiss = { frequencyDialog = false },
            onSelected = {
                frequencyDialog = false
                onIntervalChanged(it.toLong())
            },
        )
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back), fontSize = 17.sp) }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, color = CfOrange, modifier = Modifier.padding(start = 5.dp))
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), content = content)
    }
}

@Composable
private fun SettingToggle(title: String, description: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun SettingChoice(title: String, value: String, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = if (enabled) MaterialTheme.colorScheme.onSurface else Color.Gray)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, color = if (enabled) CfOrange else Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(5.dp))
        Text("›", fontSize = 25.sp, color = Color.Gray)
    }
}

@Composable
private fun ChoiceDialog(
    title: String,
    values: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                values.forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelected(key) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = key == selected, onClick = { onSelected(key) })
                        Spacer(Modifier.width(6.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun intervalLabel(minutes: Long): String = when (minutes) {
    15L -> stringResource(R.string.frequency_15_minutes)
    30L -> stringResource(R.string.frequency_30_minutes)
    60L -> stringResource(R.string.frequency_1_hour)
    180L -> stringResource(R.string.frequency_3_hours)
    360L -> stringResource(R.string.frequency_6_hours)
    720L -> stringResource(R.string.frequency_12_hours)
    else -> stringResource(R.string.frequency_24_hours)
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).statusBarsPadding()) {
            ScreenHeader(stringResource(R.string.help_title), onBack)
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item { HelpIntro() }
                item { HelpStep("1", stringResource(R.string.help_step1_title), stringResource(R.string.help_step1_body)) }
                item { HelpStep("2", stringResource(R.string.help_step2_title), stringResource(R.string.help_step2_body)) }
                item { HelpStep("3", stringResource(R.string.help_step3_title), stringResource(R.string.help_step3_body)) }
                item { HelpStep("4", stringResource(R.string.help_step4_title), stringResource(R.string.help_step4_body)) }
                item { HelpStep("5", stringResource(R.string.help_step5_title), stringResource(R.string.help_step5_body)) }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(17.dp)) {
                            Text(stringResource(R.string.help_why_title), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.help_why_body), style = MaterialTheme.typography.bodyMedium)
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
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFFFFA45B), CfOrange))).padding(21.dp)) {
            Text(stringResource(R.string.help_intro_title), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(5.dp))
            Text(stringResource(R.string.help_intro_body), color = Color.White.copy(.9f))
        }
    }
}

@Composable
private fun HelpStep(number: String, title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Surface(color = CfOrange, shape = CircleShape) {
                Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) { Text(number, color = Color.White, fontWeight = FontWeight.Bold) }
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
