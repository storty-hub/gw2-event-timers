package com.storty.gw2timers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.storty.gw2timers.data.Storage
import com.storty.gw2timers.data.ThemeMode
import com.storty.gw2timers.ui.CharactersScreen
import com.storty.gw2timers.ui.EventsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GW2TimersApp()
        }
    }
}

@Composable
fun GW2TimersApp() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Storage.load(context)) }
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(state) {
        Storage.save(context, state)
    }

    val systemDark = isSystemInDarkTheme()
    val useDark = when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (useDark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        // Surface красит весь фон под цвет темы
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> EventsScreen(
                            state = state,
                            onStateChange = { state = it }
                        )
                        1 -> CharactersScreen(
                            state = state,
                            onStateChange = { state = it }
                        )
                    }
                }

                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("📅") },
                        label = { Text("Ивенты") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("👤") },
                        label = { Text("Персонажи") }
                    )
                }
            }
        }
    }
}
