package com.terminalhouse.ai

/**
 * Fábrica que crea el proveedor correcto según el tipo seleccionado
 * (Sección 11, archivo 5; detección automática de formato según Sección 4).
 *
 *  - Formato OpenAI (chat completions): OpenAI, Mistral, DeepSeek, Kimi, Custom.
 *  - Formato Anthropic: Claude.
 *  - Formato Gemini: Gemini.
 */
object AiProviderFactory {

    /**
     * Crea un [AiProvider] listo para usarse con la configuración dada.
     *
     * @param config configuración completa (proveedor, endpoint, key, modelo).
     * @throws IllegalArgumentException si el tipo de proveedor no está soportado.
     */
    fun create(config: AiConfig): AiProvider {
        return when (config.providerType) {
            AiProviderType.ANTHROPIC -> AnthropicProvider(
                providerType = config.providerType,
                endpoint = config.endpoint,
                apiKey = config.apiKey,
                model = config.model
            )

            AiProviderType.GEMINI -> GeminiProvider(
                providerType = config.providerType,
                endpoint = config.endpoint,
                apiKey = config.apiKey,
                model = config.model
            )

            AiProviderType.OPENAI,
            AiProviderType.MISTRAL,
            AiProviderType.DEEPSEEK,
            AiProviderType.KIMI,
            AiProviderType.CUSTOM -> OpenAiCompatibleProvider(
                providerType = config.providerType,
                endpoint = config.endpoint,
                apiKey = config.apiKey,
                model = config.model
            )
        }
    }

    /**
     * Indica qué formato de API usa un proveedor, por si la UI quiere
     * mostrarlo ("compatible OpenAI", "Anthropic", "Gemini").
     */
    fun apiFormatName(type: AiProviderType): String = when (type) {
        AiProviderType.ANTHROPIC -> "Formato Anthropic"
        AiProviderType.GEMINI -> "Formato Google Gemini"
        else -> "Formato OpenAI (chat completions)"
    }
}
