package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "Dr. Oliver Hernández",
    val email: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MoodEntry(
    val id: String = "",
    val userId: String = "",
    val level: Int = 3, // 1: Muy bajo, 2: Desanimado, 3: Estable, 4: Bien, 5: Muy bien
    val levelLabel: String = "Estable",
    val feelings: List<String> = emptyList(),
    val note: String = "",
    val date: String = "", // Formato YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class JournalEntry(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "Reflexión", // Reflexión, Gratitud, Reestructuración, Libre
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Medication(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val scheduledTimes: List<String> = emptyList(), // e.g. ["08:00", "20:00"]
    val notes: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MedicationLog(
    val id: String = "",
    val userId: String = "",
    val medicationId: String = "",
    val medicationName: String = "",
    val dosage: String = "",
    val date: String = "", // YYYY-MM-DD
    val scheduledTime: String = "", // "08:00"
    @ServerTimestamp val takenAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Habit(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Salud Mental", // Salud Mental, Físico y Sueño, Social, Emocional
    val timeOfDay: String = "Mañana", // Mañana, Tarde, Noche, Todo el día
    val streakDays: Int = 0,
    val completedDates: List<String> = emptyList(), // lista de YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CalendarEvent(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val type: String = "Cita/Evento", // Cita/Evento, Sesión, Terapia, Recordatorio
    val date: String = "", // YYYY-MM-DD
    val time: String = "", // HH:mm
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClinicalScaleAssessment(
    val id: String = "",
    val userId: String = "",
    val scaleType: String = "", // "PHQ-9", "GAD-7", "EVA"
    val scaleName: String = "",
    val score: Int = 0,
    val maxScore: Int = 27,
    val severityLevel: String = "",
    val interpretation: String = "",
    val answers: List<Int> = emptyList(),
    val date: String = "", // YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null
)

