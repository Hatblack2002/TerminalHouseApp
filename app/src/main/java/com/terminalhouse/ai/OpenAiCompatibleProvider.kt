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
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Proveedor para el formato OpenAI (Sección 4, formato 1).
 *
 * Cubre: OpenAI, Mistral, DeepSeek, Kimi y cualquier endpoint
 * personalizado compatible con chat completions.
 *
 *  - Authorization: Bearer {api_key}
 *  - Envía: modelo, mensajes, tokens máximos, temperatura.
 *  - Recibe: texto en choices[0].message.content.
 */
class OpenAiCompatibleProvider(
    override val providerType: AiProviderType,
    private val endpoint: String,
    private val apiKey: String,
    private val model: String,
    private val client: OkHttpClient = defaultClient()
) : AiProvider {

    override suspend fun complete(
        messages: List<AiMessage>,
        maxTokens: Int,
        temperature: Double
    ): AiResponse = withContext(Dispatchers.IO) {
        val url = endpoint.trimEnd('/') + CHAT_COMPLETIONS_PATH

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            put(
                "messages",
                JSONArray().apply {
                    for (message in messages) {
                        put(
                            JSONObject().apply {
                                put("role", mapRole(message.role))
                                put("content", message.content)
                            }
                        )
                    }
                }
            )
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        executeAndParse(request)
    }

    /** Lanza la petición y traduce la respuesta o el error a tipos comunes. */
    private fun executeAndParse(request: Request): AiResponse {
        val response = try {
            client.newCall(request).execute()
        } catch (e: SocketTimeoutException) {
            throw AiException(MESSAGES[ERR_TIMEOUT] ?: "Timeout", httpCode = null, cause = e)
        } catch (e: UnknownHostException) {
            throw AiException(MESSAGES[ERR_NO_NETWORK] ?: "Sin conexión", cause = e)
        } catch (e: IOException) {
            throw AiException(MESSAGES[ERR_NO_NETWORK] ?: "Sin conexión", cause = e)
        }

        return response.use { resp ->
            val rawBody = try {
                resp.body?.string().orEmpty()
            } catch (e: IOException) {
                throw AiException(MESSAGES[ERR_NO_NETWORK] ?: "Sin conexión", cause = e)
            }

            if (!resp.isSuccessful) {
                throw aiError(resp.code, extractProviderError(rawBody))
            }

            try {
                val json = JSONObject(rawBody)
                val choices = json.optJSONArray("choices")
                    ?: throw AiException(MESSAGES[ERR_UNEXPECTED] ?: "Respuesta inesperada")
                val first = choices.optJSONObject(0)
                    ?: throw AiException(MESSAGES[ERR_UNEXPECTED] ?: "Respuesta inesperada")
                val content = first.optJSONObject("message")?.optString("content").orEmpty()

                val usage = json.optJSONObject("usage")
                AiResponse(
                    text = content,
                    inputTokens = usage?.optInt("prompt_tokens")?.takeIf { it > 0 },
                    outputTokens = usage?.optInt("completion_tokens")?.takeIf { it > 0 }
                )
            } catch (e: AiException) {
                throw e
            } catch (e: Exception) {
                throw AiException(MESSAGES[ERR_UNEXPECTED] ?: "Respuesta inesperada", cause = e)
            }
        }
    }

    /** Extrae {"error":{"message": "..."}} o {"message": "..."} del cuerpo si existe. */
    private fun extractProviderError(rawBody: String): String? {
        return try {
            val json = JSONObject(rawBody)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("message")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val CHAT_COMPLETIONS_PATH = "/chat/completions"

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Cliente único con timeout de 30 s (Sección 9). */
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        /** Roles del formato OpenAI: system / user / assistant. */
        fun mapRole(role: AiRole): String = when (role) {
            AiRole.SYSTEM -> "system"
            AiRole.USER -> "user"
            AiRole.ASSISTANT -> "assistant"
        }

        // Mensajes de error comprensibles (Sección 9).
        const val ERR_INVALID_KEY = "invalid_key"
        const val ERR_RATE_LIMIT = "rate_limit"
        const val ERR_SERVER = "server"
        const val ERR_TIMEOUT = "timeout"
        const val ERR_NO_NETWORK = "no_network"
        const val ERR_UNEXPECTED = "unexpected"

        val MESSAGES: Map<String, String> = mapOf(
            ERR_INVALID_KEY to "La API key es inválida o no tiene permisos para este modelo.",
            ERR_RATE_LIMIT to "Límite de peticiones alcanzado. Espera un momento e inténtalo de nuevo.",
            ERR_SERVER to "El servidor del proveedor tiene problemas. Inténtalo de nuevo en unos minutos.",
            ERR_TIMEOUT to "El proveedor tardó demasiado en responder (30 s). Inténtalo de nuevo.",
            ERR_NO_NETWORK to "Sin conexión a internet. Revisa tu red e inténtalo de nuevo.",
            ERR_UNEXPECTED to "El proveedor devolvió una respuesta inesperada."
        )

        /** Traduce un código HTTP + mensaje del proveedor a [AiException] comprensible. */
        fun aiError(httpCode: Int, providerMessage: String?): AiException {
            val key = when (httpCode) {
                401, 403 -> ERR_INVALID_KEY
                429 -> ERR_RATE_LIMIT
                in 500..599 -> ERR_SERVER
                else -> ERR_UNEXPECTED
            }
            val base = MESSAGES[key] ?: MESSAGES[ERR_UNEXPECTED]!!
            val detail = providerMessage?.takeIf { it.isNotBlank() }
            return AiException(
                message = if (detail != null) "$base ($detail)" else base,
                httpCode = httpCode
            )
        }
    }
}
