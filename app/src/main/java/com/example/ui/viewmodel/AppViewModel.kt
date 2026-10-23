package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CalendarEvent
import com.example.data.model.ClinicalScaleAssessment
import com.example.data.model.Habit
import com.example.data.model.JournalEntry
import com.example.data.model.Medication
import com.example.data.model.MedicationLog
import com.example.data.model.MoodEntry
import com.example.data.model.UserProfile
import com.example.data.repository.CuentaConmigoRepository
import com.example.ui.components.PendingGoogleProfileSync
import com.example.util.CrisisAssessment
import com.example.util.CrisisDetectionHelper
import com.example.util.ImbalanceSeverity
import com.example.util.MedicationAlarmPlayer
import com.example.util.StreetPrivacyDetector
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "AppViewModel"

data class PendingDose(
    val medication: Medication,
    val scheduledTime: String,
    val isTestDose: Boolean = false
)

data class AiDisguiseAliases(
    val diaryAlias: String = "Nota de organización y pendientes del día",
    val moodAlias: String = "Nivel de energía y jornada: Estable",
    val pillAlias: String = "Suplemento vitamínico • Hidratación",
    val agendaAlias: String = "Reunión personal programada"
)

enum class AppTab(val label: String) {
    INICIO("Inicio"),
    DIARIO("Diario"),
    ANIMO("Ánimo"),
    PASTILLAS("Pastillas"),
    HABITOS("Hábitos"),
    AGENDA("Agenda")
}

