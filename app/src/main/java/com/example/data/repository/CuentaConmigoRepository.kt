package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CalendarEvent
import com.example.data.model.ClinicalScaleAssessment
import com.example.data.model.Habit
import com.example.data.model.JournalEntry
import com.example.data.model.Medication
import com.example.data.model.MedicationLog
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Almacén en memoria para el Modo Invitado / Explorador.
 * Permite probar de manera inmediata todas las pantallas, el botón SOS, el pensamiento compasivo,
 * medicamentos y hábitos sin consultar la nube de Firestore ni requerir cuentas.
 * Se limpia al salir de la sesión temporal.
 */
object InMemoryGuestStore {
    val profile = MutableStateFlow<UserProfile?>(
        UserProfile(
            userId = "guest_explorer",
            displayName = "Dr. Oliver Hernández (Invitado)",
            email = "invitado@cuentaconmigo.app"
        )
    )

    val moodEntries = MutableStateFlow<List<MoodEntry>>(
        listOf(
            MoodEntry(
                id = "guest_mood_1",
                userId = "guest_explorer",
                level = 4,
                levelLabel = "Bien",
                feelings = listOf("Esperanzado", "Tranquilo", "Motivado"),
                note = "Sesión en modo explorador temporal. Todo funciona fluido y en memoria.",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            )
        )
    )

    val journalEntries = MutableStateFlow<List<JournalEntry>>(
        listOf(
            JournalEntry(
                id = "guest_journal_1",
                userId = "guest_explorer",
                title = "Bienvenido a tu Modo Explorador",
                content = "Este cuaderno de bienestar te permite interactuar con todas las herramientas, registrar reflexiones y revisar el apoyo emocional sin exponer tus datos personales.",
                category = "Reflexión",
                createdAt = Timestamp.now()
            )
        )
    )

    val medications = MutableStateFlow<List<Medication>>(
        listOf(
            Medication(
                id = "guest_med_1",
                userId = "guest_explorer",
                name = "Sertralina",
                dosage = "50 mg",
                scheduledTimes = listOf("08:00"),
                notes = "Tomar después del desayuno con agua",
                active = true,
                createdAt = Timestamp.now()
            ),
            Medication(
                id = "guest_med_2",
                userId = "guest_explorer",
                name = "Complejo B",
                dosage = "1 cápsula",
                scheduledTimes = listOf("14:00"),
                notes = "Refuerzo vitamínico",
                active = true,
                createdAt = Timestamp.now()
            )
        )
    )

    val medicationLogs = MutableStateFlow<List<MedicationLog>>(emptyList())

    val habits = MutableStateFlow<List<Habit>>(
        listOf(
            Habit(
                id = "guest_habit_1",
                userId = "guest_explorer",
                name = "Paseo de 15 minutos al aire libre",
                category = "Bienestar",
                timeOfDay = "Mañana",
                streakDays = 3,
                completedDates = listOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            ),
            Habit(
                id = "guest_habit_2",
                userId = "guest_explorer",
                name = "Vaso de agua al despertar",
                category = "Salud",
                timeOfDay = "Mañana",
                streakDays = 5,
                completedDates = listOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
            )
        )
    )

    val calendarEvents = MutableStateFlow<List<CalendarEvent>>(
        listOf(
            CalendarEvent(
                id = "guest_ev_1",
                userId = "guest_explorer",
                title = "Consulta médica de seguimiento",
                type = "Cita Médica",
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                time = "16:00",
                notes = "Revisión general y signos vitales"
            )
        )
    )

