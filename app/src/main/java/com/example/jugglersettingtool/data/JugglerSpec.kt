package com.example.jugglersettingtool.data

const val SETTING_COUNT = 6

data class JugglerSpec(
    val name: String,
    val bbProb: List<Double>,
    val rbProb: List<Double>,
    val grapeProb: List<Double>,
    val cherryProb: List<Double>, // チェリー確率
    val bbPay: Int,
    val rbPay: Int,
    val grapePay: Int,
    val cherryPay: Int = 2,
    val payoutRate: List<Double>,
    // 単独・重複の確率 (オプション)
    val soloBbProb: List<Double>? = null,
    val soloRbProb: List<Double>? = null,
    val cherryBbProb: List<Double>? = null,
    val cherryRbProb: List<Double>? = null
) {
    init {
        require(bbProb.size == SETTING_COUNT) { "$name のBB確率は設定1〜6の6件が必要です。" }
        require(rbProb.size == SETTING_COUNT) { "$name のRB確率は設定1〜6の6件が必要です。" }
        require(grapeProb.size == SETTING_COUNT) { "$name のブドウ確率は設定1〜6の6件が必要です。" }
        require(cherryProb.size == SETTING_COUNT) { "$name のチェリー確率は設定1〜6の6件が必要です。" }
        require(payoutRate.size == SETTING_COUNT) { "$name の機械割は設定1〜6の6件が必要です。" }
        require(listOf(bbPay, rbPay, grapePay, cherryPay).all { it > 0 }) {
            "$name の払い出し枚数は正の値が必要です。"
        }
    }

    fun getBbRate(setting: Int): Double = 1.0 / bbProb[setting.toSettingIndex()]

    fun getRbRate(setting: Int): Double = 1.0 / rbProb[setting.toSettingIndex()]

    fun getGrapeRate(setting: Int): Double = 1.0 / grapeProb[setting.toSettingIndex()]

    fun getCherryRate(setting: Int): Double = 1.0 / cherryProb[setting.toSettingIndex()]

    // 単独・重複の確率取得 (指定が無い場合は概算比率で自動按分)
    fun getSoloBbRate(setting: Int): Double =
        1.0 / (soloBbProb?.get(setting.toSettingIndex()) ?: (bbProb[setting.toSettingIndex()] / 0.72))

    fun getSoloRbRate(setting: Int): Double =
        1.0 / (soloRbProb?.get(setting.toSettingIndex()) ?: (rbProb[setting.toSettingIndex()] / 0.72))

    fun getCherryBbRate(setting: Int): Double =
        1.0 / (cherryBbProb?.get(setting.toSettingIndex()) ?: (bbProb[setting.toSettingIndex()] / 0.28))

    fun getCherryRbRate(setting: Int): Double =
        1.0 / (cherryRbProb?.get(setting.toSettingIndex()) ?: (rbProb[setting.toSettingIndex()] / 0.28))

    private fun Int.toSettingIndex(): Int {
        require(this in 1..SETTING_COUNT) { "設定は1〜$SETTING_COUNT の範囲で指定してください: $this" }
        return this - 1
    }
}

