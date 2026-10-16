package com.cleanvault.wallet.ui.screens.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanvault.wallet.core.crypto.WalletManager
import com.cleanvault.wallet.data.model.NonCuratedAssetNotice
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.repository.PortfolioRepository
import com.cleanvault.wallet.data.repository.PortfolioSummary
import com.cleanvault.wallet.data.repository.ScheduledOrderRepository
import com.cleanvault.wallet.data.repository.TokenWhitelistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val summary: PortfolioSummary = PortfolioSummary(),
    val solanaAddress: String = "",
    val evmAddress: String = "",
    val nonCuratedNotice: NonCuratedAssetNotice? = null,
    val isInitialized: Boolean = false,
    val pendingScheduledOrders: List<ScheduledOrder> = emptyList()
)

class PortfolioViewModel(
    private val walletManager: WalletManager,
    private val portfolioRepository: PortfolioRepository,
    private val whitelistRepository: TokenWhitelistRepository,
    private val scheduledOrderRepository: ScheduledOrderRepository? = null
) : ViewModel() {

    val uiState: StateFlow<PortfolioUiState> = combine(
        portfolioRepository.portfolio,
        walletManager.vaultState,
        whitelistRepository.nonCuratedNotice,
        scheduledOrderRepository?.pendingOrders ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ) { portfolio, vaultState, notice, pendingOrders ->
        PortfolioUiState(
            summary = portfolio,
            solanaAddress = vaultState.solanaAddress,
            evmAddress = vaultState.evmAddress,
            nonCuratedNotice = notice,
            isInitialized = vaultState.isInitialized,
            pendingScheduledOrders = pendingOrders
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PortfolioUiState()
    )

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            if (!walletManager.vaultState.value.isInitialized) {
                walletManager.createNewVault()
            }
            val state = walletManager.vaultState.value
            portfolioRepository.refreshBalances(state.solanaAddress, state.evmAddress)
        }
    }

    fun refresh() {
        val state = walletManager.vaultState.value
        viewModelScope.launch {
            portfolioRepository.refreshBalances(state.solanaAddress, state.evmAddress)
        }
    }

    fun cancelScheduledOrder(orderId: String) {
        viewModelScope.launch {
            scheduledOrderRepository?.cancelOrder(orderId)
        }
    }
}
