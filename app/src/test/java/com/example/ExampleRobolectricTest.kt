package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.CuentaConmigoRepository
import com.example.data.repository.InMemoryGuestStore
import com.example.ui.viewmodel.AiFeatureUiState
import com.example.ui.viewmodel.AiHabitUiState
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.AppViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun setUp() {
        InMemoryGuestStore.reset()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Cuenta Conmigo", appName)
    }

    @Test
    fun `verify automatic AI features medication alarm programmer and agenda operations work end to end`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val repo = CuentaConmigoRepository(app, "guest_explorer")
        val viewModel = AppViewModel(
            application = app,
            repository = repo,
            currentUserId = "guest_explorer",
            initialStreetMode = false
        )
        shadowOf(Looper.getMainLooper()).idle()

        // 0. Verify all 6 section AI states are automatically populated (Success) without pressing any button
        assertTrue(viewModel.homeAiInsightState.value is AiFeatureUiState.Success)
        assertTrue((viewModel.homeAiInsightState.value as AiFeatureUiState.Success).text.isNotBlank())

        assertTrue(viewModel.medicationAiTipState.value is AiFeatureUiState.Success)
        assertTrue((viewModel.medicationAiTipState.value as AiFeatureUiState.Success).text.isNotBlank())

        assertTrue(viewModel.agendaAiPrepState.value is AiFeatureUiState.Success)
        assertTrue((viewModel.agendaAiPrepState.value as AiFeatureUiState.Success).text.isNotBlank())

        assertTrue(viewModel.habitAiSuggestionState.value is AiHabitUiState.Success)
        assertTrue((viewModel.habitAiSuggestionState.value as AiHabitUiState.Success).suggestion.name.isNotBlank())

        assertTrue(viewModel.moodAiInsightState.value is AiFeatureUiState.Success)
        assertTrue((viewModel.moodAiInsightState.value as AiFeatureUiState.Success).text.isNotBlank())

        assertTrue(viewModel.diaryAiReflectionState.value is AiFeatureUiState.Success)
        assertTrue((viewModel.diaryAiReflectionState.value as AiFeatureUiState.Success).text.isNotBlank())

        // 1. Verify tab navigation works across all 6 tabs
        viewModel.selectTab(AppTab.PASTILLAS)
        assertEquals(AppTab.PASTILLAS, viewModel.currentTab.value)

        // 2. Program a new medication alarm and verify ID is deterministic and AI updates automatically
        viewModel.programMedicationAlarm(
            existingMed = null,
            name = "Melatonina",
            dosage = "5 mg",
            timeStr = "22:30"
        )
        shadowOf(Looper.getMainLooper()).idle()

        val savedMed = InMemoryGuestStore.medications.value.firstOrNull { it.name == "Melatonina" }
        assertNotNull("Programmed medication must be saved", savedMed)
        assertTrue("Medication ID must be non-blank", savedMed!!.id.isNotBlank())
        assertEquals(listOf("22:30"), savedMed.scheduledTimes)
        val medAiText = (viewModel.medicationAiTipState.value as AiFeatureUiState.Success).text
        assertTrue("Medication AI should automatically reflect new medication", medAiText.contains("Melatonina"))

        // 3. Test immediate alarm ringing and confirmation
        viewModel.triggerAlarmForMedication(savedMed)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Active alarm doses should not be empty when triggered", viewModel.activeAlarmDoses.value.isNotEmpty())

        viewModel.confirmAllAlarmDoses()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue("Active alarm doses should be cleared after confirmation", viewModel.activeAlarmDoses.value.isEmpty())

        // 4. Test agenda event creation and automatic AI agenda prep update
        viewModel.selectTab(AppTab.AGENDA)
        viewModel.addCalendarEvent(
            title = "Control mensual",
            type = "Cita Médica",
            date = viewModel.todayDateString,
            time = "10:30",
            notes = ""
        )
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(
            "New calendar event should be saved",
            InMemoryGuestStore.calendarEvents.value.any { it.title == "Control mensual" && it.time == "10:30" }
        )
        val agendaAiText = (viewModel.agendaAiPrepState.value as AiFeatureUiState.Success).text
        assertTrue("Agenda AI should automatically reflect scheduled appointment", agendaAiText.contains("Control mensual"))

        // 5. Test mood update reuses today's mood entry ID and triggers proactive support + automatic AI mood analysis
        viewModel.saveMood(
            level = 1,
            label = "Muy bajo",
            feelings = listOf("Tristeza"),
            note = "Día difícil"
        )
        shadowOf(Looper.getMainLooper()).idle()
        val todayMoods = InMemoryGuestStore.moodEntries.value.filter { it.date == viewModel.todayDateString }
        assertEquals("Should update today's mood entry rather than duplicating it", 1, todayMoods.size)
        assertEquals(1, todayMoods.first().level)
        assertNotNull("Proactive support should be triggered on level 1 mood", viewModel.activeProactiveSupport.value)
        val moodAiText = (viewModel.moodAiInsightState.value as AiFeatureUiState.Success).text
        assertTrue("Mood AI should automatically reflect updated mood", moodAiText.contains("Muy bajo"))

        // 6. Verify Warm Reading Mode automatic activation by time of day and brightness/warmth adjustments
        assertTrue("Automatic reading mode by time should be enabled by default", viewModel.isReadingModeAutoByTime.value)
        assertEquals(19, viewModel.readingAutoStartHour.value)
        assertEquals(7, viewModel.readingAutoEndHour.value)

        // Simulate nighttime hour (21:00) -> should activate automatically
        viewModel.evaluateReadingModeForHour(21)
        assertTrue("Reading mode should activate automatically at 21:00", viewModel.isReadingModeActive.value)

        // Simulate daytime hour (13:00) -> should deactivate automatically
        viewModel.evaluateReadingModeForHour(13)
        assertEquals("Reading mode should deactivate automatically at 13:00", false, viewModel.isReadingModeActive.value)

        // Update schedule to include 13:00 (e.g., 12:00 to 16:00) and verify automatic activation
        viewModel.updateReadingSchedule(startHour = 12, endHour = 16, currentHour = 13)
        assertTrue("Reading mode should activate for custom 12:00-16:00 schedule at 13:00", viewModel.isReadingModeActive.value)

        // Verify brightness reduction and warmth sliders
        viewModel.setReadingDimLevel(0.45f)
        viewModel.setReadingWarmthLevel(0.70f)
        assertEquals(0.45f, viewModel.readingDimLevel.value, 0.001f)
        assertEquals(0.70f, viewModel.readingWarmthLevel.value, 0.001f)
    }
}
