package com.storty.gw2timers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    var editingCharacter by remember { mutableStateOf<Character?>(null) }
    var deletingCharacter by remember { mutableStateOf<Character?>(null) }
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

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Персонажи",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            FilledTonalButton(
                onClick = { showAddDialog = true },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("+", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Персонаж")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.characters.isEmpty()) {
            Text(
                "Пока нет персонажей",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.characters) { character ->
                    CharacterBlock(
                        character = character,
                        onClick = { selectedCharacter = character },
                        onEdit = { editingCharacter = character },
                        onDelete = { deletingCharacter = character }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CharacterDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newCharacter ->
                onStateChange(state.copy(characters = state.characters + newCharacter))
                showAddDialog = false
            }
        )
    }

    if (editingCharacter != null) {
        CharacterDialog(
            existing = editingCharacter,
            onDismiss = { editingCharacter = null },
            onConfirm = { updated ->
                onStateChange(
                    state.copy(
                        characters = state.characters.map {
                            if (it.id == updated.id) updated else it
                        }
                    )
                )
                editingCharacter = null
            }
        )
    }

    if (deletingCharacter != null) {
        ConfirmDeleteDialog(
            title = "Удалить персонажа?",
            message = "«${deletingCharacter!!.name}» будет удалён вместе со всеми отметками.",
            onConfirm = {
                val id = deletingCharacter!!.id
                onStateChange(
                    state.copy(
                        characters = state.characters.filter { it.id != id },
                        completions = state.completions.filter { it.characterId != id }
                    )
                )
                deletingCharacter = null
            },
            onDismiss = { deletingCharacter = null }
        )
    }
}

@Composable
fun CharacterBlock(
    character: Character,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Цветная полоска слева
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .background(Color(character.color), RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                character.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                character.className,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Text(
            "✏",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { onEdit() }
                .padding(8.dp)
        )
        Text(
            "✕",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { onDelete() }
                .padding(8.dp)
        )
    }
}

@Composable
fun CharacterDialog(
    existing: Character?,
    onDismiss: () -> Unit,
    onConfirm: (Character) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var className by remember { mutableStateOf(existing?.className ?: GW2_CLASSES[0]) }
    var color by remember { mutableStateOf(existing?.color ?: COLOR_OPTIONS[5]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новый персонаж" else "Редактировать") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Класс", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ClassDropdown(selected = className, onSelect = { className = it })

                Spacer(modifier = Modifier.height(12.dp))
                Text("Цвет", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                ColorDropdown(selected = color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            Character(
                                id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                className = className,
                                color = color
                            )
                        )
                    }
                }
            ) { Text(if (existing == null) "Создать" else "Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
