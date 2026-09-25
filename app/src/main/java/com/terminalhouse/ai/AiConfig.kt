package com.terminalhouse.ai

/**
 * Configuración del agente IA elegida por el usuario (Sección 8 de la spec v0.5.0).
 *
 * Esta configuración se persiste CIFRADA en el dispositivo mediante
 * [SecureConfigStore]; nunca viaja a ningún servidor de TerminalHouse.
 *
 * @param providerType proveedor seleccionado (uno de los 7 de la Sección 0).
 * @param endpoint URL base del proveedor; solo editable si el proveedor es CUSTOM.
 * @param apiKey clave privada del usuario con su proveedor. Nunca se loguea.
 * @param modelo identificador exacto del modelo (p.ej. "gpt-4o-mini").
 */
data class AiConfig(
    val providerType: AiProviderType,
    val endpoint: String,
    val apiKey: String,
    val model: String
) {
    /** true si la configuración tiene todo lo obligatorio cubierto. */
    fun isComplete(): Boolean =
        providerType != null /* siempre no nulo en data class */ &&
            endpoint.isNotBlank() &&
            apiKey.isNotBlank() &&
            model.isNotBlank()

    companion object {
        /** Configuración vacía para el estado inicial de la pantalla de ajustes. */
        fun empty(type: AiProviderType = AiProviderType.OPENAI): AiConfig = AiConfig(
            providerType = type,
            endpoint = AiPresets.defaultEndpoint(type),
            apiKey = "",
            model = ""
        )
    }
}
