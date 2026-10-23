package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Medication
import com.example.ui.components.AiWellnessAssistantCard
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AppViewModel
import java.util.Calendar
import java.util.Locale

@Composable
fun MedicationScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val medications by viewModel.medications.collectAsState()
    val todayLogs by viewModel.todayMedLogs.collectAsState()
    val liveClock by viewModel.liveClockText.collectAsState()
    val nextAlarmInfo by viewModel.nextScheduledAlarmInfo.collectAsState()
    val medicationAiState by viewModel.medicationAiTipState.collectAsState()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()

    // Estado del Programador de Alarma en pantalla
    var selectedMedicationId by remember { mutableStateOf<String?>(null) }
    val selectedMedication = medications.firstOrNull { it.id == selectedMedicationId }

    var nameInput by remember { mutableStateOf("") }
    var dosageInput by remember { mutableStateOf("") }

    val initialCal = remember {
        Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
    }
    var selectedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var savedFeedbackText by remember { mutableStateOf<String?>(null) }

    // Cuando el usuario selecciona una pastilla existente, cargar su nombre, dosis y horario
    LaunchedEffect(selectedMedicationId) {
        val med = medications.firstOrNull { it.id == selectedMedicationId }
        if (med != null) {
            nameInput = med.name
            dosageInput = med.dosage
            val firstTime = med.scheduledTimes.firstOrNull()
            val parts = firstTime?.split(":")
            if (parts != null && parts.size == 2) {
                selectedHour = parts[0].toIntOrNull() ?: selectedHour
                selectedMinute = parts[1].toIntOrNull() ?: selectedMinute
            }
        } else {
            nameInput = ""
            dosageInput = ""
        }
    }

    val formattedAlarmTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Cabecera Limpia con Hora Actual
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pastillas y Alarmas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                letterSpacing = (-0.4).sp,
                modifier = Modifier.testTag("pastillas_header_title")
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TealSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = liveClock,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Programador de Alarma Directo y Simple
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("alarm_programmer_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TealLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Programador de Alarma",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }

                    Text(
                        text = nextAlarmInfo,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector rápido de pastilla existente o "+ Nueva"
                if (medications.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedMedicationId == null) TealSecondary else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable {
                                selectedMedicationId = null
                                savedFeedbackText = null
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (selectedMedicationId == null) Color.White else CharcoalText,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nueva pastilla",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedMedicationId == null) Color.White else CharcoalText
                                )
                            }
                        }

                        medications.forEach { med ->
                            val isSelected = selectedMedicationId == med.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) TealSecondary else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    selectedMedicationId = med.id
                                    savedFeedbackText = null
                                }
                            ) {
                                Text(
                                    text = med.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else CharcoalText,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Campos simples de Nombre y Dosis en una sola fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            savedFeedbackText = null
                        },
                        label = { Text("Pastilla", fontSize = 12.sp) },
                        placeholder = { Text("Ej. Efexor", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("medication_name_input")
                    )

                    OutlinedTextField(
                        value = dosageInput,
                        onValueChange = {
                            dosageInput = it
                            savedFeedbackText = null
                        },
                        label = { Text("Dosis", fontSize = 12.sp) },
                        placeholder = { Text("Ej. 75mg", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("medication_dosage_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector Digital de Hora y Minuto (HH : MM)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Hora (00-23)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        selectedHour = (selectedHour + 1) % 24
                                        savedFeedbackText = null
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("hour_up_button")
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Subir hora",
                                        tint = TealSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.2.dp, TealSecondary)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d", selectedHour),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalText,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedHour = (selectedHour - 1 + 24) % 24
                                        savedFeedbackText = null
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("hour_down_button")
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Bajar hora",
                                        tint = TealSecondary
                                    )
                                }
                            }

                            Text(
                                text = ":",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            // Minuto (00-59)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        selectedMinute = (selectedMinute + 1) % 60
                                        savedFeedbackText = null
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("minute_up_button")
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Subir minuto",
                                        tint = TealSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.2.dp, TealSecondary)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d", selectedMinute),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CharcoalText,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        selectedMinute = (selectedMinute - 1 + 60) % 60
                                        savedFeedbackText = null
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("minute_down_button")
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Bajar minuto",
                                        tint = TealSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Botones rápidos de hora
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickTimeChip(
                                label = "Ahora",
                                onClick = {
                                    val now = Calendar.getInstance()
                                    selectedHour = now.get(Calendar.HOUR_OF_DAY)
                                    selectedMinute = now.get(Calendar.MINUTE)
                                    savedFeedbackText = null
                                }
                            )
                            QuickTimeChip(
                                label = "+1 min",
                                onClick = {
                                    val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 1) }
                                    selectedHour = cal.get(Calendar.HOUR_OF_DAY)
                                    selectedMinute = cal.get(Calendar.MINUTE)
                                    savedFeedbackText = null
                                }
                            )
                            QuickTimeChip(
                                label = "08:00",
                                onClick = {
                                    selectedHour = 8
                                    selectedMinute = 0
                                    savedFeedbackText = null
                                }
                            )
                            QuickTimeChip(
                                label = "14:00",
                                onClick = {
                                    selectedHour = 14
                                    selectedMinute = 0
                                    savedFeedbackText = null
                                }
                            )
                            QuickTimeChip(
                                label = "21:00",
                                onClick = {
                                    selectedHour = 21
                                    selectedMinute = 0
                                    savedFeedbackText = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Principal de Guardar y Activar Alarma
                val finalName = nameInput.trim().ifBlank { selectedMedication?.name ?: "" }
                val finalDosage = dosageInput.trim().ifBlank { selectedMedication?.dosage ?: "1 dosis" }

                Button(
                    onClick = {
                        val targetName = finalName.ifBlank { "Mi Pastilla" }
                        viewModel.programMedicationAlarm(
                            existingMed = selectedMedication,
                            name = targetName,
                            dosage = finalDosage,
                            timeStr = formattedAlarmTime
                        )
                        savedFeedbackText = "Alarma programada: $targetName a las $formattedAlarmTime"
                        if (selectedMedication == null) {
                            nameInput = ""
                            dosageInput = ""
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_medication_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedMedication != null) {
                            "Actualizar Alarma ($formattedAlarmTime)"
                        } else {
                            "Programar Alarma ($formattedAlarmTime)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (savedFeedbackText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF2F855A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = savedFeedbackText ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2F855A)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Lista Única y Limpia de Pastillas
        Text(
            text = "Mis Pastillas Programadas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (medications.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E0),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Escribe tu pastilla arriba, elige la hora y pulsa 'Programar Alarma'.",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            medications.forEach { med ->
                val medItemId = "med_${med.id}"
                val isMedRevealed = revealedIds.contains(medItemId)
                val primaryTime = med.scheduledTimes.firstOrNull() ?: "08:00"
                val isTakenToday = todayLogs.any { it.medicationId == med.id && it.scheduledTime == primaryTime }
                val isSelectedInProgrammer = selectedMedicationId == med.id

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTakenToday) Color(0xFFF0FFF4) else Color.White
                    ),
                    border = if (isSelectedInProgrammer) {
                        BorderStroke(1.5.dp, TealSecondary)
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Checkbox(
                            checked = isTakenToday,
                            onCheckedChange = {
                                viewModel.toggleMedicationTaken(med, primaryTime, isTakenToday)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TealSecondary,
                                checkmarkColor = Color.White
                            )
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedMedicationId = med.id
                                    savedFeedbackText = null
                                }
                        ) {
                            PrivacyProtectedText(
                                realText = "${med.name} (${med.dosage})",
                                itemId = medItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isMedRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = aiAliases.pillAlias,
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(medItemId) },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isTakenToday) Color(0xFF276749) else CharcoalText
                            )
                            Text(
                                text = if (isTakenToday) "Tomada hoy" else "Toca la hora para cambiar alarma",
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }

                        // Chip de Hora (tocar para cargar en el programador de arriba)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isTakenToday) Color(0xFFC6F6D5) else TealLight,
                            modifier = Modifier.clickable {
                                selectedMedicationId = med.id
                                savedFeedbackText = null
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = if (isTakenToday) Color(0xFF22543D) else TealSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = med.scheduledTimes.joinToString(", "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTakenToday) Color(0xFF22543D) else TealSecondary
                                )
                            }
                        }

                        // Botón para hacer sonar la alarma de esta pastilla ahora mismo
                        IconButton(
                            onClick = { viewModel.triggerAlarmForMedication(med) },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("ring_med_alarm_${med.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Sonar alarma de ${med.name}",
                                tint = CoralPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Botón para eliminar pastilla
                        IconButton(
                            onClick = {
                                if (selectedMedicationId == med.id) {
                                    selectedMedicationId = null
                                }
                                viewModel.deleteMedication(med.id)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar ${med.name}",
                                tint = MutedSlate.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Orientación IA Compacta al pie
        AiWellnessAssistantCard(
            title = "Orientación de Tomas",
            subtitle = "Consejos breves para tus horarios.",
            buttonLabel = "Consejo",
            state = medicationAiState,
            onGenerateClick = { viewModel.generateMedicationAiGuidance() },
            accentColor = TealSecondary,
            accentBgColor = TealLight,
            testTag = "medication_ai_card"
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun QuickTimeChip(
    label: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFEDF2F7),
            contentColor = CharcoalText
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 1.dp, minHeight = 26.dp)
            .height(26.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
