package com.fourdfit.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores the session token in EncryptedSharedPreferences (AES-256 GCM, key held in the
 * Android Keystore). Passwords are never stored — only the server-issued token.
 */
class SecureTokenStore(
    private val context: Context,
) {
    private val prefs: SharedPreferences =
        try {
            createPrefs()
        } catch (e: Exception) {
            // Keystore entry can become invalid (e.g. after a device restore). Start fresh.
            context.deleteSharedPreferences(FILE_NAME)
            createPrefs()
        }

    private val _token = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val token: StateFlow<String?> = _token.asStateFlow()

    val currentToken: String? get() = _token.value
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)

    fun save(
        token: String,
        userId: String,
    ) {
        prefs
            .edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .apply()
        _token.value = token
    }

    fun clear() {
        prefs.edit().clear().apply()
        _token.value = null
    }

    private fun createPrefs(): SharedPreferences {
        val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private companion object {
        const val FILE_NAME = "fourdfit_secure_session"
        const val KEY_TOKEN = "access_token"
        const val KEY_USER_ID = "user_id"
    }
}
