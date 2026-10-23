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

sealed interface AiFeatureUiState {
    data object Idle : AiFeatureUiState
    data object Loading : AiFeatureUiState
    data class Success(val text: String) : AiFeatureUiState
    data class Error(val message: String) : AiFeatureUiState
}

data class AiHabitSuggestion(
    val name: String,
    val category: String,
    val timeOfDay: String,
    val reason: String
)

sealed interface AiHabitUiState {
    data object Idle : AiHabitUiState
    data object Loading : AiHabitUiState
    data class Success(val suggestion: AiHabitSuggestion) : AiHabitUiState
    data class Error(val message: String) : AiHabitUiState
}

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

    private val wellnessAiModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    private val habitSuggestionSchema = Schema.obj(
        properties = mapOf(
            "name" to Schema.string(description = "Short concrete daily wellness habit name in Spanish"),
            "category" to Schema.enumeration(
                values = listOf("Salud Mental", "Físico y Sueño", "Social"),
                description = "Category of the habit"
            ),
            "timeOfDay" to Schema.enumeration(
                values = listOf("Mañana", "Tarde", "Noche"),
                description = "Recommended time of day"
            ),
            "reason" to Schema.string(description = "Brief compassionate explanation of why this habit helps today in Spanish")
        )
    )

    private val habitAiModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = habitSuggestionSchema
            }
        )
    }

    // --- ESTADOS DE INTELIGENCIA ARTIFICIAL EN TODA LA APLICACIÓN ---
    private val _homeAiInsightState = MutableStateFlow<AiFeatureUiState>(AiFeatureUiState.Idle)
    val homeAiInsightState: StateFlow<AiFeatureUiState> = _homeAiInsightState.asStateFlow()

    private val _diaryAiReflectionState = MutableStateFlow<AiFeatureUiState>(AiFeatureUiState.Idle)
    val diaryAiReflectionState: StateFlow<AiFeatureUiState> = _diaryAiReflectionState.asStateFlow()

    private val _moodAiInsightState = MutableStateFlow<AiFeatureUiState>(AiFeatureUiState.Idle)
    val moodAiInsightState: StateFlow<AiFeatureUiState> = _moodAiInsightState.asStateFlow()

    private val _medicationAiTipState = MutableStateFlow<AiFeatureUiState>(AiFeatureUiState.Idle)
    val medicationAiTipState: StateFlow<AiFeatureUiState> = _medicationAiTipState.asStateFlow()

    private val _habitAiSuggestionState = MutableStateFlow<AiHabitUiState>(AiHabitUiState.Idle)
    val habitAiSuggestionState: StateFlow<AiHabitUiState> = _habitAiSuggestionState.asStateFlow()

    private val _agendaAiPrepState = MutableStateFlow<AiFeatureUiState>(AiFeatureUiState.Idle)
    val agendaAiPrepState: StateFlow<AiFeatureUiState> = _agendaAiPrepState.asStateFlow()

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

    fun revealPrivacyItemTemporarily(itemId: String) {
        viewModelScope.launch {
            _temporarilyRevealedIds.value = _temporarilyRevealedIds.value + itemId
            delay(5_000L)
            _temporarilyRevealedIds.value = _temporarilyRevealedIds.value - itemId
        }
    }

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

    // --- FLUJOS DE DATOS EN TIEMPO REAL (EAGERLY PARA RESPUESTA INMEDIATA) ---
    val profile: StateFlow<UserProfile?> = repository.observeProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val moodEntries: StateFlow<List<MoodEntry>> = repository.observeMoodEntries(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clinicalAssessments: StateFlow<List<ClinicalScaleAssessment>> = repository.observeClinicalAssessments(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.observeJournalEntries(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    // Pastillas y Alarmas
    val medications: StateFlow<List<Medication>> = repository.observeMedications(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayMedLogs: StateFlow<List<MedicationLog>> = repository.observeMedicationLogs(currentUserId, todayDateString)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeAlarmDoses = MutableStateFlow<List<PendingDose>>(emptyList())
    val activeAlarmDoses: StateFlow<List<PendingDose>> = _activeAlarmDoses.asStateFlow()

    private val _liveClockText = MutableStateFlow(
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    )
    val liveClockText: StateFlow<String> = _liveClockText.asStateFlow()

    private val _nextScheduledAlarmInfo = MutableStateFlow("Verificando horarios de alarmas...")
    val nextScheduledAlarmInfo: StateFlow<String> = _nextScheduledAlarmInfo.asStateFlow()

    private val confirmedInSession = mutableSetOf<String>()
    private val snoozedUntilMillis = mutableMapOf<String, Long>()
    private val explicitlyArmedKeys = mutableSetOf<String>()

    // Hábitos
    val habits: StateFlow<List<Habit>> = repository.observeHabits(currentUserId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedCalendarDate = MutableStateFlow(todayDateString)
    val selectedCalendarDate = _selectedCalendarDate.asStateFlow()

    fun selectCalendarDate(date: String) {
        _selectedCalendarDate.value = date
    }

    init {
        if (_autoMotionPrivacyEnabled.value) {
            streetPrivacyDetector.start()
        }

        // Sincronizar perfil de Google de forma segura dentro de viewModelScope
        viewModelScope.launch {
            if (!repository.isGuest(currentUserId)) {
                val authUser = Firebase.auth.currentUser
                if (authUser != null && authUser.uid == currentUserId) {
                    val overrideName = PendingGoogleProfileSync.consumeDisplayName()
                    repository.syncUserProfileFromAuth(authUser, overrideName = overrideName)
                }
            }
        }

        // Evaluación reactiva apenas cambian las pastillas o los registros de hoy
        viewModelScope.launch {
            combine(medications, todayMedLogs) { meds, logs -> meds to logs }
                .collectLatest { (meds, _) ->
                    for (med in meds) {
                        alarmPlayer.scheduleSystemAlarmsForToday(
                            medId = med.id,
                            medName = med.name,
                            dosage = med.dosage,
                            times = med.scheduledTimes
                        )
                    }
                    checkScheduledMedicationAlarms()
                }
        }

        // Reloj en tiempo real y monitoreo de alarmas cada 1 segundo exacto
        viewModelScope.launch {
            val clockFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
            while (true) {
                _liveClockText.value = clockFmt.format(Date())
                checkScheduledMedicationAlarms()
                delay(1_000L)
            }
        }
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

    // --- FUNCIONES DE INTELIGENCIA ARTIFICIAL (GEMINI 3.8 FLASH) EN CADA SECCIÓN ---

    fun generateHomeWellnessSummary() {
        if (_homeAiInsightState.value is AiFeatureUiState.Loading) return
        viewModelScope.launch {
            _homeAiInsightState.value = AiFeatureUiState.Loading
            try {
                val todayMood = moodEntries.value.firstOrNull { it.date == todayDateString }
                val totalMeds = medications.value.sumOf { it.scheduledTimes.size }
                val takenMeds = todayMedLogs.value.size
                val doneHabits = habits.value.count { it.completedDates.contains(todayDateString) }
                val totalHabits = habits.value.size
                val nextEvent = calendarEvents.value
                    .filter { it.date >= todayDateString }
                    .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
                    .firstOrNull()

                val prompt = """
                    Actúa como un psicólogo clínico y acompañante cálido de bienestar en la app "Cuenta Conmigo".
                    Datos actuales del usuario hoy ($todayDateString):
                    - Estado de ánimo hoy: ${todayMood?.let { "${it.levelLabel} (${it.feelings.joinToString(", ")})" } ?: "Aún sin registrar hoy"}
                    - Pastillas hoy: $takenMeds de $totalMeds tomas completadas.
                    - Hábitos hoy: $doneHabits de $totalHabits completados.
                    - Próxima cita en agenda: ${nextEvent?.let { "${it.title} (${it.type}) el ${it.date} a las ${it.time}" } ?: "Sin citas próximas"}
                    
                    Escribe en español un resumen personalizado, empático y práctico de 3 a 4 oraciones cortas con:
                    1. Un reconocimiento cálido de su progreso hoy.
                    2. Una micro-recomendación concreta para cuidar su bienestar en las próximas horas.
                    No uses formato markdown complejo ni asteriscos innecesarios.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    _homeAiInsightState.value = AiFeatureUiState.Success(text)
                } else {
                    _homeAiInsightState.value = AiFeatureUiState.Error("La IA devolvió una respuesta vacía.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Home AI summary timed out", e)
                _homeAiInsightState.value = AiFeatureUiState.Error("Tiempo de espera agotado (60s). Intenta de nuevo.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Home AI summary failed", e)
                _homeAiInsightState.value = AiFeatureUiState.Error(
                    e.localizedMessage ?: "No se pudo generar el análisis con IA."
                )
            }
        }
    }

    fun generateDiaryAiReflection(customDraft: String = "") {
        if (_diaryAiReflectionState.value is AiFeatureUiState.Loading) return
        viewModelScope.launch {
            _diaryAiReflectionState.value = AiFeatureUiState.Loading
            try {
                val recentEntries = journalEntries.value.take(3)
                val contextText = if (customDraft.isNotBlank()) {
                    "Borrador actual: $customDraft"
                } else if (recentEntries.isNotEmpty()) {
                    recentEntries.joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content.take(180)}" }
                } else {
                    "El usuario está comenzando su diario emocional y busca inspiración para reflexionar hoy."
                }

                val prompt = """
                    Actúa como un terapeuta cognitivo-conductual (TCC) compasivo dentro de "Cuenta Conmigo".
                    Analiza las siguientes reflexiones del diario del usuario:
                    $contextText
                    
                    Responde en español en máximo 4 oraciones claras y cálidas:
                    1. Valida sus emociones con empatía genuina.
                    2. Ofrece una reestructuración cognitiva amable o una pregunta reflexiva para su cuaderno.
                    Texto limpio sin símbolos markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    _diaryAiReflectionState.value = AiFeatureUiState.Success(text)
                } else {
                    _diaryAiReflectionState.value = AiFeatureUiState.Error("Respuesta vacía del asistente IA.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Diary AI reflection timed out", e)
                _diaryAiReflectionState.value = AiFeatureUiState.Error("Tiempo de espera agotado (60s).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Diary AI reflection failed", e)
                _diaryAiReflectionState.value = AiFeatureUiState.Error(
                    e.localizedMessage ?: "Error al conectar con el asistente IA."
                )
            }
        }
    }

    fun generateMoodAiAnalysis() {
        if (_moodAiInsightState.value is AiFeatureUiState.Loading) return
        viewModelScope.launch {
            _moodAiInsightState.value = AiFeatureUiState.Loading
            try {
                val recentMoods = moodEntries.value.take(5)
                val recentScales = clinicalAssessments.value.take(3)

                val moodsSummary = if (recentMoods.isNotEmpty()) {
                    recentMoods.joinToString("; ") {
                        "${it.date}: ${it.levelLabel} (nivel ${it.level}/5, emociones: ${it.feelings.joinToString(",")})"
                    }
                } else {
                    "Sin registros de estado de ánimo previos"
                }

                val scalesSummary = if (recentScales.isNotEmpty()) {
                    recentScales.joinToString("; ") {
                        "${it.scaleName}: puntaje ${it.score}/${it.maxScore} (${it.severityLevel})"
                    }
                } else {
                    "Sin evaluaciones clínicas recientes"
                }

                val prompt = """
                    Eres un especialista en bienestar emocional dentro de "Cuenta Conmigo".
                    Registros recientes de ánimo: $moodsSummary
                    Evaluaciones clínicas recientes (PHQ-9/GAD-7/EVA): $scalesSummary
                    
                    Brinda en español una orientación breve y empática (3 a 4 oraciones):
                    1. Observa la tendencia emocional con calidez y sin juzgar.
                    2. Sugiere un ejercicio práctico de regulación emocional o respiración para hoy.
                    Sin etiquetas markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    _moodAiInsightState.value = AiFeatureUiState.Success(text)
                } else {
                    _moodAiInsightState.value = AiFeatureUiState.Error("Respuesta vacía del modelo IA.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Mood AI analysis timed out", e)
                _moodAiInsightState.value = AiFeatureUiState.Error("Tiempo de espera agotado (60s).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Mood AI analysis failed", e)
                _moodAiInsightState.value = AiFeatureUiState.Error(
                    e.localizedMessage ?: "Error al analizar estado de ánimo con IA."
                )
            }
        }
    }

    fun generateMedicationAiGuidance() {
        if (_medicationAiTipState.value is AiFeatureUiState.Loading) return
        viewModelScope.launch {
            _medicationAiTipState.value = AiFeatureUiState.Loading
            try {
                val medsList = medications.value
                val medsDescription = if (medsList.isNotEmpty()) {
                    medsList.joinToString("; ") {
                        "${it.name} (${it.dosage}) en horarios ${it.scheduledTimes.joinToString(", ")} [notas: ${it.notes.ifBlank { "ninguna" }}]"
                    }
                } else {
                    "El usuario aún no ha registrado pastillas o está configurando sus primeras alarmas."
                }

                val prompt = """
                    Actúa como un asistente de adherencia terapéutica y organización diaria en "Cuenta Conmigo".
                    Pastillas y horarios configurados por el usuario: $medsDescription
                    
                    Redacta en español un consejo práctico y motivador (3 a 4 oraciones) sobre:
                    1. Cómo anclar la toma de sus pastillas a rutinas diarias (desayuno, cepillado, vaso de agua visible) para no olvidar ninguna alarma.
                    2. Un recordatorio amable de consultar siempre cualquier ajuste de dosis con su médico tratante.
                    Sin asteriscos ni markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    _medicationAiTipState.value = AiFeatureUiState.Success(text)
                } else {
                    _medicationAiTipState.value = AiFeatureUiState.Error("Respuesta vacía de la IA.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Medication AI tip timed out", e)
                _medicationAiTipState.value = AiFeatureUiState.Error("Tiempo de espera agotado (60s).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Medication AI tip failed", e)
                _medicationAiTipState.value = AiFeatureUiState.Error(
                    e.localizedMessage ?: "No se pudo obtener la orientación con IA."
                )
            }
        }
    }

    fun generateHabitAiSuggestion() {
        if (_habitAiSuggestionState.value is AiHabitUiState.Loading) return
        viewModelScope.launch {
            _habitAiSuggestionState.value = AiHabitUiState.Loading
            try {
                val existingHabits = habits.value.joinToString(", ") { it.name }
                val latestMood = moodEntries.value.firstOrNull()?.levelLabel ?: "Equilibrado"

                val prompt = """
                    Sugiere un nuevo micro-hábito diario saludable y fácil de cumplir en español para un usuario cuyo ánimo reciente es "$latestMood".
                    Hábitos que ya tiene registrados (no repetir): ${existingHabits.ifBlank { "ninguno" }}.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    habitAiModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Respuesta vacía del modelo IA.")
                val json = JSONObject(jsonString)

                val suggestion = AiHabitSuggestion(
                    name = json.optString("name", "Respiración consciente durante 3 minutos").ifBlank {
                        "Respiración consciente durante 3 minutos"
                    },
                    category = json.optString("category", "Salud Mental").ifBlank { "Salud Mental" },
                    timeOfDay = json.optString("timeOfDay", "Mañana").ifBlank { "Mañana" },
                    reason = json.optString(
                        "reason",
                        "Ayuda a reducir la tensión fisiológica y centrar la atención con suavidad."
                    )
                )
                _habitAiSuggestionState.value = AiHabitUiState.Success(suggestion)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Habit AI suggestion timed out", e)
                _habitAiSuggestionState.value = AiHabitUiState.Error("Tiempo de espera agotado (60s).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Habit AI suggestion failed", e)
                _habitAiSuggestionState.value = AiHabitUiState.Error(
                    e.localizedMessage ?: "Error al generar hábito con IA."
                )
            }
        }
    }

    fun generateAgendaAiPreparation(targetDate: String) {
        if (_agendaAiPrepState.value is AiFeatureUiState.Loading) return
        viewModelScope.launch {
            _agendaAiPrepState.value = AiFeatureUiState.Loading
            try {
                val dateEvents = calendarEvents.value.filter { it.date == targetDate }
                val upcomingEvents = calendarEvents.value
                    .filter { it.date >= todayDateString }
                    .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
                    .take(3)
                val recentMoods = moodEntries.value.take(4)
                val medsCount = medications.value.size

                val eventContext = when {
                    dateEvents.isNotEmpty() ->
                        "Citas del día $targetDate: " + dateEvents.joinToString("; ") { "${it.title} (${it.type}) a las ${it.time} - Notas: ${it.notes}" }
                    upcomingEvents.isNotEmpty() ->
                        "Próximas citas en agenda: " + upcomingEvents.joinToString("; ") { "${it.title} (${it.type}) el ${it.date} a las ${it.time}" }
                    else ->
                        "Día $targetDate sin citas médicas programadas aún."
                }

                val moodContext = if (recentMoods.isNotEmpty()) {
                    recentMoods.joinToString(", ") { "${it.date}: ${it.levelLabel}" }
                } else {
                    "Sin registros recientes"
                }

                val prompt = """
                    Actúa como un asistente inteligente de agenda clínica y bienestar en "Cuenta Conmigo".
                    Contexto de agenda: $eventContext
                    Ánimo reciente del usuario: $moodContext
                    Pastillas activas bajo seguimiento: $medsCount
                    
                    Genera en español una guía breve y estructurada (máximo 4 puntos cortos):
                    1. Si hay cita médica o psicológica, sugiere 2 o 3 preguntas o puntos clave para conversar con el profesional según su ánimo y seguimiento.
                    2. Si no hay cita ese día, ofrece una sugerencia para organizar su día con calma y equilibrio.
                    Usa texto limpio sin símbolos markdown complejos.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                if (!text.isNullOrBlank()) {
                    _agendaAiPrepState.value = AiFeatureUiState.Success(text)
                } else {
                    _agendaAiPrepState.value = AiFeatureUiState.Error("Respuesta vacía de la IA.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Agenda AI prep timed out", e)
                _agendaAiPrepState.value = AiFeatureUiState.Error("Tiempo de espera agotado (60s).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Agenda AI prep failed", e)
                _agendaAiPrepState.value = AiFeatureUiState.Error(
                    e.localizedMessage ?: "Error al preparar guía de agenda con IA."
                )
            }
        }
    }

    // --- OPERACIONES DE DATOS ---
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
                name = name.trim(),
                dosage = dosage.trim(),
                scheduledTimes = sortedTimes,
                notes = notes.trim()
            )
            sortedTimes.forEach { t ->
                val key = "${med.id}_$t"
                confirmedInSession.remove(key)
                snoozedUntilMillis.remove(key)
                explicitlyArmedKeys.add(key)
            }
            repository.saveMedication(med)
            alarmPlayer.scheduleSystemAlarmsForToday(med.id, med.name, med.dosage, sortedTimes)
            checkScheduledMedicationAlarms()
        }
    }

    fun updateMedication(med: Medication, name: String, dosage: String, times: List<String>, notes: String) {
        viewModelScope.launch {
            val sortedTimes = times.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
            med.scheduledTimes.forEach { oldTime ->
                confirmedInSession.remove("${med.id}_$oldTime")
                snoozedUntilMillis.remove("${med.id}_$oldTime")
            }
            sortedTimes.forEach { newTime ->
                val key = "${med.id}_$newTime"
                confirmedInSession.remove(key)
                snoozedUntilMillis.remove(key)
                explicitlyArmedKeys.add(key)
            }
            val updated = med.copy(
                name = name.trim(),
                dosage = dosage.trim(),
                scheduledTimes = sortedTimes,
                notes = notes.trim()
            )
            repository.saveMedication(updated)
            alarmPlayer.scheduleSystemAlarmsForToday(updated.id, updated.name, updated.dosage, sortedTimes)
            checkScheduledMedicationAlarms()
        }
    }

    /**
     * Programador directo de alarma para una pastilla nueva o existente.
     * Guarda la hora exacta HH:mm, limpia bloqueos previos y activa la alarma.
     */
    fun programMedicationAlarm(
        existingMed: Medication?,
        name: String,
        dosage: String,
        timeStr: String
    ) {
        viewModelScope.launch {
            val cleanTime = timeStr.trim()
            val cleanName = name.trim().ifBlank { existingMed?.name ?: "Mi Pastilla" }
            val cleanDosage = dosage.trim().ifBlank { existingMed?.dosage ?: "1 dosis" }

            val savedMed = if (existingMed != null) {
                existingMed.scheduledTimes.forEach { oldT ->
                    confirmedInSession.remove("${existingMed.id}_$oldT")
                    snoozedUntilMillis.remove("${existingMed.id}_$oldT")
                }
                existingMed.copy(
                    name = cleanName,
                    dosage = cleanDosage,
                    scheduledTimes = listOf(cleanTime)
                )
            } else {
                Medication(
                    userId = currentUserId,
                    name = cleanName,
                    dosage = cleanDosage,
                    scheduledTimes = listOf(cleanTime),
                    notes = ""
                )
            }

            val sessionKey = "${savedMed.id}_$cleanTime"
            confirmedInSession.remove(sessionKey)
            snoozedUntilMillis.remove(sessionKey)
            explicitlyArmedKeys.add(sessionKey)

            // Si ya estaba marcada como tomada hoy a esa hora, desmarcar para que suene la nueva alarma
            repository.unrecordMedicationTaken(savedMed.id, todayDateString, cleanTime)
            repository.saveMedication(savedMed)
            alarmPlayer.scheduleSystemAlarmsForToday(
                medId = savedMed.id,
                medName = savedMed.name,
                dosage = savedMed.dosage,
                times = listOf(cleanTime)
            )

            // Evaluar inmediatamente si la hora colocada ya llegó o es el minuto actual
            val now = Calendar.getInstance()
            val currentMinOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val parts = cleanTime.split(":")
            val schedMinOfDay = if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
            } else {
                currentMinOfDay
            }

            if (schedMinOfDay == currentMinOfDay) {
                triggerAlarmForMedication(savedMed)
            } else {
                checkScheduledMedicationAlarms()
            }
        }
    }

    fun scheduleQuickAlarmInOneMinute() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
        }
        val nextMinStr = String.format(
            Locale.US,
            "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE)
        )
        val firstMed = medications.value.firstOrNull()
        programMedicationAlarm(
            existingMed = firstMed,
            name = firstMed?.name ?: "Mi Pastilla",
            dosage = firstMed?.dosage ?: "1 comprimido",
            timeStr = nextMinStr
        )
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
                snoozedUntilMillis.remove(sessionKey)
                repository.unrecordMedicationTaken(med.id, todayDateString, scheduledTime)
            } else {
                confirmedInSession.add(sessionKey)
                snoozedUntilMillis.remove(sessionKey)
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
            _selectedCalendarDate.value = date
            repository.saveCalendarEvent(ev)
        }
    }

    fun updateCalendarEvent(
        existingEvent: CalendarEvent,
        title: String,
        type: String,
        date: String,
        time: String,
        notes: String
    ) {
        viewModelScope.launch {
            val updated = existingEvent.copy(
                title = title.trim(),
                type = type,
                date = date,
                time = time.trim(),
                notes = notes.trim()
            )
            _selectedCalendarDate.value = date
            repository.saveCalendarEvent(updated)
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
        val nowMillis = now.timeInMillis
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentSecond = now.get(Calendar.SECOND)
        val currentMinutesOfDay = currentHour * 60 + currentMinute
        val currentSecondsOfDay = currentMinutesOfDay * 60 + currentSecond

        val currentMeds = medications.value
        val logs = todayMedLogs.value

        val pending = mutableListOf<PendingDose>()
        var nextUpcomingDiffSec = Int.MAX_VALUE
        var nextUpcomingLabel: String? = null

        for (med in currentMeds) {
            for (timeStr in med.scheduledTimes) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val hour = parts[0].toIntOrNull() ?: 0
                    val minute = parts[1].toIntOrNull() ?: 0
                    val schedMinutes = hour * 60 + minute
                    val schedSeconds = schedMinutes * 60

                    val sessionKey = "${med.id}_$timeStr"
                    val isTaken = confirmedInSession.contains(sessionKey) ||
                        logs.any { it.medicationId == med.id && it.scheduledTime == timeStr }

                    val snoozedUntil = snoozedUntilMillis[sessionKey] ?: 0L
                    val isSnoozedNow = nowMillis < snoozedUntil

                    if (!isTaken) {
                        if (schedMinutes > currentMinutesOfDay) {
                            val diffSec = (schedSeconds - currentSecondsOfDay).coerceAtLeast(1)
                            if (diffSec < nextUpcomingDiffSec) {
                                nextUpcomingDiffSec = diffSec
                                val remMin = diffSec / 60
                                val remSec = diffSec % 60
                                val countdownStr = if (remMin > 0) "${remMin}m ${remSec}s" else "${remSec}s"
                                nextUpcomingLabel = "Próxima alarma: ${med.name} a las $timeStr (en $countdownStr)"
                            }
                        } else if (!isSnoozedNow && (explicitlyArmedKeys.contains(sessionKey) || (currentMinutesOfDay - schedMinutes) in 0..360)) {
                            pending.add(PendingDose(med, timeStr, isTestDose = false))
                        } else if (isSnoozedNow) {
                            val diffSec = ((snoozedUntil - nowMillis) / 1000L).toInt().coerceAtLeast(1)
                            if (diffSec < nextUpcomingDiffSec) {
                                nextUpcomingDiffSec = diffSec
                                val remMin = diffSec / 60
                                val remSec = diffSec % 60
                                nextUpcomingLabel = "Posponiendo: ${med.name} sonará en ${remMin}m ${remSec}s"
                            }
                        }
                    }
                }
            }
        }

        _nextScheduledAlarmInfo.value = when {
            pending.isNotEmpty() ->
                "🔔 Sonando: ${pending.first().medication.name} (${pending.first().scheduledTime})"
            nextUpcomingLabel != null ->
                nextUpcomingLabel
            currentMeds.isEmpty() ->
                "Sin alarmas activas"
            else ->
                "Alarmas de hoy al día"
        }

        if (pending.isNotEmpty()) {
            val currentActiveKeys = _activeAlarmDoses.value.map { "${it.medication.id}_${it.scheduledTime}" }.toSet()
            val newPendingKeys = pending.map { "${it.medication.id}_${it.scheduledTime}" }.toSet()
            if (_activeAlarmDoses.value.isEmpty() || currentActiveKeys != newPendingKeys || !alarmPlayer.isAlarmActive()) {
                _activeAlarmDoses.value = pending
                val summary = if (_isStreetModeActive.value) {
                    "Tienes un recordatorio programado en Cuenta Conmigo"
                } else {
                    "Toma pendiente: " + pending.joinToString(", ") { "${it.medication.name} (${it.medication.dosage}) - ${it.scheduledTime}" }
                }
                alarmPlayer.startAlarm(summary)
            }
        }
    }

    /**
     * Dispara inmediatamente la alarma sonora y visual para una pastilla específica.
     */
    fun triggerAlarmForMedication(med: Medication) {
        val time = med.scheduledTimes.firstOrNull() ?: String.format(
            Locale.US,
            "%02d:%02d",
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )
        val dose = PendingDose(med, time, isTestDose = false)
        _activeAlarmDoses.value = listOf(dose)
        val summary = if (_isStreetModeActive.value) {
            "Recordatorio personal activo en Cuenta Conmigo"
        } else {
            "Hora de tomar: ${med.name} (${med.dosage}) - $time"
        }
        alarmPlayer.startAlarm(summary)
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

    /**
     * Pospone temporalmente la alarma actual durante [minutes] minutos (ej. 5 min mientras busca agua),
     * deteniendo el sonido ahora y volviendo a activarla automáticamente al cumplirse el plazo.
     */
    fun snoozeMedicationAlarm(minutes: Int = 5) {
        val snoozeTarget = System.currentTimeMillis() + (minutes * 60_000L)
        for (dose in _activeAlarmDoses.value) {
            val sessionKey = "${dose.medication.id}_${dose.scheduledTime}"
            snoozedUntilMillis[sessionKey] = snoozeTarget
        }
        _activeAlarmDoses.value = emptyList()
        alarmPlayer.stopAlarm()
    }

    fun confirmDoseInAlarm(dose: PendingDose) {
        viewModelScope.launch {
            val sessionKey = "${dose.medication.id}_${dose.scheduledTime}"
            confirmedInSession.add(sessionKey)
            snoozedUntilMillis.remove(sessionKey)
            explicitlyArmedKeys.remove(sessionKey)

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
                snoozedUntilMillis.remove(sessionKey)
                explicitlyArmedKeys.remove(sessionKey)
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
