package com.example.jugglersettingtool.network

import android.content.Context
import com.example.jugglersettingtool.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object OpenAiService {

    private const val SETTINGS_NAME = "app_settings"
    private const val API_KEY_PREF = "openai_api_key"
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    private const val MODEL_NAME = "gpt-5.5"

    private val json = Json { ignoreUnknownKeys = true }
    
    // GPT-5.5 等の高度なモデルは推論に時間がかかるため、タイムアウトを120秒（2分）に延長
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(API_KEY_PREF, "").orEmpty()
    }

    fun saveApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val trimmedKey = key.trim()
        if (trimmedKey.isBlank()) {
            editor.remove(API_KEY_PREF)
        } else {
            editor.putString(API_KEY_PREF, trimmedKey)
        }
        editor.apply()
    }

    @Serializable
    data class Message(val role: String, val content: String)

    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double = 0.5
    )

    @Serializable
    data class Choice(val message: Message)

    @Serializable
    data class ChatResponse(val choices: List<Choice>)

    suspend fun getSettingAdvice(
        context: Context,
        modelName: String,
        games: Int,
        bb: Int,
        rb: Int,
        grapeProbText: String,
        estimationText: String,
        investmentYen: Int,
        recoveryCoins: Int,
        expectedValueText: String,
        hallNotes: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context).trim()
        if (apiKey.isBlank()) {
            return@withContext "OpenAI APIキーが未設定です。設定画面でAPIキーを入力してください。"
        }

        val prompt = """
            【パチスロ設定推測シミュレーション】
            機種名: $modelName
            総ゲーム数: $games G
            BIG回数: $bb 回
            REG回数: $rb 回
            ブドウ確率: $grapeProbText
            
            【収支データ】
            投資金額: $investmentYen 円
            回収枚数: $recoveryCoins 枚
            機械割ベースの期待収支: $expectedValueText
            
            【計算上の設定期待度（ベイズ推定）】
            $estimationText
            
            【ホールの状況メモ】
            $hallNotes
            
            このデータを元に、プロの視点から今後の「押し引き（続行か、やめるか）」の判断や、この台に対する設定推測、具体的な立ち回りのアドバイスを日本語で回答してください。
        """.trimIndent()

        val requestBodyObj = ChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                Message(
                    role = "system",
                    content = "あなたはパチスロ「ジャグラー」シリーズ of プロであり、データ分析の専門家です。与えられた数値データを基に、現在の設定推測と今後の立ち回りアドバイスについて、具体的で論理的な分析を日本語で返答してください。Markdown形式で綺麗に構成して出力してください。"
                ),
                Message(role = "user", content = prompt)
            )
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBodyJson = json.encodeToString(ChatRequest.serializer(), requestBodyObj)
        val body = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val (reason, solution) = analyzeError(response.code, bodyStr)
                    
                    val fullLogMessage = """
                        HTTPステータス: ${response.code}
                        原因: $reason
                        解決策: $solution
                        詳細レスポンス: $bodyStr
                    """.trimIndent()
                    
                    AppLogger.log(context, "API_CONNECTION_ERROR", fullLogMessage)
                    
                    return@withContext """
                        ❌ AI分析エラー (HTTP: ${response.code})
                        
                        【エラー原因】
                        $reason
                        
                        【解決策・アクション】
                        $solution
                    """.trimIndent()
                }

                val responseBodyStr = response.body?.string()
                    ?: throw IOException("Empty response body")

                val chatResponse = json.decodeFromString(ChatResponse.serializer(), responseBodyStr)
                return@withContext chatResponse.choices.firstOrNull()?.message?.content
                    ?: "AIからの回答が取得できませんでした。"
            }
        } catch (e: java.net.SocketTimeoutException) {
            val timeoutMsg = """
                タイムアウトエラー: OpenAI APIからの応答が制限時間内に返ってきませんでした。
                解決策: 
                1. 現在、モデル '$MODEL_NAME' は複雑な推論を行うため、回答生成に数十秒〜1分以上かかる場合があります。タイムアウト設定を120秒（2分）に延長しましたので、電波状況の良い環境でもう一度お試しください。
                2. OpenAI APIのサーバー自体が一時的に過負荷状態になっている可能性があります。しばらく時間をおいてから再度実行してください。
            """.trimIndent()
            AppLogger.log(context, "API_TIMEOUT_ERROR", timeoutMsg)
            return@withContext "通信タイムアウトが発生しました。\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        } catch (e: java.net.UnknownHostException) {
            val dnsErrorMsg = """
                接続エラー: OpenAIのサーバー名（api.openai.com）を解決できませんでした。
                解決策: 
                1. 端末が機内モードになっていないか、Wi-Fiやモバイルデータ通信が正常にインターネットに繋がっているか確認してください。
                2. エミュレータをご使用の場合は、PC本体のインターネット接続や、エミュレータのネットワーク設定（DNSなど）を再起動してお試しください。
            """.trimIndent()
            AppLogger.log(context, "API_DNS_ERROR", dnsErrorMsg)
            return@withContext "サーバーに接続できません (オフラインの可能性があります)。\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        } catch (e: Exception) {
            val networkErrorMsg = """
                接続エラー: ${e.message}
                解決策: 端末のインターネット接続状況をご確認ください。
                詳細スタックトレース: ${e.stackTraceToString()}
            """.trimIndent()
            AppLogger.log(context, "NETWORK_ERROR", networkErrorMsg)
            return@withContext "通信エラーが発生しました: ${e.message}\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        }
    }

    suspend fun getSimulatorAdvice(
        context: Context,
        dataSummary: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey(context).trim()
        if (apiKey.isBlank()) {
            return@withContext "OpenAI APIキーが未設定です。設定画面でAPIキーを入力してください。"
        }

        val prompt = """
            【パチスロ設定判別シミュレーション結果判定】
            $dataSummary
            
            この出来上がった挙動データを見て、プロの設定推測の視点から「設定いくつである可能性が高いか」、またその理由や判別ポイント（REG確率、ブドウ逆算値、差枚数など）をふまえ、論理的な考察レポートを日本語で作成してください。
        """.trimIndent()

        val requestBodyObj = ChatRequest(
            model = MODEL_NAME,
            messages = listOf(
                Message(
                    role = "system",
                    content = "あなたはパチスロ「ジャグラー」シリーズのプロであり、データ分析の専門家です。与えられたシミュレート結果のデータを元に、どの設定である可能性が高いかを論理的に推測し、設定判別のコツを含めてMarkdown形式で丁寧に出力してください。"
                ),
                Message(role = "user", content = prompt)
            )
        )

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBodyJson = json.encodeToString(ChatRequest.serializer(), requestBodyObj)
        val body = requestBodyJson.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    val (reason, solution) = analyzeError(response.code, bodyStr)
                    
                    val fullLogMessage = """
                        HTTPステータス: ${response.code}
                        原因: $reason
                        解決策: $solution
                        詳細レスポンス: $bodyStr
                    """.trimIndent()
                    
                    AppLogger.log(context, "API_CONNECTION_ERROR", fullLogMessage)
                    
                    return@withContext """
                        ❌ AI分析エラー (HTTP: ${response.code})
                        
                        【エラー原因】
                        $reason
                        
                        【解決策・アクション】
                        $solution
                    """.trimIndent()
                }

                val responseBodyStr = response.body?.string()
                    ?: throw IOException("Empty response body")

                val chatResponse = json.decodeFromString(ChatResponse.serializer(), responseBodyStr)
                return@withContext chatResponse.choices.firstOrNull()?.message?.content
                    ?: "AIからの回答が取得できませんでした。"
            }
        } catch (e: java.net.SocketTimeoutException) {
            val timeoutMsg = """
                タイムアウトエラー: OpenAI APIからの応答が制限時間内に返ってきませんでした。
                解決策: 
                1. 現在、モデル '$MODEL_NAME' は複雑な推論を行うため、回答生成に数十秒〜1分以上かかる場合があります。タイムアウト設定を120秒（2分）に延長しましたので、電波状況の良い環境でもう一度お試しください。
                2. OpenAI APIのサーバー自体が一時的に過負荷状態になっている可能性があります。しばらく時間をおいてから再度実行してください。
            """.trimIndent()
            AppLogger.log(context, "API_TIMEOUT_ERROR", timeoutMsg)
            return@withContext "通信タイムアウトが発生しました。\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        } catch (e: java.net.UnknownHostException) {
            val dnsErrorMsg = """
                接続エラー: OpenAIのサーバー名（api.openai.com）を解決できませんでした。
                解決策: 
                1. 端末が機内モードになっていないか、Wi-Fiやモバイルデータ通信が正常にインターネットに繋がっているか確認してください。
                2. エミュレータをご使用の場合は、PC本体のインターネット接続や、エミュレータのネットワーク設定（DNSなど）を再起動してお試しください。
            """.trimIndent()
            AppLogger.log(context, "API_DNS_ERROR", dnsErrorMsg)
            return@withContext "サーバーに接続できません (オフラインの可能性があります)。\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        } catch (e: Exception) {
            val networkErrorMsg = """
                接続エラー: ${e.message}
                解決策: 端末のインターネット接続状況をご確認ください。
                詳細スタックトレース: ${e.stackTraceToString()}
            """.trimIndent()
            AppLogger.log(context, "NETWORK_ERROR", networkErrorMsg)
            return@withContext "通信エラーが発生しました: ${e.message}\n詳細な原因と解決策は、設定画面のエラーログをご確認ください。"
        }
    }

    suspend fun validateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return@withContext false

        val request = Request.Builder()
            .url("https://api.openai.com/v1/models")
            .header("Authorization", "Bearer $trimmed")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.code == 200
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // エラーレスポンス分析 ＆ 解決策決定ヘルパー
    private fun analyzeError(code: Int, responseBody: String): Pair<String, String> {
        val bodyLower = responseBody.lowercase()
        return when {
            code == 404 || bodyLower.contains("model_not_found") || bodyLower.contains("does not exist") -> {
                Pair(
                    "指定されたAIモデル名（$MODEL_NAME）がOpenAI APIサーバー上に存在しないか、サポートされていません。",
                    "【開発者向け解決策】: 'OpenAiService.kt' ファイル内の定数 'MODEL_NAME' に指定された値（'$MODEL_NAME'）が間違っています。現在OpenAIで有効な正しいモデル名（例: 'gpt-4o' または 'gpt-4o-mini' など）に変更して、アプリを再ビルドしてください。"
                )
            }
            code == 401 || bodyLower.contains("invalid_api_key") -> {
                Pair(
                    "入力されたOpenAI APIキーが無効であるか、承認されませんでした。",
                    "【解決策】: アプリの『設定』画面を開き、正しいOpenAI APIキー（'sk-proj-...' で始まるキー）を入力・保存して、接続テストが合格するか確認してください。"
                )
            }
            code == 429 || bodyLower.contains("insufficient_quota") || bodyLower.contains("credit_limit") -> {
                Pair(
                    "APIの使用上限（クレジット残高不足）、またはレートリミットの上限に達しています。",
                    "【解決策】: OpenAIのAPI管理ポータル（https://platform.openai.com）にログインし、Credit Balance（チャージ残高）が残っているか、あるいは利用制限がかかっていないかをご確認ください。"
                )
            }
            code == 400 && bodyLower.contains("context_length_exceeded") -> {
                Pair(
                    "送信データの総量が多すぎるため、AIモデルの最大文字数制限を超過しました。",
                    "【解決策】: ホール状況などのカスタムメモの入力文字数を少し減らし、実戦履歴の行数を整理してから再度AI分析をお試しください。"
                )
            }
            else -> {
                Pair(
                    "OpenAI APIサーバーからエラーが返されました。HTTPコード: $code, レスポンス: $responseBody",
                    "【解決策】: ネットワーク接続が安定しているか確認し、しばらく時間を置いてから再度お試しください。"
                )
            }
        }
    }
}
