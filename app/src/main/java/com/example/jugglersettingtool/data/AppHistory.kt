package com.example.jugglersettingtool.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // YYYY-MM-DD
    val specName: String,
    val priorGames: Int,
    val priorBb: Int,
    val priorRb: Int,
    val priorDiff: Int,
    val myGames: Int,
    val myBb: Int,
    val myRb: Int,
    val myGrape: Int,
    val myCherry: Int,
    val investmentYen: Int,
    val recoveryCoins: Int,
    val profitYen: Double,
    val estimatedSetting: Int,
    // 追加フィールド
    val storeName: String = "",
    val eventName: String = "",
    val imageUri: String? = null,
    val gamesHistory: String = "", // カンマ区切りゲーム数 (例: "0,500,1000,1500")
    val setting6History: String = "" // カンマ区切り設定6確率 (例: "16.6,20.5,35.2,40.1")
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("date", date)
            put("specName", specName)
            put("priorGames", priorGames)
            put("priorBb", priorBb)
            put("priorRb", priorRb)
            put("priorDiff", priorDiff)
            put("myGames", myGames)
            put("myBb", myBb)
            put("myRb", myRb)
            put("myGrape", myGrape)
            put("myCherry", myCherry)
            put("investmentYen", investmentYen)
            put("recoveryCoins", recoveryCoins)
            put("profitYen", profitYen)
            put("estimatedSetting", estimatedSetting)
            put("storeName", storeName)
            put("eventName", eventName)
            put("imageUri", imageUri ?: "")
            put("gamesHistory", gamesHistory)
            put("setting6History", setting6History)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): HistoryItem {
            return HistoryItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                date = json.getString("date"),
                specName = json.getString("specName"),
                priorGames = json.optInt("priorGames", 0),
                priorBb = json.optInt("priorBb", 0),
                priorRb = json.optInt("priorRb", 0),
                priorDiff = json.optInt("priorDiff", 0),
                myGames = json.optInt("myGames", 0),
                myBb = json.optInt("myBb", 0),
                myRb = json.optInt("myRb", 0),
                myGrape = json.optInt("myGrape", 0),
                myCherry = json.optInt("myCherry", 0),
                investmentYen = json.optInt("investmentYen", 0),
                recoveryCoins = json.optInt("recoveryCoins", 0),
                profitYen = json.optDouble("profitYen", 0.0),
                estimatedSetting = json.optInt("estimatedSetting", 1),
                storeName = json.optString("storeName", ""),
                eventName = json.optString("eventName", ""),
                imageUri = json.optString("imageUri", "").let { if (it.isEmpty()) null else it },
                gamesHistory = json.optString("gamesHistory", ""),
                setting6History = json.optString("setting6History", "")
            )
        }
    }
}

class HistoryRepository(private val context: Context) {
    private val fileName = "juggler_history.json"
    private val file: File get() = File(context.filesDir, fileName)

    fun saveHistoryItem(item: HistoryItem) {
        val list = loadAllHistory().toMutableList()
        val index = list.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            list[index] = item
        } else {
            list.add(0, item)
        }
        saveAllHistory(list)
    }

    fun deleteHistoryItem(id: String) {
        val list = loadAllHistory().filter { it.id != id }
        saveAllHistory(list)
    }

    fun loadAllHistory(): List<HistoryItem> {
        if (!file.exists()) return emptyList()
        try {
            val jsonStr = file.readText()
            if (jsonStr.isBlank()) return emptyList()
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until jsonArray.length()) {
                list.add(HistoryItem.fromJsonObject(jsonArray.getJSONObject(i)))
            }
            return list
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getDailyProfitMap(): Map<String, Double> {
        val all = loadAllHistory()
        val map = mutableMapOf<String, Double>()
        for (item in all) {
            val current = map[item.date] ?: 0.0
            map[item.date] = current + item.profitYen
        }
        return map
    }

    private fun saveAllHistory(list: List<HistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                jsonArray.put(item.toJsonObject())
            }
            file.writeText(jsonArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
