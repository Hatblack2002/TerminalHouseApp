package com.terminalhouse.ai

/**
 * Presets oficiales de cada proveedor (Sección 3 de la spec v0.5.0):
 * endpoint por defecto y lista de modelos disponibles.
 *
 * Para [AiProviderType.CUSTOM] el usuario escribe endpoint y modelo,
 * por lo que los presets quedan vacíos.
 */
object AiPresets {

    data class ProviderPreset(
        val defaultEndpoint: String,
        val models: List<String>
    )

    private val presets: Map<AiProviderType, ProviderPreset> = mapOf(
        AiProviderType.OPENAI to ProviderPreset(
            defaultEndpoint = "https://api.openai.com/v1",
            models = listOf("gpt-4o-mini", "gpt-4o", "gpt-4-turbo", "o1-mini")
        ),
        AiProviderType.ANTHROPIC to ProviderPreset(
            defaultEndpoint = "https://api.anthropic.com/v1",
            models = listOf("claude-sonnet-4-5", "claude-opus-4-1", "claude-haiku-4-5")
        ),
        AiProviderType.GEMINI to ProviderPreset(
            defaultEndpoint = "https://generativelanguage.googleapis.com",
            models = listOf("gemini-2.0-flash", "gemini-2.5-pro", "gemini-2.5-flash")
        ),
        AiProviderType.MISTRAL to ProviderPreset(
            defaultEndpoint = "https://api.mistral.ai/v1",
            models = listOf("mistral-large-latest", "mistral-small-latest", "codestral-latest")
        ),
        AiProviderType.DEEPSEEK to ProviderPreset(
            defaultEndpoint = "https://api.deepseek.com/v1",
            models = listOf("deepseek-chat", "deepseek-coder", "deepseek-reasoner")
        ),
        AiProviderType.KIMI to ProviderPreset(
            defaultEndpoint = "https://api.moonshot.cn/v1",
            models = listOf("moonshot-v1-8k", "moonshot-v1-32k", "moonshot-v1-128k")
        ),
        AiProviderType.CUSTOM to ProviderPreset(
            defaultEndpoint = "",
            models = emptyList()
        )
    )

    fun preset(type: AiProviderType): ProviderPreset =
        presets.getValue(type)

    /** Endpoint por defecto del proveedor ("" para Personalizado). */
    fun defaultEndpoint(type: AiProviderType): String =
        preset(type).defaultEndpoint

    /** Modelos disponibles del proveedor (lista vacía para Personalizado). */
    fun models(type: AiProviderType): List<String> =
        preset(type).models
}
