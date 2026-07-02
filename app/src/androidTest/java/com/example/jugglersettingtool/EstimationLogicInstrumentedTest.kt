package com.example.jugglersettingtool

import com.example.jugglersettingtool.data.JugglerSpecProvider
import com.example.jugglersettingtool.data.SETTING_COUNT
import com.example.jugglersettingtool.utils.EstimationLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimationLogicInstrumentedTest {

    private val spec = JugglerSpecProvider.specs.first()

    @Test
    fun estimateSettings_withoutGames_returnsUniformProbabilities() {
        val probabilities = EstimationLogic.estimateSettings(
            spec = spec,
            games = 0,
            bb = 0,
            rb = 0,
            grape = 0
        )

        assertEquals(SETTING_COUNT, probabilities.size)
        assertEquals(100.0, probabilities.sum(), 0.001)
        probabilities.forEach { probability ->
            assertEquals(100.0 / SETTING_COUNT, probability, 0.001)
        }
    }

    @Test
    fun estimateSettings_withStrongRegAndGrapeData_favorsHighSettings() {
        val probabilities = EstimationLogic.estimateSettings(
            spec = spec,
            games = 3_000,
            bb = 12,
            rb = 14,
            grape = 520
        )

        val lowSettings = probabilities[0] + probabilities[1]
        val highSettings = probabilities[4] + probabilities[5]

        assertTrue(highSettings > lowSettings)
    }

    @Test
    fun calculateExpectation_withHighSettingProbability_isPositive() {
        val expectedYen = EstimationLogic.calculateExpectation(
            spec = spec,
            probabilities = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 0.0, 100.0),
            futureGames = 1_000,
            exchangeRate = 5.6
        )

        assertTrue(expectedYen > 0.0)
    }

    @Test
    fun calculateRealProfitYen_withNoRecovery_returnsInvestmentLoss() {
        val profit = EstimationLogic.calculateRealProfitYen(
            investmentYen = 10_000,
            recoveryCoins = 0,
            lendCoinsPer1000Yen = 50,
            exchangeRate = 5.6
        )

        assertEquals(-10_000.0, profit, 0.001)
    }

    @Test
    fun estimateGrapeCount_neverReturnsNegativeCount() {
        val grape = EstimationLogic.estimateGrapeCount(
            spec = spec,
            games = 1_000,
            bb = 0,
            rb = 0,
            investmentYen = 100_000,
            recoveryCoins = 0,
            lendCoinsPer1000Yen = 50
        )

        assertEquals(0, grape)
    }
}