    val clinicalAssessments = MutableStateFlow<List<ClinicalScaleAssessment>>(
        listOf(
            ClinicalScaleAssessment(
                id = "guest_eval_1",
                userId = "guest_explorer",
                scaleType = "PHQ-9",
                scaleName = "PHQ-9 • Depresión",
                score = 4,
                maxScore = 27,
                severityLevel = "Mínimo o ausente",
                interpretation = "Tus respuestas no sugieren presencia de síntomas depresivos significativos. Mantén tus rutinas de bienestar y autocuidado.",
                answers = listOf(1, 0, 1, 1, 0, 0, 1, 0, 0),
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            ),
            ClinicalScaleAssessment(
                id = "guest_eval_2",
                userId = "guest_explorer",
                scaleType = "GAD-7",
                scaleName = "GAD-7 • Ansiedad",
                score = 3,
                maxScore = 21,
                severityLevel = "Mínima",
                interpretation = "Indica un nivel de ansiedad mínimo. Tus respuestas sugieren buen balance y manejo de las situaciones cotidianas.",
                answers = listOf(1, 1, 0, 0, 1, 0, 0),
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            )
        )
    )

    fun reset() {
        profile.value = UserProfile(
            userId = "guest_explorer",
            displayName = "Dr. Oliver Hernández (Invitado)",
            email = "invitado@cuentaconmigo.app"
        )
        medicationLogs.value = emptyList()
    }
}

