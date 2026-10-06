package com.storty.gw2timers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.SortMode
import com.storty.gw2timers.data.ThemeMode

@Composable
fun SettingsScreen(
    state: AppState,
    onStateChange: (AppState) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад") }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Настройки",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ===== ТЕМА =====
        SectionTitle("Тема")
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStateChange(state.copy(themeMode = mode)) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.themeMode == mode,
                    onClick = { onStateChange(state.copy(themeMode = mode)) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    when (mode) {
                        ThemeMode.SYSTEM -> "Системная"
                        ThemeMode.LIGHT -> "Светлая"
                        ThemeMode.DARK -> "Тёмная"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ===== СОРТИРОВКА =====
        SectionTitle("Сортировка ивентов")
        SortMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onStateChange(state.copy(settings = state.settings.copy(sortMode = mode)))
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = state.settings.sortMode == mode,
                    onClick = {
                        onStateChange(state.copy(settings = state.settings.copy(sortMode = mode)))
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    when (mode) {
                        SortMode.ADDED -> "По добавлению"
                        SortMode.ALPHABETICAL -> "По алфавиту"
                        SortMode.CD_ASC -> "По возрастанию CD"
                        SortMode.CD_DESC -> "По убыванию CD"
                        SortMode.SMART -> "Умная (готовые → окно → скоро)"
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ===== ОТОБРАЖЕНИЕ =====
        SectionTitle("Отображение")
        SettingSwitch(
            label = "Показывать прогресс-бар",
            checked = state.settings.showProgressBar,
            onChange = {
                onStateChange(state.copy(settings = state.settings.copy(showProgressBar = it)))
            }
        )
        SettingSwitch(
            label = "Показывать прогресс окна CD",
            checked = state.settings.showWindowProgress,
            onChange = {
                onStateChange(state.copy(settings = state.settings.copy(showWindowProgress = it)))
            }
        )
        SettingSwitch(
            label = "Показывать «простой»",
            checked = state.settings.showIdleTime,
            onChange = {
                onStateChange(state.copy(settings = state.settings.copy(showIdleTime = it)))
            }
        )
        SettingSwitch(
            label = "Компактный режим",
            checked = state.settings.compactMode,
            onChange = {
                onStateChange(state.copy(settings = state.settings.copy(compactMode = it)))
            }
        )
        SettingSwitch(
            label = "Серые выполненные (вниз списка)",
            checked = state.settings.grayOutCompleted,
            onChange = {
                onStateChange(state.copy(settings = state.settings.copy(grayOutCompleted = it)))
            }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

@Composable
fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
