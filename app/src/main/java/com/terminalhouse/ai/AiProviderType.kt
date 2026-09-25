package com.terminalhouse.ai

/**
 * Los 7 proveedores de IA soportados por TerminalHouse (Sección 0 y 3 de la spec v0.5.0).
 *
 * El formato de API que usa cada uno se resuelve automáticamente en
 * [AiProviderFactory] según la Sección 4 de la especificación:
 *  - Formato OpenAI: OPENAI, MISTRAL, DEEPSEEK, KIMI, CUSTOM.
 *  - Formato Anthropic: ANTHROPIC.
 *  - Formato Gemini: GEMINI.
 */
enum class AiProviderType(val displayName: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic (Claude)"),
    GEMINI("Google Gemini"),
    MISTRAL("Mistral"),
    DEEPSEEK("DeepSeek"),
    KIMI("Kimi (Moonshot)"),
    CUSTOM("Personalizado");

    /** true si este proveedor admite que el usuario escriba su propio endpoint. */
    val allowsCustomEndpoint: Boolean
        get() = this == CUSTOM

    companion object {
        /** Orden de presentación en la UI (Sección 0: 7 opciones). */
        fun ordered(): List<AiProviderType> = entries.toList()
    }
}
