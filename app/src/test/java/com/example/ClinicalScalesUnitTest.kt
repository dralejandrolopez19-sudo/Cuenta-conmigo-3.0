package com.example

import com.example.util.ClinicalScales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalScalesUnitTest {

    @Test
    fun testPHQ9EvaluationMinimal() {
        val answers = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0)
        val result = ClinicalScales.evaluatePHQ9(answers)
        assertEquals(0, result.score)
        assertEquals(27, result.maxScore)
        assertEquals("Mínimo o ausente", result.severity)
        assertEquals(false, result.triggerSupport)
    }

    @Test
    fun testPHQ9EvaluationSevereAndCrisisTrigger() {
        val answers = listOf(3, 3, 3, 3, 2, 2, 2, 2, 1) // Sum = 21, item 9 = 1
        val result = ClinicalScales.evaluatePHQ9(answers)
        assertEquals(21, result.score)
        assertEquals("Severo", result.severity)
        assertTrue(result.triggerSupport)
    }

    @Test
    fun testGAD7Evaluation() {
        val answers = listOf(1, 1, 0, 0, 1, 0, 0) // Sum = 3
        val result = ClinicalScales.evaluateGAD7(answers)
        assertEquals(3, result.score)
        assertEquals(21, result.maxScore)
        assertEquals("Mínima", result.severity)
    }

    @Test
    fun testEVAEvaluation() {
        val resultLow = ClinicalScales.evaluateEVA(2)
        assertEquals("Malestar significativo", resultLow.severity)
        assertTrue(resultLow.triggerSupport)

        val resultHigh = ClinicalScales.evaluateEVA(9)
        assertEquals("Bienestar óptimo", resultHigh.severity)
        assertEquals(false, resultHigh.triggerSupport)
    }
}
