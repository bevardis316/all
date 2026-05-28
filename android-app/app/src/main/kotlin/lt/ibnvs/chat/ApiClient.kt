package lt.ibnvs.chat

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    private const val API_URL = "https://manonuotekos.lt/api/v1/ai"

    sealed class Result {
        data class Success(val text: String) : Result()
        data class Error(val message: String) : Result()
    }

    fun ask(message: String, history: List<Map<String, String>>): Result {
        return try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }

            val histArray = JSONArray().apply {
                history.forEach { h ->
                    put(JSONObject().apply {
                        put("role", h["role"])
                        put("content", h["content"])
                    })
                }
            }

            val body = JSONObject().apply {
                put("message", message)
                put("history", histArray)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val raw = (if (code < 400) conn.inputStream else conn.errorStream)
                .bufferedReader(Charsets.UTF_8)
                .readText()

            val json = JSONObject(raw)

            when {
                code == 429 -> Result.Error(json.optString("error", "Per daug užklausų. Palaukite."))
                json.optBoolean("ok") && json.has("text") -> Result.Success(json.getString("text"))
                else -> Result.Error(json.optString("error", "Netikėta klaida."))
            }
        } catch (e: Exception) {
            Result.Error("Ryšio klaida. Patikrinkite interneto ryšį.")
        }
    }
}
