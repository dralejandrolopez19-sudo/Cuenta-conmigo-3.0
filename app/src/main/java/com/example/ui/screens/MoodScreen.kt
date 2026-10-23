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
import androidx.compose.material.icons.filled.Check
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
import com.example.ui.components.AiWellnessAssistantCard
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
    "Calma", "Esperanza", "Gratitud", "Motivación",
    "Ansiedad", "Tensión", "Cansancio", "Tristeza"
)

enum class MoodViewMode(val title: String) {
    REGISTRO_DIARIO("Diario"),
    ESCALAS_CLINICAS("Escalas")
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
    val moodAiState by viewModel.moodAiInsightState.collectAsState()
    val todayMood = moodEntries.firstOrNull { it.date == viewModel.todayDateString }

    var selectedLevel by remember(todayMood) { mutableIntStateOf(todayMood?.level ?: 3) }
    val selectedFeelings = remember(todayMood) {
        mutableStateListOf<String>().apply {
            if (todayMood != null && todayMood.feelings.isNotEmpty()) {
                addAll(todayMood.feelings)
            } else {
                addAll(listOf("Calma"))
            }
        }
    }
    var noteText by remember(todayMood) { mutableStateOf(todayMood?.note ?: "") }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    var evaValue by remember { mutableFloatStateOf(7f) }
    var evaSuccessMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Cabecera Minimalista + Selector en una fila
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ánimo",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                letterSpacing = (-0.4).sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEBE6DF))
                    .padding(3.dp)
            ) {
                MoodViewMode.entries.forEach { mode ->
                    val isSelected = activeViewMode == mode
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { activeViewMode = mode }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("mood_mode_${mode.name}")
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TealSecondary else MutedSlate
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeViewMode == MoodViewMode.REGISTRO_DIARIO) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mood_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "¿Cómo te sientes hoy?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5 Emojis
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
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) opt.color.copy(alpha = 0.14f)
                                            else Color(0xFFF7FAFC)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) opt.color else Color(0xFFE2E8F0),
                                            shape = CircleShape
                                        )
                                ) {
                                    Text(text = opt.emoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 8 Etiquetas Concisas
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableFeelings.forEach { feeling ->
                            val isSelected = selectedFeelings.contains(feeling)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) TealSecondary else Color(0xFFF0F4F8))
                                    .clickable {
                                        if (isSelected) selectedFeelings.remove(feeling)
                                        else selectedFeelings.add(feeling)
                                    }
                                    .padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .padding(end = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = feeling,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else CharcoalText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Nota breve (opcional)", fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_mood_button")
                    ) {
                        Text(
                            text = if (todayMood != null) "Actualizar ánimo" else "Guardar ánimo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (saveSuccessMessage) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Registro guardado",
                            color = TealSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (moodEntries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Recientes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                moodEntries.take(5).forEach { item ->
                    val moodItemId = "mood_${item.id}"
                    val isItemRevealed = revealedIds.contains(moodItemId)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            val opt = moodOptions.firstOrNull { it.level == item.level }
                            Text(
                                text = if (isStreetModeActive && !isItemRevealed) "🔒" else (opt?.emoji ?: "😐"),
                                fontSize = 22.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
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
                                        fontSize = 13.sp,
                                        color = opt?.color ?: CharcoalText,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.date,
                                        fontSize = 11.sp,
                                        color = MutedSlate
                                    )
                                }
                                if (item.feelings.isNotEmpty() || item.note.isNotBlank()) {
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
                                        aiDisguiseText = "Registro diario",
                                        onRevealClick = { viewModel.revealPrivacyItemTemporarily(moodItemId) },
                                        fontSize = 11.sp,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // === VISTA 2: ESCALAS CLÍNICAS SIMPLIFICADAS ===
            val lastPhq9 = clinicalAssessments.firstOrNull { it.scaleType == "PHQ-9" }
            val lastGad7 = clinicalAssessments.firstOrNull { it.scaleType == "GAD-7" }

            // PHQ-9
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_phq9")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PHQ-9 • Estado de ánimo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = if (lastPhq9 != null) "Último: ${lastPhq9.score}/27 (${lastPhq9.severityLevel})" else "9 preguntas breves",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }
                    Button(
                        onClick = { activeScaleDialog = ScaleType.PHQ9 },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("start_phq9_button")
                    ) {
                        Text("Evaluar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GAD-7
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_gad7")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GAD-7 • Ansiedad",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = if (lastGad7 != null) "Último: ${lastGad7.score}/21 (${lastGad7.severityLevel})" else "7 preguntas breves",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }
                    Button(
                        onClick = { activeScaleDialog = ScaleType.GAD7 },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("start_gad7_button")
                    ) {
                        Text("Evaluar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // EVA
            val currentEvaScore = evaValue.roundToInt()
            val evaResult = ClinicalScales.evaluateEVA(currentEvaScore)

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scale_card_eva")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Termómetro EVA (0 - 10)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = evaResult.badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$currentEvaScore/10 • ${evaResult.severity}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = evaResult.badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = if (evaSuccessMessage) "✓ Guardado ($currentEvaScore/10)" else "Guardar EVA ($currentEvaScore/10)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (clinicalAssessments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Evaluaciones recientes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                clinicalAssessments.take(5).forEach { item ->
                    val scaleItemId = "scale_${item.id}"
                    val isScaleRevealed = revealedIds.contains(scaleItemId)
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PrivacyProtectedText(
                                realText = "${item.scaleType}: ${item.score}/${item.maxScore} (${item.severityLevel})",
                                itemId = scaleItemId,
                                isStreetModeActive = isStreetModeActive,
                                isRevealed = isScaleRevealed,
                                isAiDisguiseEnabled = isAiDisguiseEnabled,
                                aiDisguiseText = aiAliases.moodAlias,
                                onRevealClick = { viewModel.revealPrivacyItemTemporarily(scaleItemId) },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = CharcoalText,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = item.date,
                                fontSize = 11.sp,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // IA compacta al final
        AiWellnessAssistantCard(
            title = "Análisis Emocional",
            subtitle = "Patrones e ideas de regulación emocional.",
            buttonLabel = "Analizar",
            state = moodAiState,
            onGenerateClick = { viewModel.generateMoodAiAnalysis() },
            accentColor = TealSecondary,
            accentBgColor = TealLight,
            testTag = "mood_ai_assistant_card"
        )

        Spacer(modifier = Modifier.height(28.dp))
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
