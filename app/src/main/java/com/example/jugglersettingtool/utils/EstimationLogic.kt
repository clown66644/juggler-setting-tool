package com.example.jugglersettingtool.utils

import com.example.jugglersettingtool.data.JugglerSpec
import com.example.jugglersettingtool.data.SETTING_COUNT
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max

object EstimationLogic {

    private const val COINS_PER_GAME = 3.0
    private const val DEFAULT_CHERRY_PROBABILITY_DENOMINATOR = 35.0
    private const val PERCENT = 100.0
    private val defaultProbabilities = DoubleArray(SETTING_COUNT) { PERCENT / SETTING_COUNT }

    /**
     * 単独・チェリー重複を含めたベイズ設定推測
     */
    fun estimateSettings(
        spec: JugglerSpec,
        games: Int,
        bb: Int,
        rb: Int,
        grape: Int,
        cherry: Int = -1, // チェリー確率も統合
        soloBb: Int = -1,
        soloRb: Int = -1,
        cherryBb: Int = -1,
        cherryRb: Int = -1
    ): DoubleArray {
        val safeGames = games.coerceAtLeast(0)
        if (safeGames == 0) return defaultProbabilities.copyOf()

        val logLikelihoods = DoubleArray(SETTING_COUNT) { index ->
            val setting = index + 1
            
            // ブドウの対数尤度
            val grapeLikelihood = if (grape > 0) {
                binomialLogLikelihood(grape, safeGames, spec.getGrapeRate(setting))
            } else {
                0.0
            }

            // チェリーの対数尤度 (チェリー単体がカウントされている場合)
            val cherryLikelihood = if (cherry > 0) {
                binomialLogLikelihood(cherry, safeGames, spec.getCherryRate(setting))
            } else {
                0.0
            }

            // ボーナスの対数尤度 (単独/重複が個別に入力されている場合は個別計算)
            val bbLikelihood = if (soloBb >= 0 && cherryBb >= 0) {
                val soloBbLikelihood = binomialLogLikelihood(soloBb, safeGames, spec.getSoloBbRate(setting))
                val cherryBbLikelihood = binomialLogLikelihood(cherryBb, safeGames, spec.getCherryBbRate(setting))
                soloBbLikelihood + cherryBbLikelihood
            } else {
                binomialLogLikelihood(bb, safeGames, spec.getBbRate(setting))
            }

            val rbLikelihood = if (soloRb >= 0 && cherryRb >= 0) {
                val soloRbLikelihood = binomialLogLikelihood(soloRb, safeGames, spec.getSoloRbRate(setting))
                val cherryRbLikelihood = binomialLogLikelihood(cherryRb, safeGames, spec.getCherryRbRate(setting))
                soloRbLikelihood + cherryRbLikelihood
            } else {
                binomialLogLikelihood(rb, safeGames, spec.getRbRate(setting))
            }

            bbLikelihood + rbLikelihood + grapeLikelihood + cherryLikelihood
        }

        return normalizeLogLikelihoods(logLikelihoods)
    }

