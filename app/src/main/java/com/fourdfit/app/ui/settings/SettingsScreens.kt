package com.fourdfit.app.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Straighten
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourdfit.app.di.appViewModel
import com.fourdfit.app.domain.model.ReminderType
import com.fourdfit.app.domain.model.ThemeMode
import com.fourdfit.app.domain.model.UnitSystem
import com.fourdfit.app.notifications.NotificationHelper
import com.fourdfit.app.ui.components.AdaptiveContainer
import com.fourdfit.app.ui.components.ConfirmDialog
import com.fourdfit.app.ui.components.DisclaimerNote
import com.fourdfit.app.ui.components.GhostButton
import com.fourdfit.app.ui.components.GlassCard
import com.fourdfit.app.ui.components.ScreenHeader
import com.fourdfit.app.ui.components.SectionTitle
import com.fourdfit.app.ui.components.SegmentedToggle
import com.fourdfit.app.ui.components.screenPadding
import com.fourdfit.app.ui.theme.FourD

@Composable
private fun settingsViewModel(): SettingsViewModel =
    appViewModel { SettingsViewModel(it.settingsRepository, it.authRepository, it.stepCounter) }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenLegal: (String) -> Unit,
) {
    val vm = settingsViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = FourD.colors
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    AdaptiveContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = false),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader("Settings", onBack = onBack) }
            item {
                SettingsGroup("Account") {
                    SettingRow(Icons.Rounded.Edit, "Edit profile", "Name, birthday, height, goal and photo", onEditProfile)
                    SettingRow(Icons.Rounded.Notifications, "Notifications", "Choose which reminders you get", onOpenNotifications)
                }
            }
            item {
                SettingsGroup("Appearance") {
                    LabeledToggle(Icons.Rounded.DarkMode, "Theme") {
                        SegmentedToggle(
                            options = ThemeMode.entries,
                            selected = state.settings.themeMode,
                            label = { it.label },
                            onSelect = vm::setTheme,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    LabeledToggle(Icons.Rounded.Straighten, "Units") {
                        SegmentedToggle(
                            options = UnitSystem.entries,
                            selected = state.settings.units,
                            label = { if (it == UnitSystem.METRIC) "Metric (cm)" else "Imperial (ft, in)" },
                            onSelect = vm::setUnits,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    SettingRow(
                        icon = Icons.Rounded.Language,
                        title = "Language",
                        subtitle = "English",
                        onClick = {
                            if (!openAppLanguageSettings(context)) {
                                vm.showMessage("4D FIT is currently available in English. More languages are coming.")
                            }
                        },
                    )
                }
            }
            item {
                SettingsGroup("Accessibility") {
                    SwitchRow(
                        Icons.Rounded.Contrast,
                        "High contrast",
                        "Stronger text and borders, no background glow",
                        state.settings.highContrast,
                        vm::setHighContrast,
                    )
                    SwitchRow(
                        Icons.Rounded.Accessibility,
                        "Reduce motion",
                        "Turns off looping and decorative animation",
                        state.settings.reduceMotion,
                        vm::setReduceMotion,
                    )
                }
            }
            item {
                SettingsGroup("About") {
                    SettingRow(Icons.Rounded.PrivacyTip, "Privacy policy", null, { onOpenLegal(LegalTexts.PRIVACY) })
                    SettingRow(Icons.Rounded.Gavel, "Terms of use", null, { onOpenLegal(LegalTexts.TERMS) })
                    SettingRow(Icons.Rounded.Info, "About 4D FIT", null, { onOpenLegal(LegalTexts.ABOUT) })
                }
            }
            item {
                SettingsGroup("Account actions") {
                    SettingRow(Icons.AutoMirrored.Rounded.Logout, "Log out", "Removes your data from this device", {
                        confirmLogout = true
                    }, enabled = !state.busy)
                    SettingRow(
                        Icons.Rounded.DeleteForever,
                        "Delete account",
                        "Permanently deletes your account and data",
                        { confirmDelete = true },
                        tint = c.danger,
                        enabled = !state.busy,
                    )
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        ) { data -> Snackbar(data, containerColor = c.surfaceHigh, contentColor = c.textPrimary) }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = "Log out?",
            message = "Your progress is saved to your account. Data stored only on this device (photo, water and step logs) will be removed.",
            confirmLabel = "Log out",
            onConfirm = {
                confirmLogout = false
                vm.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This permanently deletes your profile and workout history from our servers and this device. It can't be undone.",
            confirmLabel = "Delete account",
            destructive = true,
            onConfirm = {
                confirmDelete = false
                vm.deleteAccount()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun openAppLanguageSettings(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    return runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }.isSuccess
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        SectionTitle(title)
        Spacer(Modifier.height(6.dp))
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 6.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    tint: Color? = null,
    enabled: Boolean = true,
) {
    val c = FourD.colors
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint ?: c.cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = tint ?: c.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textMuted)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = FourD.colors
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.cyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = c.violet,
                    uncheckedThumbColor = c.textSecondary,
                    uncheckedTrackColor = c.glassFillLow,
                    uncheckedBorderColor = c.glassBorder,
                ),
        )
    }
}

@Composable
private fun LabeledToggle(
    icon: ImageVector,
    title: String,
    control: @Composable () -> Unit,
) {
    val c = FourD.colors
    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = c.cyan, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        control()
    }
}

@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val vm = settingsViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val c = FourD.colors
    var canNotify by remember { mutableStateOf(NotificationHelper.canNotify(context)) }
    var pending by remember { mutableStateOf<ReminderType?>(null) }
    LifecycleResumeEffect(Unit) {
        canNotify = NotificationHelper.canNotify(context)
        onPauseOrDispose { }
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            canNotify = NotificationHelper.canNotify(context)
            val type = pending
            pending = null
            if (granted && type != null) vm.setReminder(type, true)
        }
    val anyEnabled =
        state.settings.reminders.values
            .any { it }

    AdaptiveContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = false),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader("Notifications", subtitle = "All reminders are optional and off by default", onBack = onBack) }
            if (!canNotify && anyEnabled) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.amber)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Notifications are blocked for 4D FIT, so reminders can't appear.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        GhostButton(
                            "Open notification settings",
                            { openNotificationSettings(context) },
                            icon = Icons.Rounded.NotificationsActive,
                        )
                    }
                }
            }
            item {
                GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(6.dp)) {
                    ReminderType.entries.forEach { type ->
                        SwitchRow(
                            icon = Icons.Rounded.Notifications,
                            title = type.title,
                            subtitle = type.description,
                            checked = state.settings.reminders[type] == true,
                            onCheckedChange = { enabled ->
                                val needsPermission =
                                    enabled &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationHelper.canNotify(context)
                                if (needsPermission) {
                                    pending = type
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    vm.setReminder(type, enabled)
                                }
                            },
                        )
                    }
                }
            }
            item {
                DisclaimerNote("Reminders are scheduled on your device and may arrive a few minutes late to save battery.")
            }
        }
    }
}

@Composable
fun LegalScreen(
    doc: String,
    onBack: () -> Unit,
) {
    val c = FourD.colors
    AdaptiveContainer {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = screenPadding(withBottomBar = false),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { ScreenHeader(LegalTexts.title(doc), onBack = onBack) }
            items(LegalTexts.sections(doc)) { (heading, body) ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(heading, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                }
            }
        }
    }
}
