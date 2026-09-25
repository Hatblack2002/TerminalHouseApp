package com.terminalhouse.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.terminalhouse.ai.AiConfig
import com.terminalhouse.ai.AiException
import com.terminalhouse.ai.AiPresets
import com.terminalhouse.ai.AiProviderFactory
import com.terminalhouse.ai.AiProviderType
import com.terminalhouse.ai.SecureConfigStore
import kotlinx.coroutines.launch

/**
 * Pantalla "Ajustes → Agente IA" (Sección 2 de la spec v0.5.0).
 *
 * Archivo NUEVO: no modifica ningún archivo Compose existente (Sección -1).
 * Para conectarla al resto de la app basta una llamada a este Composable
 * desde el navegador/ajustes existentes (tarea de integración aparte).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAgentSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { SecureConfigStore(context) }
    val scope = rememberCoroutineScope()

    // Estado del formulario --------------------------------------------------
    var provider by remember { mutableStateOf(AiProviderType.OPENAI) }
    var endpoint by remember { mutableStateOf(AiPresets.defaultEndpoint(AiProviderType.OPENAI)) }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }

    var showApiKey by remember { mutableStateOf(false) }
    var providerMenuOpen by remember { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    var testing by remember { mutableStateOf(false) }
    var testOk by remember { mutableStateOf<Boolean?>(null) }
    var testMessage by remember { mutableStateOf("") }

    var savedFeedback by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    // Cargar configuración persistida al abrir la pantalla (Sección 1, punto 8).
    LaunchedEffect(Unit) {
        store.load()?.let { saved ->
            provider = saved.providerType
            endpoint = saved.endpoint
            apiKey = saved.apiKey
            model = saved.model
        }
        loaded = true
    }

    val models = AiPresets.models(provider)
    val isCustom = provider.allowsCustomEndpoint

    val canTest = !testing && apiKey.isNotBlank() && model.isNotBlank() && endpoint.isNotBlank()
    val canSave = endpoint.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()

    // Acciones ---------------------------------------------------------------
    fun changeProvider(newType: AiProviderType) {
        provider = newType
        // Actualización automática de endpoint y modelos (Sección 2).
        endpoint = AiPresets.defaultEndpoint(newType)
        model = ""
        testOk = null
        testMessage = ""
        savedFeedback = ""
    }

    fun runConnectionTest() {
        val config = AiConfig(
            providerType = provider,
            endpoint = endpoint.trim(),
            apiKey = apiKey.trim(),
            model = model.trim()
        )
        testing = true
        testOk = null
        testMessage = ""
        savedFeedback = ""
        scope.launch {
            try {
                val response = AiProviderFactory.create(config).testConnection()
                testOk = true
                testMessage = "✓ Conexión correcta: ${response.text.ifBlank { "(respuesta vacía)" }}"
            } catch (e: AiException) {
                testOk = false
                testMessage = "✗ ${e.message}"
            } catch (e: Exception) {
                testOk = false
                testMessage = "✗ Sin conexión a internet. Revisa tu red e inténtalo de nuevo."
            }
            testing = false
        }
    }

    fun saveConfiguration() {
        store.save(
            AiConfig(
                providerType = provider,
                endpoint = endpoint.trim(),
                apiKey = apiKey.trim(),
                model = model.trim()
            )
        )
        savedFeedback = "Configuración guardada y cifrada en el dispositivo."
        testOk = null
        testMessage = ""
    }

    fun clearConfiguration() {
        store.clear()
        provider = AiProviderType.OPENAI
        endpoint = AiPresets.defaultEndpoint(AiProviderType.OPENAI)
        apiKey = ""
        model = ""
        testOk = null
        testMessage = ""
        savedFeedback = "Configuración borrada permanentemente."
    }

    // UI ---------------------------------------------------------------------
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Agente IA") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Selector de proveedor (dropdown con los 7).
            ExposedDropdownMenuBox(
                expanded = providerMenuOpen,
                onExpandedChange = { providerMenuOpen = it }
            ) {
                OutlinedTextField(
                    value = provider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Proveedor de IA") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerMenuOpen)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = providerMenuOpen,
                    onDismissRequest = { providerMenuOpen = false }
                ) {
                    for (type in AiProviderType.ordered()) {
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                providerMenuOpen = false
                                changeProvider(type)
                            }
                        )
                    }
                }
            }

            // Campo de endpoint: editable SOLO si el proveedor es Personalizado.
            OutlinedTextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                label = { Text("Endpoint") },
                singleLine = true,
                enabled = isCustom,
                supportingText = {
                    Text(
                        if (isCustom) {
                            "Endpoint propio compatible con OpenAI (chat completions)."
                        } else {
                            AiProviderFactory.apiFormatName(provider)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Campo de API key: oculto con puntos + opción de mostrar.
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key") },
                singleLine = true,
                visualTransformation = if (showApiKey) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = if (showApiKey) {
                                "Ocultar API key"
                            } else {
                                "Mostrar API key"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Selector de modelo: lista del proveedor, o texto libre si CUSTOM.
            if (isCustom) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                ExposedDropdownMenuBox(
                    expanded = modelMenuOpen,
                    onExpandedChange = { modelMenuOpen = it }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Modelo") },
                        placeholder = { Text("Elige un modelo") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuOpen)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = modelMenuOpen,
                        onDismissRequest = { modelMenuOpen = false }
                    ) {
                        for (candidate in models) {
                            DropdownMenuItem(
                                text = { Text(candidate) },
                                onClick = {
                                    modelMenuOpen = false
                                    model = candidate
                                    testOk = null
                                    testMessage = ""
                                }
                            )
                        }
                    }
                }
            }

            // Botón "Probar conexión" + área de resultado (Sección 9).
            Button(
                onClick = { runConnectionTest() },
                enabled = canTest,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Probando…")
                } else {
                    Text("Probar conexión")
                }
            }

            if (testMessage.isNotBlank() && !testing) {
                Text(
                    text = testMessage,
                    color = when (testOk) {
                        true -> Color(0xFF2E7D32)
                        false -> MaterialTheme.colorScheme.error
                        null -> MaterialTheme.colorScheme.onSurface
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Botón "Guardar".
            Button(
                onClick = { saveConfiguration() },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }

            if (savedFeedback.isNotBlank()) {
                Text(
                    text = savedFeedback,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Borrar configuración (Sección 8).
            OutlinedButton(
                onClick = { clearConfiguration() },
                enabled = loaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Borrar configuración")
            }

            // Texto de privacidad y transparencia (Sección 10).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "Privacidad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                PrivacyBullet("Tu API key se guarda cifrada en el dispositivo (AES-256 con el Keystore del teléfono).")
                PrivacyBullet("No se envía a ningún servidor de TerminalHouse.")
                PrivacyBullet("La app no paga por tu uso: cada llamada va directa a tu proveedor.")
                PrivacyBullet("Puedes cambiar de proveedor o borrar la configuración cuando quieras.")
                PrivacyBullet("Sin telemetría.")
            }
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("• ", style = MaterialTheme.typography.bodySmall)
        Box(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
