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

/**
 * Proveedor para el formato Anthropic (Sección 4, formato 2). Cubre: Claude.
 *
 *  - Header: x-api-key: {api_key}
 *  - Header adicional: anthropic-version: 2023-06-01
 *  - El system prompt va en un campo separado, no como mensaje.
 *  - Recibe: texto en content[0].text.
 */
class AnthropicProvider(
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
        val url = endpoint.trimEnd('/') + MESSAGES_PATH

        // Claude exige alternancia user/assistant y el system separado.
        val systemPrompt = messages
            .filter { it.role == AiRole.SYSTEM }
            .joinToString("\n\n") { it.content }

        val conversation = JSONArray().apply {
            for (message in messages.filter { it.role != AiRole.SYSTEM }) {
                put(
                    JSONObject().apply {
                        put("role", mapRole(message.role))
                        put(
                            "content",
                            JSONArray().put(
                                JSONObject().put("type", "text").put("text", message.content)
                            )
                        )
                    }
                )
            }
        }

        val body = JSONObject().apply {
            put("model", model)
            put("max_tokens", maxTokens)
            put("temperature", temperature)
            if (systemPrompt.isNotBlank()) put("system", systemPrompt)
            put("messages", conversation)
        }

        val request = Request.Builder()
            .url(url)
            .header("x-api-key", apiKey)
            .header("anthropic-version", ANTHROPIC_VERSION)
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
                    JSONObject(rawBody).optJSONObject("error")?.optString("message")
                } catch (_: Exception) {
                    null
                }
                throw OpenAiCompatibleProvider.aiError(resp.code, providerMessage)
            }

            try {
                val json = JSONObject(rawBody)
                val content = json.optJSONArray("content")
                    ?: throw AiException(
                        OpenAiCompatibleProvider.MESSAGES[OpenAiCompatibleProvider.ERR_UNEXPECTED]!!
                    )
                val text = buildString {
                    for (i in 0 until content.length()) {
                        val block = content.optJSONObject(i) ?: continue
                        if (block.optString("type") == "text") {
                            append(block.optString("text"))
                        }
                    }
                }

                val usage = json.optJSONObject("usage")
                AiResponse(
                    text = text,
                    inputTokens = usage?.optInt("input_tokens")?.takeIf { it > 0 },
                    outputTokens = usage?.optInt("output_tokens")?.takeIf { it > 0 }
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
        private const val MESSAGES_PATH = "/messages"
        private const val ANTHROPIC_VERSION = "2023-06-01"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Roles del formato Anthropic: user / assistant (system va aparte). */
        fun mapRole(role: AiRole): String = when (role) {
            AiRole.SYSTEM -> "user" // nunca se usa: SYSTEM se extrae antes
            AiRole.USER -> "user"
            AiRole.ASSISTANT -> "assistant"
        }
    }
}