class AppViewModel(
    application: Application,
    private val repository: CuentaConmigoRepository,
    val currentUserId: String,
    initialStreetMode: Boolean = false
) : AndroidViewModel(application) {

    private val alarmPlayer = MedicationAlarmPlayer(application)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayDateString: String = dateFormat.format(Date())

    private val _currentTab = MutableStateFlow(AppTab.INICIO)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- ESCUDO DE PRIVACIDAD "MODO CALLE" + SENSOR DE MOVIMIENTO + CAMUFLAJE IA ---
    private val _isStreetModeActive = MutableStateFlow(initialStreetMode)
    val isStreetModeActive: StateFlow<Boolean> = _isStreetModeActive.asStateFlow()

    private val _streetModeTriggerReason = MutableStateFlow(
        if (initialStreetMode) "Activado al iniciar sesión en modo discreto" else ""
    )
    val streetModeTriggerReason: StateFlow<String> = _streetModeTriggerReason.asStateFlow()

    private val _autoMotionPrivacyEnabled = MutableStateFlow(true)
    val autoMotionPrivacyEnabled: StateFlow<Boolean> = _autoMotionPrivacyEnabled.asStateFlow()

    private val _isAiDisguiseEnabled = MutableStateFlow(false)
    val isAiDisguiseEnabled: StateFlow<Boolean> = _isAiDisguiseEnabled.asStateFlow()

    private val _isGeneratingAiCamouflage = MutableStateFlow(false)
    val isGeneratingAiCamouflage: StateFlow<Boolean> = _isGeneratingAiCamouflage.asStateFlow()

    private val _aiPrivacyStatusMessage = MutableStateFlow<String?>(null)
    val aiPrivacyStatusMessage: StateFlow<String?> = _aiPrivacyStatusMessage.asStateFlow()

    private val _aiDisguiseAliases = MutableStateFlow(AiDisguiseAliases())
    val aiDisguiseAliases: StateFlow<AiDisguiseAliases> = _aiDisguiseAliases.asStateFlow()

    private val _temporarilyRevealedIds = MutableStateFlow<Set<String>>(emptySet())
    val temporarilyRevealedIds: StateFlow<Set<String>> = _temporarilyRevealedIds.asStateFlow()

    private val _showStreetPrivacyDialog = MutableStateFlow(false)
    val showStreetPrivacyDialog: StateFlow<Boolean> = _showStreetPrivacyDialog.asStateFlow()

    private val streetPrivacyDetector = StreetPrivacyDetector(application) { reason ->
        if (_autoMotionPrivacyEnabled.value && !_isStreetModeActive.value) {
            _isStreetModeActive.value = true
            _streetModeTriggerReason.value = reason
            _temporarilyRevealedIds.value = emptySet()
        }
    }

    val isMotionSensorAvailable: Boolean
        get() = streetPrivacyDetector.isSensorAvailable

    private val camouflageSchema = Schema.obj(
        properties = mapOf(
            "diaryAlias" to Schema.string(description = "Neutral everyday productivity note title in Spanish"),
            "moodAlias" to Schema.string(description = "Neutral daily energy or routine status in Spanish"),
            "pillAlias" to Schema.string(description = "Neutral daily vitamin or hydration reminder in Spanish"),
            "agendaAlias" to Schema.string(description = "Neutral personal errand or meeting title in Spanish")
        )
    )

    private val privacyAiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = camouflageSchema
            }
        )
    }

    fun toggleStreetModeQuick() {
        val next = !_isStreetModeActive.value
        _isStreetModeActive.value = next
        _temporarilyRevealedIds.value = emptySet()
        _streetModeTriggerReason.value = if (next) {
            "Modo Calle activo • Datos sensibles ocultos"
        } else {
            ""
        }
    }

    fun setStreetMode(enabled: Boolean, reason: String = "Activado manualmente") {
        _isStreetModeActive.value = enabled
        _temporarilyRevealedIds.value = emptySet()
        _streetModeTriggerReason.value = if (enabled) reason else ""
    }

    fun setAutoMotionPrivacy(enabled: Boolean) {
        _autoMotionPrivacyEnabled.value = enabled
        if (enabled) {
            streetPrivacyDetector.start()
        } else {
            streetPrivacyDetector.stop()
        }
    }

    fun setAiDisguiseEnabled(enabled: Boolean) {
        _isAiDisguiseEnabled.value = enabled
        if (enabled && !_isStreetModeActive.value) {
            _isStreetModeActive.value = true
            _streetModeTriggerReason.value = "Modo Calle + Camuflaje IA activo"
        }
        if (enabled) {
            generateAiCamouflageAliases()
        }
    }

    fun simulateStreetMotionTrigger() {
        _isStreetModeActive.value = true
        _temporarilyRevealedIds.value = emptySet()
        _streetModeTriggerReason.value = "Activado automáticamente al detectar movimiento en calle"
        _showStreetPrivacyDialog.value = false
    }

    fun openStreetPrivacyDialog() {
        _showStreetPrivacyDialog.value = true
    }

    fun closeStreetPrivacyDialog() {
        _showStreetPrivacyDialog.value = false
    }

    /**
     * Revela temporalmente un elemento protegido durante 5 segundos cuando el usuario lo toca.
     */
    fun revealPrivacyItemTemporarily(itemId: String) {
        viewModelScope.launch {
            _temporarilyRevealedIds.value = _temporarilyRevealedIds.value + itemId
            delay(5_000L)
            _temporarilyRevealedIds.value = _temporarilyRevealedIds.value - itemId
        }
    }

    /**
     * Utiliza Firebase AI Logic (gemini-3.8-flash) para generar textos de cobertura neutros
     * (productividad, recados, vitaminas) para mostrar en público cuando el Camuflaje IA está activo.
     */
    fun generateAiCamouflageAliases() {
        if (_isGeneratingAiCamouflage.value) return
        viewModelScope.launch {
            _isGeneratingAiCamouflage.value = true
            _aiPrivacyStatusMessage.value = null
            try {
                val prompt = """
                    Genera 4 frases cortas, naturales y completamente neutras en español (máximo 45 caracteres cada una)
                    para proteger la privacidad visual de una pantalla de móvil en la calle o transporte público:
                    1. diaryAlias: una nota cotidiana de productividad o lista de compras.
                    2. moodAlias: un resumen neutro de ritmo de trabajo o energía diaria.
                    3. pillAlias: un recordatorio inocuo de hidratación o vitamina C.
                    4. agendaAlias: una gestión administrativa o trámite cotidiano.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    privacyAiModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Respuesta vacía del modelo IA.")
                val json = JSONObject(jsonString)

                val newAliases = AiDisguiseAliases(
                    diaryAlias = json.optString("diaryAlias", "Nota de organización y pendientes del día").ifBlank {
                        "Nota de organización y pendientes del día"
                    },
                    moodAlias = json.optString("moodAlias", "Nivel de energía y jornada: Estable").ifBlank {
                        "Nivel de energía y jornada: Estable"
                    },
                    pillAlias = json.optString("pillAlias", "Suplemento vitamínico • Hidratación").ifBlank {
                        "Suplemento vitamínico • Hidratación"
                    },
                    agendaAlias = json.optString("agendaAlias", "Reunión personal programada").ifBlank {
                        "Reunión personal programada"
                    }
                )
                _aiDisguiseAliases.value = newAliases
                _isAiDisguiseEnabled.value = true
                if (!_isStreetModeActive.value) {
                    _isStreetModeActive.value = true
                    _streetModeTriggerReason.value = "Modo Calle + Camuflaje IA generado"
                }
                _aiPrivacyStatusMessage.value = "Alias de camuflaje IA actualizados con éxito."
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Camouflage generation timed out", e)
                _aiPrivacyStatusMessage.value = "Error: Tiempo de espera agotado al conectar con IA (60s)."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI Camouflage generation failed", e)
                _aiPrivacyStatusMessage.value = "Error al generar camuflaje IA: ${e.localizedMessage ?: "Fallo de servicio"}"
            } finally {
                _isGeneratingAiCamouflage.value = false
            }
        }
    }

    // Perfil
    val profile: StateFlow<UserProfile?> = repository.observeProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Estados de ánimo
    val moodEntries: StateFlow<List<MoodEntry>> = repository.observeMoodEntries(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Escalas Clínicas (PHQ-9, GAD-7, EVA)
    val clinicalAssessments: StateFlow<List<ClinicalScaleAssessment>> = repository.observeClinicalAssessments(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Diario
    val journalEntries: StateFlow<List<JournalEntry>> = repository.observeJournalEntries(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtros de diario
    private val _journalSearchQuery = MutableStateFlow("")
    val journalSearchQuery = _journalSearchQuery.asStateFlow()

    private val _journalCategoryFilter = MutableStateFlow("Todos")
    val journalCategoryFilter = _journalCategoryFilter.asStateFlow()

    fun setJournalSearchQuery(query: String) {
        _journalSearchQuery.value = query
    }

    fun setJournalCategoryFilter(cat: String) {
        _journalCategoryFilter.value = cat
    }

    // Pastillas
    val medications: StateFlow<List<Medication>> = repository.observeMedications(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayMedLogs: StateFlow<List<MedicationLog>> = repository.observeMedicationLogs(currentUserId, todayDateString)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Estado de alarma persistente de pastillas
    private val _activeAlarmDoses = MutableStateFlow<List<PendingDose>>(emptyList())
    val activeAlarmDoses: StateFlow<List<PendingDose>> = _activeAlarmDoses.asStateFlow()

    // Registro local en sesión de tomas confirmadas para evitar re-disparo mientras sincroniza Firestore
    private val confirmedInSession = mutableSetOf<String>()

    init {
        if (_autoMotionPrivacyEnabled.value) {
            streetPrivacyDetector.start()
        }

        // Sincronizar perfil de Google de forma segura dentro de viewModelScope (nunca se cancela al desmontar AuthScreen)
        viewModelScope.launch {
            if (!repository.isGuest(currentUserId)) {
                val authUser = Firebase.auth.currentUser
                if (authUser != null && authUser.uid == currentUserId) {
                    val overrideName = PendingGoogleProfileSync.consumeDisplayName()
                    repository.syncUserProfileFromAuth(authUser, overrideName = overrideName)
                }
            }
        }

        // Evaluación reactiva inmediata apenas cargan o cambian las pastillas o registros
        viewModelScope.launch {
            combine(medications, todayMedLogs) { meds, logs -> meds to logs }
                .collectLatest {
                    checkScheduledMedicationAlarms()
                }
        }
        // Monitoreo periódico cada 10 segundos para activar la alarma puntualmente al cambiar el minuto
        viewModelScope.launch {
            while (true) {
                checkScheduledMedicationAlarms()
                delay(10_000L)
            }
        }
    }

    // Hábitos
    val habits: StateFlow<List<Habit>> = repository.observeHabits(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _habitTimeFilter = MutableStateFlow("Todos")
    val habitTimeFilter = _habitTimeFilter.asStateFlow()

    private val _habitCategoryFilter = MutableStateFlow("Todos")
    val habitCategoryFilter = _habitCategoryFilter.asStateFlow()

    fun setHabitTimeFilter(time: String) {
        _habitTimeFilter.value = time
    }

    fun setHabitCategoryFilter(cat: String) {
        _habitCategoryFilter.value = cat
    }

    // Agenda / Citas
    val calendarEvents: StateFlow<List<CalendarEvent>> = repository.observeCalendarEvents(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCalendarDate = MutableStateFlow(todayDateString)
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    fun selectCalendarDate(date: String) {
        _selectedCalendarDate.value = date
    }

    // Modales y diálogos
    var showProfileDialog = MutableStateFlow(false)
    var showNewJournalDialog = MutableStateFlow(false)
    var showNewMedDialog = MutableStateFlow(false)
    var showNewHabitDialog = MutableStateFlow(false)
    var showNewEventDialog = MutableStateFlow(false)

    // Sistema de Apoyo Emocional y Crisis SOS
    private val _showCrisisSupportDialog = MutableStateFlow(false)
    val showCrisisSupportDialog: StateFlow<Boolean> = _showCrisisSupportDialog.asStateFlow()

    private val _activeProactiveSupport = MutableStateFlow<CrisisAssessment?>(null)
    val activeProactiveSupport: StateFlow<CrisisAssessment?> = _activeProactiveSupport.asStateFlow()

    fun openCrisisSupport() {
        _showCrisisSupportDialog.value = true
    }

    fun closeCrisisSupport() {
        _showCrisisSupportDialog.value = false
    }

    fun dismissProactiveSupport() {
        _activeProactiveSupport.value = null
    }

    fun triggerProactiveEvaluation(level: Int? = null, text: String = "", feelings: List<String> = emptyList()) {
        val assessment = CrisisDetectionHelper.evaluate(level = level, textContent = text, feelings = feelings)
        if (assessment.hasImbalance) {
            _activeProactiveSupport.value = assessment
        }
    }

    // Operaciones
    fun updateProfileName(name: String, email: String) {
        viewModelScope.launch {
            repository.saveProfile(name, email)
        }
    }

    fun saveMood(level: Int, label: String, feelings: List<String>, note: String) {
        viewModelScope.launch {
            val entry = MoodEntry(
                userId = currentUserId,
                level = level,
                levelLabel = label,
                feelings = feelings,
                note = note,
                date = todayDateString
            )
            repository.saveMoodEntry(entry)

            // Detección proactiva de desbalance emocional
            val assessment = CrisisDetectionHelper.evaluate(
                level = level,
                textContent = note,
                feelings = feelings
            )
            if (assessment.hasImbalance) {
                _activeProactiveSupport.value = assessment
            }
        }
    }

    fun saveClinicalAssessment(
        scaleType: String,
        scaleName: String,
        score: Int,
        maxScore: Int,
        severityLevel: String,
        interpretation: String,
        answers: List<Int>,
        triggerSupport: Boolean = false
    ) {
        viewModelScope.launch {
            val entry = ClinicalScaleAssessment(
                userId = currentUserId,
                scaleType = scaleType,
                scaleName = scaleName,
                score = score,
                maxScore = maxScore,
                severityLevel = severityLevel,
                interpretation = interpretation,
                answers = answers,
                date = todayDateString
            )
            repository.saveClinicalAssessment(entry)

            if (triggerSupport) {
                _activeProactiveSupport.value = CrisisAssessment(
                    hasImbalance = true,
                    severity = ImbalanceSeverity.HIGH,
                    triggerReason = "Resultado en evaluación $scaleName: $severityLevel. Estamos contigo."
                )
            }
        }
    }

    fun saveJournal(title: String, content: String, category: String) {
        viewModelScope.launch {
            val entry = JournalEntry(
                userId = currentUserId,
                title = title,
                content = content,
                category = category
            )
            repository.saveJournalEntry(entry)

            // Detección proactiva ante palabras de crisis en diario
            val assessment = CrisisDetectionHelper.evaluate(
                textContent = "$title $content"
            )
            if (assessment.hasImbalance) {
                _activeProactiveSupport.value = assessment
            }
        }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch {
            repository.deleteJournalEntry(id)
        }
    }

    fun addMedication(name: String, dosage: String, times: List<String>, notes: String) {
        viewModelScope.launch {
            val sortedTimes = times.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
            val med = Medication(
                userId = currentUserId,
                name = name,
                dosage = dosage,
                scheduledTimes = sortedTimes,
                notes = notes
            )
            repository.saveMedication(med)
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            repository.deleteMedication(id)
        }
    }

    fun toggleMedicationTaken(med: Medication, scheduledTime: String, isTaken: Boolean) {
        viewModelScope.launch {
            val sessionKey = "${med.id}_$scheduledTime"
            if (isTaken) {
                confirmedInSession.remove(sessionKey)
                repository.unrecordMedicationTaken(med.id, todayDateString, scheduledTime)
            } else {
                confirmedInSession.add(sessionKey)
                repository.recordMedicationTaken(med, todayDateString, scheduledTime)
            }
        }
    }

    fun addHabit(name: String, category: String, timeOfDay: String) {
        viewModelScope.launch {
            val habit = Habit(
                userId = currentUserId,
                name = name,
                category = category,
                timeOfDay = timeOfDay
            )
            repository.saveHabit(habit)
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }

    fun toggleHabitToday(habit: Habit) {
        viewModelScope.launch {
            repository.toggleHabitDate(habit, todayDateString)
        }
    }

    fun addCalendarEvent(title: String, type: String, date: String, time: String, notes: String) {
        viewModelScope.launch {
            val ev = CalendarEvent(
                userId = currentUserId,
                title = title,
                type = type,
                date = date,
                time = time,
                notes = notes
            )
            repository.saveCalendarEvent(ev)
        }
    }

    fun deleteCalendarEvent(id: String) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
        }
    }

    // --- SISTEMA DE ALARMA DE PASTILLAS Y CONTROL DE APAGADO ---

    private fun checkScheduledMedicationAlarms() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentMinutesOfDay = currentHour * 60 + currentMinute

        val currentMeds = medications.value
        val logs = todayMedLogs.value

        val pending = mutableListOf<PendingDose>()
        for (med in currentMeds) {
            for (timeStr in med.scheduledTimes) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0
                    val schedMinutes = hour * 60 + minute

                    val sessionKey = "${med.id}_$timeStr"
                    val isTaken = confirmedInSession.contains(sessionKey) ||
                        logs.any { it.medicationId == med.id && it.scheduledTime == timeStr }

                    if (!isTaken && currentMinutesOfDay >= schedMinutes && (currentMinutesOfDay - schedMinutes) < 180) {
                        pending.add(PendingDose(med, timeStr, isTestDose = false))
                    }
                }
            }
        }

        if (pending.isNotEmpty() && _activeAlarmDoses.value.isEmpty()) {
            _activeAlarmDoses.value = pending
            val summary = if (_isStreetModeActive.value) {
                "Tienes un recordatorio programado en Cuenta Conmigo"
            } else {
                "Pendiente: " + pending.joinToString(", ") { "${it.medication.name} (${it.medication.dosage}) a las ${it.scheduledTime}" }
            }
            alarmPlayer.startAlarm(summary)
        }
    }

    fun triggerTestMedicationAlarm() {
        val currentMeds = medications.value
        val logs = todayMedLogs.value

        val pending = mutableListOf<PendingDose>()
        for (med in currentMeds) {
            for (timeStr in med.scheduledTimes) {
                val isTaken = logs.any { it.medicationId == med.id && it.scheduledTime == timeStr }
                if (!isTaken) {
                    pending.add(PendingDose(med, timeStr, isTestDose = false))
                }
            }
        }

        if (pending.isEmpty() && currentMeds.isNotEmpty()) {
            val firstMed = currentMeds.first()
            val time = firstMed.scheduledTimes.firstOrNull() ?: "08:00"
            pending.add(PendingDose(firstMed, time, isTestDose = true))
        }

        if (pending.isEmpty()) {
            val now = Calendar.getInstance()
            val currentTimeStr = String.format(
                Locale.US,
                "%02d:%02d",
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE)
            )
            val demoMed = Medication(
                id = "demo_test_pill",
                userId = currentUserId,
                name = "Pastilla de Prueba",
                dosage = "1 comprimido",
                scheduledTimes = listOf(currentTimeStr),
                notes = "Alarma de prueba activa. Confirma la toma para apagar el sonido."
            )
            pending.add(PendingDose(demoMed, currentTimeStr, isTestDose = true))
        }

        _activeAlarmDoses.value = pending
        val summary = if (_isStreetModeActive.value) {
            "Recordatorio personal activo en Cuenta Conmigo"
        } else {
            "Toma requerida: " + pending.joinToString(", ") { "${it.medication.name} (${it.medication.dosage}) - ${it.scheduledTime}" }
        }
        alarmPlayer.startAlarm(summary)
    }

    fun confirmDoseInAlarm(dose: PendingDose) {
        viewModelScope.launch {
            val sessionKey = "${dose.medication.id}_${dose.scheduledTime}"
            confirmedInSession.add(sessionKey)

            if (!dose.isTestDose && dose.medication.id != "demo_test_pill") {
                repository.recordMedicationTaken(dose.medication, todayDateString, dose.scheduledTime)
            }
            val updated = _activeAlarmDoses.value.filterNot {
                it.medication.id == dose.medication.id && it.scheduledTime == dose.scheduledTime
            }
            _activeAlarmDoses.value = updated
            if (updated.isEmpty()) {
                alarmPlayer.stopAlarm()
            }
        }
    }

    fun confirmAllAlarmDoses() {
        viewModelScope.launch {
            val dosesToConfirm = _activeAlarmDoses.value
            for (dose in dosesToConfirm) {
                val sessionKey = "${dose.medication.id}_${dose.scheduledTime}"
                confirmedInSession.add(sessionKey)
                if (!dose.isTestDose && dose.medication.id != "demo_test_pill") {
                    repository.recordMedicationTaken(dose.medication, todayDateString, dose.scheduledTime)
                }
            }
            _activeAlarmDoses.value = emptyList()
            alarmPlayer.stopAlarm()
        }
    }

    override fun onCleared() {
        super.onCleared()
        streetPrivacyDetector.stop()
        alarmPlayer.stopAlarm()
    }
}
