package com.cleanvault.wallet.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed encrypted local storage using Android Keystore and EncryptedSharedPreferences.
 * Uses AES-256-GCM for values and AES-256-SIV for keys.
 */
class SecureStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "cleanvault_secure_store",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_MNEMONIC = "vault_mnemonic"
        private const val KEY_SOLANA_PRIV = "vault_solana_priv"
        private const val KEY_EVM_PRIV = "vault_evm_priv"
        private const val KEY_SOLANA_ADDR = "vault_solana_addr"
        private const val KEY_EVM_ADDR = "vault_evm_addr"
        private const val KEY_HAS_VAULT = "vault_exists"
        private const val KEY_BIOMETRICS_ENABLED = "biometrics_enabled"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "last_backup_timestamp"
        private const val KEY_EXPERIENCE_LEVEL = "user_experience_level"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    var hasVault: Boolean
        get() = prefs.getBoolean(KEY_HAS_VAULT, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_VAULT, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var userExperienceLevel: String
        get() = prefs.getString(KEY_EXPERIENCE_LEVEL, "BEGINNER") ?: "BEGINNER"
        set(value) = prefs.edit().putString(KEY_EXPERIENCE_LEVEL, value).apply()

    var biometricsEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRICS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRICS_ENABLED, value).apply()

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, value).apply()

    fun storeMnemonic(mnemonic: String) {
        prefs.edit().putString(KEY_MNEMONIC, mnemonic).apply()
    }

    fun getMnemonic(): String? {
        return prefs.getString(KEY_MNEMONIC, null)
    }

    fun storeAddresses(solanaAddress: String, evmAddress: String) {
        prefs.edit()
            .putString(KEY_SOLANA_ADDR, solanaAddress)
            .putString(KEY_EVM_ADDR, evmAddress)
            .apply()
    }

    fun getSolanaAddress(): String? = prefs.getString(KEY_SOLANA_ADDR, null)
    fun getEvmAddress(): String? = prefs.getString(KEY_EVM_ADDR, null)

    fun storePrivateKeys(solanaPrivateKey: String, evmPrivateKey: String) {
        prefs.edit()
            .putString(KEY_SOLANA_PRIV, solanaPrivateKey)
            .putString(KEY_EVM_PRIV, evmPrivateKey)
            .apply()
    }

    fun getSolanaPrivateKey(): String? = prefs.getString(KEY_SOLANA_PRIV, null)
    fun getEvmPrivateKey(): String? = prefs.getString(KEY_EVM_PRIV, null)

    fun clearVault() {
        prefs.edit().clear().apply()
    }
}
