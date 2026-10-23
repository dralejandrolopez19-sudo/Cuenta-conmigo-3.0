package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.CoralLight
import com.example.ui.theme.CoralPrimary
import com.example.ui.theme.MutedSlate
import com.example.ui.components.AiWellnessAssistantCard
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.PurpleLight
import com.example.ui.theme.SoftCreamBackground
import com.example.ui.theme.TealLight
import com.example.ui.theme.TealSecondary
import com.example.ui.viewmodel.AppTab
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.example.ui.viewmodel.AppViewModel

val compassionateQuotes = listOf(
    "Los pensamientos no son hechos absolutos. Puedes observarlos con curiosidad y amabilidad sin juzgarte a ti mismo.",
    "Está bien no tener todas las respuestas hoy. Un día y un paso a la vez es suficiente para avanzar.",
    "Respira hondo. Este instante es tu espacio seguro; tienes derecho a descansar y pausar el ritmo.",
    "Trátate a ti mismo con la misma ternura y paciencia con la que tratarías a un ser querido que sufre.",
    "Tu valor como persona no depende de tu productividad de hoy. Existir y cuidarte ya es un logro.",
    "Las emociones son como olas: alcanzan su punto máximo y luego se retiran suavemente si les permites ser.",
    "No necesitas ser perfecto para merecer paz, amor propio y tranquilidad en tu vida.",
    "Cuando sientas autocrítica, pon una mano sobre tu pecho y recuérdate: 'Estoy haciéndolo lo mejor que puedo'.",
    "Permítete cometer errores; son la señal viva de que estás aprendiendo y creciendo.",
    "Incluso en los días difíciles, las partes sanas y resilientes de ti siguen presentes y vivas.",
    "La calma no significa ausencia de ruido, sino paz en medio de la tormenta interna.",
    "Aceptarte hoy tal como eres es el primer paso compasivo hacia cualquier transformación.",
    "Honra tus límites. Decir 'no' a las demandas externas a menudo es decir 'sí' a tu propia salud mental.",
    "La neuroplasticidad nos recuerda que la mente puede sanar y crear nuevos caminos con constancia y ternura.",
    "No te apresures a sanar todo de una vez. La semilla florece en la oscuridad antes de ver la luz.",
    "Lo que sientes en este momento es temporal. Todo estado emocional tiene un principio y un final.",
    "Hoy no tienes que cargar el peso de mañana ni el remordimiento de ayer. Solo habita este presente.",
    "Agradece a tu mente por intentar protegerte, incluso cuando lo hace mediante la preocupación.",
    "La autocompasión es un músculo que se entrena cada vez que decides no castigarte por ser humano.",
    "Date permiso para sentir alegría en pequeñas cosas: una taza caliente, el aire fresco, un suspiro profundo.",
    "No eres tus heridas; eres la conciencia sabia y bondadosa que aprende a sanarlas.",
    "Cuando todo parezca abrumador, reduce tu horizonte: ¿qué es lo único que necesitas hacer en los próximos 10 minutos?",
    "Tu sensibilidad no es una debilidad; es la brújula que te permite conectar con lo más auténtico de ti.",
    "Soltar la necesidad de control es abrir espacio para la serenidad y la confianza en tu proceso.",
    "Celebrar victorias diminutas hoy es construir los cimientos de tu bienestar de mañana.",
    "La comparación con otros solo mide apariencias externas. Tu camino interior es único y sagrado.",
    "Cuidar de tu cuerpo con descanso, agua y alimento amable es una forma directa de agradecer a la vida.",
    "Permítete no saber. La incertidumbre también es un espacio donde florecen nuevas posibilidades.",
    "Incluso si hoy sentiste que retrocediste, estás en un nuevo punto de conciencia; la espiral avanza hacia arriba.",
    "Habla contigo mismo con voz de refugio, no de exigencia. Tu diálogo interno construye tu hogar mental.",
    "Estás aquí, respirando, intentándolo de nuevo. Cuenta contigo mismo: eres tu aliado más valioso."
)

