package com.example.jugglersettingtool.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.Screen
import com.example.jugglersettingtool.data.JugglerSpec
import com.example.jugglersettingtool.data.JugglerSpecProvider
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.theme.TextPrimary
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.EstimationLogic
import kotlinx.coroutines.delay
import kotlin.random.Random

enum class PlaySpeed {
    Normal,
    Fast
}

@Composable
fun JugglerSimulatorScreen(viewModel: JugglerViewModel) {
    val scrollState = rememberScrollState()

    // シミュレーター基本設定
    var selectedSimSpec by remember { mutableStateOf(JugglerSpecProvider.specs.first()) }
    var specDropdownExpanded by remember { mutableStateOf(false) }

    var targetGames by remember { mutableIntStateOf(9000) } // 目標回転数
    var targetGamesInput by remember { mutableStateOf("9000") } // テキスト入力用
    var isFreeUchi by remember { mutableStateOf(true) }     // フリー打ち or 完全奪取
    
    // シミュレートモード (true: 設定隠し当て, false: 設定指定)
    var isHideSetting by remember { mutableStateOf(true) }
    // 指定する設定値 (1〜6)
    var selectedSetting by remember { mutableIntStateOf(6) }

    // 裏でランダムに設定される、または指定された実戦設定値 (1〜6)
    var hiddenSetting by remember { mutableIntStateOf(Random.nextInt(1, 7)) }

    // シミュレーター内部カウンター
    var simGames by remember { mutableIntStateOf(0) }
    var simBb by remember { mutableIntStateOf(0) }
    var simRb by remember { mutableIntStateOf(0) }
    var simGrape by remember { mutableIntStateOf(0) }
    var simCherry by remember { mutableIntStateOf(0) }
    var simCurrentGames by remember { mutableIntStateOf(0) }
    var simDiffCoins by remember { mutableIntStateOf(0) }

    // 連チャン・ハマり記録
    var simMaxHamari by remember { mutableIntStateOf(0) }
    var simMaxCombo by remember { mutableIntStateOf(0) }
    var currentCombo by remember { mutableIntStateOf(0) } // 現在進行形の連チャン数

    // オート実戦状態
    var isAutoPlaying by remember { mutableStateOf(false) }
    var playSpeedMode by remember { mutableStateOf(PlaySpeed.Normal) } // 通常オート / 高速オート

    // 回答 ＆ 答え合わせ状態
    var showAnswerDialog by remember { mutableStateOf(false) }
    var guessedSetting by remember { mutableIntStateOf(0) }
    var isAnswered by remember { mutableStateOf(false) }

    // スランプグラフ履歴
    val diffHistory = remember { mutableListOf(0) }

    // シミュレータ起動時の初期リセット
    fun resetSimulator(newSpec: JugglerSpec = selectedSimSpec) {
        hiddenSetting = if (isHideSetting) Random.nextInt(1, 7) else selectedSetting
        simGames = 0
        simBb = 0
        simRb = 0
        simGrape = 0
        simCherry = 0
        simCurrentGames = 0
        simDiffCoins = 0
        simMaxHamari = 0
        simMaxCombo = 0
        currentCombo = 0
        isAutoPlaying = false
        isAnswered = false
        guessedSetting = 0
        viewModel.simAiAdvice = ""
        diffHistory.clear()
        diffHistory.add(0)
    }

    // 1G消化の抽選処理 (スペックと隠し設定に基づいて決定)
    fun playOneGame() {
        if (simGames >= targetGames) {
            isAutoPlaying = false
            return
        }

        val settingIdx = hiddenSetting - 1
        val spec = selectedSimSpec

        // 確率データ (設定別)
        val grapeProb = 1.0 / spec.grapeProb[settingIdx]
        val cherryProb = 1.0 / 33.0
        val bbProb = 1.0 / spec.bbProb[settingIdx]
        val rbProb = 1.0 / spec.rbProb[settingIdx]

        // ベル・ピエロ確率
        val bellProb = 1.0 / 1024.0
        val pieroProb = 1.0 / 1024.0

        simGames++
        simCurrentGames++
        simDiffCoins -= 3 // 3枚投入

        // リアルタイムで最大ハマりを更新 (ボーナス間ハマりが現在進行形で最大値を超えたとき)
        if (simCurrentGames > simMaxHamari) {
            simMaxHamari = simCurrentGames
        }

        val rand = Random.nextDouble()

        // ボーナス当選判定時の共通処理 (連チャン判定含む)
        fun onBonusWon() {
            if (simCurrentGames <= 100) {
                currentCombo++
                if (currentCombo > simMaxCombo) {
                    simMaxCombo = currentCombo
                }
            } else {
                currentCombo = 1
                if (simMaxCombo == 0) {
                    simMaxCombo = 1
                }
            }
            simCurrentGames = 0
        }

        when {
            // BIG当選
            rand < bbProb -> {
                simBb++
                onBonusWon()
                simDiffCoins += 240 // BIG払い出し
            }
            // REG当選
            rand < bbProb + rbProb -> {
                simRb++
                onBonusWon()
                simDiffCoins += 96 // REG払い出し
            }
            // ブドウ当選 (8枚払い出し)
            rand < bbProb + rbProb + grapeProb -> {
                simGrape++
                simDiffCoins += spec.grapePay
            }
            // チェリー当選 (2枚払い出し)
            rand < bbProb + rbProb + grapeProb + cherryProb -> {
                simCherry++
                if (isFreeUchi) {
                    // フリー打ちは 66.7% のみ獲得
                    if (Random.nextDouble() < 0.667) {
                        simDiffCoins += spec.cherryPay
                    }
                } else {
                    simDiffCoins += spec.cherryPay
                }
            }
            // ベル当選 (14枚払い出し・完全奪取時のみ獲得)
            rand < bbProb + rbProb + grapeProb + cherryProb + bellProb -> {
                if (!isFreeUchi) {
                    simDiffCoins += 14
                }
            }
            // ピエロ当選 (10枚払い出し・完全奪取時のみ獲得)
            rand < bbProb + rbProb + grapeProb + cherryProb + bellProb + pieroProb -> {
                if (!isFreeUchi) {
                    simDiffCoins += 10
                }
            }
        }

        // 定期的にグラフをプロットする (G数に応じてプロット間隔を最適化)
        val plotInterval = if (targetGames <= 10000) 20 else 50
        if (simGames % plotInterval == 0 || simGames == targetGames) {
            diffHistory.add(simDiffCoins)
        }
    }

    // 通常・高速オートプレイ・コルーチン
    LaunchedEffect(isAutoPlaying, playSpeedMode) {
        if (isAutoPlaying) {
            val delayTime = when (playSpeedMode) {
                PlaySpeed.Normal -> 30L
                PlaySpeed.Fast -> 1L // 爆速
            }
            while (isAutoPlaying && simGames < targetGames) {
                delay(delayTime)
                playOneGame()
            }
            if (simGames >= targetGames) {
                isAutoPlaying = false
            }
        }
    }

    // ツール自体のベイズ設定推測結果の算出 (がりぞうブドウ確率定義がそのままベイズ推定にも適用されます)
    val simProbabilities = remember(simGames, simBb, simRb, simGrape, selectedSimSpec) {
        if (simGames <= 0) {
            DoubleArray(6) { 16.67 }
        } else {
            EstimationLogic.estimateSettings(
                spec = selectedSimSpec,
                games = simGames,
                bb = simBb,
                rb = simRb,
                grape = simGrape,
                cherry = simCherry
            )
        }
    }

    // 実際のブドウ出現率
    val actualGrapeDenominator = remember(simGames, simGrape) {
        if (simGames <= 0 || simGrape <= 0) 0.0 else simGames.toDouble() / simGrape
    }

    ScreenColumn(title = "設定判別シミュレーター") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    isAutoPlaying = false
                    viewModel.openWelcomeScreen()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "戻る",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("ホームに戻る", color = MaterialTheme.colorScheme.primary)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. シミュレーター設定カード
            SectionCard(title = "シミュレータ設定") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 機種ドロップダウン
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("実戦シミュレート機種: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Box {
                            Text(
                                text = selectedSimSpec.name + " ▼",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { specDropdownExpanded = true }
                                    .padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                            DropdownMenu(
                                expanded = specDropdownExpanded,
                                onDismissRequest = { specDropdownExpanded = false }
                            ) {
                                JugglerSpecProvider.specs.forEach { spec ->
                                    DropdownMenuItem(
                                        text = { Text(spec.name) },
                                        onClick = {
                                            selectedSimSpec = spec
                                            specDropdownExpanded = false
                                            resetSimulator(spec)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 目標回転数 (自由入力欄 ＋ プリセット)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("目標回転数 (最大30,000G): ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = targetGamesInput,
                                onValueChange = {
                                    targetGamesInput = it
                                    val parsed = it.toIntOrNull()?.coerceIn(1, 30000) ?: 9000
                                    targetGames = parsed
                                    resetSimulator()
                                },
                                modifier = Modifier.width(120.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = TextPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                        }

                        // プリセット
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(1000, 3000, 5000, 9000).forEach { preset ->
                                    Button(
                                        onClick = {
                                            targetGames = preset
                                            targetGamesInput = preset.toString()
                                            resetSimulator()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (targetGames == preset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        val textColor = if (targetGames == preset) Color.White else TextPrimary
                                        Text("${preset}G", fontSize = 11.sp, color = textColor)
                                    }
                                }
                            }
                        }
                    }

                    // 設定選択モード
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("設定モード: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    isHideSetting = true
                                    resetSimulator()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isHideSetting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("隠し当て", fontSize = 11.sp, color = if (isHideSetting) Color.White else TextPrimary)
                            }
                            Button(
                                onClick = {
                                    isHideSetting = false
                                    resetSimulator()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isHideSetting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("設定指定", fontSize = 11.sp, color = if (!isHideSetting) Color.White else TextPrimary)
                            }
                        }
                    }

                    // 設定指定モード時の設定選択
                    if (!isHideSetting) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("指定設定値: ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                (1..6).forEach { setting ->
                                    Button(
                                        onClick = {
                                            selectedSetting = setting
                                            resetSimulator()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSetting == setting) NeonGreen else MaterialTheme.colorScheme.surface
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.height(28.dp).width(34.dp)
                                    ) {
                                        val textColor = if (selectedSetting == setting) Color.Black else TextPrimary
                                        Text("$setting", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 打ち方設定
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("フリー打ち (小役こぼしあり)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "チェリーこぼし率33.3%, ベル・ピエロ全こぼし",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = isFreeUchi,
                            onCheckedChange = {
                                isFreeUchi = it
                                resetSimulator()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                        )
                    }
                }
            }

            // 2. データカウンター (漆黒LED風)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF070707)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.DarkGray)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "JUGGLER SIM DATA COUNTER",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )

                    // 1行目: TOTAL, BIG, REG
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOTAL G", fontSize = 10.sp, color = Color.LightGray)
                            Text("$simGames / ${targetGames}G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonGreen, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BIG (BB)", fontSize = 10.sp, color = Color.LightGray)
                            val bbRatioText = if (simBb > 0) "1/${simGames / simBb}" else "- "
                            Text("$simBb ($bbRatioText)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF3B30), fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REG (RB)", fontSize = 10.sp, color = Color.LightGray)
                            val rbRatioText = if (simRb > 0) "1/${simGames / simRb}" else "- "
                            Text("$simRb ($rbRatioText)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD60A), fontFamily = FontFamily.Monospace)
                        }
                    }

                    // 2行目: ブドウ, 現在ハマり, 差枚数
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ブドウ", fontSize = 10.sp, color = Color.LightGray)
                            val grapeText = if (actualGrapeDenominator > 0) "1/${String.format(java.util.Locale.US, "%.2f", actualGrapeDenominator)}" else "- "
                            Text(grapeText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5AC8FA), fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("現在ハマり", fontSize = 10.sp, color = Color.LightGray)
                            Text("$simCurrentGames G", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("差枚数 (Coins)", fontSize = 10.sp, color = Color.LightGray)
                            Text(
                                text = "${if (simDiffCoins >= 0) "+" else ""}$simDiffCoins 枚",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (simDiffCoins >= 0) NeonGreen else Color(0xFFFF375F),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // 3行目: 最大ハマり, 最大連チャン, チェリー(完全奪取時のみ)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("最大ハマり", fontSize = 10.sp, color = Color.LightGray)
                            Text("$simMaxHamari G", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("最大連チャン", fontSize = 10.sp, color = Color.LightGray)
                            Text(if (simMaxCombo > 0) "$simMaxCombo 連" else "- ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD60A), fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (!isFreeUchi) "チェリー" else "チェリー (非表示)", fontSize = 10.sp, color = Color.LightGray)
                            val cherryText = if (!isFreeUchi && simCherry > 0) {
                                "1/${String.format(java.util.Locale.US, "%.1f", simGames.toDouble() / simCherry)}"
                            } else {
                                "- "
                            }
                            Text(cherryText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (!isFreeUchi) Color(0xFFFF375F) else Color.DarkGray, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // 3. グラフ
            SectionCard(title = "スランプグラフ") {
                val liveHistory = remember(diffHistory, simDiffCoins) {
                    diffHistory.toList() + simDiffCoins
                }
                SlumpChart(
                    history = liveHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                        .padding(6.dp)
                )
            }

            // 4. 実戦コントロール
            SectionCard(title = "実戦制御") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 通常レバーON
                    Button(
                        onClick = {
                            if (simGames < targetGames) {
                                playOneGame()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("レバーON (1G手動消化)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // オート実戦操作盤
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // オート開始/一時停止
                        Button(
                            onClick = {
                                isAutoPlaying = !isAutoPlaying
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoPlaying) Color(0xFFFF9500) else MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isAutoPlaying) "オート一時停止" else "オート開始",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // オート速度切替 (通常 / 高速)
                        Button(
                            onClick = {
                                playSpeedMode = if (playSpeedMode == PlaySpeed.Normal) PlaySpeed.Fast else PlaySpeed.Normal
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (playSpeedMode == PlaySpeed.Fast) NeonGreen else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val textColor = if (playSpeedMode == PlaySpeed.Fast) Color.Black else TextPrimary
                            Text(
                                text = if (playSpeedMode == PlaySpeed.Fast) "速度: 高速オート" else "速度: 通常オート",
                                fontSize = 12.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 超高速オート (一瞬で完了)
                    Button(
                        onClick = {
                            isAutoPlaying = false
                            val startG = simGames
                            for (i in startG until targetGames) {
                                playOneGame()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5856D6)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("一瞬で目標G数まで回す (超高速)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // リセット
                    Button(
                        onClick = { resetSimulator() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("シミュレーターリセット", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 5. 設定予想または挙動データ表示
            if (isHideSetting) {
                SectionCard(title = "設定を当てる") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "出来上がった挙動を見て、この台の設定を予想してください！",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..6).forEach { setting ->
                                Button(
                                    onClick = {
                                        if (!isAnswered) {
                                            guessedSetting = setting
                                            isAnswered = true
                                            showAnswerDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isAnswered && simGames > 0,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("$setting", fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }

                        // 答え合わせ完了後の結果表示
                        if (isAnswered) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (guessedSetting == hiddenSetting) NeonGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                )
                            ) {
                                ResultDataBlock(
                                    isSuccess = guessedSetting == hiddenSetting,
                                    bannerText = if (guessedSetting == hiddenSetting) "🎉 正解です！おめでとうございます！" else "❌ 残念！不正解です。",
                                    detailText = "【正解】 設定 $hiddenSetting  (あなたの予想: 設定 $guessedSetting)",
                                    hiddenSetting = hiddenSetting,
                                    simGames = simGames,
                                    simBb = simBb,
                                    simRb = simRb,
                                    actualGrapeDenominator = actualGrapeDenominator,
                                    simCherry = simCherry,
                                    simMaxHamari = simMaxHamari,
                                    simMaxCombo = simMaxCombo,
                                    simDiffCoins = simDiffCoins,
                                    isFreeUchi = isFreeUchi,
                                    simProbabilities = simProbabilities,
                                    selectedSimSpec = selectedSimSpec,
                                    viewModel = viewModel,
                                    showAiSection = false // 回答後は、カード単体ではなく共通で下部に表示するので非表示にする
                                )
                            }
                        }

                        // AI判定ツール (回答前・回答後に関わらず常に予想のヒントとしてカード内に表示)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💡 AI設定判定ツール (予想のヒント)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )

                        if (viewModel.simAiAdvice.isEmpty() && !viewModel.isSimAiLoading) {
                            Button(
                                onClick = {
                                    val dataSummary = """
                                        機種: ${selectedSimSpec.name}
                                        総ゲーム数: $simGames G
                                        BIG: $simBb 回 (1/${if (simBb>0) simGames/simBb else 0})
                                        REG: $simRb 回 (1/${if (simRb>0) simGames/simRb else 0})
                                        ブドウ確率: 1/${String.format(java.util.Locale.US, "%.2f", actualGrapeDenominator)}
                                        チェリー確率: ${if (!isFreeUchi) "1/" + String.format(java.util.Locale.US, "%.1f", simGames.toDouble() / simCherry) else "フリー打ち(こぼしあり)"}
                                        最大ハマり: $simMaxHamari G
                                        最大連チャン: $simMaxCombo 連
                                        最終差枚数: $simDiffCoins 枚
                                        打ち方: ${if (isFreeUchi) "フリー打ち" else "小役完全奪取"}
                                        ベイズ推定設定割合: ${simProbabilities.mapIndexed{i,p->"設定${i+1}:${String.format(java.util.Locale.US, "%.1f", p)}%"}.joinToString(", ")}
                                    """.trimIndent()
                                    viewModel.requestAiSimulatorAdvice(dataSummary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = simGames > 0
                            ) {
                                Text("AIに設定を判定させる", fontSize = 12.sp)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = viewModel.simAiAdvice,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            } else {
                SectionCard(title = "設定 $selectedSetting の挙動データ") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "現在の実戦機種・設定条件に基づいたツール推測とAIによるデータ考察です。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            ResultDataBlock(
                                isSuccess = true,
                                bannerText = "設定 $selectedSetting で稼働中",
                                detailText = "指定された設定で実戦シミュレートを行っています。",
                                hiddenSetting = selectedSetting,
                                simGames = simGames,
                                simBb = simBb,
                                simRb = simRb,
                                actualGrapeDenominator = actualGrapeDenominator,
                                simCherry = simCherry,
                                simMaxHamari = simMaxHamari,
                                simMaxCombo = simMaxCombo,
                                simDiffCoins = simDiffCoins,
                                isFreeUchi = isFreeUchi,
                                simProbabilities = simProbabilities,
                                selectedSimSpec = selectedSimSpec,
                                viewModel = viewModel,
                                showAiSection = true
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ResultDataBlock(
    isSuccess: Boolean,
    bannerText: String,
    detailText: String,
    hiddenSetting: Int,
    simGames: Int,
    simBb: Int,
    simRb: Int,
    actualGrapeDenominator: Double,
    simCherry: Int,
    simMaxHamari: Int,
    simMaxCombo: Int,
    simDiffCoins: Int,
    isFreeUchi: Boolean,
    simProbabilities: DoubleArray,
    selectedSimSpec: JugglerSpec,
    viewModel: JugglerViewModel,
    showAiSection: Boolean
) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = bannerText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSuccess) NeonGreen else MaterialTheme.colorScheme.secondary
        )
        Text(
            text = detailText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("■ ツールによるベイズ判定確率:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        simProbabilities.forEachIndexed { idx, prob ->
            val percent = String.format(java.util.Locale.US, "%.1f", prob)
            val isActual = idx + 1 == hiddenSetting
            Text(
                text = "設定${idx + 1}: ${percent}% ${if (isActual) "← (本物)" else ""}",
                fontSize = 12.sp,
                fontWeight = if (isActual) FontWeight.Bold else FontWeight.Normal,
                color = if (isActual) NeonGreen else TextPrimary
            )
        }

        if (showAiSection) {
            Spacer(modifier = Modifier.height(6.dp))

            // AI判定セクション
            Text("■ AI設定考察・アドバイス:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            
            if (viewModel.simAiAdvice.isEmpty() && !viewModel.isSimAiLoading) {
                Button(
                    onClick = {
                        val dataSummary = """
                            機種: ${selectedSimSpec.name}
                            設定値: 設定 $hiddenSetting
                            総ゲーム数: $simGames G
                            BIG: $simBb 回 (1/${if (simBb>0) simGames/simBb else 0})
                            REG: $simRb 回 (1/${if (simRb>0) simGames/simRb else 0})
                            ブドウ確率: 1/${String.format(java.util.Locale.US, "%.2f", actualGrapeDenominator)}
                            チェリー確率: ${if (!isFreeUchi) "1/" + String.format(java.util.Locale.US, "%.1f", simGames.toDouble() / simCherry) else "フリー打ち(ここぼしあり)"}
                            最大ハマり: $simMaxHamari G
                            最大連チャン: $simMaxCombo 連
                            最終差枚数: $simDiffCoins 枚
                            打ち方: ${if (isFreeUchi) "フリー打ち" else "小役完全奪取"}
                            ベイズ推定設定割合: ${simProbabilities.mapIndexed{i,p->"設定${i+1}:${String.format(java.util.Locale.US, "%.1f", p)}%"}.joinToString(", ")}
                        """.trimIndent()
                        viewModel.requestAiSimulatorAdvice(dataSummary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = simGames > 0
                ) {
                    Text("AIに設定を判定させる", fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(10.dp)
                ) {
                    Text(
                        text = viewModel.simAiAdvice,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
@Composable
fun SlumpChart(
    history: List<Int>,
    modifier: Modifier = Modifier
) {
    val neonGreen = NeonGreen
    val alertRed = Color(0xFFFF375F)

    // グリッド線用のテキストペイント
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.argb(120, 200, 200, 200)
            textSize = 22f
            typeface = android.graphics.Typeface.MONOSPACE
            textAlign = android.graphics.Paint.Align.RIGHT
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        // 0ライン (基準線)
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
        )

        // 基準線「±0」テキスト
        drawContext.canvas.nativeCanvas.drawText(
            "±0",
            width - 15f,
            centerY - 6f,
            textPaint
        )

        if (history.isNotEmpty()) {
            val maxDiff = history.maxOf { Math.abs(it) }.coerceAtLeast(100)
            val yFactor = centerY / maxDiff.toFloat()

            // 0基準から500枚ごとの水平グリッド線を表示
            val gridStep = 500
            var currentGrid = gridStep
            while (currentGrid <= maxDiff) {
                val yOffsetUpper = centerY - (currentGrid * yFactor)
                val yOffsetLower = centerY + (currentGrid * yFactor)

                // プラス側（上部）
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, yOffsetUpper),
                    end = Offset(width, yOffsetUpper),
                    strokeWidth = 0.5.dp.toPx()
                )
                // プラス枚数テキスト
                drawContext.canvas.nativeCanvas.drawText(
                    "+$currentGrid 枚",
                    width - 15f,
                    yOffsetUpper - 6f,
                    textPaint
                )

                // マイナス側（下部）
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, yOffsetLower),
                    end = Offset(width, yOffsetLower),
                    strokeWidth = 0.5.dp.toPx()
                )
                // マイナス枚数テキスト
                drawContext.canvas.nativeCanvas.drawText(
                    "-$currentGrid 枚",
                    width - 15f,
                    yOffsetLower - 6f,
                    textPaint
                )
                currentGrid += gridStep
            }

            val xFactor = width / (history.size.coerceAtLeast(2) - 1).toFloat()
            val points = history.mapIndexed { index, diff ->
                Offset(index * xFactor, centerY - (diff * yFactor))
            }

            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                val lineColor = if (history.last() >= 0) neonGreen else alertRed
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}
