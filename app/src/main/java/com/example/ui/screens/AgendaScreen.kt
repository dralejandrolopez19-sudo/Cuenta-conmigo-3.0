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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.components.PrivacyProtectedText
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
    EventCategoryOption("Terapia / Psicología", Icons.Default.Psychology, PurpleAccent, PurpleLight),
    EventCategoryOption("Consulta Médica", Icons.Default.MedicalServices, TealSecondary, TealLight),
    EventCategoryOption("Bienestar / Personal", Icons.Default.Spa, Color(0xFF2F855A), Color(0xFFE6FFFA)),
    EventCategoryOption("Recordatorio", Icons.Default.NotificationsActive, CoralPrimary, CoralLight)
)

private fun formatReadableSpanishDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = parser.parse(dateStr) ?: return dateStr
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale.forLanguageTag("es-ES"))
        formatter.format(parsed).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
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
    val habits by viewModel.habits.collectAsState()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()

    var showAddEventDialog by remember { mutableStateOf(false) }

    // Inicializar año y mes a partir de selectedDate o fecha actual
    val initialCal = remember {
        val cal = Calendar.getInstance()
        val parts = selectedDate.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (y != null && m != null) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m - 1)
            }
        }
        cal
    }

    var currentYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

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

    val monthPrefix = remember(currentYear, currentMonth) {
        String.format(Locale.US, "%04d-%02d", currentYear, currentMonth + 1)
    }

    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (monthCalendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysOfWeekLabels = listOf("L", "M", "X", "J", "V", "S", "D")

    // Estadísticas del mes visualizado
    val monthEventsCount = remember(events, monthPrefix) {
        events.count { it.date.startsWith(monthPrefix) }
    }
    val monthMoodsCount = remember(moodEntries, monthPrefix) {
        moodEntries.count { it.date.startsWith(monthPrefix) }
    }
    val monthJournalsCount = remember(journalEntries, monthPrefix) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        journalEntries.count { entry ->
            entry.createdAt?.toDate()?.let { sdf.format(it).startsWith(monthPrefix) } == true
        }
    }

    // Registros del día seleccionado
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
    val dayCompletedHabits = remember(habits, selectedDate) {
        habits.filter { it.completedDates.contains(selectedDate) }
    }

    val hasAnyActivities = dayEvents.isNotEmpty() ||
        dayMood != null ||
        dayJournals.isNotEmpty() ||
        dayCompletedHabits.isNotEmpty()

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
            // Encabezado Principal con botón "Hoy" y "+ Cita"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Calendario y Agenda",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "Tus citas, emociones y avances día a día.",
                        fontSize = 13.sp,
                        color = MutedSlate
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val todayParts = viewModel.todayDateString.split("-")
                            if (todayParts.size == 3) {
                                currentYear = todayParts[0].toIntOrNull() ?: currentYear
                                currentMonth = (todayParts[1].toIntOrNull() ?: (currentMonth + 1)) - 1
                            }
                            viewModel.selectCalendarDate(viewModel.todayDateString)
                        },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, TealSecondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealSecondary),
                        modifier = Modifier.testTag("jump_to_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Ir a hoy",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hoy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showAddEventDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier.testTag("header_add_event_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cita", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Resumen Mensual Rápido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MonthStatChip(
                    count = monthEventsCount,
                    label = "Citas del mes",
                    color = TealSecondary,
                    bgColor = TealLight,
                    modifier = Modifier.weight(1f)
                )
                MonthStatChip(
                    count = monthMoodsCount,
                    label = "Días con ánimo",
                    color = CoralPrimary,
                    bgColor = CoralLight,
                    modifier = Modifier.weight(1f)
                )
                MonthStatChip(
                    count = monthJournalsCount,
                    label = "Notas de diario",
                    color = PurpleAccent,
                    bgColor = PurpleLight,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta Principal del Calendario Mensual
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Cabecera de navegación de mes
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
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Mes anterior",
                                tint = CoralPrimary
                            )
                        }

                        Text(
                            text = monthName,
                            fontSize = 18.sp,
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
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Mes siguiente",
                                tint = CoralPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cabecera de días de la semana
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        daysOfWeekLabels.forEach { label ->
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MutedSlate,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(38.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Cuadrícula de semanas
                    val totalSlots = firstDayOfWeek + daysInMonth
                    val weeksCount = (totalSlots + 6) / 7
                    val sdfJournal = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

                    for (week in 0 until weeksCount) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            for (dayOfWeek in 0..6) {
                                val slotIndex = week * 7 + dayOfWeek
                                val dayNum = slotIndex - firstDayOfWeek + 1

                                if (dayNum in 1..daysInMonth) {
                                    val dateStr = String.format(
                                        Locale.US,
                                        "%04d-%02d-%02d",
                                        currentYear,
                                        currentMonth + 1,
                                        dayNum
                                    )
                                    val isSelected = dateStr == selectedDate
                                    val isToday = dateStr == viewModel.todayDateString

                                    val hasMood = moodEntries.any { it.date == dateStr }
                                    val hasJournal = journalEntries.any {
                                        it.createdAt?.toDate()?.let { d -> sdfJournal.format(d) } == dateStr
                                    }
                                    val hasEvent = events.any { it.date == dateStr }
                                    val hasHabit = habits.any { it.completedDates.contains(dateStr) }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> CoralPrimary
                                                    isToday -> CoralLight.copy(alpha = 0.6f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .then(
                                                if (isToday && !isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        CoralPrimary,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { viewModel.selectCalendarDate(dateStr) }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> CoralPrimary
                                                    else -> CharcoalText
                                                }
                                            )

                                            // Indicadores de actividad del día
                                            if (hasMood || hasJournal || hasEvent || hasHabit) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    if (hasEvent) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else TealSecondary)
                                                        )
                                                    }
                                                    if (hasMood) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else CoralPrimary)
                                                        )
                                                    }
                                                    if (hasJournal) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else PurpleAccent)
                                                        )
                                                    }
                                                    if (hasHabit) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color.White else Color(0xFF38A169))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Leyenda de puntos de actividad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(color = TealSecondary, label = "Cita")
                        LegendItem(color = CoralPrimary, label = "Ánimo")
                        LegendItem(color = PurpleAccent, label = "Diario")
                        LegendItem(color = Color(0xFF38A169), label = "Hábito")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cabecera de Actividades de la Fecha Seleccionada
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatReadableSpanishDate(selectedDate),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )
                Text(
                    text = if (selectedDate == viewModel.todayDateString) "Registros y agenda de hoy ($selectedDate)" else "Registros del $selectedDate",
                    fontSize = 12.sp,
                    color = MutedSlate
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenido del Día Seleccionado
            if (!hasAnyActivities) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E0),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No hay citas ni registros en esta fecha",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pulsa '+ Cita' arriba para programar una consulta, sesión de terapia o recordatorio personal.",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Citas y Eventos programados para ese día
                    dayEvents.forEach { ev ->
                        val eventItemId = "agenda_${ev.id}"
                        val isEventRevealed = revealedIds.contains(eventItemId)
                        val catOption = eventCategories.firstOrNull { it.label == ev.type }
                            ?: EventCategoryOption(ev.type.ifBlank { "Cita/Evento" }, Icons.Default.CalendarMonth, TealSecondary, TealLight)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(catOption.bgColor)
                                ) {
                                    Icon(
                                        imageVector = catOption.icon,
                                        contentDescription = null,
                                        tint = catOption.tint,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        PrivacyProtectedText(
                                            realText = ev.title,
                                            itemId = eventItemId,
                                            isStreetModeActive = isStreetModeActive,
                                            isRevealed = isEventRevealed,
                                            isAiDisguiseEnabled = isAiDisguiseEnabled,
                                            aiDisguiseText = aiAliases.agendaAlias,
                                            onRevealClick = { viewModel.revealPrivacyItemTemporarily(eventItemId) },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = CharcoalText,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (ev.time.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = catOption.bgColor
                                            ) {
                                                Text(
                                                    text = ev.time,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = catOption.tint,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isStreetModeActive && !isEventRevealed) "Compromiso personal" else ev.type.ifBlank { "Cita / Evento" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = catOption.tint
                                    )
                                    if (ev.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        PrivacyProtectedText(
                                            realText = ev.notes,
                                            itemId = eventItemId,
                                            isStreetModeActive = isStreetModeActive,
                                            isRevealed = isEventRevealed,
                                            isAiDisguiseEnabled = isAiDisguiseEnabled,
                                            aiDisguiseText = "Nota de agenda privada",
                                            onRevealClick = { viewModel.revealPrivacyItemTemporarily(eventItemId) },
                                            fontSize = 12.sp,
                                            color = MutedSlate
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteCalendarEvent(ev.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar cita",
                                        tint = MutedSlate.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }

                    // Estado de Ánimo del día
                    if (dayMood != null) {
                        val dayMoodItemId = "agenda_mood_${dayMood.id}"
                        val isDayMoodRevealed = revealedIds.contains(dayMoodItemId)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isStreetModeActive && !isDayMoodRevealed) {
                                        viewModel.revealPrivacyItemTemporarily(dayMoodItemId)
                                    } else {
                                        viewModel.selectTab(AppTab.ANIMO)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(CoralLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mood,
                                        contentDescription = null,
                                        tint = CoralPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    PrivacyProtectedText(
                                        realText = "Estado de Ánimo: ${dayMood.levelLabel}",
                                        itemId = dayMoodItemId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isDayMoodRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = aiAliases.moodAlias,
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayMoodItemId) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    if (dayMood.feelings.isNotEmpty() || dayMood.note.isNotBlank()) {
                                        val moodDetails = listOfNotNull(
                                            dayMood.feelings.joinToString(", ").ifBlank { null },
                                            dayMood.note.ifBlank { null }
                                        ).joinToString(" • ")
                                        PrivacyProtectedText(
                                            realText = moodDetails,
                                            itemId = dayMoodItemId,
                                            isStreetModeActive = isStreetModeActive,
                                            isRevealed = isDayMoodRevealed,
                                            isAiDisguiseEnabled = isAiDisguiseEnabled,
                                            aiDisguiseText = "Balance diario registrado",
                                            onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayMoodItemId) },
                                            fontSize = 12.sp,
                                            color = MutedSlate,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Entradas de Diario del día
                    dayJournals.forEach { journal ->
                        val dayJournalId = "agenda_journal_${journal.id}"
                        val isDayJournalRevealed = revealedIds.contains(dayJournalId)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isStreetModeActive && !isDayJournalRevealed) {
                                        viewModel.revealPrivacyItemTemporarily(dayJournalId)
                                    } else {
                                        viewModel.selectTab(AppTab.DIARIO)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PurpleLight)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = PurpleAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    PrivacyProtectedText(
                                        realText = "Diario: ${journal.title}",
                                        itemId = dayJournalId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isDayJournalRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = aiAliases.diaryAlias,
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayJournalId) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    PrivacyProtectedText(
                                        realText = journal.content,
                                        itemId = dayJournalId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isDayJournalRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = "Apunte privado • Toca para ver",
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(dayJournalId) },
                                        fontSize = 12.sp,
                                        color = MutedSlate,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    // Hábitos completados ese día
                    if (dayCompletedHabits.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectTab(AppTab.HABITOS) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC6F6D5))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2F855A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hábitos completados (${dayCompletedHabits.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = CharcoalText
                                    )
                                    Text(
                                        text = dayCompletedHabits.joinToString(", ") { it.name },
                                        fontSize = 12.sp,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showAddEventDialog) {
            AddEventDialog(
                date = selectedDate,
                onDismiss = { showAddEventDialog = false },
                onSave = { title, type, date, time, notes ->
                    viewModel.addCalendarEvent(title, type, date, time, notes)
                    showAddEventDialog = false
                }
            )
        }
    }
}

@Composable
private fun MonthStatChip(
    count: Int,
    label: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MutedSlate,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MutedSlate
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddEventDialog(
    date: String,
    onDismiss: () -> Unit,
    onSave: (title: String, type: String, date: String, time: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00") }
    var notes by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(eventCategories.first().label) }
    var showTimePicker by remember { mutableStateOf(false) }

    val quickTimes = listOf("09:00", "11:00", "16:00", "18:00")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Agendar Cita o Evento",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CharcoalText
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Fecha: ${formatReadableSpanishDate(date)} ($date)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título de la cita o evento") },
                    placeholder = { Text("Ej. Sesión de psicología, Control médico") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Categoría:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(6.dp))

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.tint,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else CharcoalText
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hora de la cita:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSlate
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickTimes.forEach { qTime ->
                        val isSelected = time == qTime
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealSecondary else Color(0xFFEDF2F7))
                                .clickable { time = qTime }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = qTime,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TealSecondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hora seleccionada: $time (Cambiar HH:mm)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o preguntas para la cita (opcional)") },
                    placeholder = { Text("Ej. Llevar resultados, comentar sueño...") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(title.trim(), selectedType, date, time.trim(), notes.trim())
                    }
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                modifier = Modifier.testTag("save_event_button")
            ) {
                Text("Guardar")
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

    if (showTimePicker) {
        EventTimePickerDialog(
            initialTime = time,
            onDismiss = { showTimePicker = false },
            onTimeConfirmed = { newTime ->
                time = newTime
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun EventTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onTimeConfirmed: (String) -> Unit
) {
    val parts = initialTime.split(":")
    var hour by remember { mutableIntStateOf(parts.getOrNull(0)?.toIntOrNull() ?: 10) }
    var minute by remember { mutableIntStateOf(parts.getOrNull(1)?.toIntOrNull() ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Elegir Hora de la Cita",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CharcoalText
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { hour = (hour + 1) % 24 }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir hora", tint = TealSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TealLight,
                        border = BorderStroke(1.5.dp, TealSecondary)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", hour),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(onClick = { hour = (hour - 1 + 24) % 24 }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar hora", tint = TealSecondary)
                    }
                    Text("Hora", fontSize = 11.sp, color = MutedSlate)
                }

                Text(
                    text = ":",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { minute = (minute + 5) % 60 }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir minutos", tint = TealSecondary)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TealLight,
                        border = BorderStroke(1.5.dp, TealSecondary)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d", minute),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                    IconButton(onClick = { minute = (minute - 5 + 60) % 60 }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar minutos", tint = TealSecondary)
                    }
                    Text("Minutos", fontSize = 11.sp, color = MutedSlate)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTimeConfirmed(String.format(Locale.US, "%02d:%02d", hour, minute))
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Text("Confirmar")
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
