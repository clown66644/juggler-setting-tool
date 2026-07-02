package com.example.jugglersettingtool.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.theme.NeonYellow
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.formatHitRatio

@Composable
fun AiScreen(viewModel: JugglerViewModel) {

    ScreenColumn(title = "AI 設定推測 & アドバイス") {
        SectionCard(title = "推測対象データ") {
            LabelValueRow(label = "機種:", value = viewModel.selectedSpec.name)
            LabelValueRow(label = "ゲーム数:", value = "${viewModel.totalGames} G")
            LabelValueRow(label = "BB/RB:", value = "${viewModel.totalBb} / ${viewModel.totalRb}")
            LabelValueRow(
                label = "ブドウ確率:",
                value = formatHitRatio(viewModel.totalGames, viewModel.totalGrape, digits = 2, emptyText = "未入力")
            )
        }

        SectionCard(title = "ホールの状況メモ (自由入力)") {
            OutlinedTextField(
                value = viewModel.hallNotes,
                onValueChange = viewModel::updateHallNotes,
                placeholder = {
                    Text("例: 本日は月一の特定日。角から3台目に高設定が入る傾向あり。")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4
            )

            Button(
                onClick = { viewModel.requestAiAdvice() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isAiLoading && viewModel.totalGames > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (viewModel.isAiLoading) {
                    SmallLoadingIndicator()
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("分析中...")
                } else {
                    Text("AI詳細設定推測を実行", fontWeight = FontWeight.Bold)
                }
            }

            if (viewModel.totalGames <= 0) {
                Text(
                    text = "※AI推測を実行するには総ゲーム数を入力してください。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        if (viewModel.aiAdvice.isNotEmpty()) {
            SectionCard(title = "AI分析結果 & アドバイス") {
                HorizontalDivider(color = MaterialTheme.colorScheme.background)
                parseMarkdown(viewModel.aiAdvice).forEach { element ->
                    MarkdownText(element)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SmallLoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = Color.White,
        strokeWidth = 2.dp
    )
}

@Composable
private fun MarkdownText(element: MarkdownElement) {
    when (element) {
        is MarkdownElement.Header -> Text(
            text = element.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NeonYellow,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        is MarkdownElement.BulletPoint -> Row(
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = "• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            Text(text = element.text, fontSize = 14.sp)
        }

        is MarkdownElement.NormalText -> Text(
            text = element.text,
            fontSize = 14.sp,
            modifier = Modifier.padding(vertical = 2.dp),
            lineHeight = 20.sp
        )
    }
}

private sealed class MarkdownElement {
    data class Header(val text: String) : MarkdownElement()
    data class BulletPoint(val text: String) : MarkdownElement()
    data class NormalText(val text: String) : MarkdownElement()
}

private fun parseMarkdown(text: String): List<MarkdownElement> =
    text.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { line ->
            when {
                line.startsWith("###") -> MarkdownElement.Header(line.removePrefix("###").trim())
                line.startsWith("##") -> MarkdownElement.Header(line.removePrefix("##").trim())
                line.startsWith("#") -> MarkdownElement.Header(line.removePrefix("#").trim())
                line.startsWith("-") -> MarkdownElement.BulletPoint(line.removePrefix("-").trim())
                line.startsWith("*") -> MarkdownElement.BulletPoint(line.removePrefix("*").trim())
                else -> MarkdownElement.NormalText(line.replace("**", ""))
            }
        }
        .toList()
