package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AiHabitUiState
import com.example.ui.viewmodel.AppViewModel

@Composable
fun HabitsScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsState()
    val habitAiState by viewModel.habitAiSuggestionState.collectAsState()
    var habitAddedFeedback by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val todayStr = viewModel.todayDateString
    val completedTodayCount = habits.count { it.completedDates.contains(todayStr) }
    val totalCount = habits.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Cabecera Minimalista + Progreso + Botón Compacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Hábitos",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        letterSpacing = (-0.4).sp
                    )
                    if (totalCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CoralLight
                        ) {
                            Text(
                                text = "$completedTodayCount/$totalCount hoy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CoralPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("add_habit_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hábito",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Lista Directa de Hábitos o Estado Vacío Minimalista
            if (habits.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(CoralLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sin hábitos registrados",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Crea un hábito pequeño o pide una sugerencia abajo.",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    habits.forEach { habit ->
                        val isDone = habit.completedDates.contains(todayStr)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDone) Color(0xFFF0FFF4) else Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHabitToday(habit) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Checkbox(
                                    checked = isDone,
                                    onCheckedChange = { viewModel.toggleHabitToday(habit) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = TealSecondary,
                                        checkmarkColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) Color(0xFF276749) else CharcoalText
                                    )
                                    Text(
                                        text = "${habit.category} • ${habit.timeOfDay}",
                                        fontSize = 11.sp,
                                        color = MutedSlate
                                    )
                                }

                                if (habit.streakDays > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFFF5F5))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🔥 ${habit.streakDays}d",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralPrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.deleteHabit(habit.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar hábito",
                                        tint = MutedSlate.copy(alpha = 0.6f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta Compacta de Micro-Hábitos con IA al final
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_ai_suggestion_card")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(TealLight)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "IA de Hábitos",
                                    tint = TealSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Micro-Hábitos con IA",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = "Sugiere un hábito según tu ánimo y rutina.",
                                    fontSize = 11.sp,
                                    color = MutedSlate,
                                    lineHeight = 14.sp
                                )
                            }
                        }

                        if (habitAiState !is AiHabitUiState.Loading) {
                            Button(
                                onClick = {
                                    habitAddedFeedback = false
                                    viewModel.generateHabitAiSuggestion()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                                    .height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (habitAiState is AiHabitUiState.Success) "Otro" else "Sugerir",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    when (val state = habitAiState) {
                        is AiHabitUiState.Idle -> {}
                        is AiHabitUiState.Loading -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TealLight.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = TealSecondary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Diseñando micro-hábito...",
                                    fontSize = 11.sp,
                                    color = CharcoalText
                                )
                            }
                        }
                        is AiHabitUiState.Success -> {
                            val suggestion = state.suggestion
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TealLight.copy(alpha = 0.35f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "✨ ${suggestion.name}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${suggestion.category} • ${suggestion.timeOfDay}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealSecondary
                                )
                                if (suggestion.reason.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = suggestion.reason,
                                        fontSize = 11.sp,
                                        color = CharcoalText,
                                        lineHeight = 15.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.addHabit(
                                            name = suggestion.name,
                                            category = suggestion.category,
                                            timeOfDay = suggestion.timeOfDay
                                        )
                                        habitAddedFeedback = true
                                    },
                                    enabled = !habitAddedFeedback,
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (habitAddedFeedback) Color(0xFF38A169) else TealSecondary
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 1.dp, minHeight = 30.dp)
                                        .height(30.dp)
                                ) {
                                    Text(
                                        text = if (habitAddedFeedback) "✓ Agregado" else "+ Agregar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        is AiHabitUiState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                fontSize = 11.sp,
                                color = CoralPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Dialog for Custom Habit
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, category, timeOfDay ->
                    viewModel.addHabit(name, category, timeOfDay)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, timeOfDay: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Salud Mental") }
    var timeOfDay by remember { mutableStateOf("Mañana") }

    val categories = listOf("Salud Mental", "Físico y Sueño", "Social", "Emocional")
    val times = listOf("Mañana", "Tarde", "Noche", "Todo el día")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Crear Hábito Personal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = CharcoalText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Diseña una acción concreta nacida de tu propia reflexión para sostener en el tiempo.",
                    fontSize = 12.sp,
                    color = MutedSlate
                )
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del hábito") },
                    placeholder = { Text("Ej. Escribir en diario, paseo 15 min, hidratarme") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Categoría:",
                    fontSize = 12.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CoralPrimary else Color(0xFFEDF2F7))
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Momento del día:",
                    fontSize = 12.sp,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    times.forEach { t ->
                        val isSelected = timeOfDay == t
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealSecondary else Color(0xFFEDF2F7))
                                .clickable { timeOfDay = t }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = t,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), category, timeOfDay)
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}
