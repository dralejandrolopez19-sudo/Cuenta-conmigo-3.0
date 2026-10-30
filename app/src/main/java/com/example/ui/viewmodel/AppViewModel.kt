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
import kotlinx.coroutines.Job
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
import java.util.UUID

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
    data class Success(val text: String, val isSyncing: Boolean = false) : AiFeatureUiState
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
    data class Success(val suggestion: AiHabitSuggestion, val isSyncing: Boolean = false) : AiHabitUiState
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

    // --- MODO DE LECTURA CÁLIDO Y BRILLO REDUCIDO (DIARIO Y AGENDA) ---
    private val _readingAutoStartHour = MutableStateFlow(19)
    val readingAutoStartHour: StateFlow<Int> = _readingAutoStartHour.asStateFlow()

    private val _readingAutoEndHour = MutableStateFlow(7)
    val readingAutoEndHour: StateFlow<Int> = _readingAutoEndHour.asStateFlow()

    private val _isReadingModeAutoByTime = MutableStateFlow(true)
    val isReadingModeAutoByTime: StateFlow<Boolean> = _isReadingModeAutoByTime.asStateFlow()

    private val _isReadingModeActive = MutableStateFlow(
        isHourInReadingSchedule(Calendar.getInstance().get(Calendar.HOUR_OF_DAY), 19, 7)
    )
    val isReadingModeActive: StateFlow<Boolean> = _isReadingModeActive.asStateFlow()

    private val _readingDimLevel = MutableStateFlow(0.35f)
    val readingDimLevel: StateFlow<Float> = _readingDimLevel.asStateFlow()

    private val _readingWarmthLevel = MutableStateFlow(0.60f)
    val readingWarmthLevel: StateFlow<Float> = _readingWarmthLevel.asStateFlow()

    private val _showReadingModeDialog = MutableStateFlow(false)
    val showReadingModeDialog: StateFlow<Boolean> = _showReadingModeDialog.asStateFlow()

    private fun isHourInReadingSchedule(hour: Int, start: Int, end: Int): Boolean {
        return if (start <= end) {
            hour in start..end
        } else {
            hour >= start || hour < end
        }
    }

    fun evaluateReadingModeForHour(hourOfDay: Int) {
        if (_isReadingModeAutoByTime.value) {
            _isReadingModeActive.value = isHourInReadingSchedule(
                hour = hourOfDay,
                start = _readingAutoStartHour.value,
                end = _readingAutoEndHour.value
            )
        }
    }

    fun setReadingModeEnabled(enabled: Boolean) {
        _isReadingModeActive.value = enabled
    }

    fun toggleReadingMode() {
        _isReadingModeActive.value = !_isReadingModeActive.value
    }

    fun setReadingModeAutoByTime(enabled: Boolean) {
        _isReadingModeAutoByTime.value = enabled
        if (enabled) {
            evaluateReadingModeForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        }
    }

    fun updateReadingSchedule(
        startHour: Int,
        endHour: Int,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ) {
        _readingAutoStartHour.value = startHour.coerceIn(0, 23)
        _readingAutoEndHour.value = endHour.coerceIn(0, 23)
        if (_isReadingModeAutoByTime.value) {
            evaluateReadingModeForHour(currentHour)
        }
    }

    fun setReadingDimLevel(level: Float) {
        _readingDimLevel.value = level.coerceIn(0.10f, 0.75f)
    }

    fun setReadingWarmthLevel(level: Float) {
        _readingWarmthLevel.value = level.coerceIn(0.15f, 1.0f)
    }

    fun openReadingModeDialog() {
        _showReadingModeDialog.value = true
    }

    fun closeReadingModeDialog() {
        _showReadingModeDialog.value = false
    }

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
    private val silencedAlarmKeys = mutableSetOf<String>()

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

    // --- ESTADOS DE INTELIGENCIA ARTIFICIAL AUTOMÁTICA EN CADA ACÁPITE ---
    private var homeAiJob: Job? = null
    private var diaryAiJob: Job? = null
    private var moodAiJob: Job? = null
    private var medicationAiJob: Job? = null
    private var habitAiJob: Job? = null
    private var agendaAiJob: Job? = null

    private val _homeAiInsightState = MutableStateFlow<AiFeatureUiState>(
        AiFeatureUiState.Success(buildContextualHomeSummary())
    )
    val homeAiInsightState: StateFlow<AiFeatureUiState> = _homeAiInsightState.asStateFlow()

    private val _diaryAiReflectionState = MutableStateFlow<AiFeatureUiState>(
        AiFeatureUiState.Success(buildContextualDiaryReflection())
    )
    val diaryAiReflectionState: StateFlow<AiFeatureUiState> = _diaryAiReflectionState.asStateFlow()

    private val _moodAiInsightState = MutableStateFlow<AiFeatureUiState>(
        AiFeatureUiState.Success(buildContextualMoodAnalysis())
    )
    val moodAiInsightState: StateFlow<AiFeatureUiState> = _moodAiInsightState.asStateFlow()

    private val _medicationAiTipState = MutableStateFlow<AiFeatureUiState>(
        AiFeatureUiState.Success(buildContextualMedicationGuidance())
    )
    val medicationAiTipState: StateFlow<AiFeatureUiState> = _medicationAiTipState.asStateFlow()

    private val _habitAiSuggestionState = MutableStateFlow<AiHabitUiState>(
        AiHabitUiState.Success(buildContextualHabitSuggestion())
    )
    val habitAiSuggestionState: StateFlow<AiHabitUiState> = _habitAiSuggestionState.asStateFlow()

    private val _agendaAiPrepState = MutableStateFlow<AiFeatureUiState>(
        AiFeatureUiState.Success(buildContextualAgendaPrep(todayDateString))
    )
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
        if (next) {
            generateAiCamouflageAliases()
        }
    }

    fun setStreetMode(enabled: Boolean, reason: String = "Activado manualmente") {
        _isStreetModeActive.value = enabled
        _temporarilyRevealedIds.value = emptySet()
        _streetModeTriggerReason.value = if (enabled) reason else ""
        if (enabled) {
            generateAiCamouflageAliases()
        }
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
            _streetModeTriggerReason.value = "Modo Calle activo"
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
        generateAiCamouflageAliases()
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
                    _streetModeTriggerReason.value = "Modo Calle + Camuflaje IA activo"
                }
                _aiPrivacyStatusMessage.value = "Alias de camuflaje IA sincronizados automáticamente."
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "AI Camouflage generation timed out, using active aliases", e)
                _aiPrivacyStatusMessage.value = "Camuflaje IA activo con alias discretos predeterminados."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI Camouflage fallback used", e)
                _aiPrivacyStatusMessage.value = "Camuflaje IA activo con alias discretos."
            } finally {
                _isGeneratingAiCamouflage.value = false
            }
        }
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

        // 1. Automatización IA en INICIO: reacciona automáticamente a cambios de ánimo, pastillas, hábitos y agenda
        viewModelScope.launch {
            combine(
                moodEntries,
                medications,
                todayMedLogs,
                habits,
                calendarEvents
            ) { moods, meds, logs, habs, events ->
                "${moods.size}_${moods.firstOrNull()?.level}_${meds.size}_${logs.size}_${habs.size}_${habs.sumOf { it.completedDates.size }}_${events.size}"
            }.collectLatest {
                generateHomeWellnessSummary()
            }
        }

        // 2. Automatización IA en PASTILLAS: reacciona automáticamente a pastillas y tomas registradas
        viewModelScope.launch {
            combine(medications, todayMedLogs) { meds, logs ->
                "${meds.joinToString { "${it.id}:${it.name}:${it.scheduledTimes.joinToString()}" }}_${logs.size}"
            }.collectLatest {
                generateMedicationAiGuidance()
            }
        }

        // 3. Automatización IA en AGENDA: reacciona automáticamente a la fecha seleccionada y citas
        viewModelScope.launch {
            combine(calendarEvents, _selectedCalendarDate, moodEntries) { events, date, moods ->
                "${date}_${events.filter { it.date == date }.joinToString { "${it.id}:${it.title}:${it.time}" }}_${moods.firstOrNull()?.level}"
            }.collectLatest {
                generateAgendaAiPreparation(_selectedCalendarDate.value)
            }
        }

        // 4. Automatización IA en HÁBITOS: reacciona automáticamente a hábitos y estado de ánimo
        viewModelScope.launch {
            combine(habits, moodEntries) { habs, moods ->
                "${habs.joinToString { "${it.id}:${it.completedDates.size}" }}_${moods.firstOrNull()?.levelLabel}"
            }.collectLatest {
                generateHabitAiSuggestion()
            }
        }

        // 5. Automatización IA en ÁNIMO Y ESCALAS: reacciona automáticamente a registros de ánimo y evaluaciones clínicas
        viewModelScope.launch {
            combine(moodEntries, clinicalAssessments) { moods, scales ->
                "${moods.firstOrNull()?.let { "${it.date}:${it.level}:${it.feelings.joinToString()}:${it.note}" }}_${scales.firstOrNull()?.let { "${it.scaleType}:${it.score}" }}"
            }.collectLatest {
                generateMoodAiAnalysis()
            }
        }

        // 6. Automatización IA en DIARIO: reacciona automáticamente a notas y filtro de categoría
        viewModelScope.launch {
            combine(journalEntries, _journalCategoryFilter) { entries, category ->
                "${category}_${entries.firstOrNull()?.let { "${it.id}:${it.title}:${it.content.hashCode()}" }}_${entries.size}"
            }.collectLatest {
                generateDiaryAiReflection()
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

    // --- SÍNTESIS CONTEXTUAL INMEDIATA + GEMINI 3.8 FLASH EN CADA ACÁPITE ---

    private fun buildContextualHomeSummary(): String {
        val todayMood = moodEntries.value.firstOrNull { it.date == todayDateString }
        val totalMeds = medications.value.sumOf { it.scheduledTimes.size }
        val takenMeds = todayMedLogs.value.size
        val doneHabits = habits.value.count { it.completedDates.contains(todayDateString) }
        val totalHabits = habits.value.size
        val nextEvent = calendarEvents.value
            .filter { it.date >= todayDateString }
            .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
            .firstOrNull()

        val statusSummary = buildList {
            if (todayMood != null) add("ánimo ${todayMood.levelLabel.lowercase()}")
            if (totalMeds > 0) add("$takenMeds/$totalMeds tomas")
            if (totalHabits > 0) add("$doneHabits/$totalHabits hábitos")
        }.joinToString(" • ")

        val prefix = if (statusSummary.isNotBlank()) "Hoy: $statusSummary. " else ""
        val tip = if (nextEvent != null) {
            "Próxima cita: '${nextEvent.title}' (${nextEvent.date} ${nextEvent.time})."
        } else {
            "Regálate tres respiraciones profundas y avanza a tu propio ritmo."
        }

        return "$prefix$tip"
    }

    private fun buildContextualDiaryReflection(customDraft: String = ""): String {
        val category = _journalCategoryFilter.value
        val entries = journalEntries.value.let { list ->
            if (category == "Todos") list else list.filter { it.category.equals(category, ignoreCase = true) }
        }
        val latest = entries.firstOrNull()
        return when {
            customDraft.isNotBlank() ->
                "Poner en palabras lo que sientes alivia la mente; obsérvalo hoy sin juzgarte."
            latest != null ->
                "A partir de '${latest.title}': recuerda que cada emoción es temporal; ¿qué pequeño gesto amable puedes darte hoy?"
            else ->
                "Escribir ayuda a soltar tensión mental. Anota una idea breve o algo que agradezcas hoy."
        }
    }

    private fun buildContextualMoodAnalysis(): String {
        val latestMood = moodEntries.value.firstOrNull()
        val latestScale = clinicalAssessments.value.firstOrNull()

        val moodObservation = if (latestMood != null) {
            "Último registro: '${latestMood.levelLabel}' (${latestMood.level}/5)."
        } else {
            "Registra tu ánimo arriba para recibir pautas adaptadas a tu energía de hoy."
        }

        val scalePart = if (latestScale != null) {
            " ${latestScale.scaleType}: ${latestScale.score}/${latestScale.maxScore}."
        } else ""

        val recommendation = when (latestMood?.level ?: 3) {
            1, 2 -> " Reduce la autoexigencia y haz 3 respiraciones lentas (4-4-6)."
            3 -> " Haz una breve pausa lejos de pantallas para estirar hombros y cuello."
            else -> " Aprovecha esta calma para sostener tus hábitos del día."
        }

        return "$moodObservation$scalePart$recommendation"
    }

    private fun buildContextualMedicationGuidance(): String {
        val medsList = medications.value
        val takenCount = todayMedLogs.value.size
        val totalDoses = medsList.sumOf { it.scheduledTimes.size }
        return if (medsList.isNotEmpty()) {
            val scheduleSummary = medsList.joinToString(", ") { "${it.name} (${it.scheduledTimes.joinToString("/")})" }
            "Programación activa: $scheduleSummary ($takenCount/$totalDoses tomas hoy). Asocia cada toma a un hábito fijo."
        } else {
            "Escribe arriba el nombre de tu pastilla y elige la hora para activar su alarma diaria."
        }
    }

    private fun buildContextualHabitSuggestion(): AiHabitSuggestion {
        val existingNames = habits.value.map { it.name.lowercase() }
        val latestMoodLevel = moodEntries.value.firstOrNull()?.level ?: 3

        val candidates = listOf(
            AiHabitSuggestion(
                name = "Respiración diafragmática durante 3 minutos",
                category = "Salud Mental",
                timeOfDay = "Mañana",
                reason = "Disminuye la tensión emocional desde el inicio del día."
            ),
            AiHabitSuggestion(
                name = "Caminata suave de 10 minutos sin pantallas",
                category = "Físico y Sueño",
                timeOfDay = "Tarde",
                reason = "Despeja la mente y regula tu energía diaria."
            ),
            AiHabitSuggestion(
                name = "Anotar 3 cosas que salieron bien hoy",
                category = "Salud Mental",
                timeOfDay = "Noche",
                reason = "Favorece un descanso nocturno más sereno."
            ),
            AiHabitSuggestion(
                name = "Enviar un mensaje afectuoso a alguien cercano",
                category = "Social",
                timeOfDay = "Tarde",
                reason = "Fortalece tu red de apoyo emocional."
            )
        )

        return if (latestMoodLevel <= 2) {
            candidates.firstOrNull { candidate -> existingNames.none { it.contains("respiración") } }
                ?: candidates.firstOrNull { candidate -> existingNames.none { it == candidate.name.lowercase() } }
                ?: candidates.first()
        } else {
            candidates.firstOrNull { candidate -> existingNames.none { it == candidate.name.lowercase() } }
                ?: candidates.first()
        }
    }

    private fun buildContextualAgendaPrep(targetDate: String): String {
        val dateEvents = calendarEvents.value.filter { it.date == targetDate }.sortedBy { it.time }
        val upcomingEvents = calendarEvents.value
            .filter { it.date >= todayDateString }
            .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
            .take(2)
        val recentMood = moodEntries.value.firstOrNull()?.levelLabel ?: "Estable"

        return when {
            dateEvents.isNotEmpty() -> {
                val firstEv = dateEvents.first()
                "Para '${firstEv.title}' (${firstEv.time}), comenta tu ánimo reciente ($recentMood) y lleva anotadas tus dudas."
            }
            upcomingEvents.isNotEmpty() -> {
                val nextEv = upcomingEvents.first()
                "Día libre el $targetDate. Tu próxima cita es '${nextEv.title}' el ${nextEv.date} a las ${nextEv.time}."
            }
            else -> {
                "Sin citas el $targetDate. Buen momento para organizar tu descanso o agendar tu próximo control."
            }
        }
    }

    fun generateHomeWellnessSummary() {
        homeAiJob?.cancel()
        val fallbackText = buildContextualHomeSummary()
        val currentText = (_homeAiInsightState.value as? AiFeatureUiState.Success)?.text ?: fallbackText
        _homeAiInsightState.value = AiFeatureUiState.Success(text = currentText.ifBlank { fallbackText }, isSyncing = true)

        homeAiJob = viewModelScope.launch {
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
                    Eres un acompañante sereno de bienestar en "Cuenta Conmigo".
                    Datos hoy ($todayDateString): ánimo=${todayMood?.levelLabel ?: "sin registrar"}, pastillas=$takenMeds/$totalMeds, hábitos=$doneHabits/$totalHabits, próxima cita=${nextEvent?.let { "${it.title} (${it.date} ${it.time})" } ?: "ninguna"}.
                    Escribe en español una sola frase o máximo 2 oraciones breves (máximo 22 palabras) con el balance de hoy y un consejo suave. Sin markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                _homeAiInsightState.value = AiFeatureUiState.Success(
                    text = if (!text.isNullOrBlank()) text else buildContextualHomeSummary(),
                    isSyncing = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Home AI using contextual synthesis: ${e.message}")
                _homeAiInsightState.value = AiFeatureUiState.Success(
                    text = buildContextualHomeSummary(),
                    isSyncing = false
                )
            }
        }
    }

    fun generateDiaryAiReflection(customDraft: String = "") {
        diaryAiJob?.cancel()
        val fallbackText = buildContextualDiaryReflection(customDraft)
        _diaryAiReflectionState.value = AiFeatureUiState.Success(text = fallbackText, isSyncing = true)

        diaryAiJob = viewModelScope.launch {
            try {
                val recentEntries = journalEntries.value.take(2)
                val contextText = if (customDraft.isNotBlank()) {
                    "Borrador: $customDraft"
                } else if (recentEntries.isNotEmpty()) {
                    recentEntries.joinToString("\n") { "- [${it.category}] ${it.title}: ${it.content.take(140)}" }
                } else {
                    "Inicio de diario emocional."
                }

                val prompt = """
                    Actúa como un terapeuta compasivo en "Cuenta Conmigo".
                    Notas recientes: $contextText
                    Escribe en español máximo 2 oraciones breves (máximo 24 palabras) validando su emoción y dejando una pregunta reflexiva corta. Sin markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                _diaryAiReflectionState.value = AiFeatureUiState.Success(
                    text = if (!text.isNullOrBlank()) text else buildContextualDiaryReflection(customDraft),
                    isSyncing = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Diary AI using contextual synthesis: ${e.message}")
                _diaryAiReflectionState.value = AiFeatureUiState.Success(
                    text = buildContextualDiaryReflection(customDraft),
                    isSyncing = false
                )
            }
        }
    }

    fun generateMoodAiAnalysis() {
        moodAiJob?.cancel()
        val fallbackText = buildContextualMoodAnalysis()
        _moodAiInsightState.value = AiFeatureUiState.Success(text = fallbackText, isSyncing = true)

        moodAiJob = viewModelScope.launch {
            try {
                val recentMoods = moodEntries.value.take(3)
                val moodsSummary = if (recentMoods.isNotEmpty()) {
                    recentMoods.joinToString("; ") { "${it.date}: ${it.levelLabel} (${it.level}/5)" }
                } else {
                    "Sin registros previos"
                }

                val prompt = """
                    Especialista en bienestar emocional en "Cuenta Conmigo".
                    Ánimo reciente: $moodsSummary.
                    Responde en español en máximo 2 oraciones cortas (máximo 22 palabras), mencionando su estado reciente y una micro-pausa práctica. Sin markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                _moodAiInsightState.value = AiFeatureUiState.Success(
                    text = if (!text.isNullOrBlank()) text else buildContextualMoodAnalysis(),
                    isSyncing = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Mood AI using contextual synthesis: ${e.message}")
                _moodAiInsightState.value = AiFeatureUiState.Success(
                    text = buildContextualMoodAnalysis(),
                    isSyncing = false
                )
            }
        }
    }

    fun generateMedicationAiGuidance() {
        medicationAiJob?.cancel()
        val fallbackText = buildContextualMedicationGuidance()
        _medicationAiTipState.value = AiFeatureUiState.Success(text = fallbackText, isSyncing = true)

        medicationAiJob = viewModelScope.launch {
            try {
                val medsList = medications.value
                val medsDescription = if (medsList.isNotEmpty()) {
                    medsList.joinToString("; ") { "${it.name} (${it.scheduledTimes.joinToString(", ")})" }
                } else {
                    "Sin pastillas registradas aún."
                }

                val prompt = """
                    Asistente de adherencia en "Cuenta Conmigo".
                    Pastillas: $medsDescription.
                    Escribe en español una sola oración breve (máximo 20 palabras) sobre cómo recordar sus tomas hoy. Si hay pastillas, menciona su nombre. Sin markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                _medicationAiTipState.value = AiFeatureUiState.Success(
                    text = if (!text.isNullOrBlank()) text else buildContextualMedicationGuidance(),
                    isSyncing = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Medication AI using contextual synthesis: ${e.message}")
                _medicationAiTipState.value = AiFeatureUiState.Success(
                    text = buildContextualMedicationGuidance(),
                    isSyncing = false
                )
            }
        }
    }

    fun generateHabitAiSuggestion() {
        habitAiJob?.cancel()
        val fallbackSuggestion = buildContextualHabitSuggestion()
        _habitAiSuggestionState.value = AiHabitUiState.Success(suggestion = fallbackSuggestion, isSyncing = true)

        habitAiJob = viewModelScope.launch {
            try {
                val existingHabits = habits.value.joinToString(", ") { it.name }
                val latestMood = moodEntries.value.firstOrNull()?.levelLabel ?: "Equilibrado"

                val prompt = """
                    Sugiere un nuevo micro-hábito diario breve en español (máximo 6 palabras en "name") para ánimo "$latestMood".
                    No repetir: ${existingHabits.ifBlank { "ninguno" }}.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    habitAiModel.generateContent(prompt)
                }
                val jsonString = response.text ?: throw IllegalStateException("Respuesta vacía del modelo IA.")
                val json = JSONObject(jsonString)

                val suggestion = AiHabitSuggestion(
                    name = json.optString("name", fallbackSuggestion.name).ifBlank {
                        fallbackSuggestion.name
                    },
                    category = json.optString("category", fallbackSuggestion.category).ifBlank {
                        fallbackSuggestion.category
                    },
                    timeOfDay = json.optString("timeOfDay", fallbackSuggestion.timeOfDay).ifBlank {
                        fallbackSuggestion.timeOfDay
                    },
                    reason = json.optString("reason", fallbackSuggestion.reason).ifBlank {
                        fallbackSuggestion.reason
                    }
                )
                _habitAiSuggestionState.value = AiHabitUiState.Success(suggestion = suggestion, isSyncing = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Habit AI using contextual synthesis: ${e.message}")
                _habitAiSuggestionState.value = AiHabitUiState.Success(
                    suggestion = buildContextualHabitSuggestion(),
                    isSyncing = false
                )
            }
        }
    }

    fun generateAgendaAiPreparation(targetDate: String) {
        agendaAiJob?.cancel()
        val fallbackText = buildContextualAgendaPrep(targetDate)
        _agendaAiPrepState.value = AiFeatureUiState.Success(text = fallbackText, isSyncing = true)

        agendaAiJob = viewModelScope.launch {
            try {
                val dateEvents = calendarEvents.value.filter { it.date == targetDate }
                val upcomingEvents = calendarEvents.value
                    .filter { it.date >= todayDateString }
                    .sortedWith(compareBy<CalendarEvent> { it.date }.thenBy { it.time })
                    .take(2)
                val recentMoods = moodEntries.value.take(2)

                val eventContext = when {
                    dateEvents.isNotEmpty() ->
                        "Citas el $targetDate: " + dateEvents.joinToString("; ") { "${it.title} (${it.type}) a las ${it.time}" }
                    upcomingEvents.isNotEmpty() ->
                        "Próxima cita: " + upcomingEvents.joinToString("; ") { "${it.title} el ${it.date} a las ${it.time}" }
                    else ->
                        "Día $targetDate sin citas."
                }

                val moodContext = if (recentMoods.isNotEmpty()) {
                    recentMoods.joinToString(", ") { it.levelLabel }
                } else {
                    "Estable"
                }

                val prompt = """
                    Asistente de agenda en "Cuenta Conmigo".
                    Contexto: $eventContext. Ánimo reciente: $moodContext.
                    Escribe en español máximo 2 oraciones breves (máximo 22 palabras) para preparar su cita o su día con calma. Si hay cita, incluye su título. Sin markdown.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    wellnessAiModel.generateContent(prompt)
                }
                val text = response.text?.trim()
                _agendaAiPrepState.value = AiFeatureUiState.Success(
                    text = if (!text.isNullOrBlank()) text else buildContextualAgendaPrep(targetDate),
                    isSyncing = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Agenda AI using contextual synthesis: ${e.message}")
                _agendaAiPrepState.value = AiFeatureUiState.Success(
                    text = buildContextualAgendaPrep(targetDate),
                    isSyncing = false
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
            val existingTodayId = moodEntries.value.firstOrNull { it.date == todayDateString }?.id ?: ""
            val entry = MoodEntry(
                id = existingTodayId,
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
            val generatedId = "med_${UUID.randomUUID().toString().replace("-", "").take(12)}"
            val med = Medication(
                id = generatedId,
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
                silencedAlarmKeys.remove(key)
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
                silencedAlarmKeys.remove("${med.id}_$oldTime")
            }
            sortedTimes.forEach { newTime ->
                val key = "${med.id}_$newTime"
                confirmedInSession.remove(key)
                snoozedUntilMillis.remove(key)
                silencedAlarmKeys.remove(key)
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

            val savedMed = if (existingMed != null && existingMed.id.isNotBlank()) {
                existingMed.scheduledTimes.forEach { oldT ->
                    confirmedInSession.remove("${existingMed.id}_$oldT")
                    snoozedUntilMillis.remove("${existingMed.id}_$oldT")
                    silencedAlarmKeys.remove("${existingMed.id}_$oldT")
                }
                existingMed.copy(
                    name = cleanName,
                    dosage = cleanDosage,
                    scheduledTimes = listOf(cleanTime)
                )
            } else {
                val generatedId = "med_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                Medication(
                    id = generatedId,
                    userId = currentUserId,
                    name = cleanName,
                    dosage = cleanDosage,
                    scheduledTimes = listOf(cleanTime),
                    notes = existingMed?.notes ?: ""
                )
            }

            val sessionKey = "${savedMed.id}_$cleanTime"
            confirmedInSession.remove(sessionKey)
            snoozedUntilMillis.remove(sessionKey)
            silencedAlarmKeys.remove(sessionKey)

            repository.unrecordMedicationTaken(savedMed.id, todayDateString, cleanTime)
            repository.saveMedication(savedMed)
            alarmPlayer.scheduleSystemAlarmsForToday(
                medId = savedMed.id,
                medName = savedMed.name,
                dosage = savedMed.dosage,
                times = listOf(cleanTime)
            )
            checkScheduledMedicationAlarms()
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
            val remainingDoses = _activeAlarmDoses.value.filterNot { it.medication.id == id }
            _activeAlarmDoses.value = remainingDoses
            if (remainingDoses.isEmpty()) {
                alarmPlayer.stopAlarm()
            }
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
                silencedAlarmKeys.add(sessionKey)
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

    fun adoptAndCompleteAiHabitToday(suggestion: AiHabitSuggestion) {
        viewModelScope.launch {
            val existing = habits.value.firstOrNull { it.name.equals(suggestion.name, ignoreCase = true) }
            if (existing != null) {
                if (!existing.completedDates.contains(todayDateString)) {
                    repository.toggleHabitDate(existing, todayDateString)
                }
            } else {
                val newHabit = Habit(
                    userId = currentUserId,
                    name = suggestion.name,
                    category = suggestion.category,
                    timeOfDay = suggestion.timeOfDay,
                    completedDates = listOf(todayDateString),
                    streakDays = 1
                )
                repository.saveHabit(newHabit)
            }
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
                    val isSilenced = silencedAlarmKeys.contains(sessionKey)

                    val snoozedUntil = snoozedUntilMillis[sessionKey] ?: 0L
                    val isSnoozedNow = nowMillis < snoozedUntil
                    val snoozeJustExpired = snoozedUntil > 0L && nowMillis >= snoozedUntil

                    if (!isTaken) {
                        if (schedMinutes > currentMinutesOfDay) {
                            val diffSec = (schedSeconds - currentSecondsOfDay).coerceAtLeast(1)
                            if (diffSec < nextUpcomingDiffSec) {
                                nextUpcomingDiffSec = diffSec
                                val remMin = diffSec / 60
                                val remSec = diffSec % 60
                                val countdownStr = if (remMin > 0) "${remMin}m ${remSec}s" else "${remSec}s"
                                nextUpcomingLabel = "Próxima: ${med.name} a las $timeStr ($countdownStr)"
                            }
                        } else if (!isSilenced && !isSnoozedNow && (currentMinutesOfDay == schedMinutes || snoozeJustExpired)) {
                            pending.add(PendingDose(med, timeStr, isTestDose = false))
                        } else if (isSnoozedNow) {
                            val diffSec = ((snoozedUntil - nowMillis) / 1000L).toInt().coerceAtLeast(1)
                            if (diffSec < nextUpcomingDiffSec) {
                                nextUpcomingDiffSec = diffSec
                                val remMin = diffSec / 60
                                val remSec = diffSec % 60
                                nextUpcomingLabel = "Pospuesta: ${med.name} en ${remMin}m ${remSec}s"
                            }
                        }
                    }
                }
            }
        }

        _nextScheduledAlarmInfo.value = when {
            pending.isNotEmpty() ->
                "Sonando: ${pending.first().medication.name} (${pending.first().scheduledTime})"
            nextUpcomingLabel != null ->
                nextUpcomingLabel
            currentMeds.isEmpty() ->
                "Sin alarmas"
            else ->
                "Horarios configurados"
        }

        if (pending.isNotEmpty()) {
            val currentActiveKeys = _activeAlarmDoses.value.map { "${it.medication.id}_${it.scheduledTime}" }.toSet()
            val newPendingKeys = pending.map { "${it.medication.id}_${it.scheduledTime}" }.toSet()
            if (_activeAlarmDoses.value.isEmpty() || currentActiveKeys != newPendingKeys || !alarmPlayer.isAlarmActive()) {
                _activeAlarmDoses.value = pending
                val summary = if (_isStreetModeActive.value) {
                    "Tienes un recordatorio programado en Cuenta Conmigo"
                } else {
                    "Hora de tu pastilla: " + pending.joinToString(", ") { "${it.medication.name} (${it.medication.dosage}) - ${it.scheduledTime}" }
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
                notes = "Alarma de prueba activa."
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

    fun dismissActiveAlarm() {
        for (dose in _activeAlarmDoses.value) {
            val sessionKey = "${dose.medication.id}_${dose.scheduledTime}"
            silencedAlarmKeys.add(sessionKey)
            snoozedUntilMillis.remove(sessionKey)
        }
        _activeAlarmDoses.value = emptyList()
        alarmPlayer.stopAlarm()
    }

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
            silencedAlarmKeys.add(sessionKey)
            snoozedUntilMillis.remove(sessionKey)

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
                silencedAlarmKeys.add(sessionKey)
                snoozedUntilMillis.remove(sessionKey)
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
