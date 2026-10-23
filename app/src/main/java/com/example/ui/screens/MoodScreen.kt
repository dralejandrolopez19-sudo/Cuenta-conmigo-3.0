package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.components.PrivacyProtectedText
import com.example.ui.viewmodel.AppViewModel
import com.example.util.ClinicalScales
import kotlin.math.roundToInt

data class MoodOption(
    val level: Int,
    val label: String,
    val emoji: String,
    val color: Color
)

val moodOptions = listOf(
    MoodOption(1, "Muy bajo", "😢", Color(0xFFE53E3E)),
    MoodOption(2, "Desanimado", "😔", Color(0xFFDD6B20)),
    MoodOption(3, "Estable", "😐", TealSecondary),
    MoodOption(4, "Bien", "🙂", Color(0xFF38A169)),
    MoodOption(5, "Muy bien", "😄", Color(0xFF319795))
)

val availableFeelings = listOf(
    "Calma", "Esperanza", "Paz interior", "Gratitud", "Motivación",
    "Preocupaciones constantes", "Mente acelerada", "Dificultad para concentrarme",
    "Tensión en el cuerpo", "Difícil relajarme", "Inquietud", "Irritabilidad",
    "Poco interés o desgano", "Cansancio físico", "Autoexigencia", "Soledad"
)

enum class MoodViewMode(val title: String) {
    REGISTRO_DIARIO("Registro Diario"),
    ESCALAS_CLINICAS("Escalas Clínicas")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    var activeViewMode by remember { mutableStateOf(MoodViewMode.REGISTRO_DIARIO) }
    var activeScaleDialog by remember { mutableStateOf<ScaleType?>(null) }

    val moodEntries by viewModel.moodEntries.collectAsState()
    val clinicalAssessments by viewModel.clinicalAssessments.collectAsState()
    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val revealedIds by viewModel.temporarilyRevealedIds.collectAsState()
    val todayMood = moodEntries.firstOrNull { it.date == viewModel.todayDateString }

