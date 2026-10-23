package com.example.util

enum class ImbalanceSeverity {
    NONE,
    MODERATE,
    HIGH
}

data class CrisisAssessment(
    val hasImbalance: Boolean,
    val severity: ImbalanceSeverity,
    val triggerReason: String
)

object CrisisDetectionHelper {

    private val HIGH_RISK_KEYWORDS = listOf(
        "morir", "suicid", "hacerme daño", "quitarme la vida",
        "terminar con todo", "no quiero vivir", "acabar con todo",
        "ya no aguanto", "no puedo más", "no puedo seguir",
        "desesperación total", "no encuentro salida", "autolesion"
    )

    private val MODERATE_RISK_KEYWORDS = listOf(
        "crisis", "colapso", "angustia", "desesperad", "pánico",
        "dolor insoportable", "mucho dolor", "muy solo", "soledad absoluta",
        "llorando sin parar", "ansiedad extrema", "no tengo fuerzas"
    )

    fun evaluate(level: Int? = null, textContent: String = "", feelings: List<String> = emptyList()): CrisisAssessment {
        val lowerText = textContent.lowercase().trim()

        // 1. Revisar palabras clave de alto riesgo
        for (kw in HIGH_RISK_KEYWORDS) {
            if (lowerText.contains(kw)) {
                return CrisisAssessment(
                    hasImbalance = true,
                    severity = ImbalanceSeverity.HIGH,
                    triggerReason = "Identificamos expresiones de dolor o crisis profunda."
                )
            }
        }

        // 2. Revisar nivel de ánimo muy bajo (1)
        if (level == 1) {
            return CrisisAssessment(
                hasImbalance = true,
                severity = ImbalanceSeverity.HIGH,
                triggerReason = "Registraste un estado de ánimo muy bajo el día de hoy."
            )
        }

        // 3. Revisar palabras de riesgo moderado
        for (kw in MODERATE_RISK_KEYWORDS) {
            if (lowerText.contains(kw)) {
                return CrisisAssessment(
                    hasImbalance = true,
                    severity = ImbalanceSeverity.MODERATE,
                    triggerReason = "Sentimos que estás pasando por un momento de inquietud o tensión emocional."
                )
            }
        }

        // 4. Revisar nivel de ánimo desanimado (2)
        if (level == 2) {
            return CrisisAssessment(
                hasImbalance = true,
                severity = ImbalanceSeverity.MODERATE,
                triggerReason = "Registraste desánimo en tu balance del día."
            )
        }

        return CrisisAssessment(
            hasImbalance = false,
            severity = ImbalanceSeverity.NONE,
            triggerReason = ""
        )
    }
}
