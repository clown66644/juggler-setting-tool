package com.example.jugglersettingtool.ui.screens

import com.example.jugglersettingtool.data.HistoryItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.theme.TextPrimary
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.formatYen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: JugglerViewModel) {
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: カレンダー収支, 1: ホール別傾向分析
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val monthYearStr = SimpleDateFormat("yyyy年 M月", Locale.JAPANESE).format(calendar.time)

    val daysList = remember(year, month) { generateDaysForMonth(calendar) }

    val totalMonthlyProfit = remember(viewModel.dailyProfitMap, year, month) {
        calculateMonthlyProfit(viewModel.dailyProfitMap, year, month)
    }

    // ホール別分析データの集計
    val storeStats = remember(viewModel.historyList) {
        aggregateStoreStats(viewModel.historyList)
    }

    ScreenColumn(title = "カレンダー & ホール分析") {
        // タブ切り替え
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp)
        ) {
            val calTabBg = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent
            val storeTabBg = if (activeTab == 1) MaterialTheme.colorScheme.secondary else Color.Transparent
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(calTabBg)
                    .clickable { activeTab = 0 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "カレンダー収支",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 0) Color.White else TextPrimary.copy(alpha = 0.8f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(storeTabBg)
                    .clickable { activeTab = 1 }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ホール設定分析",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == 1) Color.White else TextPrimary.copy(alpha = 0.8f)
                )
            }
        }

        if (activeTab == 0) {
            // ================= カレンダー収支表示 =================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val nextCal = calendar.clone() as Calendar
                    nextCal.add(Calendar.MONTH, -1)
                    calendar = nextCal
                }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "前月")
                }

                Text(
                    text = monthYearStr,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = {
                    val nextCal = calendar.clone() as Calendar
                    nextCal.add(Calendar.MONTH, 1)
                    calendar = nextCal
                }) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "翌月")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当月の合計収支",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatYen(totalMonthlyProfit, signed = true),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalMonthlyProfit >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val weekdays = listOf("日", "月", "火", "水", "木", "金", "土")
                weekdays.forEachIndexed { index, day ->
                    val color = when (index) {
                        0 -> MaterialTheme.colorScheme.secondary
                        6 -> MaterialTheme.colorScheme.primary
                        else -> TextPrimary
                    }
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(daysList.size) { index ->
                    val dayData = daysList[index]
                    if (dayData == null) {
                        Box(modifier = Modifier.aspectRatio(0.85f))
                    } else {
                        val dateKey = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayData)
                        val dailyProfit = viewModel.dailyProfitMap[dateKey]

                        CalendarCell(
                            day = dayData,
                            profit = dailyProfit,
                            modifier = Modifier.aspectRatio(0.85f)
                        )
                    }
                }
            }
        } else {
            // ================= ホール別設定分析 =================
            if (storeStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "分析データがありません (店舗名を入力した履歴が必要です)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(storeStats) { stat ->
                        StoreStatCard(stat = stat)
                    }
                }
            }
        }
    }
}

data class StoreStat(
    val storeName: String,
    val totalProfit: Double,
    val count: Int,
    val avgSetting: Double
)

private fun aggregateStoreStats(historyList: List<HistoryItem>): List<StoreStat> {
    val storeMap = mutableMapOf<String, MutableList<HistoryItem>>()
    for (item in historyList) {
        val name = item.storeName.ifEmpty { "店舗名未登録" }
        val list = storeMap[name] ?: mutableListOf()
        list.add(item)
        storeMap[name] = list
    }

    return storeMap.map { (name, list) ->
        val profit = list.sumOf { it.profitYen }
        val avgSetting = list.map { it.estimatedSetting.toDouble() }.average()
        StoreStat(
            storeName = name,
            totalProfit = profit,
            count = list.size,
            avgSetting = if (avgSetting.isNaN()) 1.0 else avgSetting
        )
    }.sortedByDescending { it.totalProfit } // 収支の良い順に並び替え
}

@Composable
private fun StoreStatCard(stat: StoreStat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = stat.storeName,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("実戦回数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("${stat.count} 回", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("平均推定設定", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        text = String.format(Locale.US, "設定 %.2f", stat.avgSetting),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("通算収支", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(
                        text = formatYen(stat.totalProfit, signed = true),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stat.totalProfit >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    day: Int,
    profit: Double?,
    modifier: Modifier = Modifier
) {
    val isPositive = profit != null && profit >= 0
    
    val bgColor = when {
        profit != null && isPositive -> Color(0x2039FF14)
        profit != null && !isPositive -> Color(0x20FF073A)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        profit != null && isPositive -> NeonGreen
        profit != null && !isPositive -> MaterialTheme.colorScheme.secondary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = day.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (profit != null) {
                val profitK = profit / 1000.0
                val profitStr = if (isPositive) {
                    "+${String.format(Locale.US, "%.0f", profitK)}k"
                } else {
                    "${String.format(Locale.US, "%.0f", profitK)}k"
                }
                
                Text(
                    text = profitStr,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isPositive) NeonGreen else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.End),
                    maxLines = 1
                )
            }
        }
    }
}

private fun generateDaysForMonth(calendar: Calendar): List<Int?> {
    val tempCal = calendar.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)
    val startDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
    val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val days = mutableListOf<Int?>()
    for (i in 0 until startDayOfWeek) {
        days.add(null)
    }
    for (i in 1..maxDays) {
        days.add(i)
    }
    while (days.size % 7 != 0) {
        days.add(null)
    }
    return days
}

private fun calculateMonthlyProfit(dailyProfitMap: Map<String, Double>, year: Int, month: Int): Double {
    var sum = 0.0
    val monthPrefix = String.format(Locale.US, "%d-%02d-", year, month + 1)
    for ((dateKey, profit) in dailyProfitMap) {
        if (dateKey.startsWith(monthPrefix)) {
            sum += profit
        }
    }
    return sum
}
