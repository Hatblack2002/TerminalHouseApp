package com.example.service

import com.example.model.AiMessage
import java.util.UUID

object AiAgentService {

    fun getInitialGreeting(): AiMessage {
        return AiMessage(
            id = UUID.randomUUID().toString(),
            isUser = false,
            text = "Hola, soy tu agente IA. Puedo ayudarte a crear apps, instalar herramientas, ejecutar comandos, analizar archivos y mucho más."
        )
    }

    fun processQuery(prompt: String): AiMessage {
        val lower = prompt.lowercase().trim()

        return when {
            lower.contains("crear una app") || lower.contains("app nativa") || lower.contains("android") -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Para crear una app nativa de Android en TerminalHouse, podemos inicializar un proyecto con Gradle y Jetpack Compose.\n\nEjecuta el siguiente comando para generar la estructura base:",
                    suggestedCommand = "mkdir -p ~/projects/MyAndroidApp && cd ~/projects/MyAndroidApp && ls -la"
                )
            }
            lower.contains("instalar") || lower.contains("herramienta") || lower.contains("paquete") -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Puedes instalar herramientas de desarrollo de Ubuntu usando apt. Por ejemplo, para instalar herramientas esenciales de compilación y Python:",
                    suggestedCommand = "apt update && apt install -y build-essential python3 git curl"
                )
            }
            lower.contains("ejecutar un comando") || lower.contains("comando") -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Puedes ejecutar cualquier comando Linux estándar en la terminal. Aquí tienes uno para ver la identidad real del sistema:",
                    suggestedCommand = "cat /etc/os-release && uname -a"
                )
            }
            lower.contains("analizar") || lower.contains("archivo") -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "Para analizar archivos o verificar el contenido de tu directorio actual con permisos y tamaños detallados:",
                    suggestedCommand = "ls -lah /root"
                )
            }
            lower.contains("neofetch") -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "neofetch no viene instalado en el rootfs base. Para ver la distribución REAL ejecuta:",
                    suggestedCommand = "cat /etc/os-release"
                )
            }
            else -> {
                AiMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = "He recibido tu consulta sobre \"$prompt\". Te recomiendo ejecutar el siguiente comando para inspeccionar el entorno:",
                    suggestedCommand = "uname -a && free -h"
                )
            }
        }
    }
}
