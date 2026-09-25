package com.terminalhouse.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Almacenamiento CIFRADO de la configuración del agente IA
 * (Sección 8 de la spec v0.5.0).
 *
 * Garantías implementadas:
 *  - Cifrado AES-256: la clave maestra vive en el Keystore del dispositivo
 *    (AES256-GCM) y el archivo de preferencias se cifra con ella.
 *  - Si el teléfono no está desbloqueado, el Keystore no libera la clave
 *    y los datos no se pueden descifrar.
 *  - La API key nunca se escribe en logs (esta clase no registra nada).
 *  - La API key nunca sale del dispositivo: solo se envía al endpoint del
 *    proveedor elegido por el propio usuario.
 *  - [clear] elimina permanentemente toda la configuración.
 */
class SecureConfigStore(context: Context) {

    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Guarda (o actualiza) la configuración completa cifrada. */
    fun save(config: AiConfig) {
        prefs.edit()
            .putString(KEY_PROVIDER, config.providerType.name)
            .putString(KEY_ENDPOINT, config.endpoint)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.model)
            .apply()
    }

    /**
     * Carga la configuración guardada; devuelve null si no hay configuración
     * previa (nunca se ha guardado o se borró con [clear]).
     */
    fun load(): AiConfig? {
        val providerName = prefs.getString(KEY_PROVIDER, null) ?: return null
        val providerType = runCatching { AiProviderType.valueOf(providerName) }
            .getOrNull() ?: return null

        return AiConfig(
            providerType = providerType,
            endpoint = prefs.getString(KEY_ENDPOINT, "").orEmpty(),
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
            model = prefs.getString(KEY_MODEL, "").orEmpty()
        )
    }

    /** Indica si existe una configuración guardada. */
    fun hasConfig(): Boolean = prefs.contains(KEY_PROVIDER)

    /** Borra PERMANENTEMENTE toda la configuración, incluida la API key. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE = "terminalhouse_ai_secure_prefs"
        const val KEY_PROVIDER = "provider"
        const val KEY_ENDPOINT = "endpoint"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
    }
}