object JugglerSpecProvider {
    val specs = listOf(
        JugglerSpec(
            name = "アイムジャグラーEX",
            bbProb = listOf(273.1, 269.7, 269.7, 259.0, 259.0, 255.0),
            rbProb = listOf(439.8, 399.6, 331.0, 315.1, 255.0, 255.0),
            grapeProb = listOf(6.02, 6.02, 6.02, 6.02, 6.02, 5.78),
            cherryProb = listOf(33.0, 33.0, 33.0, 33.0, 33.0, 33.0),
            bbPay = 252,
            rbPay = 96,
            grapePay = 8,
            payoutRate = listOf(97.0, 98.0, 99.5, 101.1, 103.3, 105.5),
            soloBbProb = listOf(390.1, 385.5, 385.5, 368.2, 368.2, 356.2),
            soloRbProb = listOf(728.2, 655.4, 512.0, 481.9, 362.1, 362.1),
            cherryBbProb = listOf(1285.0, 1285.0, 1285.0, 1285.0, 1285.0, 1285.0),
            cherryRbProb = listOf(1092.3, 1024.0, 862.3, 862.3, 862.3, 862.3)
        ),
        JugglerSpec(
            name = "マイジャグラーV",
            bbProb = listOf(273.1, 270.8, 266.4, 254.0, 240.9, 229.1),
            rbProb = listOf(409.6, 385.5, 336.1, 290.0, 268.6, 229.1),
            grapeProb = listOf(5.90, 5.85, 5.80, 5.78, 5.76, 5.66),
            cherryProb = listOf(35.2, 34.9, 34.4, 34.0, 33.5, 33.0),
            bbPay = 240,
            rbPay = 96,
            grapePay = 8,
            payoutRate = listOf(97.0, 98.0, 99.9, 102.8, 105.3, 109.4),
            soloBbProb = listOf(402.1, 397.2, 387.8, 381.0, 362.1, 344.9),
            soloRbProb = listOf(668.7, 630.2, 532.8, 439.8, 402.1, 334.4),
            cherryBbProb = listOf(1213.6, 1191.6, 1170.3, 1092.3, 1024.0, 936.2),
            cherryRbProb = listOf(1057.0, 993.0, 910.2, 851.1, 809.1, 728.2)
        ),
        JugglerSpec(
            name = "ファンキージャグラー2",
            bbProb = listOf(266.4, 259.0, 256.0, 249.2, 240.9, 219.9),
            rbProb = listOf(439.8, 407.1, 366.1, 322.8, 299.3, 262.1),
            grapeProb = listOf(5.94, 5.92, 5.88, 5.83, 5.76, 5.67),
            cherryProb = listOf(36.4, 36.1, 35.8, 35.5, 35.2, 34.9),
            bbPay = 240,
            rbPay = 96,
            grapePay = 8,
            payoutRate = listOf(97.0, 98.5, 99.8, 102.0, 104.3, 109.0)
        ),
        JugglerSpec(
            name = "ハッピージャグラーVIII",
            bbProb = listOf(273.1, 270.8, 263.2, 254.0, 239.2, 226.0),
            rbProb = listOf(397.2, 362.1, 319.7, 280.1, 251.1, 226.0),
            grapeProb = listOf(6.04, 6.01, 5.98, 5.84, 5.81, 5.79),
            cherryProb = listOf(35.3, 35.3, 35.3, 35.3, 35.3, 35.3),
            bbPay = 240,
            rbPay = 96,
            grapePay = 8,
            payoutRate = listOf(97.0, 97.9, 99.9, 102.9, 105.8, 108.4)
        ),
        JugglerSpec(
            name = "ゴーゴージャグラー3",
            bbProb = listOf(259.0, 257.0, 255.0, 244.5, 234.1, 220.0),
            rbProb = listOf(354.2, 334.4, 309.1, 278.9, 255.0, 220.0),
            grapeProb = listOf(6.25, 6.20, 6.15, 6.07, 6.00, 5.93),
            cherryProb = listOf(34.1, 34.0, 33.9, 33.8, 33.7, 33.5),
            bbPay = 240,
            rbPay = 96,
            grapePay = 7,
            payoutRate = listOf(97.0, 98.2, 99.4, 101.6, 103.8, 106.0)
        ),
        JugglerSpec(
            name = "ミスタージャグラー",
            bbProb = listOf(268.6, 267.5, 264.3, 251.1, 239.2, 224.4),
            rbProb = listOf(374.5, 350.5, 321.3, 280.1, 251.1, 224.4),
            grapeProb = listOf(6.29, 6.22, 6.15, 6.09, 6.02, 5.96),
            cherryProb = listOf(33.7, 33.6, 33.5, 33.2, 33.0, 32.8),
            bbPay = 240,
            rbPay = 96,
            grapePay = 8,
            payoutRate = listOf(97.0, 98.1, 99.6, 102.3, 105.3, 108.5)
        )
    )
}
