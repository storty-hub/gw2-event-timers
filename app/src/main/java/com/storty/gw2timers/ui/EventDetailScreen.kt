@Composable
fun ManualCompletionDialog(
    event: GameEvent,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    // Сейчас в ЛОКАЛЬНОМ времени телефона
    val nowLocal = Calendar.getInstance()
    var hour by remember { mutableStateOf(nowLocal.get(Calendar.HOUR_OF_DAY).toString()) }
    var minute by remember { mutableStateOf(nowLocal.get(Calendar.MINUTE).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    val tzName = java.util.TimeZone.getDefault().getDisplayName(
        false,
        java.util.TimeZone.SHORT
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Когда выполнен?") },
        text = {
            Column {
                Text(
                    "Локальное время телефона ($tzName). Если сейчас — оставь как есть.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hour,
                        onValueChange = {
                            hour = it.filter { c -> c.isDigit() }.take(2)
                            error = null
                        },
                        label = { Text("ЧЧ") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":", fontSize = 20.sp)
                    OutlinedTextField(
                        value = minute,
                        onValueChange = {
                            minute = it.filter { c -> c.isDigit() }.take(2)
                            error = null
                        },
                        label = { Text("ММ") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hour.toIntOrNull()
                    val m = minute.toIntOrNull()
                    if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                        error = "Введи корректное время: 00–23 ч, 00–59 мин"
                        return@TextButton
                    }

                    // Собираем время в ЛОКАЛЬНОМ часовом поясе
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, h)
                    cal.set(Calendar.MINUTE, m)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)

                    val now = System.currentTimeMillis()
                    val timestamp: Long = when {
                        // Время в будущем — ставим "сейчас"
                        cal.timeInMillis > now -> now
                        // Если сейчас, скажем, 03:00, а введено 23:00 — значит это вчера
                        // (введено раньше текущего времени, но кажется "недавним")
                        now - cal.timeInMillis > 23 * 60 * 60 * 1000L -> {
                            cal.add(Calendar.DAY_OF_YEAR, -1)
                            cal.timeInMillis
                        }
                        else -> cal.timeInMillis
                    }

                    onConfirm(timestamp)
                }
            ) { Text("ОК") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
