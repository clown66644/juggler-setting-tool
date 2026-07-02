package com.example.jugglersettingtool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.EstimationLogic
import com.example.jugglersettingtool.utils.formatHitRatio
import com.example.jugglersettingtool.utils.formatYen

@Composable
fun ExpectationScreen(viewModel: JugglerViewModel) {
    var investmentText by remember { mutableStateOf(viewModel.investmentYen.toInputText()) }
    var recoveryText by remember { mutableStateOf(viewModel.recoveryCoins.toInputText()) }
    var lendText by remember { mutableStateOf(viewModel.lendRate.toString()) }
    var exchangeText by remember { mutableStateOf(viewModel.exchangeRate) }
    var futureGamesText by remember { mutableStateOf(viewModel.futureGames.toString()) }

    val exchangeRate = exchangeText.toDoubleOrNull() ?: 5.6
    val realProfitYen = EstimationLogic.calculateRealProfitYen(
        investmentYen = viewModel.investmentYen,
        recoveryCoins = viewModel.recoveryCoins,
        lendCoinsPer1000Yen = viewModel.lendRate,
        exchangeRate = exchangeRate
    )

    ScreenColumn(title = "期待値 & 収支計算") {
        // レート設定
        SectionCard(title = "レート設定") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberTextField(
                    value = lendText,
                    onValueChange = {
                        lendText = it
                        viewModel.updateLendRate(it)
                    },
                    label = "貸出 (枚/1000円)",
                    modifier = Modifier.weight(1f)
                )
                NumberTextField(
                    value = exchangeText,
                    onValueChange = {
                        exchangeText = it
                        viewModel.updateExchangeRate(it)
                    },
                    label = "交換率 (枚/100円)",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 実戦店舗・イベント入力 (新規追加: マイホール傾向分析用)
        SectionCard(title = "実戦ホール情報 (傾向分析用)") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberTextField(
                    value = viewModel.storeName,
                    onValueChange = viewModel::updateStoreName,
                    label = "店舗名",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例: マルハン〇〇店") }
                )
                NumberTextField(
                    value = viewModel.eventName,
                    onValueChange = viewModel::updateEventName,
                    label = "イベント名・特定日タグ",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("例: 7のつく日、月一特定日") }
                )
            }
        }

        // 実戦収支入力
        SectionCard(title = "実戦収支入力") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 投資額
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberTextField(
                            value = investmentText,
                            onValueChange = {
                                investmentText = it
                                viewModel.updateInvestmentYen(it)
                            },
                            label = "自分の投資額 (円)",
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            QuickAddCoinButton(text = "+1k") {
                                val current = viewModel.investmentYen
                                val next = current + 1000
                                investmentText = next.toString()
                                viewModel.updateInvestmentYen(investmentText)
                            }
                            QuickAddCoinButton(text = "+5k") {
                                val current = viewModel.investmentYen
                                val next = current + 5000
                                investmentText = next.toString()
                                viewModel.updateInvestmentYen(investmentText)
                            }
                            QuickAddCoinButton(text = "+10k") {
                                val current = viewModel.investmentYen
                                val next = current + 10000
                                investmentText = next.toString()
                                viewModel.updateInvestmentYen(investmentText)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))

                // 回収枚数
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NumberTextField(
                            value = recoveryText,
                            onValueChange = {
                                recoveryText = it
                                viewModel.updateRecoveryCoins(it)
                            },
                            label = "現在の持ちメダル / 回収枚数 (枚)",
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            QuickAddCoinButton(text = "+50枚") {
                                viewModel.addRecoveryCoins(50)
                                recoveryText = viewModel.recoveryCoins.toString()
                            }
                            QuickAddCoinButton(text = "+200枚") {
                                viewModel.addRecoveryCoins(200)
                                recoveryText = viewModel.recoveryCoins.toString()
                            }
                            QuickAddCoinButton(text = "+500枚") {
                                viewModel.addRecoveryCoins(500)
                                recoveryText = viewModel.recoveryCoins.toString()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                recoveryText = ""
                                viewModel.updateRecoveryCoins("0")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Text("枚数リセット (0枚)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }

        // 差枚数サマリー
        SectionCard(title = "スランプグラフ連動差枚") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("前任者差枚", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "${if (viewModel.priorDiff >= 0) "+" else ""}${viewModel.priorDiff} 枚",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.priorDiff >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("自分の差枚", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            text = "${if (viewModel.myDiffCoins >= 0) "+" else ""}${viewModel.myDiffCoins} 枚",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.myDiffCoins >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.background, modifier = Modifier.padding(vertical = 4.dp))

                Text("現在の当日累計0基準差枚数", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                val totalDiff = viewModel.totalDiffCoins
                Text(
                    text = "${if (totalDiff >= 0) "+" else ""}$totalDiff 枚",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (totalDiff >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "※前任者データから逆算されたブドウ確率は自動的にベイズ推定に組み込まれています。",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // 今後の期待値計算
        SectionCard(title = "今後の期待値計算") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NumberTextField(
                    value = futureGamesText,
                    onValueChange = {
                        futureGamesText = it
                        viewModel.updateFutureGames(it)
                    },
                    label = "今後回すゲーム数 (G)",
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FutureGamesButton(
                        text = "1000G",
                        games = 1000,
                        onClick = {
                            futureGamesText = it.toString()
                            viewModel.applyFutureGamesPreset(it)
                        }
                    )
                    FutureGamesButton(
                        text = "3000G",
                        games = 3000,
                        onClick = {
                            futureGamesText = it.toString()
                            viewModel.applyFutureGamesPreset(it)
                        }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.background)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProfitSummary(
                    label = "現在の実収支",
                    value = realProfitYen,
                    positiveColor = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                ProfitSummary(
                    label = "今後の期待収支",
                    value = viewModel.expectedYen,
                    positiveColor = Color(0xFFFFD60A),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun QuickAddCoinButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FutureGamesButton(
    text: String,
    games: Int,
    onClick: (Int) -> Unit
) {
    Button(
        onClick = { onClick(games) },
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text)
    }
}

@Composable
private fun ProfitSummary(
    label: String,
    value: Double,
    positiveColor: Color,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(
            text = formatYen(value, signed = true),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (value >= 0) positiveColor else MaterialTheme.colorScheme.secondary
        )
    }
}

private fun Int.toInputText(): String = if (this == 0) "" else toString()
