package com.example.jugglersettingtool.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jugglersettingtool.Screen
import com.example.jugglersettingtool.network.OpenAiService
import com.example.jugglersettingtool.theme.NeonGreen
import com.example.jugglersettingtool.ui.ApiKeyStatus
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.utils.AppLogger

@Composable
fun SettingsScreen(viewModel: JugglerViewModel) {
    val context = LocalContext.current
    var apiKeyText by remember(context) { mutableStateOf(OpenAiService.getApiKey(context)) }
    var logsText by remember(context) { mutableStateOf(AppLogger.readLogs(context)) }

    ScreenColumn(title = "設定 & ログ管理") {
        SectionCard(title = "OpenAI API キー設定") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = {
                        apiKeyText = it
                        OpenAiService.saveApiKey(context, it)
                        viewModel.testApiKeyConnection(it)
                    },
                    label = { Text("API キー") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { viewModel.testApiKeyConnection(apiKeyText) },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("接続テスト", fontSize = 11.sp)
                }
            }

            // 承認ステータス表示
            val statusColor = when (viewModel.apiKeyStatus) {
                ApiKeyStatus.Valid -> NeonGreen
                ApiKeyStatus.Invalid -> MaterialTheme.colorScheme.secondary
                ApiKeyStatus.Checking -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            val statusText = when (viewModel.apiKeyStatus) {
                ApiKeyStatus.Valid -> "✓ 承認されました (APIキーは有効です)"
                ApiKeyStatus.Invalid -> "✗ 承認エラー (キーが無効か、通信エラー)"
                ApiKeyStatus.Checking -> "接続テスト中..."
                else -> "未検証 (APIキーを入力するか接続テストを押してください)"
            }

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "※APIキーは端末内に保存され、OpenAI APIとの通信時だけ利用されます。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        SectionCard(title = "アプリ内エラーログ") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = logsText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { logsText = AppLogger.readLogs(context) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ログを更新")
                }
                Button(
                    onClick = {
                        AppLogger.clearLogs(context)
                        logsText = AppLogger.readLogs(context)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("ログを消去")
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
