package com.example

import com.example.data.model.CrisisDirectory
import com.example.util.CrisisDetectionHelper
import com.example.util.ImbalanceSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrisisSupportUnitTest {

    @Test
    fun testDominicanRepublicHelplinesAreAccurate() {
        val primary = CrisisDirectory.DOMINICAN_REPUBLIC_PRIMARY
        assertEquals("8092001400", primary.cleanDialNumber)
        assertEquals("DO", primary.countryCode)
        assertTrue(primary.isPrimaryDominicanRepublic)

        val rdList = CrisisDirectory.DOMINICAN_REPUBLIC_LIST
        assertTrue(rdList.any { it.cleanDialNumber == "8092001400" })
        assertTrue(rdList.any { it.cleanDialNumber == "911" })
        assertTrue(rdList.any { it.cleanDialNumber == "8092001202" })
    }

    @Test
    fun testGlobalHelplinesSearch() {
        val usResults = CrisisDirectory.search("Estados Unidos")
        assertTrue(usResults.isNotEmpty())
        assertEquals("988", usResults.first().cleanDialNumber)

        val esResults = CrisisDirectory.search("España")
        assertTrue(esResults.isNotEmpty())
        assertTrue(esResults.any { it.cleanDialNumber == "024" })

        val mxResults = CrisisDirectory.search("México")
        assertTrue(mxResults.isNotEmpty())
        assertTrue(mxResults.any { it.cleanDialNumber == "8009112000" })
    }

    @Test
    fun testCrisisDetectionOnSevereExpressions() {
        val severe = CrisisDetectionHelper.evaluate(
            level = 3,
            textContent = "Siento que no puedo más y ya no aguanto este dolor"
        )
        assertTrue(severe.hasImbalance)
        assertEquals(ImbalanceSeverity.HIGH, severe.severity)
    }

    @Test
    fun testCrisisDetectionOnLowMoodLevel() {
        val lowMood = CrisisDetectionHelper.evaluate(level = 1)
        assertTrue(lowMood.hasImbalance)
        assertEquals(ImbalanceSeverity.HIGH, lowMood.severity)

        val discouragedMood = CrisisDetectionHelper.evaluate(level = 2)
        assertTrue(discouragedMood.hasImbalance)
        assertEquals(ImbalanceSeverity.MODERATE, discouragedMood.severity)
    }

    @Test
    fun testNoCrisisOnNormalMood() {
        val balanced = CrisisDetectionHelper.evaluate(
            level = 4,
            textContent = "Hoy fue un día tranquilo y salí a caminar"
        )
        assertFalse(balanced.hasImbalance)
        assertEquals(ImbalanceSeverity.NONE, balanced.severity)
    }
}
