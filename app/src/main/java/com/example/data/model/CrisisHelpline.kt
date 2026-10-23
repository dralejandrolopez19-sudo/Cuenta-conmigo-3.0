package com.example.data.model

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

data class CrisisHelpline(
    val countryCode: String,
    val countryName: String,
    val flagEmoji: String,
    val serviceName: String,
    val organization: String,
    val phoneNumber: String,
    val cleanDialNumber: String,
    val description: String,
    val is24Hours: Boolean = true,
    val isFree: Boolean = true,
    val isPrimaryDominicanRepublic: Boolean = false
)

object CrisisDirectory {

    fun dial(context: Context, rawNumber: String) {
        val clean = rawNumber.replace("[^0-9*#+]".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$clean".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Manejo seguro en caso de dispositivos sin app de teléfono
        }
    }

    val DOMINICAN_REPUBLIC_PRIMARY = CrisisHelpline(
        countryCode = "DO",
        countryName = "República Dominicana",
        flagEmoji = "🇩🇴",
        serviceName = "Cuida tu Salud Mental (Centro de Contacto)",
        organization = "Ministerio de Salud Pública (MSP)",
        phoneNumber = "809-200-1400",
        cleanDialNumber = "8092001400",
        description = "Línea de orientación psicológica, contención emocional y canalización a centros de salud mental de la red pública.",
        is24Hours = true,
        isFree = true,
        isPrimaryDominicanRepublic = true
    )

    val DOMINICAN_REPUBLIC_LIST = listOf(
        DOMINICAN_REPUBLIC_PRIMARY,
        CrisisHelpline(
            countryCode = "DO",
            countryName = "República Dominicana",
            flagEmoji = "🇩🇴",
            serviceName = "Sistema Nacional de Emergencias 9-1-1",
            organization = "Gobierno de la República Dominicana",
            phoneNumber = "911",
            cleanDialNumber = "911",
            description = "Para emergencias médicas críticas, riesgo inminente de vida o situaciones agudas de rescate.",
            is24Hours = true,
            isFree = true
        ),
        CrisisHelpline(
            countryCode = "DO",
            countryName = "República Dominicana",
            flagEmoji = "🇩🇴",
            serviceName = "Línea Vida (Crisis Familiar y Violencia)",
            organization = "Procuraduría General de la República",
            phoneNumber = "809-200-1202",
            cleanDialNumber = "8092001202",
            description = "Atención e intervención en crisis emocional, maltrato y situaciones de riesgo intrafamiliar y personal.",
            is24Hours = true,
            isFree = true
        )
    )

