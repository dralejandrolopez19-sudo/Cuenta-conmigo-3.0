package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AppViewModel
import com.example.util.ClinicalScales
import com.example.util.ScaleQuestion
import com.example.util.ScaleResult

enum class ScaleType {
    PHQ9,
    GAD7
}

@Composable
fun ClinicalScaleDialog(
    scaleType: ScaleType,
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val title = when (scaleType) {
        ScaleType.PHQ9 -> "PHQ-9 • Depresión"
        ScaleType.GAD7 -> "GAD-7 • Ansiedad"
    }

    val subtitle = when (scaleType) {
        ScaleType.PHQ9 -> "Cuestionario sobre la Salud del Paciente"
        ScaleType.GAD7 -> "Escala de Ansiedad Generalizada"
    }

    val questions: List<ScaleQuestion> = when (scaleType) {
        ScaleType.PHQ9 -> ClinicalScales.PHQ9_QUESTIONS
        ScaleType.GAD7 -> ClinicalScales.GAD7_QUESTIONS
    }

    val answers = remember {
        mutableStateListOf<Int>().apply {
            repeat(questions.size) { add(-1) }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    var scaleResult by remember { mutableStateOf<ScaleResult?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SoftCreamBackground,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("clinical_scale_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Bar: Title & Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MutedSlate
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scale_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MutedSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isCompleted) {
                    // Progress Bar
                    val progress = (currentIndex + 1).toFloat() / questions.size.toFloat()
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = TealSecondary,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pregunta ${currentIndex + 1} de ${questions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TealSecondary
                        )
                        Text(
                            text = "Últimas 2 semanas",
                            fontSize = 12.sp,
                            color = MutedSlate
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Question Content with Animation
                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "question_transition"
                    ) { index ->
                        val question = questions[index]
                        Column {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "¿Con qué frecuencia has tenido molestias por el siguiente motivo?",
                                        fontSize = 12.sp,
                                        color = MutedSlate,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    Text(
                                        text = question.text,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = CharcoalText,
                                        lineHeight = 22.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Frequency Options
                            ClinicalScales.STANDARD_FREQUENCY_OPTIONS.forEach { opt ->
                                val isSelected = answers[index] == opt.value
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) TealLight else Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) TealSecondary else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            answers[index] = opt.value
                                        }
                                        .testTag("scale_option_${opt.value}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                            .fillMaxWidth()
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isSelected) TealSecondary else MutedSlate,
                                                    shape = CircleShape
                                                )
                                                .background(if (isSelected) TealSecondary else Color.Transparent)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = opt.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) CharcoalText else CharcoalText
                                            )
                                            Text(
                                                text = when (opt.value) {
                                                    0 -> "0 días"
                                                    1 -> "Menos de una semana (1-7 días)"
                                                    2 -> "Más de una semana (7-11 días)"
                                                    else -> "Prácticamente a diario (12-14 días)"
                                                },
                                                fontSize = 11.sp,
                                                color = MutedSlate
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentIndex > 0) {
                            OutlinedButton(
                                onClick = { currentIndex-- },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Anterior")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        val canProceed = answers[currentIndex] >= 0
                        val isLast = currentIndex == questions.size - 1

                        Button(
                            onClick = {
                                if (isLast) {
                                    val finalAnswers = answers.toList()
                                    val result = when (scaleType) {
                                        ScaleType.PHQ9 -> ClinicalScales.evaluatePHQ9(finalAnswers)
                                        ScaleType.GAD7 -> ClinicalScales.evaluateGAD7(finalAnswers)
                                    }
                                    scaleResult = result
                                    isCompleted = true
                                } else {
                                    currentIndex++
                                }
                            },
                            enabled = canProceed,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLast) CoralPrimary else TealSecondary
                            ),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("scale_next_button")
                        ) {
                            Text(
                                text = if (isLast) "Finalizar y Evaluar" else "Siguiente",
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isLast) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // --- RESULTS SCREEN ---
                    val res = scaleResult
                    if (res != null) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(res.badgeColor.copy(alpha = 0.15f))
                                ) {
                                    Text(
                                        text = "${res.score}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = res.badgeColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Puntaje Total: ${res.score} / ${res.maxScore}",
                                    fontSize = 14.sp,
                                    color = MutedSlate
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = res.badgeColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Nivel: ${res.severity}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = res.badgeColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = res.interpretation,
                                    fontSize = 13.sp,
                                    color = CharcoalText,
                                    lineHeight = 19.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Informative medical disclaimer
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF7FAFC))
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MutedSlate,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(top = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Este cuestionario estandarizado es una herramienta de tamizaje y autoconocimiento. No sustituye la consulta médica ni el diagnóstico clínico personalizado.",
                                        fontSize = 11.sp,
                                        color = MutedSlate,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        // Immediate crisis hotline support card if triggered
                        if (res.triggerSupport) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CoralLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = null,
                                            tint = CoralPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Acompañamiento prioritario",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = CharcoalText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tus respuestas reflejan momentos de alta carga emocional o pensamientos difíciles. No tienes que sobrellevarlo a solas.",
                                        fontSize = 12.sp,
                                        color = CharcoalText
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            viewModel.openCrisisSupport()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhoneInTalk,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ver Líneas de Apoyo Gratuitas", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Save action
                        var savedDone by remember { mutableStateOf(false) }

                        if (!savedDone) {
                            Button(
                                onClick = {
                                    viewModel.saveClinicalAssessment(
                                        scaleType = if (scaleType == ScaleType.PHQ9) "PHQ-9" else "GAD-7",
                                        scaleName = title,
                                        score = res.score,
                                        maxScore = res.maxScore,
                                        severityLevel = res.severity,
                                        interpretation = res.interpretation,
                                        answers = answers.toList(),
                                        triggerSupport = res.triggerSupport
                                    )
                                    savedDone = true
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_scale_result_button")
                            ) {
                                Text(
                                    text = "Guardar en mi Historial de Salud",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Text(
                                text = "✓ Evaluación guardada correctamente en tu historial.",
                                color = TealSecondary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }
}
