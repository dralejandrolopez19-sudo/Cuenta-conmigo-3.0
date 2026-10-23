package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarEvent
import com.example.ui.components.AiWellnessAssistantCard
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class EventCategoryOption(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bgColor: Color
)

private val eventCategories = listOf(
    EventCategoryOption("Terapia", Icons.Default.Psychology, PurpleAccent, PurpleLight),
    EventCategoryOption("Médico", Icons.Default.MedicalServices, TealSecondary, TealLight),
    EventCategoryOption("Bienestar", Icons.Default.Spa, Color(0xFF2F855A), Color(0xFFE6FFFA)),
    EventCategoryOption("Recordatorio", Icons.Default.NotificationsActive, CoralPrimary, CoralLight)
)

private fun formatReadableSpanishDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(dateStr) ?: return dateStr
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        formatter.format(parsed).replaceFirstChar { it.uppercase() }
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun AgendaScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedCalendarDate.collectAsState()
    val events by viewModel.calendarEvents.collectAsState()
    val moodEntries by viewModel.moodEntries.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val agendaAiState by viewModel.agendaAiPrepState.collectAsState()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    val initialCal = remember { Calendar.getInstance() }
    var currentYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    LaunchedEffect(selectedDate) {
        val parts = selectedDate.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (y != null && m != null) {
                currentYear = y
                currentMonth = m - 1
            }
        }
    }

    val monthCalendar = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(currentYear, currentMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES"))
        sdf.format(monthCalendar.time).replaceFirstChar { it.uppercase() }
    }

    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (monthCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysOfWeekLabels = listOf("L", "M", "X", "J", "V", "S", "D")

    val dayEvents = remember(events, selectedDate) {
        events.filter { it.date == selectedDate }.sortedBy { it.time }
    }
    val dayMood = remember(moodEntries, selectedDate) {
        moodEntries.firstOrNull { it.date == selectedDate }
    }
    val dayJournals = remember(journalEntries, selectedDate) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        journalEntries.filter { entry ->
            entry.createdAt?.toDate()?.let { sdf.format(it) } == selectedDate
        }
    }

    val hasDayRecords = dayEvents.isNotEmpty() || dayMood != null || dayJournals.isNotEmpty()

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
            // 1. Cabecera Minimalista
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agenda",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    letterSpacing = (-0.4).sp,
                    modifier = Modifier.testTag("agenda_header_title")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedDate != viewModel.todayDateString) {
                        OutlinedButton(
                            onClick = { viewModel.selectCalendarDate(viewModel.todayDateString) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealSecondary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
                                .height(32.dp)
                                .testTag("today_quick_button")
                        ) {
                            Text("Hoy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Button(
                        onClick = {
                            editingEvent = null
                            showAddEventDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
                            .height(32.dp)
                            .testTag("header_add_event_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agendar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Calendario Mensual Minimalista
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_card")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (currentMonth == Calendar.JANUARY) {
                                    currentMonth = Calendar.DECEMBER
                                    currentYear -= 1
                                } else {
                                    currentMonth -= 1
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mes anterior",
                                tint = CharcoalText,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = monthName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )

                        IconButton(
                            onClick = {
                                if (currentMonth == Calendar.DECEMBER) {
                                    currentMonth = Calendar.JANUARY
                                    currentYear += 1
                                } else {
                                    currentMonth += 1
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Mes siguiente",
                                tint = CharcoalText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        daysOfWeekLabels.forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedSlate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Cuadrícula limpia del mes
                    val totalCells = firstDayOfWeek + daysInMonth
                    val rows = (totalCells + 6) / 7

                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (c in 0 until 7) {
                                val cellIndex = r * 7 + c
                                val dayNumber = cellIndex - firstDayOfWeek + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val dateStr = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        currentYear,
                                        currentMonth + 1,
                                        dayNumber
                                    )
                                    val isSelected = dateStr == selectedDate
                                    val isToday = dateStr == viewModel.todayDateString
                                    val hasEvent = events.any { it.date == dateStr }
                                    val hasMood = moodEntries.any { it.date == dateStr }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> TealSecondary
                                                    isToday -> TealLight
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(1.dp, TealSecondary, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { viewModel.selectCalendarDate(dateStr) }
                                            .testTag("calendar_day_$dayNumber")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNumber",
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.White else CharcoalText
                                            )

                                            if (hasEvent || hasMood) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> Color.White
                                                                hasEvent -> TealSecondary
                                                                else -> CoralPrimary
                                                            }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(34.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Fecha Seleccionada y Citas del Día
            Text(
                text = formatReadableSpanishDate(selectedDate),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (!hasDayRecords) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            editingEvent = null
                            showAddEventDialog = true
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = MutedSlate.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sin citas en esta fecha",
                                fontSize = 13.sp,
                                color = MutedSlate
                            )
                        }
                        Text(
                            text = "+ Añadir cita",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealSecondary
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayEvents.forEach { ev ->
                        val eventItemId = "agenda_${ev.id}"
                        val isEventRevealed = revealedIds.contains(eventItemId)
                        val catOption = eventCategories.firstOrNull { ev.type.contains(it.label, ignoreCase = true) }
                            ?: EventCategoryOption(ev.type.ifBlank { "Cita" }, Icons.Default.CalendarMonth, TealSecondary, TealLight)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(catOption.bgColor)
                                ) {
                                    Icon(
                                        imageVector = catOption.icon,
                                        contentDescription = null,
                                        tint = catOption.tint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    PrivacyProtectedText(
                                        realText = ev.title,
                                        itemId = eventItemId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isEventRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = aiAliases.agendaAlias,
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(eventItemId) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    if (ev.notes.isNotBlank()) {
                                        PrivacyProtectedText(
                                            realText = ev.notes,
                                            itemId = eventItemId,
                                            isStreetModeActive = isStreetModeActive,
                                            isRevealed = isEventRevealed,
                                            isAiDisguiseEnabled = isAiDisguiseEnabled,
                                            aiDisguiseText = "Nota privada",
                                            onRevealClick = { viewModel.revealPrivacyItemTemporarily(eventItemId) },
                                            fontSize = 11.sp,
                                            color = MutedSlate,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (ev.time.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catOption.bgColor
                                    ) {
                                        Text(
                                            text = ev.time,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = catOption.tint,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingEvent = ev
                                        showAddEventDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_event_${ev.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar cita",
                                        tint = TealSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteCalendarEvent(ev.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar cita",
                                        tint = MutedSlate.copy(alpha = 0.6f),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (dayMood != null) {
                        val dayMoodItemId = "agenda_mood_${dayMood.id}"
                        val isDayMoodRevealed = revealedIds.contains(dayMoodItemId)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectTab(AppTab.ANIMO) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mood,
                                    contentDescription = null,
                                    tint = CoralPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                PrivacyProtectedText(
                                    realText = "Ánimo registrado: ${dayMood.levelLabel}",
                                    itemId = dayMoodItemId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isDayMoodRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = aiAliases.moodAlias,
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayMoodItemId) },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalText
                                )
                            }
                        }
                    }

                    dayJournals.forEach { journal ->
                        val dayJournalId = "agenda_journal_${journal.id}"
                        val isDayJournalRevealed = revealedIds.contains(dayJournalId)
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectTab(AppTab.DIARIO) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                PrivacyProtectedText(
                                    realText = "Diario: ${journal.title}",
                                    itemId = dayJournalId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isDayJournalRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = aiAliases.diaryAlias,
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayJournalId) },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = CharcoalText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Asistente IA Compacto al pie
            AiWellnessAssistantCard(
                title = "Preparador de Cita",
                subtitle = "Preguntas clave para tu consulta o balance del día.",
                buttonLabel = "Preparar",
                state = agendaAiState,
                onGenerateClick = { viewModel.generateAgendaAiPreparation(selectedDate) },
                accentColor = PurpleAccent,
                accentBgColor = PurpleLight,
                testTag = "agenda_ai_card"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showAddEventDialog) {
            AddEventDialog(
                initialDate = editingEvent?.date ?: selectedDate,
                initialEvent = editingEvent,
                onDismiss = {
                    showAddEventDialog = false
                    editingEvent = null
                },
                onSave = { title, type, date, time, notes ->
                    val existing = editingEvent
                    if (existing != null) {
                        viewModel.updateCalendarEvent(existing, title, type, date, time, notes)
                    } else {
                        viewModel.addCalendarEvent(title, type, date, time, notes)
                    }
                    showAddEventDialog = false
                    editingEvent = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEventDialog(
    initialDate: String,
    initialEvent: CalendarEvent? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, type: String, date: String, time: String, notes: String) -> Unit
) {
    var title by remember(initialEvent) { mutableStateOf(initialEvent?.title ?: "") }
    var notes by remember(initialEvent) { mutableStateOf(initialEvent?.notes ?: "") }
    var selectedType by remember(initialEvent) {
        mutableStateOf(initialEvent?.type?.ifBlank { eventCategories.first().label } ?: eventCategories.first().label)
    }

    val initialTimeParts = (initialEvent?.time ?: "10:00").split(":")
    var hour by remember(initialEvent) { mutableIntStateOf(initialTimeParts.getOrNull(0)?.toIntOrNull() ?: 10) }
    var minute by remember(initialEvent) { mutableIntStateOf(initialTimeParts.getOrNull(1)?.toIntOrNull() ?: 0) }
    val formattedTime = String.format(Locale.US, "%02d:%02d", hour, minute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialEvent != null) "Editar Cita" else "Nueva Cita",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CharcoalText
                )
                Text(
                    text = formatReadableSpanishDate(initialDate),
                    fontSize = 12.sp,
                    color = TealSecondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Motivo o título") },
                    placeholder = { Text("Ej. Psicología, Control médico") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    eventCategories.forEach { cat ->
                        val isSelected = selectedType == cat.label
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) cat.tint else Color(0xFFEDF2F7),
                            modifier = Modifier.clickable { selectedType = cat.label }
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else CharcoalText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector directo de Hora (HH:mm)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { hour = (hour + 1) % 24 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir hora", tint = TealSecondary)
                        }
                        Text(
                            text = String.format(Locale.US, "%02d", hour),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        IconButton(
                            onClick = { hour = (hour - 1 + 24) % 24 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar hora", tint = TealSecondary)
                        }
                    }

                    Text(
                        text = ":",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { minute = (minute + 15) % 60 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir minuto", tint = TealSecondary)
                        }
                        Text(
                            text = String.format(Locale.US, "%02d", minute),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        IconButton(
                            onClick = { minute = (minute - 15 + 60) % 60 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar minuto", tint = TealSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), selectedType, initialDate, formattedTime, notes.trim())
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}
