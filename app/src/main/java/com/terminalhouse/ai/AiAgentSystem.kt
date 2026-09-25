package com.terminalhouse.ai

/**
 * System prompt del agente y construcción del contexto del terminal
 * (Secciones 6 y 7 de la spec v0.5.0).
 */
object AiAgentSystem {

    /**
     * System prompt que se envía SIEMPRE primero (Sección 6).
     * Define identidad, idioma, formato de comandos, seguridad,
     * reintento, concisión, honestidad y limitaciones del entorno.
     */
    const val SYSTEM_PROMPT: String = """Eres el agente técnico de TerminalHouse: un asistente que vive dentro de una app de Android que ejecuta un Ubuntu 24.04 real sobre PRoot. El usuario tiene una shell bash real (login root en el rootfs) y puede ejecutar comandos.

Reglas que debes seguir siempre:

1. Idioma: responde en el mismo idioma en el que te escriba el usuario.

2. Formato de comandos: cuando propongas un comando de shell, rodéalo SIEMPRE con bloques ```bash ``` para que la interfaz pueda ofrecer el botón "Ejecutar". Un comando por bloque.

3. Seguridad: nunca propongas comandos destructivos (rm -rf indiscriminados, formateos, borrado del rootfs, dd sobre dispositivos). Si la tarea lo requiere, advierte antes y sugiere la variante más segura.

4. Reintento: si el usuario te reporta que un comando falló, lee la salida que te dé, explica la causa en una frase y propón la alternativa corregida.

5. Concisión: no expliques lo obvio. Ve directo al grano: comando + explicación mínima necesaria.

6. Honestidad: no inventes rutas, paquetes ni archivos. Si no sabes cómo está el sistema, pide al usuario que ejecute un comando de lectura y comparta la salida.

7. Limitaciones del entorno: PRoot corre como root dentro del rootfs, pero las capacidades reales están limitadas por Android. NO hay acceso a hardware real, ni a particiones del sistema, ni a otros procesos fuera del rootfs. No prometas cosas que PRoot no puede hacer (kernels, drivers, systemctl real, hardware).

8. Contexto del terminal: cuando el usuario te envíe contexto (directorio actual, último comando, última salida del terminal), úsalo sin preguntar ni repetirlo.

9. Enfoque: eres un asistente técnico de terminal. Ayuda con bash, paquetes apt, scripts, archivos, Python y herramientas disponibles en el rootfs."""

    /** Máximo de caracteres de salida del terminal que se envían (Sección 7). */
    const val MAX_CONTEXT_OUTPUT_CHARS: Int = 2000

    /**
     * Trunca una salida larga conservando el principio y el final y
     * cortando el medio (regla explícita de la Sección 7).
     */
    fun truncateMiddle(text: String, maxChars: Int = MAX_CONTEXT_OUTPUT_CHARS): String {
        if (text.length <= maxChars) return text
        val kept = maxChars - SEPARATOR.length
        val headLen = kept - kept / 2
        val tailLen = kept / 2
        val head = text.take(headLen)
        val tail = text.takeLast(tailLen)
        return head + SEPARATOR + tail
    }

    /**
     * Construye el bloque de contexto del terminal (Sección 7):
     * directorio actual, último comando y última salida (truncada).
     */
    fun buildTerminalContext(
        currentDirectory: String?,
        lastCommand: String?,
        lastOutput: String?
    ): String {
        val parts = mutableListOf<String>()
        if (!currentDirectory.isNullOrBlank()) {
            parts += "Directorio actual: $currentDirectory"
        }
        if (!lastCommand.isNullOrBlank()) {
            parts += "Último comando ejecutado: $lastCommand"
        }
        if (!lastOutput.isNullOrBlank()) {
            parts += "Última salida del terminal:\n" + truncateMiddle(lastOutput)
        }
        if (parts.isEmpty()) return ""
        return "[Contexto del terminal]\n" + parts.joinToString("\n")
    }

    /**
     * Ensambla la conversación completa para enviar al proveedor (Sección 5 y 7):
     *
     *  1. SYSTEM: el system prompt (siempre primero).
     *  2. Historial USER/ASSISTANT previo (multi-turno).
     *  3. USER actual: texto del usuario + bloque de contexto (si hay).
     *
     * @param userText lo que escribe el usuario.
     * @param terminalContext bloque generado por [buildTerminalContext] (puede ser "").
     * @param history mensajes previos USER/ASSISTANT de la conversación.
     */
    fun buildMessages(
        userText: String,
        terminalContext: String = "",
        history: List<AiMessage> = emptyList()
    ): List<AiMessage> {
        val messages = mutableListOf<AiMessage>(
            AiMessage(role = AiRole.SYSTEM, content = SYSTEM_PROMPT)
        )
        for (message in history) {
            if (message.role == AiRole.SYSTEM) continue
            messages += message
        }

        val composedUser = if (terminalContext.isBlank()) {
            userText
        } else {
            "$userText\n\n$terminalContext"
        }
        messages += AiMessage(role = AiRole.USER, content = composedUser)
        return messages
    }

    private const val SEPARATOR = "\n…[salida truncada]…\n"
}
