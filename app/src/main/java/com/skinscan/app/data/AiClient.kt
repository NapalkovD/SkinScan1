package com.skinscan.app.data

import android.graphics.Bitmap
import android.util.Base64
import com.skinscan.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Клиент к любому API, совместимому с форматом /chat/completions
 * (Qwen-VL / DashScope, OpenRouter, и т.д.). Настройки берутся из local.properties.
 */
object AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """Ты — ассистент по предварительной визуальной оценке состояния кожи по фотографии (дерматологический помощник). Ты НЕ ставишь окончательный диагноз и не заменяешь врача.
Внимательно изучи фото: цвет, форму, границы, размер, поверхность, наличие отёка, покраснения, гноя, корок, кровотечения.
Если на фото нет кожи/раны/дефекта, объясни это в summary и верни пустые списки.
Отвечай СТРОГО валидным JSON на русском языке, без markdown и без текста вне JSON, по схеме:
{
 "summary": "краткое описание того, что видно на фото (2-4 предложения)",
 "severity": "low | medium | high",
 "conditions": [ {"name": "возможное состояние", "likelihood": "высокая | средняя | низкая", "description": "почему так считаешь, 1-2 предложения"} ],
 "recommendations": ["практический совет по уходу или дальнейшим действиям"],
 "see_doctor": true или false,
 "urgent_signs": ["признаки, при которых нужно срочно обратиться к врачу"]
}
Давай от 1 до 4 возможных состояний, отсортируй по вероятности. Будь осторожен: при подозрении на меланому, глубокую рану, признаки инфекции, ожог, быстрое изменение образования — severity high и see_doctor true."""

    fun toBase64(bmp: Bitmap): String {
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyze(bmp: Bitmap, note: String): Analysis = withContext(Dispatchers.IO) {
        if (BuildConfig.AI_API_KEY.isBlank()) {
            throw IOException("Не задан API-ключ нейросети. Укажите AI_API_KEY в local.properties и пересоберите приложение.")
        }
        val dataUrl = "data:image/jpeg;base64," + toBase64(bmp)
        val userText = buildString {
            append("Проанализируй фото.")
            if (note.isNotBlank()) append(" Жалобы и симптомы пользователя: ").append(note.trim())
        }
        val userContent = JSONArray()
            .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", dataUrl)))
            .put(JSONObject().put("type", "text").put("text", userText))

        val body = JSONObject()
            .put("model", BuildConfig.AI_MODEL)
            .put("temperature", 0.2)
            .put(
                "messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                    .put(JSONObject().put("role", "user").put("content", userContent))
            )

        val url = BuildConfig.AI_BASE_URL.trimEnd('/') + "/chat/completions"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${BuildConfig.AI_API_KEY}")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val hint = when (resp.code) {
                        401, 403 -> "Неверный или неактивный API-ключ."
                        429 -> "Превышен лимит запросов, попробуйте позже."
                        else -> txt.take(200)
                    }
                    throw IOException("Ошибка сервера ${resp.code}. $hint")
                }
                val content = JSONObject(txt)
                    .getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content")
                Analysis.parse(content)
            }
        } catch (e: java.net.UnknownHostException) {
            throw IOException("Нет подключения к интернету или сервис недоступен.")
        } catch (e: java.net.SocketTimeoutException) {
            throw IOException("Сервер слишком долго отвечает. Попробуйте ещё раз.")
        }
    }
}
