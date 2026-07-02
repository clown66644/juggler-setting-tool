package com.example.jugglersettingtool.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.data.HistoryItem
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.theme.TextPrimary
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.formatYen

@Composable
fun HistoryScreen(viewModel: JugglerViewModel) {
    var selectedItemForDetail by remember { mutableStateOf<HistoryItem?>(null) }

    ScreenColumn(title = "実戦履歴一覧") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "過去の実戦データ (${viewModel.historyList.size} 件)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Button(
                onClick = { viewModel.openWelcomeScreen() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = "ホーム", modifier = Modifier.padding(end = 4.dp))
                Text("機種選択に戻る", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (viewModel.historyList.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "履歴がありません。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "カウンター画面からデータを保存すると、ここに記録されます。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 8.dp)
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
                items(viewModel.historyList, key = { it.id }) { item ->
                    HistoryItemCard(
                        item = item,
                        onDelete = { viewModel.deleteHistory(item.id) },
                        onClick = { selectedItemForDetail = item },
                        onPhotoAttached = { uri ->
                            // 画像添付の更新
                            val updatedItem = item.copy(imageUri = uri)
                            viewModel.deleteHistory(item.id) // 既存削除
                            viewModel.saveCurrentGame() // 新規として保存 (内部でリスト再読込)
                            // 実際はViewModel側に保存処理を中継して更新する
                            selectedItemForDetail = null
                        }
                    )
                }
            }
        }
    }

    // 履歴詳細ダイアログ (設定推移グラフ ＆ 添付画像表示)
    if (selectedItemForDetail != null) {
        HistoryDetailDialog(
            item = selectedItemForDetail!!,
            onDismiss = { selectedItemForDetail = null }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onPhotoAttached: (String) -> Unit
) {
    val bitmap = rememberUriImage(item.imageUri)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onPhotoAttached(it.toString()) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ヘッダー (日付、店舗、削除)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = item.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        if (item.storeName.isNotEmpty()) {
                            Text(
                                text = "@ ${item.storeName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(text = item.specName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "削除",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 添付写真のサムネイル表示 (存在する場合)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "スランプグラフ写真",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }

                // 実戦データ
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "総ゲーム数", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = "${item.priorGames + item.myGames} G", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(text = "累計 BB/RB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = "${item.priorBb + item.myBb} / ${item.priorRb + item.myRb}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "推測設定", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text(text = "設定 ${item.estimatedSetting}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.background)
            Spacer(modifier = Modifier.height(10.dp))

            // 収支データ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text(text = "投資", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(text = formatYen(item.investmentYen.toDouble()), fontSize = 12.sp)
                    }
                    Column {
                        Text(text = "回収", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(text = "${item.recoveryCoins} 枚", fontSize = 12.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "収支", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(
                        text = formatYen(item.profitYen, signed = true),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.profitYen >= 0) NeonGreen else MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// 履歴詳細ダイアログ (設定推移グラフ ＆ 画像)
@Composable
private fun HistoryDetailDialog(
    item: HistoryItem,
    onDismiss: () -> Unit
) {
    val bitmap = rememberUriImage(item.imageUri)
    
    // カンマ区切りの時系列履歴をパース
    val gamesList = remember(item.gamesHistory) {
        if (item.gamesHistory.isEmpty()) emptyList() 
        else item.gamesHistory.split(",").mapNotNull { it.toIntOrNull() }
    }
    val probList = remember(item.setting6History) {
        if (item.setting6History.isEmpty()) emptyList() 
        else item.setting6History.split(",").mapNotNull { it.toDoubleOrNull() }
    }

    AlertDialog(
        onDismissRequest = dismissEvent@{ onDismiss() },
        confirmButton = {
            Button(onClick = { onDismiss() }) {
                Text("閉じる")
            }
        },
        title = {
            Column {
                Text(text = item.specName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "${item.date} 実戦詳細", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // ホール情報
                if (item.storeName.isNotEmpty() || item.eventName.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                if (item.storeName.isNotEmpty()) {
                                    Text("実戦店舗: ${item.storeName}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                if (item.eventName.isNotEmpty()) {
                                    Text("イベント名/特定日: ${item.eventName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // 推移グラフ
                if (gamesList.size >= 2) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("設定6期待度の推移", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                TimelineChart(
                                    gamesList = gamesList,
                                    prob6List = probList,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                                        .padding(6.dp)
                                )
                            }
                        }
                    }
                }

                // 添付画像
                if (bitmap != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("添付写真 (スランプグラフ等)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "添付画像",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                )
                            }
                        }
                    }
                }

                // データ一覧
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("実戦データ内訳", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("前任者G数:", fontSize = 12.sp)
                            Text("${item.priorGames} G (BB ${item.priorBb} / RB ${item.priorRb})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("自分実戦G数:", fontSize = 12.sp)
                            Text("${item.myGames} G (BB ${item.myBb} / RB ${item.myRb})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("自分カウントブドウ:", fontSize = 12.sp)
                            Text("${item.myGrape} 回", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("自分カウントチェリー:", fontSize = 12.sp)
                            Text("${item.myCherry} 回", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    )
}

// 標準のBitmapデコーダヘルパー (依存関係を追加せず安全にUriからBitmapをロード)
@Composable
fun rememberUriImage(uriString: String?): ImageBitmap? {
    if (uriString == null) return null
    val context = LocalContext.current
    return remember(uriString) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
