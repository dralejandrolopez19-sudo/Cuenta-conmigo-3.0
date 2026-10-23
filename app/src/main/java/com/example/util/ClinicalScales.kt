package com.example.util

import androidx.compose.ui.graphics.Color

data class ScaleOption(
    val value: Int,
    val label: String
)

data class ScaleQuestion(
    val id: Int,
    val text: String
)

data class ScaleResult(
    val score: Int,
    val maxScore: Int,
    val severity: String,
    val interpretation: String,
    val badgeColor: Color,
    val triggerSupport: Boolean = false
)

object ClinicalScales {

    val STANDARD_FREQUENCY_OPTIONS = listOf(
        ScaleOption(0, "Para nada"),
        ScaleOption(1, "Varios días"),
        ScaleOption(2, "Más de la mitad de los días"),
        ScaleOption(3, "Casi todos los días")
    )

    // PHQ-9 (Patient Health Questionnaire - 9)
    val PHQ9_QUESTIONS = listOf(
        ScaleQuestion(1, "Poco interés o placer en hacer las cosas."),
        ScaleQuestion(2, "Sentirse decaído/a, desanimado/a, deprimido/a o sin esperanzas."),
        ScaleQuestion(3, "Dificultad para conciliar el sueño, despertares nocturnos o dormir demasiado."),
        ScaleQuestion(4, "Sentirse cansado/a o con muy poca energía."),
        ScaleQuestion(5, "Poco apetito o tendencia a comer en exceso."),
        ScaleQuestion(6, "Sentir malestar contigo mismo/a, sentirte un fracaso o que has defraudado a los tuyos."),
        ScaleQuestion(7, "Dificultad para concentrarte en actividades como leer, trabajar o ver televisión."),
        ScaleQuestion(8, "Moverte o hablar tan lentamente que otros lo noten, o lo opuesto: inquietud motora y agitación."),
        ScaleQuestion(9, "Pensamientos de que estarías mejor muerto/a o deseos de hacerte daño de alguna manera.")
    )

    fun evaluatePHQ9(answers: List<Int>): ScaleResult {
        val score = answers.sum()
        val hasIdeation = answers.getOrNull(8)?.let { it > 0 } ?: false

        return when {
            score in 0..4 -> ScaleResult(
                score = score,
                maxScore = 27,
                severity = "Mínimo o ausente",
                interpretation = "Tus respuestas no sugieren presencia de síntomas depresivos significativos. Mantén tus rutinas de bienestar y autocuidado.",
                badgeColor = Color(0xFF38A169),
                triggerSupport = hasIdeation
            )
            score in 5..9 -> ScaleResult(
                score = score,
                maxScore = 27,
                severity = "Leve",
                interpretation = "Indica sintomatología depresiva leve. Es recomendable cultivar espacios de descanso, diálogo con personas de confianza y hábitos reconfortantes.",
                badgeColor = Color(0xFFD69E2E),
                triggerSupport = hasIdeation
            )
            score in 10..14 -> ScaleResult(
                score = score,
                maxScore = 27,
                severity = "Moderado",
                interpretation = "Indica sintomatología moderada. Puede estar afectando tu vitalidad y descanso. Se aconseja considerar consulta con un profesional de la salud.",
                badgeColor = Color(0xFFDD6B20),
                triggerSupport = hasIdeation
            )
            score in 15..19 -> ScaleResult(
                score = score,
                maxScore = 27,
                severity = "Moderadamente severo",
                interpretation = "Indica sintomatología depresiva moderadamente severa. Te sugerimos buscar apoyo médico o psicológico para un plan de acompañamiento activo.",
                badgeColor = Color(0xFFFF6B6B),
                triggerSupport = true
            )
            else -> ScaleResult(
                score = score,
                maxScore = 27,
                severity = "Severo",
                interpretation = "Indica sintomatología depresiva severa. Es prioritario que te apoyes en tu red cercana y consultes con especialistas de salud mental.",
                badgeColor = Color(0xFFE53E3E),
                triggerSupport = true
            )
        }
    }

    // GAD-7 (Generalized Anxiety Disorder 7-item)
    val GAD7_QUESTIONS = listOf(
        ScaleQuestion(1, "Sentirse nervioso/a, intranquilo/a, ansioso/a o con los nervios de punta."),
        ScaleQuestion(2, "No poder parar o controlar las preocupaciones constantes."),
        ScaleQuestion(3, "Preocuparse excesivamente por diversas situaciones de la vida diaria."),
        ScaleQuestion(4, "Dificultad para relajarse y distender la tensión corporal."),
        ScaleQuestion(5, "Estar tan inquieto/a que resulta difícil permanecer quieto/a o en calma."),
        ScaleQuestion(6, "Sentirse fácilmente irritable, impaciente o con poca tolerancia."),
        ScaleQuestion(7, "Sentir miedo repentino, como si algo terrible fuera a suceder.")
    )

    fun evaluateGAD7(answers: List<Int>): ScaleResult {
        val score = answers.sum()

        return when {
            score in 0..4 -> ScaleResult(
                score = score,
                maxScore = 21,
                severity = "Mínima",
                interpretation = "Indica un nivel de ansiedad mínimo. Tus respuestas sugieren buen balance y manejo de las situaciones cotidianas.",
                badgeColor = Color(0xFF38A169),
                triggerSupport = false
            )
            score in 5..9 -> ScaleResult(
                score = score,
                maxScore = 21,
                severity = "Leve",
                interpretation = "Indica ansiedad leve. Respiraciones diafragmáticas, pausas conscientes y límites de sobrecarga pueden ayudarte a recuperar serenidad.",
                badgeColor = Color(0xFFD69E2E),
                triggerSupport = false
            )
            score in 10..14 -> ScaleResult(
                score = score,
                maxScore = 21,
                severity = "Moderada",
                interpretation = "Indica ansiedad moderada. Es posible que interfiera en tu concentración o sueño. Es recomendable valorar apoyo psicoterapéutico.",
                badgeColor = Color(0xFFDD6B20),
                triggerSupport = false
            )
            else -> ScaleResult(
                score = score,
                maxScore = 21,
                severity = "Severa",
                interpretation = "Indica ansiedad severa. Sugerimos consultar con un especialista para aprender pautas personalizadas de regulación neurovegetativa y alivio del estrés.",
                badgeColor = Color(0xFFE53E3E),
                triggerSupport = true
            )
        }
    }

    // EVA Emocional (0 - 10)
    fun evaluateEVA(level: Int): ScaleResult {
        return when (level) {
            in 0..3 -> ScaleResult(
                score = level,
                maxScore = 10,
                severity = "Malestar significativo",
                interpretation = "Percibes un malestar emocional elevado en este momento. Tómate una pausa amable y busca un espacio de contención o respiración.",
                badgeColor = Color(0xFFE53E3E),
                triggerSupport = level <= 2
            )
            in 4..6 -> ScaleResult(
                score = level,
                maxScore = 10,
                severity = "Equilibrio moderado",
                interpretation = "Te encuentras en una zona neutral o de transición. Escucha tus necesidades de descanso y ritmo pausado.",
                badgeColor = Color(0xFFD69E2E),
                triggerSupport = false
            )
            else -> ScaleResult(
                score = level,
                maxScore = 10,
                severity = "Bienestar óptimo",
                interpretation = "Experimentas serenidad y bienestar emocional. Disfruta de esta sensación de estabilidad y gratitud.",
                badgeColor = Color(0xFF38A169),
                triggerSupport = false
            )
        }
    }
}
