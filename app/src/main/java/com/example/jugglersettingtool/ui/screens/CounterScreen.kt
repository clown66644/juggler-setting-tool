package com.example.jugglersettingtool.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.theme.TextPrimary
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.formatHitRatio
import com.example.jugglersettingtool.utils.formatPercent

@Composable
fun CounterScreen(viewModel: JugglerViewModel) {
    var isPriorExpanded by remember { mutableStateOf(viewModel.priorGames == 0) }

    ScreenColumn(title = "カウンター & 設定推測") {
        // 並行判別タブ (メイン台 vs 周辺候補台)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp)
        ) {
            val mainTabBg = if (viewModel.activeCounterTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent
            val subTabBg = if (viewModel.activeCounterTab == 1) MaterialTheme.colorScheme.secondary else Color.Transparent
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(mainTabBg)
                    .clickable { viewModel.switchCounterTab(0) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "メイン台 (実戦中)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.activeCounterTab == 0) Color.White else TextPrimary.copy(alpha = 0.8f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(subTabBg)
                    .clickable { viewModel.switchCounterTab(1) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "周辺候補台 (キープ)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (viewModel.activeCounterTab == 1) Color.White else TextPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // 並行判別比較パネル (2台の比較)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "設定6期待度 比較:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "メイン台: ${formatPercent(viewModel.probabilities[5])}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text(
                        text = "候補台: ${formatPercent(viewModel.subProbabilities[5])}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        if (viewModel.activeCounterTab == 0) {
            // ================= メイン台表示 =================

            // 機種表示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "実戦機種: ${viewModel.selectedSpec.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "機種変更は履歴・ホームから",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // 前任者アコーディオン
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPriorExpanded = !isPriorExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "打つ前の履歴 (前任者データ)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!isPriorExpanded) {
                                Text(
                                    text = "${viewModel.priorGames}G | BB ${viewModel.priorBb} | RB ${viewModel.priorRb} | 差枚 ${if (viewModel.priorDiff >= 0) "+" else ""}${viewModel.priorDiff}枚",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isPriorExpanded && viewModel.priorGames > 0 && viewModel.priorGrape > 0) {
                                Text(
                                    text = "逆算ブドウ: ${formatHitRatio(viewModel.priorGames, viewModel.priorGrape, digits = 2)}",
                                    fontSize = 11.sp,
                                    color = NeonGreen,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isPriorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "開閉"
                            )
                        }
                    }

                    AnimatedVisibility(visible = isPriorExpanded) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NumberTextField(
                                    value = viewModel.priorGames.toInputText(),
                                    onValueChange = viewModel::updatePriorGames,
                                    label = "前任者ゲーム数",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("0 G") }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    QuickAddButton(text = "+1000", onClick = { viewModel.addPriorGames(1000) })
                                    QuickAddButton(text = "+500", onClick = { viewModel.addPriorGames(500) })
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberTextField(
                                    value = viewModel.priorBb.toInputText(),
                                    onValueChange = viewModel::updatePriorBb,
                                    label = "前任者 BIG",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("0 回") }
                                )
                                NumberTextField(
                                    value = viewModel.priorRb.toInputText(),
                                    onValueChange = viewModel::updatePriorRb,
                                    label = "前任者 REG",
                                    keyboardType = KeyboardType.Number,
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("0 回") }
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.background)
                                            .padding(2.dp)
                                    ) {
                                        Text(
                                            text = "＋",
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (!viewModel.priorDiffIsMinus) MaterialTheme.colorScheme.primary else Color.Transparent)
                                                .clickable { viewModel.togglePriorDiffSign(false) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!viewModel.priorDiffIsMinus) Color.White else TextPrimary
                                        )
                                        Text(
                                            text = "ー",
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (viewModel.priorDiffIsMinus) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                                .clickable { viewModel.togglePriorDiffSign(true) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (viewModel.priorDiffIsMinus) Color.White else TextPrimary
                                        )
                                    }

                                    NumberTextField(
                                        value = if (viewModel.priorDiff == 0) "" else Math.abs(viewModel.priorDiff).toString(),
                                        onValueChange = viewModel::updatePriorDiff,
                                        label = "打ち始め時の0基準差枚数 (枚)",
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("0 枚") }
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val cur = viewModel.priorDiff
                                            viewModel.updatePriorDiff((cur - 1000).toString())
                                            if (viewModel.priorDiff < 0) viewModel.togglePriorDiffSign(true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                                    ) {
                                        Text("-1000枚", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Button(
                                        onClick = {
                                            val cur = viewModel.priorDiff
                                            viewModel.updatePriorDiff((cur - 500).toString())
                                            if (viewModel.priorDiff < 0) viewModel.togglePriorDiffSign(true)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                                    ) {
                                        Text("-500枚", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Button(
                                        onClick = {
                                            val cur = viewModel.priorDiff
                                            viewModel.updatePriorDiff((cur + 500).toString())
                                            if (viewModel.priorDiff >= 0) viewModel.togglePriorDiffSign(false)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                                    ) {
                                        Text("+500枚", fontSize = 10.sp, color = NeonGreen)
                                    }
                                    Button(
                                        onClick = {
                                            val cur = viewModel.priorDiff
                                            viewModel.updatePriorDiff((cur + 1000).toString())
                                            if (viewModel.priorDiff >= 0) viewModel.togglePriorDiffSign(false)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                                    ) {
                                        Text("+1000枚", fontSize = 10.sp, color = NeonGreen)
                                    }
                                }

                                Button(
                                    onClick = { isPriorExpanded = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("適用して閉じる", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 自分データ (実戦履歴)
            SectionCard(title = "自分データ (実戦履歴)") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 自分の目押し設定
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "自分の目押し設定 (ブドウ逆算の自動補正に連動)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("チェリーを狙い打ちする (取りこぼしなし)", fontSize = 12.sp)
                            Switch(
                                checked = viewModel.eyeMoshiCherry,
                                onCheckedChange = viewModel::updateEyeMoshiCherry,
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ベル・ピエロも狙い打ちする (完全奪取)", fontSize = 12.sp)
                            Switch(
                                checked = viewModel.eyeMoshiBellPiere,
                                onCheckedChange = viewModel::updateEyeMoshiBellPiere,
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberTextField(
                            value = viewModel.myGames.toInputText(),
                            onValueChange = viewModel::updateMyGames,
                            label = "自分のゲーム数",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("0 G") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            QuickAddButton(text = "+100 G", onClick = { viewModel.addMyGames(100) })
                            QuickAddButton(text = "+10 G", onClick = { viewModel.addMyGames(10) })
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("チェリー重複を個別カウントする", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("※単独と重複の比率から設定推測精度を高めます。", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = viewModel.isCherryDetailMode,
                            onCheckedChange = viewModel::updateCherryDetailMode,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (!viewModel.isCherryDetailMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CounterCard(
                                    label = "自分 BIG",
                                    count = viewModel.myBb,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onIncrement = viewModel::incrementMyBb,
                                    onDecrement = viewModel::decrementMyBb,
                                    modifier = Modifier.weight(1f)
                                )
                                CounterCard(
                                    label = "自分 REG",
                                    count = viewModel.myRb,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    onIncrement = viewModel::incrementMyRb,
                                    onDecrement = viewModel::decrementMyRb,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CounterCard(
                                    label = "マイブドウ",
                                    count = viewModel.myGrape,
                                    color = NeonGreen,
                                    onIncrement = viewModel::incrementMyGrape,
                                    onDecrement = viewModel::decrementMyGrape,
                                    modifier = Modifier.weight(1f)
                                )
                                CounterCard(
                                    label = "マイチェリー",
                                    count = viewModel.myCherry,
                                    color = Color(0xFFFF9F0A),
                                    onIncrement = viewModel::incrementMyCherry,
                                    onDecrement = viewModel::decrementMyCherry,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CounterCard(
                                    label = "単独 BIG",
                                    count = viewModel.mySoloBb,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onIncrement = viewModel::incrementMySoloBb,
                                    onDecrement = viewModel::decrementMySoloBb,
                                    modifier = Modifier.weight(1f)
                                )
                                CounterCard(
                                    label = "重複 BIG",
                                    count = viewModel.myCherryBb,
                                    color = Color(0xFFFF9F0A),
                                    onIncrement = viewModel::incrementMyCherryBb,
                                    onDecrement = viewModel::decrementMyCherryBb,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CounterCard(
                                    label = "単独 REG",
                                    count = viewModel.mySoloRb,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    onIncrement = viewModel::incrementMySoloRb,
                                    onDecrement = viewModel::decrementMySoloRb,
                                    modifier = Modifier.weight(1f)
                                )
                                CounterCard(
                                    label = "重複 REG",
                                    count = viewModel.myCherryRb,
                                    color = Color(0xFFBF5AF2),
                                    onIncrement = viewModel::incrementMyCherryRb,
                                    onDecrement = viewModel::decrementMyCherryRb,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CounterCard(
                                    label = "マイブドウ",
                                    count = viewModel.myGrape,
                                    color = NeonGreen,
                                    onIncrement = viewModel::incrementMyGrape,
                                    onDecrement = viewModel::decrementMyGrape,
                                    modifier = Modifier.weight(1f)
                                )
                                CounterCard(
                                    label = "通常チェリー",
                                    count = viewModel.myCherry,
                                    color = Color(0xFF5AC8FA),
                                    onIncrement = viewModel::incrementMyCherry,
                                    onDecrement = viewModel::decrementMyCherry,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // やめどき/続行 自動ボーダー判定ダッシュボード
            SectionCard(title = "やめどき判定 (粘り・勝ち逃げボーダー)") {
                val score = viewModel.continueRecommendationScore
                val recommendationText = when {
                    score >= 70 -> "続行推奨 (設定良挙動・高設定濃厚)"
                    score >= 40 -> "様子見推奨 (挙動確認中・ボーダーライン)"
                    else -> "やめ推奨 (低設定懸念・勝ち逃げ推奨)"
                }
                val barColor = when {
                    score >= 70 -> NeonGreen
                    score >= 40 -> Color(0xFFFFD60A)
                    else -> MaterialTheme.colorScheme.secondary
                }
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = recommendationText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = barColor
                    )
                    
                    // アニメーションプログレスバー
                    val animatedProgress by animateFloatAsState(
                        targetValue = score / 100f,
                        label = "Continue Recommendation Bar"
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("続行推奨度: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .background(barColor)
                            )
                        }
                        Text(
                            text = "$score %",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            // 設定推測の時系列推移チャート (Canvasによる折れ線描画)
            SectionCard(title = "設定6確率推移チャート (時系列データ)") {
                if (viewModel.gamesHistoryList.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "データ収集中 (ゲーム数や小役が増えると自動描画されます)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    val games = viewModel.gamesHistoryList
                    val probs = viewModel.setting6HistoryList
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "設定6期待度の推移 (G数軸)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        TimelineChart(
                            gamesList = games,
                            prob6List = probs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }
            }

            // 当日累計データ
            SectionCard(title = "当日累計データ (合算結果)") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "総ゲーム数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.totalGames} G", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "BIG回数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.totalBb} 回", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "REG回数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(text = "${viewModel.totalRb} 回", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.background)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val totalBonus = viewModel.totalBb + viewModel.totalRb
                    RatioSummary(
                        label = "ボーナス合算",
                        value = formatHitRatio(viewModel.totalGames, totalBonus, digits = 1),
                        color = MaterialTheme.colorScheme.primary
                    )
                    RatioSummary(
                        label = "累計ブドウ確率",
                        value = formatHitRatio(viewModel.totalGames, viewModel.totalGrape, digits = 2),
                        color = NeonGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "自分実測チェリー確率: ${formatHitRatio(viewModel.myGames, viewModel.myCherry, digits = 2)} (${viewModel.myCherry}回)",
                        fontSize = 11.sp,
                        color = Color(0xFFFF9F0A),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (viewModel.priorGrape > 0 || viewModel.myGrape > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "※ブドウ内訳 [前任者逆算: ${viewModel.priorGrape}回 | 自分実測: ${viewModel.myGrape}回]",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // 推測結果
            SectionCard(title = "ベイズ設定推測結果 (累計ベース)") {
                viewModel.probabilities.forEachIndexed { index, probability ->
                    SettingProbabilityRow(
                        setting = index + 1,
                        probability = probability
                    )
                }
            }

            // 保存ボタン
            Button(
                onClick = { viewModel.saveCurrentGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("実戦データを終了して履歴に保存", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }

        } else {
            // ================= 周辺候補台表示 (サブカウンター) =================
            SectionCard(title = "周辺候補台のカウント (キープ用)") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "周囲の気になる良挙動台や、空きそうな台のデータを裏でメモ・カウントできます。スランプグラフ上の差枚数からブドウ確率を自動で逆算して設定推測に連動します。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberTextField(
                            value = viewModel.subGames.toInputText(),
                            onValueChange = viewModel::updateSubGames,
                            label = "候補台のゲーム数",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("0 G") }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            QuickAddButton(text = "+100 G", onClick = { viewModel.addSubGames(100) })
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CounterCard(
                            label = "候補台 BIG",
                            count = viewModel.subBb,
                            color = MaterialTheme.colorScheme.secondary,
                            onIncrement = viewModel::incrementSubBb,
                            onDecrement = viewModel::decrementSubBb,
                            modifier = Modifier.weight(1f)
                        )
                        CounterCard(
                            label = "候補台 REG",
                            count = viewModel.subRb,
                            color = MaterialTheme.colorScheme.tertiary,
                            onIncrement = viewModel::incrementSubRb,
                            onDecrement = viewModel::decrementSubRb,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 候補台の差枚数入力 (＋/ー トグル ＆ クイック調整ボタン)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "＋",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (!viewModel.subDiffIsMinus) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { viewModel.toggleSubDiffSign(false) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!viewModel.subDiffIsMinus) Color.White else TextPrimary
                                )
                                Text(
                                    text = "ー",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (viewModel.subDiffIsMinus) MaterialTheme.colorScheme.secondary else Color.Transparent)
                                        .clickable { viewModel.toggleSubDiffSign(true) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (viewModel.subDiffIsMinus) Color.White else TextPrimary
                                )
                            }

                            NumberTextField(
                                value = if (viewModel.subDiff == 0) "" else Math.abs(viewModel.subDiff).toString(),
                                onValueChange = viewModel::updateSubDiff,
                                label = "候補台の0基準差枚数 (枚)",
                                keyboardType = KeyboardType.Number,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("0 枚") }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    val cur = viewModel.subDiff
                                    viewModel.updateSubDiff((cur - 1000).toString())
                                    if (viewModel.subDiff < 0) viewModel.toggleSubDiffSign(true)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Text("-1000枚", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Button(
                                onClick = {
                                    val cur = viewModel.subDiff
                                    viewModel.updateSubDiff((cur - 500).toString())
                                    if (viewModel.subDiff < 0) viewModel.toggleSubDiffSign(true)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Text("-500枚", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Button(
                                onClick = {
                                    val cur = viewModel.subDiff
                                    viewModel.updateSubDiff((cur + 500).toString())
                                    if (viewModel.subDiff >= 0) viewModel.toggleSubDiffSign(false)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Text("+500枚", fontSize = 10.sp, color = NeonGreen)
                            }
                            Button(
                                onClick = {
                                    val cur = viewModel.subDiff
                                    viewModel.updateSubDiff((cur + 1000).toString())
                                    if (viewModel.subDiff >= 0) viewModel.toggleSubDiffSign(false)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                            ) {
                                Text("+1000枚", fontSize = 10.sp, color = NeonGreen)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val totalSubBonus = viewModel.subBb + viewModel.subRb
                        RatioSummary(
                            label = "合算確率",
                            value = formatHitRatio(viewModel.subGames, totalSubBonus, digits = 1),
                            color = MaterialTheme.colorScheme.primary
                        )
                        RatioSummary(
                            label = "逆算ブドウ確率",
                            value = formatHitRatio(viewModel.subGames, viewModel.subGrape, digits = 2),
                            color = NeonGreen
                        )
                    }
                }
            }

            SectionCard(title = "周辺候補台の設定推測結果") {
                viewModel.subProbabilities.forEachIndexed { index, probability ->
                    SettingProbabilityRow(
                        setting = index + 1,
                        probability = probability
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun TimelineChart(
    gamesList: List<Int>,
    prob6List: List<Double>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val neonGreen = NeonGreen

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxG = gamesList.maxOrNull()?.toDouble() ?: 1.0
        val minG = gamesList.minOrNull()?.toDouble() ?: 0.0
        val gRange = if (maxG == minG) 1.0 else maxG - minG

        // 横線のガイド (0%, 25%, 50%, 75%, 100%)
        val gridLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        gridLines.forEach { ratio ->
            val y = height * (1f - ratio)
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val points = mutableListOf<Offset>()
        for (i in gamesList.indices) {
            val g = gamesList[i].toDouble()
            val prob = prob6List[i] / 100.0 // 0.0 〜 1.0

            val x = if (gRange == 0.0) 0f else ((g - minG) / gRange * width).toFloat()
            val y = (height * (1.0 - prob)).toFloat()
            points.add(Offset(x, y))
        }

        // 折れ線の描画
        if (points.size >= 2) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }
            drawPath(
                path = path,
                color = neonGreen,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // 各プロット点の描画
            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    center = point,
                    radius = 3.dp.toPx()
                )
                drawCircle(
                    color = neonGreen,
                    center = point,
                    radius = 1.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun QuickAddButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text(text, fontSize = 11.sp)
    }
}

@Composable
private fun RatioSummary(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SettingProbabilityRow(setting: Int, probability: Double) {
    val animatedProgress by animateFloatAsState(
        targetValue = (probability / 100.0).toFloat(),
        label = "Setting $setting Progress"
    )
    val highlightColor = if (setting >= 5) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "設定 $setting", fontSize = 12.sp, modifier = Modifier.width(50.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(highlightColor)
            )
        }
        Text(
            text = formatPercent(probability),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(50.dp)
                .padding(start = 8.dp),
            color = if (setting >= 5) MaterialTheme.colorScheme.secondary else TextPrimary
        )
    }
}

@Composable
fun CounterCard(
    label: String,
    count: Int,
    color: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = onDecrement,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
                ) {
                    Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onIncrement,
                    modifier = Modifier.weight(1.3f),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    val contentColor = if (color == NeonGreen || color == Color(0xFFFF9F0A) || color == Color(0xFF5AC8FA)) Color.Black else TextPrimary
                    Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
    }
}

private fun Int.toInputText(): String = if (this == 0) "" else toString()
