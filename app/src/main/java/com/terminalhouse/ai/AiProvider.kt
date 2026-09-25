package com.terminalhouse.ai

/**
 * Tipos de datos comunes e interfaz de proveedor (Sección 11, archivo 1 y Sección 5).
 *
 * Toda comunicación con cualquier proveedor pasa por [AiProvider], de modo que
 * el resto de la app es agnóstico al proveedor elegido (Sección 12).
 */

/** Rol de un mensaje en la conversación (Sección 5). */
enum class AiRole { SYSTEM, USER, ASSISTANT }

/** Un mensaje individual: rol + contenido de texto. */
data class AiMessage(
    val role: AiRole,
    val content: String
)

/**
 * Respuesta normalizada de un proveedor.
 *
 * @param text texto generado por el modelo.
 * @param inputTokens tokens de entrada usados (null si el proveedor no los reporta).
 * @param outputTokens tokens de salida usados (null si el proveedor no los reporta).
 */
data class AiResponse(
    val text: String,
    val inputTokens: Int? = null,
    val outputTokens: Int? = null
)

/**
 * Excepción con mensaje comprensible para un usuario común (Sección 9).
 *
 * @param message descripción clara del fallo (sin códigos HTTP crudos).
 * @param httpCode código HTTP original si la falla vino de una respuesta del servidor.
 * @param cause causa técnica original, opcional, para diagnóstico interno.
 */
class AiException(
    override val message: String,
    val httpCode: Int? = null,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Abstracción única para hablar con cualquier proveedor (Sección 4).
 *
 * Las implementaciones deben:
 *  - ejecutar la llamada de red en un hilo de fondo (Dispatchers.IO),
 *  - aplicar timeout de 30 segundos (Sección 9),
 *  - traducir los errores a [AiException] con mensaje comprensible.
 */
interface AiProvider {

    /** Proveedor concreto que representa esta implementación. */
    val providerType: AiProviderType

    /**
     * Envía la conversación completa y devuelve la respuesta del modelo.
     *
     * @param messages lista con SYSTEM (opcional), USER y ASSISTANT (historial).
     * @param maxTokens límite de tokens de salida.
     * @param temperature creatividad del modelo (0.0 - 2.0 según proveedor).
     */
    suspend fun complete(
        messages: List<AiMessage>,
        maxTokens: Int = 1024,
        temperature: Double = 0.3
    ): AiResponse

    /**
     * Prueba de conexión mínima (Sección 9): pide al modelo que responda
     * solo "OK" y devuelve la respuesta recibida. Lanza [AiException] si falla.
     */
    suspend fun testConnection(): AiResponse =
        complete(
            messages = listOf(
                AiMessage(role = AiRole.USER, content = "Responde únicamente con la palabra OK")
            ),
            maxTokens = 16,
            temperature = 0.0
        )
}