fun getDailyQuoteIndex(dateString: String, total: Int): Int {
    val hash = kotlin.math.abs(dateString.hashCode())
    return hash % total
}

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsState()
    val moodEntries by viewModel.moodEntries.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val todayMedLogs by viewModel.todayMedLogs.collectAsState()
    val habits by viewModel.habits.collectAsState()

    val isStreetModeActive by viewModel.isStreetModeActive.collectAsState()
    val isAiDisguiseEnabled by viewModel.isAiDisguiseEnabled.collectAsState()
    val aiAliases by viewModel.aiDisguiseAliases.collectAsState()
    val homeAiState by viewModel.homeAiInsightState.collectAsState()

    // El índice base cambia automáticamente cada día según la fecha del sistema
    var quoteOffset by remember(viewModel.todayDateString) { mutableIntStateOf(0) }
    val baseIndex = remember(viewModel.todayDateString) {
        getDailyQuoteIndex(viewModel.todayDateString, compassionateQuotes.size)
    }
    val currentQuoteIndex = (baseIndex + quoteOffset) % compassionateQuotes.size

    // Computar estados con protección de Modo Calle
    val todayMood = moodEntries.firstOrNull { it.date == viewModel.todayDateString }
    val moodTitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> "Jornada activa"
        isStreetModeActive -> "••••••"
        else -> todayMood?.levelLabel ?: "Sin registrar"
    }
    val moodSubtitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> aiAliases.moodAlias
        isStreetModeActive -> "Protegido en Modo Calle"
        todayMood != null -> "Registrado hoy"
        else -> "Toca para registrar"
    }

    val totalDoses = medications.sumOf { it.scheduledTimes.size }
    val takenDoses = todayMedLogs.size
    val medTitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> "Rutina diaria"
        isStreetModeActive -> "••••••"
        medications.isEmpty() -> "0 tomas"
        else -> "$takenDoses tomas"
    }
    val medSubtitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> aiAliases.pillAlias
        isStreetModeActive -> "Protegido en Modo Calle"
        medications.isEmpty() -> "Sin pastillas aún"
        totalDoses > 0 -> "$takenDoses de $totalDoses tomadas"
        else -> "Al día"
    }

    val completedHabitsToday = habits.count { it.completedDates.contains(viewModel.todayDateString) }
    val habitsTitle = if (habits.isEmpty()) "0 hábitos" else "$completedHabitsToday/${habits.size} hábitos"
    val habitsSubtitle = if (habits.isEmpty()) "Toca para comenzar" else "Completados hoy"

    val journalTitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> "Apuntes"
        isStreetModeActive -> "••••••"
        else -> "${journalEntries.size} notas"
    }
    val journalSubtitle = when {
        isStreetModeActive && isAiDisguiseEnabled -> aiAliases.diaryAlias
        isStreetModeActive -> "Protegido en Modo Calle"
        journalEntries.isEmpty() -> "Toca para escribir"
        else -> "Ver reflexiones"
    }

    val rawDisplayName = profile?.displayName?.trim()?.ifBlank { null }
        ?: Firebase.auth.currentUser?.displayName?.trim()?.ifBlank { null }
        ?: Firebase.auth.currentUser?.email?.substringBefore("@")?.replace(".", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }?.ifBlank { null }
        ?: "Bienvenido"
    val displayName = if (isStreetModeActive) "Mi Espacio" else rawDisplayName

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SoftCreamBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Cabecera Minimalista con Imagen Sereno
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("home_header_banner")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(id = R.drawable.img_wellness_header),
                    contentDescription = "Paisaje relajante",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "Hola, $displayName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Un paso a la vez.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cuadrícula 2x2 Minimalista
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActivityCard(
                title = moodTitle,
                category = "Ánimo",
                subtitle = moodSubtitle,
                icon = Icons.Default.Mood,
                iconTint = CoralPrimary,
                iconBg = CoralLight,
                categoryColor = CoralPrimary,
                onClick = { viewModel.selectTab(AppTab.ANIMO) },
                modifier = Modifier.weight(1f)
            )

            ActivityCard(
                title = medTitle,
                category = "Pastillas",
                subtitle = medSubtitle,
                icon = Icons.Default.Medication,
                iconTint = TealSecondary,
                iconBg = TealLight,
                categoryColor = TealSecondary,
                onClick = { viewModel.selectTab(AppTab.PASTILLAS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActivityCard(
                title = habitsTitle,
                category = "Hábitos",
                subtitle = habitsSubtitle,
                icon = Icons.Default.Spa,
                iconTint = TealSecondary,
                iconBg = TealLight,
                categoryColor = TealSecondary,
                onClick = { viewModel.selectTab(AppTab.HABITOS) },
                modifier = Modifier.weight(1f)
            )

            ActivityCard(
                title = journalTitle,
                category = "Diario",
                subtitle = journalSubtitle,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                iconTint = CoralPrimary,
                iconBg = CoralLight,
                categoryColor = CoralPrimary,
                onClick = { viewModel.selectTab(AppTab.DIARIO) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pensamiento Compasivo Minimalista
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    quoteOffset = (quoteOffset + 1) % compassionateQuotes.size
                }
                .testTag("compassionate_thought_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CoralLight)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Compasión",
                        tint = CoralPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "\"${compassionateQuotes[currentQuoteIndex]}\"",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = CharcoalText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Asistente IA Compacto
        AiWellnessAssistantCard(
            title = "Guía de Hoy",
            subtitle = "Balance breve de tu día con IA.",
            buttonLabel = "Guía IA",
            state = homeAiState,
            onGenerateClick = { viewModel.generateHomeWellnessSummary() },
            accentColor = PurpleAccent,
            accentBgColor = PurpleLight,
            testTag = "home_ai_assistant_card"
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ActivityCard(
    title: String,
    category: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    categoryColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .heightIn(min = 104.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = category,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MutedSlate.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$category • $subtitle",
                    fontSize = 11.sp,
                    color = categoryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