    var selectedLevel by remember(todayMood) { mutableIntStateOf(todayMood?.level ?: 3) }
    val selectedFeelings = remember(todayMood) {
        mutableStateListOf<String>().apply {
            if (todayMood != null && todayMood.feelings.isNotEmpty()) {
                addAll(todayMood.feelings)
            } else {
                addAll(listOf("Calma", "Esperanza"))
            }
        }
    }
    var noteText by remember(todayMood) { mutableStateOf(todayMood?.note ?: "") }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    // EVA State
    var evaValue by remember { mutableFloatStateOf(7f) }
    var evaSuccessMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header
        Text(
            text = "Mi Estado de Ánimo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Un espacio sereno y confidencial para escucharte y monitorear tu salud mental.",
            fontSize = 13.sp,
            color = MutedSlate,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Minimalist Segmented Pill Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEBE6DF))
                .padding(4.dp)
        ) {
            MoodViewMode.entries.forEach { mode ->
                val isSelected = activeViewMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { activeViewMode = mode }
                        .padding(vertical = 10.dp)
                        .testTag("mood_mode_${mode.name}")
                ) {
                    Text(
                        text = mode.title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TealSecondary else MutedSlate
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeViewMode == MoodViewMode.REGISTRO_DIARIO) {
            // === VISTA 1: REGISTRO DIARIO HABITUAL ===
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Question
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TealLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "¿Cómo te sientes en este momento?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                            Text(
                                text = "Elige la opción que mejor describa tu sentir actual",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 5 Mood Emoji Buttons (Responsivo para todo ancho de pantalla)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        moodOptions.forEach { opt ->
                            val isSelected = selectedLevel == opt.level
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedLevel = opt.level }
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) opt.color.copy(alpha = 0.12f)
                                            else Color(0xFFF7FAFC)
                                        )
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) opt.color else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        )
                                ) {
                                    Text(
                                        text = opt.emoji,
                                        fontSize = 24.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = opt.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) opt.color else MutedSlate,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Feelings & Sensations
                    Text(
                        text = "¿Qué sensaciones y emociones acompañan tu día?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableFeelings.forEach { feeling ->
                            val isSelected = selectedFeelings.contains(feeling)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) TealSecondary else Color(0xFFF0F4F8)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            selectedFeelings.remove(feeling)
                                        } else {
                                            selectedFeelings.add(feeling)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .padding(end = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = feeling,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else CharcoalText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notes
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Apunte o reflexión adicional (opcional)") },
                        placeholder = {
                            Text(
                                text = "¿Hay algo en particular que haya acompañado este sentir?",
                                fontSize = 12.sp,
                                color = MutedSlate
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val label = moodOptions.firstOrNull { it.level == selectedLevel }?.label ?: "Estable"
                            viewModel.saveMood(
                                level = selectedLevel,
                                label = label,
                                feelings = selectedFeelings.toList(),
                                note = noteText.trim()
                            )
                            saveSuccessMessage = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_mood_button")
                    ) {
                        Text(
                            text = if (todayMood != null) "Actualizar registro de hoy" else "Guardar registro de hoy",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    if (saveSuccessMessage) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "✓ Tu registro de bienestar ha sido guardado exitosamente.",
                            color = TealSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Moods History
            if (moodEntries.isNotEmpty()) {
                Text(
                    text = "Historial Reciente de Ánimo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                moodEntries.take(7).forEach { item ->
                    val moodItemId = "mood_${item.id}"
                    val isItemRevealed = revealedIds.contains(moodItemId)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            val opt = moodOptions.firstOrNull { it.level == item.level }
                            Text(
                                text = if (isStreetModeActive && !isItemRevealed) "🔒" else (opt?.emoji ?: "😐"),
                                fontSize = 26.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PrivacyProtectedText(
                                        realText = item.levelLabel,
                                        itemId = moodItemId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isItemRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = aiAliases.moodAlias,
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(moodItemId) },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = opt?.color ?: CharcoalText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.date,
                                        fontSize = 12.sp,
                                        color = MutedSlate
                                    )
                                }
                                if (item.feelings.isNotEmpty() || item.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val detailText = buildString {
                                        if (item.feelings.isNotEmpty()) append(item.feelings.joinToString(" • "))
                                        if (item.note.isNotBlank()) {
                                            if (isNotEmpty()) append(" — ")
                                            append(item.note)
                                        }
                                    }
                                    PrivacyProtectedText(
                                        realText = detailText,
                                        itemId = moodItemId,
                                        isStreetModeActive = isStreetModeActive,
                                        isRevealed = isItemRevealed,
                                        isAiDisguiseEnabled = isAiDisguiseEnabled,
                                        aiDisguiseText = "Registro de rutina diaria",
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(moodItemId) },
                                        fontSize = 12.sp,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === VISTA 2: ESCALAS CLÍNICAS (PHQ-9, GAD-7, EVA) ===
            val lastPhq9 = clinicalAssessments.firstOrNull { it.scaleType == "PHQ-9" }
            val lastGad7 = clinicalAssessments.firstOrNull { it.scaleType == "GAD-7" }

            // Intro Header
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TealLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = TealSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Instrumentos Clínicos Validados",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = "Cuestionarios médicos estandarizados para evaluar síntomas y guiar tu progreso con precisión.",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scale 1: PHQ-9 (Depresión)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_phq9")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PHQ-9 • Depresión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TealLight
                        ) {
                            Text(
                                text = "9 preguntas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TealSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Cuestionario de Salud del Paciente para tamizaje y monitoreo de la severidad del estado de ánimo durante las últimas 2 semanas.",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last score info
                    if (lastPhq9 != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF7FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Último: ${lastPhq9.score}/27 (${lastPhq9.severityLevel})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = lastPhq9.date,
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { activeScaleDialog = ScaleType.PHQ9 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_phq9_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lastPhq9 != null) "Repetir evaluación PHQ-9" else "Realizar evaluación PHQ-9",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scale 2: GAD-7 (Ansiedad)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_gad7")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GAD-7 • Ansiedad",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEFCBF)
                        ) {
                            Text(
                                text = "7 preguntas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB7791F),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Escala de Ansiedad Generalizada para evaluar preocupación constante, inquietud motora y tensión nerviosa en las últimas 2 semanas.",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last score info
                    if (lastGad7 != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF7FAFC),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Último: ${lastGad7.score}/21 (${lastGad7.severityLevel})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CharcoalText
                                )
                                Text(
                                    text = lastGad7.date,
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = { activeScaleDialog = ScaleType.GAD7 },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("start_gad7_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lastGad7 != null) "Repetir evaluación GAD-7" else "Realizar evaluación GAD-7",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scale 3: EVA (Escala Visual Analógica Emocional 0-10)
            val currentEvaScore = evaValue.roundToInt()
            val evaResult = ClinicalScales.evaluateEVA(currentEvaScore)

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_eva")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = TealSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EVA • Termómetro de Bienestar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = evaResult.badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$currentEvaScore / 10",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = evaResult.badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "¿Cómo calificarías tu nivel de bienestar emocional general en este instante? (0 = Malestar máximo, 10 = Máximo bienestar)",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = evaValue,
                        onValueChange = {
                            evaValue = it
                            evaSuccessMessage = false
                        },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = evaResult.badgeColor,
                            activeTrackColor = evaResult.badgeColor,
                            inactiveTrackColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0: Malestar", fontSize = 11.sp, color = MutedSlate)
                        Text(
                            text = evaResult.severity,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = evaResult.badgeColor
                        )
                        Text(text = "10: Bienestar", fontSize = 11.sp, color = MutedSlate)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            viewModel.saveClinicalAssessment(
                                scaleType = "EVA",
                                scaleName = "EVA • Termómetro Emocional",
                                score = currentEvaScore,
                                maxScore = 10,
                                severityLevel = evaResult.severity,
                                interpretation = evaResult.interpretation,
                                answers = listOf(currentEvaScore),
                                triggerSupport = evaResult.triggerSupport
                            )
                            evaSuccessMessage = true
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Guardar nivel EVA ($currentEvaScore / 10)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (evaSuccessMessage) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "✓ Medición EVA registrada correctamente.",
                            color = TealSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clinical Scale History
            Text(
                text = "Historial de Evaluaciones Clínicas",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (clinicalAssessments.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MutedSlate,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aún no has registrado evaluaciones clínicas.",
                            fontSize = 13.sp,
                            color = MutedSlate
                        )
                    }
                }
            } else {
                clinicalAssessments.take(10).forEach { item ->
                    val scaleItemId = "scale_${item.id}"
                    val isScaleRevealed = revealedIds.contains(scaleItemId)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PrivacyProtectedText(
                                    realText = item.scaleName.ifBlank { item.scaleType },
                                    itemId = scaleItemId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isScaleRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = aiAliases.moodAlias,
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(scaleItemId) },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CharcoalText,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.date,
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            PrivacyProtectedText(
                                realText = "${item.score} / ${item.maxScore} • ${item.severityLevel}",
                                itemId = scaleItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isScaleRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = "Balance general completado",
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(scaleItemId) },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealSecondary
                            )

                            if (item.interpretation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                PrivacyProtectedText(
                                    realText = item.interpretation,
                                    itemId = scaleItemId,
                                    isStreetModeActive = isStreetModeActive,
                                    isRevealed = isScaleRevealed,
                                    isAiDisguiseEnabled = isAiDisguiseEnabled,
                                    aiDisguiseText = "Toca para mostrar resultado clínico por 5 segundos",
                                    onRevealClick = { viewModel.revealPrivacyItemTemporarily(scaleItemId) },
                                    fontSize = 12.sp,
                                    color = MutedSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Modal Dialog when taking an assessment
    activeScaleDialog?.let { scaleType ->
        ClinicalScaleDialog(
            scaleType = scaleType,
            viewModel = viewModel,
            onDismiss = { activeScaleDialog = null }
        )
    }
}