class CuentaConmigoRepository(
    private val db: FirebaseFirestore,
    val currentUserId: String = ""
) {

    constructor(context: Context, currentUserId: String = "") : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        currentUserId
    )

    private val auth = Firebase.auth

    fun isGuest(uid: String = currentUserId): Boolean {
        return uid.startsWith("guest_") || (currentUserId.isNotBlank() && currentUserId.startsWith("guest_"))
    }

    fun requireUserId(): String {
        if (isGuest()) return currentUserId.ifBlank { "guest_explorer" }
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado en Firebase")
    }

    // --- PERFIL DE USUARIO ---
    fun observeProfile(uid: String): Flow<UserProfile?> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.profile.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(null)
        }
        return flow {
            val path = "users/$uid/profile/main"
            val docRef = db.collection("users").document(uid).collection("profile").document("main")
            docRef.snapshots()
                .map { snapshot ->
                    val profile = snapshot.toObject(UserProfile::class.java)
                    val authUser = auth.currentUser
                    if (authUser != null && authUser.uid == uid) {
                        val authName = authUser.displayName?.trim()?.ifBlank { null }
                        val firestoreName = profile?.displayName?.trim()?.ifBlank { null }
                        val resolvedName = when {
                            firestoreName != null && firestoreName != "Dr. Oliver Hernández" -> firestoreName
                            authName != null -> authName
                            firestoreName != null -> firestoreName
                            else -> authUser.email?.substringBefore("@")?.replace(".", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } } ?: "Usuario"
                        }
                        UserProfile(
                            userId = uid,
                            displayName = resolvedName,
                            email = profile?.email?.ifBlank { null } ?: authUser.email ?: ""
                        )
                    } else {
                        profile
                    }
                }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.READ, path)
                    }
                    val authUser = auth.currentUser
                    if (authUser != null && authUser.uid == uid) {
                        emit(
                            UserProfile(
                                userId = uid,
                                displayName = authUser.displayName?.trim()?.ifBlank { null }
                                    ?: authUser.email?.substringBefore("@")?.replace(".", " ")?.replace("_", " ")?.split(" ")?.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                                    ?: "Usuario",
                                email = authUser.email ?: ""
                            )
                        )
                    } else {
                        emit(null)
                    }
                }
                .collect { emit(it) }
        }
    }

    suspend fun syncUserProfileFromAuth(user: FirebaseUser, overrideName: String? = null) = withContext(NonCancellable) {
        if (isGuest(user.uid)) return@withContext
        val uid = user.uid
        val docRef = db.collection("users").document(uid).collection("profile").document("main")

        val snapshot = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }

        val currentSavedName = snapshot?.getString("displayName")?.trim()?.ifBlank { null }
        val googleName = overrideName?.trim()?.ifBlank { null } ?: user.displayName?.trim()?.ifBlank { null }
        val authEmail = user.email ?: ""
        val emailDerivedName = authEmail.substringBefore("@")
            .replace(".", " ")
            .replace("_", " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            .ifBlank { null }

        val finalName = when {
            !overrideName.isNullOrBlank() -> overrideName.trim()
            currentSavedName != null && currentSavedName != "Dr. Oliver Hernández" -> currentSavedName
            googleName != null -> googleName
            emailDerivedName != null -> emailDerivedName
            currentSavedName != null -> currentSavedName
            else -> "Usuario"
        }

        val data = mutableMapOf<String, Any>(
            "userId" to uid,
            "displayName" to finalName,
            "email" to authEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (snapshot == null || !snapshot.exists()) {
            data["createdAt"] = FieldValue.serverTimestamp()
        }

        try {
            docRef.set(data, SetOptions.merge()).await()
            try {
                val profileUpdates = userProfileChangeRequest {
                    displayName = finalName
                }
                user.updateProfile(profileUpdates).await()
            } catch (e: Exception) {
                // Actualización opcional de FirebaseAuth displayName
            }
        } catch (e: CancellationException) {
            // Cancelación cooperativa de corrutina al cambiar composición; no es un error de Firestore
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun saveProfile(name: String, email: String) {
        if (isGuest()) {
            val uid = currentUserId.ifBlank { "guest_explorer" }
            InMemoryGuestStore.profile.value = UserProfile(
                userId = uid,
                displayName = name.trim(),
                email = email.trim()
            )
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        val trimmedName = name.trim().ifBlank { "Dr. Oliver Hernández" }
        val trimmedEmail = email.trim()
        val data = mapOf(
            "userId" to uid,
            "displayName" to trimmedName,
            "email" to trimmedEmail,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data, SetOptions.merge()).await()
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.uid == uid) {
                try {
                    val profileUpdates = userProfileChangeRequest {
                        displayName = trimmedName
                    }
                    currentUser.updateProfile(profileUpdates).await()
                } catch (e: Exception) {
                    // Actualización local opcional si falla en auth
                }
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- ESTADO DE ÁNIMO ---
    fun observeMoodEntries(uid: String): Flow<List<MoodEntry>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.moodEntries.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/mood_entries"
            db.collection("users").document(uid).collection("mood_entries")
                .snapshots()
                .map { snap -> snap.toObjects(MoodEntry::class.java).sortedByDescending { it.date } }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveMoodEntry(entry: MoodEntry) {
        if (isGuest(entry.userId)) {
            val id = entry.id.ifBlank { "guest_mood_${UUID.randomUUID().toString().take(8)}" }
            val updated = entry.copy(id = id)
            InMemoryGuestStore.moodEntries.value = listOf(updated) + InMemoryGuestStore.moodEntries.value.filter { it.id != id }
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("mood_entries")
        val docRef = if (entry.id.isNotBlank()) colRef.document(entry.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "level" to entry.level,
            "levelLabel" to entry.levelLabel,
            "feelings" to entry.feelings,
            "note" to entry.note,
            "date" to entry.date,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- ESCALAS CLÍNICAS (PHQ-9, GAD-7, EVA) ---
    fun observeClinicalAssessments(uid: String): Flow<List<ClinicalScaleAssessment>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.clinicalAssessments.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/clinical_assessments"
            db.collection("users").document(uid).collection("clinical_assessments")
                .snapshots()
                .map { snap -> snap.toObjects(ClinicalScaleAssessment::class.java).sortedByDescending { it.date } }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveClinicalAssessment(assessment: ClinicalScaleAssessment) {
        if (isGuest(assessment.userId)) {
            val id = assessment.id.ifBlank { "guest_scale_${UUID.randomUUID().toString().take(8)}" }
            val updated = assessment.copy(id = id)
            InMemoryGuestStore.clinicalAssessments.value = listOf(updated) + InMemoryGuestStore.clinicalAssessments.value.filter { it.id != id }
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("clinical_assessments")
        val docRef = if (assessment.id.isNotBlank()) colRef.document(assessment.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "scaleType" to assessment.scaleType,
            "scaleName" to assessment.scaleName,
            "score" to assessment.score,
            "maxScore" to assessment.maxScore,
            "severityLevel" to assessment.severityLevel,
            "interpretation" to assessment.interpretation,
            "answers" to assessment.answers,
            "date" to assessment.date,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- DIARIO DE PENSAMIENTOS ---
    fun observeJournalEntries(uid: String): Flow<List<JournalEntry>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.journalEntries.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/journal_entries"
            db.collection("users").document(uid).collection("journal_entries")
                .snapshots()
                .map { snap ->
                    snap.toObjects(JournalEntry::class.java)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveJournalEntry(entry: JournalEntry) {
        if (isGuest(entry.userId)) {
            val id = entry.id.ifBlank { "guest_journal_${UUID.randomUUID().toString().take(8)}" }
            val updated = entry.copy(id = id, createdAt = entry.createdAt ?: Timestamp.now())
            InMemoryGuestStore.journalEntries.value = listOf(updated) + InMemoryGuestStore.journalEntries.value.filter { it.id != id }
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("journal_entries")
        val docRef = if (entry.id.isNotBlank()) colRef.document(entry.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to entry.title,
            "content" to entry.content,
            "category" to entry.category,
            "createdAt" to (entry.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteJournalEntry(entryId: String) {
        if (isGuest()) {
            InMemoryGuestStore.journalEntries.value = InMemoryGuestStore.journalEntries.value.filter { it.id != entryId }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("journal_entries").document(entryId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- MEDICACIÓN Y TOMAS ---
    fun observeMedications(uid: String): Flow<List<Medication>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.medications.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/medications"
            db.collection("users").document(uid).collection("medications")
                .snapshots()
                .map { snap -> snap.toObjects(Medication::class.java).filter { it.active } }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    fun observeMedicationLogs(uid: String, date: String): Flow<List<MedicationLog>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.medicationLogs.map { logs ->
                logs.filter { it.date == date }
            }
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/medication_logs"
            db.collection("users").document(uid).collection("medication_logs")
                .whereEqualTo("date", date)
                .snapshots()
                .map { snap -> snap.toObjects(MedicationLog::class.java) }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveMedication(med: Medication) {
        if (isGuest(med.userId)) {
            val id = med.id.ifBlank { "guest_med_${UUID.randomUUID().toString().take(8)}" }
            val updated = med.copy(id = id, createdAt = med.createdAt ?: Timestamp.now())
            InMemoryGuestStore.medications.value = InMemoryGuestStore.medications.value.filter { it.id != id } + updated
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("medications")
        val docRef = if (med.id.isNotBlank()) colRef.document(med.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to med.name,
            "dosage" to med.dosage,
            "scheduledTimes" to med.scheduledTimes,
            "notes" to med.notes,
            "active" to med.active,
            "createdAt" to (med.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteMedication(medId: String) {
        if (isGuest()) {
            InMemoryGuestStore.medications.value = InMemoryGuestStore.medications.value.filter { it.id != medId }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("medications").document(medId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun recordMedicationTaken(med: Medication, date: String, scheduledTime: String) {
        if (isGuest(med.userId)) {
            val log = MedicationLog(
                userId = med.userId,
                medicationId = med.id,
                medicationName = med.name,
                dosage = med.dosage,
                date = date,
                scheduledTime = scheduledTime,
                takenAt = Timestamp.now()
            )
            InMemoryGuestStore.medicationLogs.value = InMemoryGuestStore.medicationLogs.value.filter {
                !(it.date == date && it.medicationId == med.id && it.scheduledTime == scheduledTime)
            } + log
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("medication_logs")
        val logDoc = colRef.document("${date}_${med.id}_${scheduledTime.replace(":", "")}")
        val payload = mapOf(
            "userId" to uid,
            "medicationId" to med.id,
            "medicationName" to med.name,
            "dosage" to med.dosage,
            "date" to date,
            "scheduledTime" to scheduledTime,
            "takenAt" to FieldValue.serverTimestamp()
        )
        try {
            logDoc.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logDoc.path)
            throw e
        }
    }

    suspend fun unrecordMedicationTaken(medId: String, date: String, scheduledTime: String) {
        if (isGuest()) {
            InMemoryGuestStore.medicationLogs.value = InMemoryGuestStore.medicationLogs.value.filter {
                !(it.date == date && it.medicationId == medId && it.scheduledTime == scheduledTime)
            }
            return
        }
        val uid = requireUserId()
        val logDoc = db.collection("users").document(uid).collection("medication_logs")
            .document("${date}_${medId}_${scheduledTime.replace(":", "")}")
        try {
            logDoc.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, logDoc.path)
            throw e
        }
    }

    // --- HÁBITOS ---
    fun observeHabits(uid: String): Flow<List<Habit>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.habits.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/habits"
            db.collection("users").document(uid).collection("habits")
                .snapshots()
                .map { snap -> snap.toObjects(Habit::class.java) }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveHabit(habit: Habit) {
        if (isGuest(habit.userId)) {
            val id = habit.id.ifBlank { "guest_habit_${UUID.randomUUID().toString().take(8)}" }
            val updated = habit.copy(id = id, createdAt = habit.createdAt ?: Timestamp.now())
            InMemoryGuestStore.habits.value = InMemoryGuestStore.habits.value.filter { it.id != id } + updated
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("habits")
        val docRef = if (habit.id.isNotBlank()) colRef.document(habit.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "name" to habit.name,
            "category" to habit.category,
            "timeOfDay" to habit.timeOfDay,
            "streakDays" to habit.streakDays,
            "completedDates" to habit.completedDates,
            "createdAt" to (habit.createdAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteHabit(habitId: String) {
        if (isGuest()) {
            InMemoryGuestStore.habits.value = InMemoryGuestStore.habits.value.filter { it.id != habitId }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun toggleHabitDate(habit: Habit, date: String) {
        val isCompleted = habit.completedDates.contains(date)
        val updatedDates = if (isCompleted) {
            habit.completedDates - date
        } else {
            habit.completedDates + date
        }
        val streak = calculateStreak(updatedDates)
        saveHabit(habit.copy(completedDates = updatedDates, streakDays = streak))
    }

    private fun calculateStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        return dates.distinct().size
    }

    // --- AGENDA / CITAS ---
    fun observeCalendarEvents(uid: String): Flow<List<CalendarEvent>> {
        if (isGuest(uid)) {
            return InMemoryGuestStore.calendarEvents.asStateFlow()
        }
        if (uid.isBlank()) {
            return flowOf(emptyList())
        }
        return flow {
            val path = "users/$uid/calendar_events"
            db.collection("users").document(uid).collection("calendar_events")
                .snapshots()
                .map { snap -> snap.toObjects(CalendarEvent::class.java) }
                .catch { e ->
                    if (e is Exception && auth.currentUser?.uid == uid) {
                        handleFirestoreError(e, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
                .collect { emit(it) }
        }
    }

    suspend fun saveCalendarEvent(event: CalendarEvent) {
        if (isGuest(event.userId)) {
            val id = event.id.ifBlank { "guest_ev_${UUID.randomUUID().toString().take(8)}" }
            val updated = event.copy(id = id)
            InMemoryGuestStore.calendarEvents.value = InMemoryGuestStore.calendarEvents.value.filter { it.id != id } + updated
            return
        }
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("calendar_events")
        val docRef = if (event.id.isNotBlank()) colRef.document(event.id) else colRef.document()
        val payload = mapOf(
            "userId" to uid,
            "title" to event.title,
            "type" to event.type,
            "date" to event.date,
            "time" to event.time,
            "notes" to event.notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCalendarEvent(eventId: String) {
        if (isGuest()) {
            InMemoryGuestStore.calendarEvents.value = InMemoryGuestStore.calendarEvents.value.filter { it.id != eventId }
            return
        }
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("calendar_events").document(eventId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