    /**
     * 目押し設定を考慮したブドウ逆算
     */
    fun estimateGrapeCount(
        spec: JugglerSpec,
        games: Int,
        bb: Int,
        rb: Int,
        investmentYen: Int,
        recoveryCoins: Int,
        lendCoinsPer1000Yen: Int,
        eyeMoshiCherry: Boolean = true,
        eyeMoshiBellPiere: Boolean = false
    ): Int {
        val safeGames = games.coerceAtLeast(0)
        if (safeGames == 0) return 0

        // 投資・回収差枚
        val investedCoins = (investmentYen.coerceAtLeast(0) / 1000.0) *
            lendCoinsPer1000Yen.coerceAtLeast(0)
        val actualCoinDiff = recoveryCoins.coerceAtLeast(0) - investedCoins
        
        // ボーナスによる払い出し枚数
        val bonusCoins = (bb.coerceAtLeast(0) * spec.bbPay) + (rb.coerceAtLeast(0) * spec.rbPay)
        
        // 通常ゲームでの純増減枚数
        val normalGameCoinDiff = actualCoinDiff - bonusCoins
        
        // 通常ゲームで投入された総枚数 (3枚掛け)
        val totalInputCoins = safeGames * COINS_PER_GAME
        
        // 通常ゲームでの小役による総払い出し枚数
        val smallRolePayoutCoins = normalGameCoinDiff + totalInputCoins

        // チェリー狙いの有無によるチェリー払い出し期待値の補正 (フリー打ちは期待値約66.7%に低下)
        val cherryRateFactor = if (eyeMoshiCherry) 1.0 else 0.667
        val estimatedCherryPayoutCoins =
            (safeGames / DEFAULT_CHERRY_PROBABILITY_DENOMINATOR) * spec.cherryPay * cherryRateFactor

        // ベル・ピエロ狙いの有無による払い出し期待値の補正 (狙わないとほぼ100%取りこぼす)
        val estimatedBellPierePayoutCoins = if (eyeMoshiBellPiere) {
            (safeGames / 500.0) * 10.0
        } else {
            0.0
        }

        // ブドウによる払い出し期待枚数 (小役総払い出し - チェリー期待枚数 - ベルピエロ期待枚数)
        val estimatedGrapePayoutCoins = smallRolePayoutCoins - estimatedCherryPayoutCoins - estimatedBellPierePayoutCoins

        return max(0, (estimatedGrapePayoutCoins / spec.grapePay).toInt())
    }

    fun calculateExpectation(
        spec: JugglerSpec,
        probabilities: DoubleArray,
        futureGames: Int,
        exchangeRate: Double
    ): Double {
        if (futureGames <= 0 || exchangeRate <= 0.0) return 0.0

        val safeProbabilities = probabilities
            .take(SETTING_COUNT)
            .map { it.coerceAtLeast(0.0) / PERCENT }
            .padToSettingCount()
        val probabilityTotal = safeProbabilities.sum()
        if (probabilityTotal <= 0.0) return 0.0

        val averagePayoutRate = safeProbabilities.withIndex().sumOf { (index, probability) ->
            (probability / probabilityTotal) * (spec.payoutRate[index] / PERCENT)
        }
        val expectedCoinDiff = futureGames * COINS_PER_GAME * (averagePayoutRate - 1.0)
        val yenPerCoin = PERCENT / exchangeRate

        return expectedCoinDiff * yenPerCoin
    }

    fun calculateRealProfitYen(
        investmentYen: Int,
        recoveryCoins: Int,
        lendCoinsPer1000Yen: Int,
        exchangeRate: Double
    ): Double {
        if (exchangeRate <= 0.0) return 0.0

        val safeInvestmentYen = investmentYen.coerceAtLeast(0)
        val safeRecoveryCoins = recoveryCoins.coerceAtLeast(0)
        val investedCoins = (safeInvestmentYen / 1000.0) * lendCoinsPer1000Yen.coerceAtLeast(0)
        val coinDiff = safeRecoveryCoins - investedCoins
        val yenPerCoin = PERCENT / exchangeRate

        return if (coinDiff >= 0) {
            coinDiff * yenPerCoin
        } else {
            (safeRecoveryCoins * yenPerCoin) - safeInvestmentYen
        }
    }

    private fun binomialLogLikelihood(successes: Int, trials: Int, probability: Double): Double {
        val boundedSuccesses = successes.coerceIn(0, trials)
        return boundedSuccesses * ln(probability) +
            (trials - boundedSuccesses) * ln(1.0 - probability)
    }

    private fun normalizeLogLikelihoods(logLikelihoods: DoubleArray): DoubleArray {
        val maxLogLikelihood = logLikelihoods.reduce(::max)
        val rawLikelihoods = DoubleArray(SETTING_COUNT) { index ->
            exp(logLikelihoods[index] - maxLogLikelihood)
        }
        val sumRawLikelihoods = rawLikelihoods.sum()
        if (sumRawLikelihoods <= 0.0) return defaultProbabilities.copyOf()

        return DoubleArray(SETTING_COUNT) { index ->
            (rawLikelihoods[index] / sumRawLikelihoods) * PERCENT
        }
    }

    private fun List<Double>.padToSettingCount(): DoubleArray {
        val result = DoubleArray(SETTING_COUNT)
        for (i in 0 until SETTING_COUNT) {
            result[i] = if (i < this.size) this[i] else 0.0
        }
        return result
    }
}
