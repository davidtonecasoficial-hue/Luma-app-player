package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.config.AppConfig
import com.example.data.model.AccentColorOption
import com.example.data.model.AppLanguage
import com.example.data.model.GlassQuality
import com.example.data.model.SyncState
import com.example.data.model.SyncedFolderEntity
import com.example.data.model.ThemeMode
import com.example.data.model.UserPreferences
import com.example.player.DeviceCodecGroup
import com.example.ui.i18n.formatRelativeTimestamp
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassStyle
import com.example.ui.theme.accentPaletteFor

@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    syncedFolders: List<SyncedFolderEntity>,
    syncState: SyncState,
    totalMediaCount: Int,
    deviceCodecs: List<DeviceCodecGroup>,
    onSetThemeMode: (ThemeMode) -> Unit,
    onSetAccentColor: (AccentColorOption) -> Unit,
    onSetGlassQuality: (GlassQuality) -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetReduceMotion: (Boolean) -> Unit,
    onSetAlbumRotation: (Boolean) -> Unit,
    onSetVisualizer: (Boolean) -> Unit,
    onSetDynamicGradient: (Boolean) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetSwipeGestures: (Boolean) -> Unit,
    onSetCrossfade: (Boolean, Int?) -> Unit,
    onSetResumeSession: (Boolean) -> Unit,
    onSetMinimalistDefault: (Boolean) -> Unit,
    onLinkFolder: () -> Unit,
    onRemoveFolder: (Long) -> Unit,
    onSyncNow: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    bottomContentPadding: Dp
) {
    val style = LocalLiquidGlassStyle.current
    var showCodecsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 14.dp,
                bottom = bottomContentPadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Language Switcher (Multi-Language Support)
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = style.accentPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.setting_language), style = MaterialTheme.typography.titleMedium)
                        }
                        val languages = listOf(
                            AppLanguage.PORTUGUESE to stringResource(R.string.lang_pt),
                            AppLanguage.ENGLISH to stringResource(R.string.lang_en),
                            AppLanguage.SPANISH to stringResource(R.string.lang_es),
                            AppLanguage.FRENCH to stringResource(R.string.lang_fr),
                            AppLanguage.SYSTEM to stringResource(R.string.lang_system)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(languages) { (lang, label) ->
                                FilterChip(
                                    selected = preferences.language == lang,
                                    onClick = { onSetLanguage(lang) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("lang_chip_${lang.tag}")
                                )
                            }
                        }
                    }
                }
            }

            // Appearance & Liquid Glass
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.section_appearance), style = MaterialTheme.typography.titleMedium)

                        Text(stringResource(R.string.setting_theme), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val themes = listOf(
                                ThemeMode.LIGHT to stringResource(R.string.theme_light),
                                ThemeMode.DARK to stringResource(R.string.theme_dark),
                                ThemeMode.SYSTEM to stringResource(R.string.theme_system)
                            )
                            themes.forEach { (mode, label) ->
                                FilterChip(
                                    selected = preferences.themeMode == mode,
                                    onClick = { onSetThemeMode(mode) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.key}")
                                )
                            }
                        }

                        Text(stringResource(R.string.setting_accent), style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentColorOption.entries.forEach { option ->
                                val palette = accentPaletteFor(option, style.isDark)
                                val selected = preferences.accentColor == option
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(palette.primary)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { onSetAccentColor(option) }
                                        .testTag("accent_swatch_${option.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = option.key,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(stringResource(R.string.setting_glass_quality), style = MaterialTheme.typography.labelLarge)
                        val qualities = listOf(
                            GlassQuality.HIGH to stringResource(R.string.glass_high),
                            GlassQuality.BALANCED to stringResource(R.string.glass_balanced),
                            GlassQuality.REDUCED to stringResource(R.string.glass_reduced)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(qualities) { (quality, label) ->
                                FilterChip(
                                    selected = preferences.glassQuality == quality,
                                    onClick = { onSetGlassQuality(quality) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // Offline Library & Synchronization
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.section_offline_sync), style = MaterialTheme.typography.titleMedium)

                        val statusText = when (syncState) {
                            is SyncState.Syncing -> stringResource(R.string.sync_status_syncing)
                            else -> if (preferences.lastSyncTimestamp > 0L) {
                                stringResource(
                                    R.string.sync_status_synced,
                                    formatRelativeTimestamp(preferences.lastSyncTimestamp),
                                    totalMediaCount
                                )
                            } else {
                                stringResource(R.string.sync_status_never)
                            }
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onSyncNow,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_sync_now_btn")
                            ) {
                                Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_sync_now))
                            }
                            OutlinedButton(
                                onClick = onLinkFolder,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_import_folder))
                            }
                        }

                        if (syncedFolders.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.synced_folders_count, syncedFolders.size),
                                style = MaterialTheme.typography.labelLarge
                            )
                            syncedFolders.forEach { folder ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${folder.folderName} (${folder.itemCount})",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onRemoveFolder(folder.id) }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        OutlinedButton(
                            onClick = onExportBackup,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.export_backup))
                        }

                        OutlinedButton(
                            onClick = onImportBackup,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.import_backup))
                        }
                    }
                }
            }

            // Animations & Accessibility
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.section_motion), style = MaterialTheme.typography.titleMedium)
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_reduce_motion),
                            checked = preferences.reduceMotion,
                            onCheckedChange = onSetReduceMotion
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_album_rotation),
                            checked = preferences.albumArtRotation,
                            onCheckedChange = onSetAlbumRotation
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_visualizer),
                            checked = preferences.visualizerEnabled,
                            onCheckedChange = onSetVisualizer
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_dynamic_gradient),
                            checked = preferences.dynamicGradientEnabled,
                            onCheckedChange = onSetDynamicGradient
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_haptics),
                            checked = preferences.hapticsEnabled,
                            onCheckedChange = onSetHaptics
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_swipe_gestures),
                            checked = preferences.swipeGesturesEnabled,
                            onCheckedChange = onSetSwipeGestures
                        )
                    }
                }
            }

            // Playback Preferences
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(stringResource(R.string.section_playback), style = MaterialTheme.typography.titleMedium)
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_crossfade),
                            checked = preferences.crossfadeEnabled,
                            onCheckedChange = { onSetCrossfade(it, preferences.crossfadeDurationSec) }
                        )
                        if (preferences.crossfadeEnabled) {
                            Text(
                                text = stringResource(R.string.setting_crossfade_duration, preferences.crossfadeDurationSec),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = preferences.crossfadeDurationSec.toFloat(),
                                onValueChange = { onSetCrossfade(true, it.toInt()) },
                                valueRange = 1f..8f,
                                steps = 6
                            )
                        }
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_resume_session),
                            checked = preferences.resumeLastSession,
                            onCheckedChange = onSetResumeSession
                        )
                        SettingSwitchRow(
                            label = stringResource(R.string.setting_minimalist_default),
                            checked = preferences.minimalistModeDefault,
                            onCheckedChange = onSetMinimalistDefault
                        )
                    }
                }
            }

            // Device Codecs, Privacy & About
            item {
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(stringResource(R.string.section_device_codecs), style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { showCodecsDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inspect_codecs_btn")
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.device_codecs_button))
                        }

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = style.accentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.privacy_note_title), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = stringResource(R.string.privacy_note_body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(stringResource(R.string.about_title), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = stringResource(R.string.about_body, AppConfig.APP_VERSION),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCodecsDialog) {
        AlertDialog(
            onDismissRequest = { showCodecsDialog = false },
            title = { Text(stringResource(R.string.device_codecs_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.device_codecs_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deviceCodecs, key = { it.mimeType }) { codec ->
                            Column {
                                Text(
                                    text = codec.friendlyName,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${codec.mimeType} • ${codec.codecNames.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodecsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
