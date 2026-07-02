package com.example.jugglersettingtool.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jugglersettingtool.Screen
import com.example.jugglersettingtool.data.HistoryItem
import com.example.jugglersettingtool.data.HistoryRepository
import com.example.jugglersettingtool.data.JugglerSpec
import com.example.jugglersettingtool.data.JugglerSpecProvider
import com.example.jugglersettingtool.network.OpenAiService
import com.example.jugglersettingtool.utils.EstimationLogic
import com.example.jugglersettingtool.utils.formatHitRatio
import com.example.jugglersettingtool.utils.formatPercent
import com.example.jugglersettingtool.utils.formatYen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ApiKeyStatus {
    Unchecked,
    Checking,
    Valid,
    Invalid
}

class JugglerViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val DEFAULT_LEND_RATE = 50
        const val DEFAULT_EXCHANGE_RATE = "5.6"
        const val DEFAULT_FUTURE_GAMES = 1000
    }

    private val historyRepository = HistoryRepository(application)

    // 画面遷移管理
    var currentScreen by mutableStateOf(Screen.Counter)
        private set

    // ウェルカム表示フラグ
    var showWelcomeScreen by mutableStateOf(true)
        private set

    // 対象機種
    var selectedSpec by mutableStateOf(JugglerSpecProvider.specs.first())
        private set

    // 目押し設定
    var eyeMoshiCherry by mutableStateOf(true)
        private set
    var eyeMoshiBellPiere by mutableStateOf(false)
        private set

    // 詳細カウントモード
    var isCherryDetailMode by mutableStateOf(false)
        private set

    // 周辺台キープ・並行判別のアクティブフラグ (0: メイン台, 1: 周辺候補台)
    var activeCounterTab by mutableIntStateOf(0)
        private set

    // --- メイン台データ ---
    var priorGames by mutableIntStateOf(0)
        private set
    var priorBb by mutableIntStateOf(0)
        private set
    var priorRb by mutableIntStateOf(0)
        private set
    var priorDiff by mutableIntStateOf(0)
        private set
    var priorDiffIsMinus by mutableStateOf(false)
    var priorGrape by mutableIntStateOf(0)
        private set

    var myGames by mutableIntStateOf(0)
        private set
    var myBb by mutableIntStateOf(0)
        private set
    var myRb by mutableIntStateOf(0)
        private set
    var myGrape by mutableIntStateOf(0)
        private set
    var myCherry by mutableIntStateOf(0)

    var mySoloBb by mutableIntStateOf(0)
        private set
    var mySoloRb by mutableIntStateOf(0)
        private set
    var myCherryBb by mutableIntStateOf(0)
        private set
    var myCherryRb by mutableIntStateOf(0)
        private set

    // --- 周辺候補台データ (ブドウ入力は削除し差枚数から自動逆算する) ---
    var subGames by mutableIntStateOf(0)
        private set
    var subBb by mutableIntStateOf(0)
        private set
    var subRb by mutableIntStateOf(0)
        private set
    var subDiff by mutableIntStateOf(0)
        private set
    var subDiffIsMinus by mutableStateOf(false)
    var subGrape by mutableIntStateOf(0) // 差枚数から自動で逆算されるブドウ数
        private set

    // --- 店舗・写真・推移データの状態 ---
    var storeName by mutableStateOf("")
        private set
    var eventName by mutableStateOf("")
        private set
    var selectedImageUri by mutableStateOf<String?>(null)
        private set

    // 設定推測の時系列プロットデータ (メイン台用)
    var gamesHistoryList = mutableListOf<Int>()
    var setting6HistoryList = mutableListOf<Double>()

    // 収支・レート関連
    var lendRate by mutableIntStateOf(DEFAULT_LEND_RATE)
        private set
    var exchangeRate by mutableStateOf(DEFAULT_EXCHANGE_RATE)
        private set
    var investmentYen by mutableIntStateOf(0)
        private set
    var recoveryCoins by mutableIntStateOf(0)
        private set
    var futureGames by mutableIntStateOf(DEFAULT_FUTURE_GAMES)
        private set

    // 合算累計データ (メイン台)
    val totalGames: Int get() = priorGames + myGames
    val totalBb: Int get() = priorBb + myBb
    val totalRb: Int get() = priorRb + myRb
    val totalCherry: Int get() = myCherry
    val totalGrape: Int get() = priorGrape + myGrape

    val totalSoloBb: Int get() = if (isCherryDetailMode) mySoloBb else -1
    val totalSoloRb: Int get() = if (isCherryDetailMode) mySoloRb else -1
    val totalCherryBb: Int get() = if (isCherryDetailMode) myCherryBb else -1
    val totalCherryRb: Int get() = if (isCherryDetailMode) myCherryRb else -1

    val myDiffCoins: Int get() {
        val investCoins = (investmentYen / 1000.0) * lendRate
        return (recoveryCoins - investCoins).toInt()
    }
    val totalDiffCoins: Int get() = priorDiff + myDiffCoins

    // メイン台の推測結果
    var probabilities by mutableStateOf(DoubleArray(6) { 16.67 })
        private set

    // 周辺候補台の推測結果
    var subProbabilities by mutableStateOf(DoubleArray(6) { 16.67 })
        private set

    // 期待収支
    var expectedYen by mutableStateOf(0.0)
        private set

    // AIメモ・アドバイス
    var hallNotes by mutableStateOf("")
        private set
    var aiAdvice by mutableStateOf("")
        private set
    var isAiLoading by mutableStateOf(false)
        private set

    // シミュレーターAI判定用プロパティ
    var simAiAdvice by mutableStateOf("")
    var isSimAiLoading by mutableStateOf(false)
        private set

    // 履歴・カレンダーデータ
    var historyList by mutableStateOf<List<HistoryItem>>(emptyList())
        private set
    var dailyProfitMap by mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    // --- やめどき・続行 自動判定プロパティ ---
    val continueRecommendationScore: Int get() {
        if (totalGames <= 0) return 50
        
        val highSettingProb = probabilities[3] + probabilities[4] + probabilities[5]
        var score = highSettingProb.toInt()

        if (totalGames < 1500) {
            val weight = totalGames.toDouble() / 1500.0
            score = (score * weight + 50.0 * (1.0 - weight)).toInt()
        }

        val regRatio = if (totalRb > 0) totalGames.toDouble() / totalRb else 9999.0
        val targetReg = selectedSpec.rbProb[5]
        if (regRatio <= targetReg) {
            score = (score + 15).coerceAtMost(100)
        } else if (regRatio > selectedSpec.rbProb[0] * 1.2) {
            score = (score - 20).coerceAtLeast(0)
        }

        if (totalGames >= 3000 && highSettingProb < 40.0 && totalDiffCoins >= 1000) {
            score = (score - 25).coerceAtLeast(5)
        }

        return score.coerceIn(0, 100)
    }

    // APIキー検証ステータス
    var apiKeyStatus by mutableStateOf(ApiKeyStatus.Unchecked)
        private set

    fun testApiKeyConnection(key: String) {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) {
            apiKeyStatus = ApiKeyStatus.Unchecked
            return
        }
        apiKeyStatus = ApiKeyStatus.Checking
        viewModelScope.launch {
            val isValid = OpenAiService.validateApiKey(trimmed)
            apiKeyStatus = if (isValid) ApiKeyStatus.Valid else ApiKeyStatus.Invalid
        }
    }

    init {
        loadHistory()
        // 保存済みのAPIキーがあれば自動でバックグラウンド検証する
        val savedKey = OpenAiService.getApiKey(application)
        if (savedKey.isNotEmpty()) {
            testApiKeyConnection(savedKey)
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun closeWelcomeScreen() {
        showWelcomeScreen = false
        recordHistorySnapshot()
    }

    fun openWelcomeScreen() {
        showWelcomeScreen = true
    }

    fun switchCounterTab(index: Int) {
        activeCounterTab = index
    }

    fun updateStoreName(value: String) {
        storeName = value
    }

    fun updateEventName(value: String) {
        eventName = value
    }

    fun updateSelectedImageUri(value: String?) {
        selectedImageUri = value
    }

    fun selectSpec(spec: JugglerSpec) {
        selectedSpec = spec
        calculatePriorGrape()
        calculateSubGrape()
        recalculate()
    }

    // 目押し設定の更新
    fun updateEyeMoshiCherry(value: Boolean) {
        eyeMoshiCherry = value
        recalculate()
    }

    fun updateEyeMoshiBellPiere(value: Boolean) {
        eyeMoshiBellPiere = value
        recalculate()
    }

    fun updateCherryDetailMode(value: Boolean) {
        isCherryDetailMode = value
        recalculate()
    }

    // 前任者データの更新
    fun updatePriorGames(value: String) {
        priorGames = value.toNonNegativeInt()
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }
    fun updatePriorBb(value: String) {
        priorBb = value.toNonNegativeInt()
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }
    fun updatePriorRb(value: String) {
        priorRb = value.toNonNegativeInt()
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }
    fun updatePriorDiff(value: String) {
        val num = value.toNonNegativeInt()
        priorDiff = if (priorDiffIsMinus) -num else num
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }
    fun togglePriorDiffSign(isMinus: Boolean) {
        priorDiffIsMinus = isMinus
        val absVal = Math.abs(priorDiff)
        priorDiff = if (isMinus) -absVal else absVal
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }

    fun addPriorGames(delta: Int) {
        priorGames = (priorGames + delta).coerceAtLeast(0)
        calculatePriorGrape()
        recalculate()
        recordHistorySnapshot()
    }

    private fun calculatePriorGrape() {
        if (priorGames <= 0) {
            priorGrape = 0
            return
        }
        priorGrape = EstimationLogic.estimateGrapeCount(
            spec = selectedSpec,
            games = priorGames,
            bb = priorBb,
            rb = priorRb,
            investmentYen = 0,
            recoveryCoins = priorDiff,
            lendCoinsPer1000Yen = 1000,
            eyeMoshiCherry = true,
            eyeMoshiBellPiere = false
        )
    }

    // 自分データの更新
    fun updateMyGames(value: String) {
        myGames = value.toNonNegativeInt()
        recalculate()
        recordHistorySnapshot()
    }

    fun addMyGames(delta: Int) {
        myGames = (myGames + delta).coerceAtLeast(0)
        recalculate()
        recordHistorySnapshot()
    }

    fun updateMyGrape(value: String) {
        myGrape = value.toNonNegativeInt()
        recalculate()
        recordHistorySnapshot()
    }

    fun updateMyCherry(value: String) {
        myCherry = value.toNonNegativeInt()
        recalculate()
        recordHistorySnapshot()
    }

    fun incrementMyBb() = updateCounter { myBb++ }
    fun decrementMyBb() = updateCounter { myBb = (myBb - 1).coerceAtLeast(0) }

    fun incrementMyRb() = updateCounter { myRb++ }
    fun decrementMyRb() = updateCounter { myRb = (myRb - 1).coerceAtLeast(0) }

    fun incrementMyGrape() = updateCounter { myGrape++ }
    fun decrementMyGrape() = updateCounter { myGrape = (myGrape - 1).coerceAtLeast(0) }

    fun incrementMyCherry() = updateCounter { myCherry++ }
    fun decrementMyCherry() = updateCounter { myCherry = (myCherry - 1).coerceAtLeast(0) }

    // 単独/重複
    fun incrementMySoloBb() = updateCounter { 
        mySoloBb++ 
        myBb++
    }
    fun decrementMySoloBb() = updateCounter { 
        mySoloBb = (mySoloBb - 1).coerceAtLeast(0)
        myBb = (myBb - 1).coerceAtLeast(0)
    }

    fun incrementMySoloRb() = updateCounter { 
        mySoloRb++ 
        myRb++
    }
    fun decrementMySoloRb() = updateCounter { 
        mySoloRb = (mySoloRb - 1).coerceAtLeast(0)
        myRb = (myRb - 1).coerceAtLeast(0)
    }

    fun incrementMyCherryBb() = updateCounter { 
        myCherryBb++ 
        myBb++
    }
    fun decrementMyCherryBb() = updateCounter { 
        myCherryBb = (myCherryBb - 1).coerceAtLeast(0)
        myBb = (myBb - 1).coerceAtLeast(0)
    }

    fun incrementMyCherryRb() = updateCounter { 
        myCherryRb++ 
        myRb++
    }
    fun decrementMyCherryRb() = updateCounter { 
        myCherryRb = (myCherryRb - 1).coerceAtLeast(0)
        myRb = (myRb - 1).coerceAtLeast(0)
    }

    // --- 周辺候補台の更新関数 (ブドウは自動計算のためインクリメント等は削除) ---
    fun updateSubGames(value: String) {
        subGames = value.toNonNegativeInt()
        calculateSubGrape()
        recalculateSub()
    }
    fun updateSubBb(value: String) {
        subBb = value.toNonNegativeInt()
        calculateSubGrape()
        recalculateSub()
    }
    fun updateSubRb(value: String) {
        subRb = value.toNonNegativeInt()
        calculateSubGrape()
        recalculateSub()
    }
    fun updateSubDiff(value: String) {
        val num = value.toNonNegativeInt()
        subDiff = if (subDiffIsMinus) -num else num
        calculateSubGrape()
        recalculateSub()
    }
    fun toggleSubDiffSign(isMinus: Boolean) {
        subDiffIsMinus = isMinus
        val absVal = Math.abs(subDiff)
        subDiff = if (isMinus) -absVal else absVal
        calculateSubGrape()
        recalculateSub()
    }
    fun addSubGames(delta: Int) {
        subGames = (subGames + delta).coerceAtLeast(0)
        calculateSubGrape()
        recalculateSub()
    }
    fun incrementSubBb() { subBb++; calculateSubGrape(); recalculateSub() }
    fun decrementSubBb() { subBb = (subBb - 1).coerceAtLeast(0); calculateSubGrape(); recalculateSub() }
    fun incrementSubRb() { subRb++; calculateSubGrape(); recalculateSub() }
    fun decrementSubRb() { subRb = (subRb - 1).coerceAtLeast(0); calculateSubGrape(); recalculateSub() }

    // 候補台のブドウ自動逆算ロジック (前任者と同様にチェリー狙いのみ=trueで逆算)
    private fun calculateSubGrape() {
        if (subGames <= 0) {
            subGrape = 0
            return
        }
        subGrape = EstimationLogic.estimateGrapeCount(
            spec = selectedSpec,
            games = subGames,
            bb = subBb,
            rb = subRb,
            investmentYen = 0,
            recoveryCoins = subDiff,
            lendCoinsPer1000Yen = 1000,
            eyeMoshiCherry = true,
            eyeMoshiBellPiere = false
        )
    }

    // レート・収支の更新
    fun updateLendRate(value: String) {
        lendRate = value.toNonNegativeIntOrDefault(DEFAULT_LEND_RATE)
        recalculate()
    }

    fun updateExchangeRate(value: String) {
        exchangeRate = value
        recalculate()
    }

    fun updateInvestmentYen(value: String) {
        investmentYen = value.toNonNegativeInt()
        recalculate()
    }

    fun updateRecoveryCoins(value: String) {
        recoveryCoins = value.toNonNegativeInt()
        recalculate()
    }

    fun addRecoveryCoins(delta: Int) {
        recoveryCoins = (recoveryCoins + delta).coerceAtLeast(0)
        recalculate()
    }

    fun updateFutureGames(value: String) {
        futureGames = value.toNonNegativeInt()
        recalculate()
    }

    fun applyFutureGamesPreset(value: Int) {
        futureGames = value.coerceAtLeast(0)
        recalculate()
    }

    fun updateHallNotes(value: String) {
        hallNotes = value
    }

    fun recalculate() {
        probabilities = EstimationLogic.estimateSettings(
            spec = selectedSpec,
            games = totalGames,
            bb = totalBb,
            rb = totalRb,
            grape = totalGrape,
            cherry = totalCherry,
            soloBb = totalSoloBb,
            soloRb = totalSoloRb,
            cherryBb = totalCherryBb,
            cherryRb = totalCherryRb
        )
        val rate = exchangeRate.toDoubleOrNull() ?: DEFAULT_EXCHANGE_RATE.toDouble()
        expectedYen = EstimationLogic.calculateExpectation(selectedSpec, probabilities, futureGames, rate)
    }

    private fun recalculateSub() {
        subProbabilities = EstimationLogic.estimateSettings(
            spec = selectedSpec,
            games = subGames,
            bb = subBb,
            rb = subRb,
            grape = subGrape
        )
    }

    // 設定推測（設定6の確率）の時系列推移プロットの記録
    private fun recordHistorySnapshot() {
        val currentG = totalGames
        if (currentG <= 0) return

        // 最初の記録は無条件で追加
        if (gamesHistoryList.isEmpty()) {
            gamesHistoryList.add(currentG)
            setting6HistoryList.add(probabilities[5])
            return
        }

        // 前回記録時より30G以上進んでいる場合のみプロットを追加する
        val lastG = gamesHistoryList.last()
        if (currentG - lastG >= 30) {
            gamesHistoryList.add(currentG)
            setting6HistoryList.add(probabilities[5])
        }
    }

    // 自分の投資・回収から自分のブドウ数を逆算する
    fun runMyGrapeEstimation() {
        val estimatedMy = EstimationLogic.estimateGrapeCount(
            spec = selectedSpec,
            games = myGames,
            bb = myBb,
            rb = myRb,
            investmentYen = investmentYen,
            recoveryCoins = recoveryCoins,
            lendCoinsPer1000Yen = lendRate,
            eyeMoshiCherry = eyeMoshiCherry,
            eyeMoshiBellPiere = eyeMoshiBellPiere
        )
        myGrape = estimatedMy
        recalculate()
        recordHistorySnapshot()
    }

    // 実戦履歴保存
    fun saveCurrentGame() {
        if (totalGames <= 0) return

        val rate = exchangeRate.toDoubleOrNull() ?: DEFAULT_EXCHANGE_RATE.toDouble()
        val profit = EstimationLogic.calculateRealProfitYen(investmentYen, recoveryCoins, lendRate, rate)
        
        var maxProb = -1.0
        var bestSetting = 1
        probabilities.forEachIndexed { index, prob ->
            if (prob > maxProb) {
                maxProb = prob
                bestSetting = index + 1
            }
        }

        val gamesHistStr = gamesHistoryList.joinToString(",")
        val set6HistStr = setting6HistoryList.joinToString(",")

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val item = HistoryItem(
            date = todayStr,
            specName = selectedSpec.name,
            priorGames = priorGames,
            priorBb = priorBb,
            priorRb = priorRb,
            priorDiff = priorDiff,
            myGames = myGames,
            myBb = myBb,
            myRb = myRb,
            myGrape = myGrape,
            myCherry = myCherry,
            investmentYen = investmentYen,
            recoveryCoins = recoveryCoins,
            profitYen = profit,
            estimatedSetting = bestSetting,
            storeName = storeName,
            eventName = eventName,
            imageUri = selectedImageUri,
            gamesHistory = gamesHistStr,
            setting6History = set6HistStr
        )
        historyRepository.saveHistoryItem(item)
        loadHistory()
        resetMyData()
    }

    fun deleteHistory(id: String) {
        historyRepository.deleteHistoryItem(id)
        loadHistory()
    }

    fun loadHistory() {
        historyList = historyRepository.loadAllHistory()
        dailyProfitMap = historyRepository.getDailyProfitMap()
    }

    private fun resetMyData() {
        myGames = 0
        myBb = 0
        myRb = 0
        myGrape = 0
        myCherry = 0
        mySoloBb = 0
        mySoloRb = 0
        myCherryBb = 0
        myCherryRb = 0
        
        subGames = 0
        subBb = 0
        subRb = 0
        subDiff = 0
        subDiffIsMinus = false
        subGrape = 0

        investmentYen = 0
        recoveryCoins = 0
        priorGames = 0
        priorBb = 0
        priorRb = 0
        priorDiff = 0
        priorDiffIsMinus = false
        priorGrape = 0
        
        storeName = ""
        eventName = ""
        selectedImageUri = null
        
        gamesHistoryList.clear()
        setting6HistoryList.clear()
        
        recalculate()
        recalculateSub()
    }

    fun requestAiAdvice() {
        if (isAiLoading) return

        isAiLoading = true
        aiAdvice = "AIが分析中..."
        val context = getApplication<Application>()
        viewModelScope.launch {
            val grapeText = if (totalGrape > 0 && totalGames > 0) {
                "1/${String.format(java.util.Locale.US, "%.2f", totalGames.toDouble() / totalGrape)} (累計 $totalGrape 回, 前任者逆算: $priorGrape, 自分実測: $myGrape)"
            } else {
                "未入力"
            }

            val cherryText = if (myCherry > 0 && myGames > 0) {
                "1/${String.format(java.util.Locale.US, "%.2f", myGames.toDouble() / myCherry)} (自分実測 $myCherry 回)"
            } else {
                "未入力"
            }

            val estimationText = probabilities.mapIndexed { index, probability ->
                "設定${index + 1}: ${formatPercent(probability)}"
            }.joinToString("\n")

            val expectedText = "${formatYen(expectedYen)} (今後 $futureGames G遊技時)"

            val myDataText = "【自分データ】ゲーム数: $myGames G, BB: $myBb, RB: $myRb, ブドウ: $myGrape 回, チェリー: $myCherry 回"
            if (isCherryDetailMode) {
                myDataText + " (単独BB: $mySoloBb, 単独RB: $mySoloRb, 重複BB: $myCherryBb, 重複RB: $myCherryRb)"
            }
            val priorDataText = "【前任者データ】ゲーム数: $priorGames G, BB: $priorBb, RB: $priorRb, 打ち始め時差枚: $priorDiff 枚, 前任逆算ブドウ: $priorGrape 回"
            val diffText = "【現在の0基準累計差枚数】$totalDiffCoins 枚 (自分の差枚: $myDiffCoins 枚)"
            val storeText = "【店舗情報】店舗名: $storeName, イベント・特定日: $eventName"

            val customNotes = """
                $hallNotes
                ---
                $storeText
                $priorDataText
                $myDataText
                $diffText
                【続行推奨度】$continueRecommendationScore %
            """.trimIndent()

            try {
                aiAdvice = OpenAiService.getSettingAdvice(
                    context = context,
                    modelName = selectedSpec.name,
                    games = totalGames,
                    bb = totalBb,
                    rb = totalRb,
                    grapeProbText = grapeText,
                    estimationText = estimationText,
                    investmentYen = investmentYen,
                    recoveryCoins = recoveryCoins,
                    expectedValueText = expectedText,
                    hallNotes = customNotes
                )
            } finally {
                isAiLoading = false
            }
        }
    }

    fun requestAiSimulatorAdvice(dataSummary: String) {
        if (isSimAiLoading) return

        isSimAiLoading = true
        simAiAdvice = "AIが分析中..."
        val context = getApplication<Application>()
        viewModelScope.launch {
            try {
                simAiAdvice = OpenAiService.getSimulatorAdvice(context, dataSummary)
            } catch (e: Exception) {
                simAiAdvice = "分析エラー: ${e.message}"
            } finally {
                isSimAiLoading = false
            }
        }
    }

    private fun updateCounter(update: () -> Unit) {
        update()
        recalculate()
        recordHistorySnapshot()
    }

    private fun String.toNonNegativeInt(): Int =
        toIntOrNull()?.coerceAtLeast(0) ?: 0

    private fun String.toNonNegativeIntOrDefault(defaultValue: Int): Int =
        toIntOrNull()?.coerceAtLeast(0) ?: defaultValue
}
