package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.theme.CharcoalText
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
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    data class DoseItem(
        val medication: Medication,
        val time: String,
        val isTaken: Boolean
    )

    val todayDoses = medications.flatMap { med ->
        med.scheduledTimes.map { time ->
            val isTaken = todayLogs.any { it.medicationId == med.id && it.scheduledTime == time }
            DoseItem(med, time, isTaken)
        }
    }.sortedBy { it.time }

    val totalDoses = todayDoses.size
    val takenDoses = todayDoses.count { it.isTaken }
    val progress = if (totalDoses > 0) takenDoses.toFloat() / totalDoses else 0f
    val progressPercent = (progress * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Encabezado Principal: "Pastillas"
        Text(
            text = "Pastillas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            letterSpacing = (-0.4).sp,
            modifier = Modifier.testTag("pastillas_header_title")
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Control diario de tomas y alarmas.",
                fontSize = 13.sp,
                color = MutedSlate,
                modifier = Modifier.weight(1f, fill = false)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.triggerTestMedicationAlarm() },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFC53030)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC53030)),
                    modifier = Modifier.testTag("test_alarm_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Probar alarma",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Alarma", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                    modifier = Modifier.testTag("add_medication_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tarjeta informativa de Alarma Persistente
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5)),
            border = BorderStroke(1.dp, Color(0xFFFED7D7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFED7D7))
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFC53030),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarma Activa de Pastillas",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9B2C2C)
                    )
                    Text(
                        text = "La alarma sonará y vibrará a la hora programada y NO se apagará hasta que confirmes las pastillas que debes tomar.",
                        fontSize = 12.sp,
                        color = Color(0xFF742A2A),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tarjeta de Progreso de Hoy
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TealLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tomas de Hoy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = if (totalDoses > 0) "$takenDoses de $totalDoses tomas completadas" else "Sin tomas programadas",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }
                    }

                    Text(
                        text = "$progressPercent%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = TealSecondary,
                    trackColor = Color(0xFFEDF2F7)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Mantener tus horarios al día cuida tu bienestar y estabilidad diaria.",
                    fontSize = 12.sp,
                    color = MutedSlate,
                    lineHeight = 17.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Checklist de Hoy
        if (todayDoses.isNotEmpty()) {
            Text(
                text = "Horarios y Tomas Programadas",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            todayDoses.forEach { dose ->
                val doseItemId = "dose_${dose.medication.id}_${dose.time}"
                val isDoseRevealed = revealedIds.contains(doseItemId)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (dose.isTaken) Color(0xFFF0FFF4) else Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isStreetModeActive && !isDoseRevealed) {
                                    viewModel.revealPrivacyItemTemporarily(doseItemId)
                                } else {
                                    viewModel.toggleMedicationTaken(dose.medication, dose.time, dose.isTaken)
                                }
                            }
                            .padding(14.dp)
                    ) {
                        Checkbox(
                            checked = dose.isTaken,
                            onCheckedChange = {
                                viewModel.toggleMedicationTaken(dose.medication, dose.time, dose.isTaken)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TealSecondary,
                                checkmarkColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            PrivacyProtectedText(
                                realText = "${dose.medication.name} (${dose.medication.dosage})",
                                itemId = doseItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isDoseRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = aiAliases.pillAlias,
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(doseItemId) },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dose.isTaken) Color(0xFF276749) else CharcoalText
                            )
                            if (dose.medication.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                PrivacyProtectedText(
                                    realText = dose.medication.notes,
                                    itemId = doseItemId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isDoseRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = "Hábito diario programado",
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(doseItemId) },
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dose.isTaken) Color(0xFFC6F6D5) else TealLight)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = dose.time,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dose.isTaken) Color(0xFF22543D) else TealSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Lista de Pastillas
        Text(
            text = "Mis Pastillas",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (medications.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E0),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No tienes pastillas registradas aún.\nToca '+ Añadir' para configurar tus pastillas y horarios de alarma.",
                        fontSize = 13.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                }
            }
        } else {
            medications.forEach { med ->
                val medItemId = "med_${med.id}"
                val isMedRevealed = revealedIds.contains(medItemId)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            PrivacyProtectedText(
                                realText = med.name,
                                itemId = medItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isMedRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = aiAliases.pillAlias,
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(medItemId) },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            PrivacyProtectedText(
                                realText = "Dosis: ${med.dosage}",
                                itemId = medItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isMedRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = "Frecuencia diaria programada",
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(medItemId) },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TealSecondary
                            )
                            if (med.scheduledTimes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Alarmas: ${med.scheduledTimes.joinToString(", ")}",
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                            if (med.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                PrivacyProtectedText(
                                    realText = "Indicaciones: ${med.notes}",
                                    itemId = medItemId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isMedRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = "Nota privada protegida",
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(medItemId) },
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.deleteMedication(med.id) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar pastilla",
                                tint = MutedSlate.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, dosage, times, notes ->
                viewModel.addMedication(name, dosage, times, notes)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, dosage: String, times: List<String>, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val times = remember { mutableStateListOf("08:00") }
    var showCustomTimePicker by remember { mutableStateOf(false) }

    val presetTimes = listOf("08:00", "14:00", "20:00", "22:00")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Registrar Pastilla",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CharcoalText
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la pastilla") },
                    placeholder = { Text("Ej. Sertralina, Vitamina D") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosis") },
                    placeholder = { Text("Ej. 50 mg, 1 pastilla") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("medication_dosage_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Horarios rápidos:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MutedSlate
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetTimes.forEach { timeStr ->
                        val isSelected = times.contains(timeStr)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TealSecondary else Color(0xFFEDF2F7))
                                .clickable {
                                    if (isSelected) times.remove(timeStr)
                                    else times.add(timeStr)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = timeStr,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else CharcoalText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón para abrir selector de hora libre (HH:mm)
                OutlinedButton(
                    onClick = { showCustomTimePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TealSecondary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealSecondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_custom_time_picker_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Seleccionar hora libre",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Elegir hora libre (HH:mm)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (times.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Alarmas seleccionadas:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MutedSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        times.sorted().forEach { selectedTime ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = TealLight,
                                border = BorderStroke(1.dp, TealSecondary.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { times.remove(selectedTime) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = TealSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedTime,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TealSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Quitar horario $selectedTime",
                                        tint = TealSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Indicaciones (opcional)") },
                    placeholder = { Text("Ej. Tomar con agua después de comer") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dosage.isNotBlank()) {
                        val finalTimes = if (times.isEmpty()) listOf("08:00") else times.distinct().sorted()
                        onSave(name.trim(), dosage.trim(), finalTimes, notes.trim())
                    }
                },
                enabled = name.isNotBlank() && dosage.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                modifier = Modifier.testTag("save_medication_button")
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

    if (showCustomTimePicker) {
        CustomTimePickerDialog(
            onDismiss = { showCustomTimePicker = false },
            onTimeSelected = { formattedTime ->
                if (!times.contains(formattedTime)) {
                    times.add(formattedTime)
                }
                showCustomTimePicker = false
            }
        )
    }
}

@Composable
private fun CustomTimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val now = remember { Calendar.getInstance() }
    var selectedHour by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Seleccionar Hora de Alarma",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = CharcoalText
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Ajusta la hora y los minutos exactos (formato 24h):",
                    fontSize = 13.sp,
                    color = MutedSlate,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selector de Hora (00 - 23)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { selectedHour = (selectedHour + 1) % 24 },
                            modifier = Modifier.testTag("hour_up_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir hora", tint = TealSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TealLight,
                            border = BorderStroke(1.5.dp, TealSecondary)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", selectedHour),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        IconButton(
                            onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                            modifier = Modifier.testTag("hour_down_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar hora", tint = TealSecondary)
                        }
                        Text("Horas", fontSize = 11.sp, color = MutedSlate)
                    }

                    Text(
                        text = ":",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Selector de Minutos (00 - 59)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = { selectedMinute = (selectedMinute + 1) % 60 },
                            modifier = Modifier.testTag("minute_up_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Subir minuto", tint = TealSecondary)
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TealLight,
                            border = BorderStroke(1.5.dp, TealSecondary)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d", selectedMinute),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }
                        IconButton(
                            onClick = { selectedMinute = (selectedMinute - 1 + 60) % 60 },
                            modifier = Modifier.testTag("minute_down_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Bajar minuto", tint = TealSecondary)
                        }
                        Text("Minutos", fontSize = 11.sp, color = MutedSlate)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                    onTimeSelected(formatted)
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                modifier = Modifier.testTag("confirm_custom_time_button")
            ) {
                Text("Añadir Hora")
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
