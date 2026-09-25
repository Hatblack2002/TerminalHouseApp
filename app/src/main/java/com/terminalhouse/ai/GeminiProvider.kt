package com.terminalhouse.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.net.UnknownHostException

/**
 * Proveedor para el formato Google Gemini (Sección 4, formato 3). Cubre: Gemini.
 *
 *  - La API key va en la URL como query param.
 *  - El system prompt va en systemInstruction.
 *  - Los roles son "user" y "model" (no "assistant").
 *  - Recibe: texto en candidates[0].content.parts[0].text.
 */
class GeminiProvider(
    override val providerType: AiProviderType,
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
    private val client: OkHttpClient = OpenAiCompatibleProvider.defaultClient()
) : AiProvider {

    override suspend fun complete(
        messages: List<AiMessage>,
        maxTokens: Int,
        temperature: Double
    ): AiResponse = withContext(Dispatchers.IO) {
        val systemPrompt = messages
            .filter { it.role == AiRole.SYSTEM }
            .joinToString("\n\n") { it.content }

        val contents = JSONArray().apply {
            for (message in messages.filter { it.role != AiRole.SYSTEM }) {
                put(
                    JSONObject().apply {
                        put("role", mapRole(message.role))
                        put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", message.content))
                        )
                    }
                )
            }
        }

        val body = JSONObject().apply {
            if (systemPrompt.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                )
            }
            put("contents", contents)
            put(
                "generationConfig",
                JSONObject().apply {
                    put("maxOutputTokens", maxTokens)
                    put("temperature", temperature)
                }
            )
        }

        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val url = endpoint.trimEnd('/') +
            "/v1beta/models/" + model.trim() +
            ":generateContent?key=" + encodedKey

        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        executeAndParse(request)
    }

    private fun executeAndParse(request: Request): AiResponse {
        val response = try {
            client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw AiException(
                OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_TIMEOUT]!!,
                cause = e
            )
        } catch (e: UnknownHostException) {
            throw AiException(
                OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_NO_NETWORK]!!,
                cause = e
            )
        } catch (e: IOException) {
            throw AiException(
                OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_NO_NETWORK]!!,
                cause = e
            )
        }

        return response.use { resp ->
            val rawBody = try {
                resp.body?.string().orEmpty()
            } catch (e: IOException) {
                throw AiException(
                    OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_NO_NETWORK]!!,
                    cause = e
                )
            }

            if (!resp.isSuccessful) {
                val providerMessage = try {
                    JSONObject(rawBody)
                        .optJSONObject("error")?.optString("message")
                } catch (_: Exception) {
                    null
                }
                throw OpenAiCompatibleProvider.aiError(resp.code, providerMessage)
            }

            try {
                val json = JSONObject(rawBody)
                val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                    ?: throw AiException(
                        OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_UNEXPECTED]!!
                    )
                val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
                    ?: throw AiException(
                        OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_UNEXPECTED]!!
                    )
                val text = buildString {
                    for (i in 0 until parts.length()) {
                        val part = parts.optJSONObject(i) ?: continue
                        append(part.optString("text"))
                    }
                }

                val usage = json.optJSONObject("usageMetadata")
                AiResponse(
                    text = text,
                    inputTokens = usage?.optInt("promptTokenCount")?.takeIf { it > 0 },
                    outputTokens = usage?.optInt("candidatesTokenCount")?.takeIf { it > 0 }
                )
            } catch (e: AiException) {
                throw e
            } catch (e: Exception) {
                throw AiException(
                    OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_UNEXPECTED]!!,
                    cause = e
                )
            }
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Roles del formato Gemini: user / model. */
        fun mapRole(role: AiRole): String = when (role) {
            AiRole.SYSTEM -> "user" // nunca se usa: SYSTEM va en systemInstruction
            AiRole.USER -> "user"
            AiRole.ASSISTANT -> "model"
        }
    }
}
