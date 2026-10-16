package com.cleanvault.wallet.core.crypto

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.cleanvault.wallet.core.security.BiometricAuthHelper
import com.cleanvault.wallet.core.security.SecureStorage
import com.cleanvault.wallet.data.model.Blockchain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class VaultState(
    val isInitialized: Boolean = false,
    val solanaAddress: String = "",
    val evmAddress: String = "",
    val biometricsActive: Boolean = true,
    val isBackedUp: Boolean = false
)

data class VaultExportBundle(
    val mnemonic: String?,
    val solanaAddress: String,
    val solanaPrivateKey: String,
    val evmAddress: String,
    val evmPrivateKey: String,
    val recoveryInstructions: String = "You can import these keys into Phantom (for Solana RWAs) or MetaMask/Rabby (for Robinhood Chain EVM) to access all assets including any uncurated tokens."
)

class WalletManager(
    private val context: Context,
    private val secureStorage: SecureStorage = SecureStorage(context),
    private val biometricAuthHelper: BiometricAuthHelper = BiometricAuthHelper(context)
) {

    private val _vaultState = MutableStateFlow(VaultState())
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    init {
        loadExistingVault()
    }

    private fun loadExistingVault() {
        if (secureStorage.hasVault) {
            val solAddress = secureStorage.getSolanaAddress() ?: ""
            val evmAddress = secureStorage.getEvmAddress() ?: ""
            _vaultState.value = VaultState(
                isInitialized = true,
                solanaAddress = solAddress,
                evmAddress = evmAddress,
                biometricsActive = secureStorage.biometricsEnabled,
                isBackedUp = secureStorage.lastBackupTimestamp > 0
            )
        }
    }

    /**
     * 1. Generate new vault (Default Recommendation)
     * Generates a 12-word BIP-39 mnemonic, derives Solana + Robinhood Chain keys,
     * and encrypts them via Hardware Keystore AES-256-GCM.
     */
    suspend fun createNewVault(wordCount: Int = 12): Result<VaultState> = withContext(Dispatchers.Default) {
        try {
            val mnemonic = Bip39Helper.generateMnemonic(wordCount)
            val seed = Bip39Helper.mnemonicToSeed(mnemonic)

            val solanaKey = KeyDerivation.deriveSolanaKey(seed)
            val evmKey = KeyDerivation.deriveRobinhoodChainKey(seed)

            secureStorage.storeMnemonic(mnemonic)
            secureStorage.storeAddresses(solanaKey.address, evmKey.address)
            secureStorage.storePrivateKeys(solanaKey.privateKeyHexOrBase58, evmKey.privateKeyHexOrBase58)
            secureStorage.hasVault = true
            secureStorage.lastBackupTimestamp = 0

            val newState = VaultState(
                isInitialized = true,
                solanaAddress = solanaKey.address,
                evmAddress = evmKey.address,
                biometricsActive = secureStorage.biometricsEnabled,
                isBackedUp = false
            )
            _vaultState.value = newState
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 2. Import existing wallet via Seed Phrase (12 or 24 words)
     */
    suspend fun importFromMnemonic(mnemonic: String): Result<VaultState> = withContext(Dispatchers.Default) {
        val cleanMnemonic = mnemonic.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }.joinToString(" ")
        if (!Bip39Helper.validateMnemonic(cleanMnemonic)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid BIP-39 mnemonic phrase. Must be 12 or 24 words."))
        }

        try {
            val seed = Bip39Helper.mnemonicToSeed(cleanMnemonic)
            val solanaKey = KeyDerivation.deriveSolanaKey(seed)
            val evmKey = KeyDerivation.deriveRobinhoodChainKey(seed)

            secureStorage.storeMnemonic(cleanMnemonic)
            secureStorage.storeAddresses(solanaKey.address, evmKey.address)
            secureStorage.storePrivateKeys(solanaKey.privateKeyHexOrBase58, evmKey.privateKeyHexOrBase58)
            secureStorage.hasVault = true
            secureStorage.lastBackupTimestamp = System.currentTimeMillis()

            val newState = VaultState(
                isInitialized = true,
                solanaAddress = solanaKey.address,
                evmAddress = evmKey.address,
                biometricsActive = secureStorage.biometricsEnabled,
                isBackedUp = true
            )
            _vaultState.value = newState
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import directly from individual private keys
     */
    suspend fun importFromPrivateKeys(solanaPrivKey: String, evmPrivKey: String): Result<VaultState> = withContext(Dispatchers.Default) {
        try {
            val solanaKey = KeyDerivation.deriveSolanaFromPrivateKey(solanaPrivKey)
            val evmKey = KeyDerivation.deriveEvmFromPrivateKey(evmPrivKey)

            secureStorage.storeAddresses(solanaKey.address, evmKey.address)
            secureStorage.storePrivateKeys(solanaKey.privateKeyHexOrBase58, evmKey.privateKeyHexOrBase58)
            secureStorage.hasVault = true
            secureStorage.lastBackupTimestamp = System.currentTimeMillis()

            val newState = VaultState(
                isInitialized = true,
                solanaAddress = solanaKey.address,
                evmAddress = evmKey.address,
                biometricsActive = secureStorage.biometricsEnabled,
                isBackedUp = true
            )
            _vaultState.value = newState
            Result.success(newState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 3. Export private keys and mnemonic phrase with Biometric Authentication
     * Required so users can recover or withdraw unlisted assets anytime via Phantom or MetaMask.
     */
    fun exportKeysWithBiometrics(
        activity: FragmentActivity,
        onSuccess: (VaultExportBundle) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!secureStorage.hasVault) {
            onError("No active vault found.")
            return
        }

        if (secureStorage.biometricsEnabled && biometricAuthHelper.isBiometricAvailable()) {
            biometricAuthHelper.promptBiometric(
                activity = activity,
                title = "Export Vault Private Keys",
                subtitle = "Biometric confirmation required for non-custodial security",
                onSuccess = {
                    val bundle = buildExportBundle()
                    if (bundle != null) {
                        secureStorage.lastBackupTimestamp = System.currentTimeMillis()
                        _vaultState.value = _vaultState.value.copy(isBackedUp = true)
                        onSuccess(bundle)
                    } else {
                        onError("Unable to retrieve encrypted keys from Android Keystore.")
                    }
                },
                onError = onError
            )
        } else {
            // Fallback when biometrics is disabled or unavailable on hardware
            val bundle = buildExportBundle()
            if (bundle != null) {
                onSuccess(bundle)
            } else {
                onError("Unable to decrypt keys.")
            }
        }
    }

    private fun buildExportBundle(): VaultExportBundle? {
        val mnemonic = secureStorage.getMnemonic()
        val solPriv = secureStorage.getSolanaPrivateKey() ?: return null
        val evmPriv = secureStorage.getEvmPrivateKey() ?: return null
        val solAddr = secureStorage.getSolanaAddress() ?: ""
        val evmAddr = secureStorage.getEvmAddress() ?: ""

        return VaultExportBundle(
            mnemonic = mnemonic,
            solanaAddress = solAddr,
            solanaPrivateKey = solPriv,
            evmAddress = evmAddr,
            evmPrivateKey = evmPriv
        )
    }

    fun markBackupConfirmed() {
        secureStorage.lastBackupTimestamp = System.currentTimeMillis()
        _vaultState.value = _vaultState.value.copy(isBackedUp = true)
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        secureStorage.biometricsEnabled = enabled
        _vaultState.value = _vaultState.value.copy(biometricsActive = enabled)
    }

    fun deleteVault() {
        secureStorage.clearVault()
        _vaultState.value = VaultState(isInitialized = false)
    }

    fun getAddressForChain(chain: Blockchain): String {
        return when (chain) {
            Blockchain.SOLANA -> _vaultState.value.solanaAddress
            Blockchain.ROBINHOOD_CHAIN -> _vaultState.value.evmAddress
        }
    }

    fun getSolanaPrivateKey(): String? = secureStorage.getSolanaPrivateKey()
    fun getEvmPrivateKey(): String? = secureStorage.getEvmPrivateKey()
}
