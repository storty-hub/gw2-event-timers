package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storty.gw2timers.data.AppState
import com.storty.gw2timers.data.Character

@Composable
fun CharactersScreen(
    state: AppState,
    onStateChange: (AppState) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCharacter by remember { mutableStateOf<Character?>(null) }

    if (selectedCharacter != null) {
        CharacterDetailScreen(
            character = selectedCharacter!!,
            state = state,
            onStateChange = onStateChange,
            onBack = { selectedCharacter = null }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Персонажи", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddDialog = true }) {
                Text("+ Добавить")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.characters.isEmpty()) {
            Text("Пока нет персонажей. Нажми «+ Добавить», чтобы создать первого.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.characters) { character ->
                    CharacterBlock(
                        character = character,
                        onClick = { selectedCharacter = character }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCharacterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newCharacter ->
                onStateChange(state.copy(characters = state.characters + newCharacter))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CharacterBlock(
    character: Character,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(character.color))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                character.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                character.className,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun AddCharacterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF2196F3.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый персонаж") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = className,
                    onValueChange = { className = it },
                    label = { Text("Класс") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Цвет:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val colors = listOf(
                        0xFF4CAF50.toInt(),
                        0xFF2196F3.toInt(),
                        0xFFFF9800.toInt(),
                        0xFFE91E63.toInt(),
                        0xFF9C27B0.toInt()
                    )
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(c), RoundedCornerShape(50))
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            Character(
                                name = name,
                                className = className.ifBlank { "—" },
                                color = color
                            )
                        )
                    }
                }
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
