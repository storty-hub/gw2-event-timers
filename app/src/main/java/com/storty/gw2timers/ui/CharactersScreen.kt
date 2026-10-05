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

val GW2_CLASSES = listOf(
    "Ranger",
    "Thief",
    "Engineer",
    "Elementalist",
    "Mesmer",
    "Necromancer",
    "Warrior",
    "Guardian",
    "Revenant"
)

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
            message = "«${deletingCharacter!!.name}» будет удалён. Все его отметки выполнения тоже исчезнут.",
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(character.color))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
            ) {
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
            Text(
                "✏",
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onEdit() }
                    .padding(8.dp)
            )
            Text(
                "✕",
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(8.dp)
            )
        }
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
    var color by remember { mutableStateOf(existing?.color ?: 0xFF2196F3.toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Новый персонаж" else "Редактировать персонажа") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Класс:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))

                GW2_CLASSES.forEach { cls ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { className = cls }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = className == cls,
                            onClick = { className = cls }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(cls)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Цвет:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ColorPalette(
                    selected = color,
                    onSelect = { color = it }
                )
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
