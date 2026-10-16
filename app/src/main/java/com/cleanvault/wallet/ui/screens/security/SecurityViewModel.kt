package com.cleanvault.wallet.ui.screens.security

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.cleanvault.wallet.core.crypto.VaultExportBundle
import com.cleanvault.wallet.core.crypto.VaultState
import com.cleanvault.wallet.core.crypto.WalletManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SecurityUiState(
    val vaultState: VaultState = VaultState(),
    val exportBundle: VaultExportBundle? = null,
    val isExportRevealed: Boolean = false,
    val solanaRpc: String = "https://api.mainnet-beta.solana.com",
    val robinhoodChainRpc: String = "https://rpc.robinhood-orbit.arbitrum.io",
    val errorMessage: String? = null
)

class SecurityViewModel(
    private val walletManager: WalletManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState())
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = _uiState.value.copy(
            vaultState = walletManager.vaultState.value
        )
    }

    fun requestExportWithBiometrics(activity: FragmentActivity) {
        walletManager.exportKeysWithBiometrics(
            activity = activity,
            onSuccess = { bundle ->
                _uiState.value = _uiState.value.copy(
                    exportBundle = bundle,
                    isExportRevealed = true,
                    errorMessage = null
                )
            },
            onError = { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err
                )
            }
        )
    }

    fun hideExportDetails() {
        _uiState.value = _uiState.value.copy(
            isExportRevealed = false,
            exportBundle = null
        )
    }

    fun toggleBiometrics(enabled: Boolean) {
        walletManager.setBiometricsEnabled(enabled)
        _uiState.value = _uiState.value.copy(
            vaultState = walletManager.vaultState.value
        )
    }

    fun markBackupComplete() {
        walletManager.markBackupConfirmed()
        _uiState.value = _uiState.value.copy(
            vaultState = walletManager.vaultState.value
        )
    }

    fun updateSolanaRpc(rpc: String) {
        _uiState.value = _uiState.value.copy(solanaRpc = rpc)
    }

    fun updateRobinhoodChainRpc(rpc: String) {
        _uiState.value = _uiState.value.copy(robinhoodChainRpc = rpc)
    }
}