    val GLOBAL_LIST = listOf(
        // República Dominicana
        DOMINICAN_REPUBLIC_PRIMARY,
        // Estados Unidos y Canadá
        CrisisHelpline(
            countryCode = "US",
            countryName = "Estados Unidos",
            flagEmoji = "🇺🇸",
            serviceName = "988 Suicide & Crisis Lifeline",
            organization = "Substance Abuse and Mental Health Services (SAMHSA)",
            phoneNumber = "988",
            cleanDialNumber = "988",
            description = "Línea nacional gratuita para prevención de suicidio y crisis emocional. Atención en español e inglés 24/7."
        ),
        CrisisHelpline(
            countryCode = "CA",
            countryName = "Canadá",
            flagEmoji = "🇨🇦",
            serviceName = "988 Suicide Crisis Helpline",
            organization = "Public Health Agency of Canada",
            phoneNumber = "988",
            cleanDialNumber = "988",
            description = "Línea nacional para prevención del suicidio y apoyo en crisis emocional 24/7 en todo Canadá."
        ),
        // España
        CrisisHelpline(
            countryCode = "ES",
            countryName = "España",
            flagEmoji = "🇪🇸",
            serviceName = "Línea 024 de Atención a la Conducta Suicida",
            organization = "Ministerio de Sanidad de España",
            phoneNumber = "024",
            cleanDialNumber = "024",
            description = "Servicio público, gratuito, confidencial y disponible 24 horas al día los 365 días del año."
        ),
        CrisisHelpline(
            countryCode = "ES",
            countryName = "España",
            flagEmoji = "🇪🇸",
            serviceName = "Teléfono de la Esperanza",
            organization = "Asociación Internacional Teléfono de la Esperanza",
            phoneNumber = "717 003 717",
            cleanDialNumber = "717003717",
            description = "Escucha activa, apoyo psicológico e intervención en crisis emocional las 24 horas."
        ),
        // México
        CrisisHelpline(
            countryCode = "MX",
            countryName = "México",
            flagEmoji = "🇲🇽",
            serviceName = "Línea de la Vida",
            organization = "Comisión Nacional de Salud Mental y Adicciones (CONASAMA)",
            phoneNumber = "800 911 2000",
            cleanDialNumber = "8009112000",
            description = "Atención especializada en problemas de salud mental, depresión y crisis emocional 24/7."
        ),
        // Colombia
        CrisisHelpline(
            countryCode = "CO",
            countryName = "Colombia",
            flagEmoji = "🇨🇴",
            serviceName = "Línea 106 (El Poder de Ser Escuchado)",
            organization = "Secretaría Distrital de Salud / Minsalud",
            phoneNumber = "106",
            cleanDialNumber = "106",
            description = "Espacio de escucha y ayuda psicológica profesional disponible las 24 horas."
        ),
        CrisisHelpline(
            countryCode = "CO",
            countryName = "Colombia",
            flagEmoji = "🇨🇴",
            serviceName = "Línea 192 (Salud Mental Nacional)",
            organization = "Ministerio de Salud y Protección Social",
            phoneNumber = "192",
            cleanDialNumber = "192",
            description = "Orientación en salud mental y primeros auxilios psicológicos."
        ),
        // Argentina
        CrisisHelpline(
            countryCode = "AR",
            countryName = "Argentina",
            flagEmoji = "🇦🇷",
            serviceName = "Centro de Asistencia al Suicida (CAS)",
            organization = "CAS Argentina",
            phoneNumber = "135",
            cleanDialNumber = "135",
            description = "Línea gratuita de ayuda y contención emocional desde CABA y Gran Buenos Aires (o 011-5275-1135)."
        ),
        // Chile
        CrisisHelpline(
            countryCode = "CL",
            countryName = "Chile",
            flagEmoji = "🇨🇱",
            serviceName = "Línea *4141 (No Estás Solo)",
            organization = "Ministerio de Salud de Chile",
            phoneNumber = "*4141",
            cleanDialNumber = "*4141",
            description = "Línea telefónica gratuita para personas que estén enfrentando una crisis de salud mental severa."
        ),
        // Perú
        CrisisHelpline(
            countryCode = "PE",
            countryName = "Perú",
            flagEmoji = "🇵🇪",
            serviceName = "Línea 113 (Opción 5)",
            organization = "Ministerio de Salud del Perú (MINSA)",
            phoneNumber = "113",
            cleanDialNumber = "113",
            description = "Atención telefónica gratuita y confidencial en salud mental y soporte emocional 24 horas."
        ),
        // Puerto Rico
        CrisisHelpline(
            countryCode = "PR",
            countryName = "Puerto Rico",
            flagEmoji = "🇵🇷",
            serviceName = "Línea PAS (Primera Ayuda Psicosocial)",
            organization = "ASSMCA Puerto Rico",
            phoneNumber = "1-800-981-0023",
            cleanDialNumber = "18009810023",
            description = "Desahogo, consejería en crisis y canalización a servicios de salud mental las 24 horas."
        ),
        // Costa Rica
        CrisisHelpline(
            countryCode = "CR",
            countryName = "Costa Rica",
            flagEmoji = "🇨🇷",
            serviceName = "Línea Aquí Estoy",
            organization = "Colegio de Profesionales en Psicología",
            phoneNumber = "800-8000-642",
            cleanDialNumber = "8008000642",
            description = "Orientación psicológica e intervención en crisis y prevención del suicidio."
        ),
        // Panamá
        CrisisHelpline(
            countryCode = "PA",
            countryName = "Panamá",
            flagEmoji = "🇵🇦",
            serviceName = "Línea de Apoyo Emocional 169",
            organization = "Ministerio de Salud de Panamá",
            phoneNumber = "169",
            cleanDialNumber = "169",
            description = "Orientación psicológica en crisis y soporte emocional para la población."
        ),
        // Ecuador
        CrisisHelpline(
            countryCode = "EC",
            countryName = "Ecuador",
            flagEmoji = "🇪🇨",
            serviceName = "Línea 171 (Opción 6)",
            organization = "Ministerio de Salud Pública del Ecuador",
            phoneNumber = "171",
            cleanDialNumber = "171",
            description = "Atención y asesoría gratuita en salud mental y primeros auxilios emocionales."
        ),
        // Uruguay
        CrisisHelpline(
            countryCode = "UY",
            countryName = "Uruguay",
            flagEmoji = "🇺🇾",
            serviceName = "Línea Vida *0767",
            organization = "Administración de los Servicios de Salud del Estado (ASSE)",
            phoneNumber = "*0767",
            cleanDialNumber = "*0767",
            description = "Línea gratuita de prevención del suicidio y contención emocional (o 0800-0767)."
        ),
        // Reino Unido
        CrisisHelpline(
            countryCode = "GB",
            countryName = "Reino Unido",
            flagEmoji = "🇬🇧",
            serviceName = "Samaritans Helpline",
            organization = "Samaritans UK",
            phoneNumber = "116 123",
            cleanDialNumber = "116123",
            description = "Servicio de escucha confidencial y apoyo emocional 24/7 sin juicio."
        ),
        // Unión Europea / Internacional
        CrisisHelpline(
            countryCode = "EU",
            countryName = "Europa / Internacional",
            flagEmoji = "🌍",
            serviceName = "Número Único de Emergencias Europeo",
            organization = "Unión Europea",
            phoneNumber = "112",
            cleanDialNumber = "112",
            description = "Acceso inmediato y gratuito a servicios de auxilio y urgencias médicas en toda Europa."
        )
    )

    fun search(query: String): List<CrisisHelpline> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return GLOBAL_LIST
        return GLOBAL_LIST.filter {
            it.countryName.lowercase().contains(q) ||
            it.serviceName.lowercase().contains(q) ||
            it.phoneNumber.contains(q) ||
            it.organization.lowercase().contains(q)
        }
    }
}
