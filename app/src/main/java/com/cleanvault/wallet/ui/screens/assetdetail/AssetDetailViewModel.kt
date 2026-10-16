package com.cleanvault.wallet.ui.screens.assetdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanvault.wallet.data.model.TokenBalance
import com.cleanvault.wallet.data.model.VerifiedToken
import com.cleanvault.wallet.data.repository.PortfolioRepository
import com.cleanvault.wallet.data.repository.TokenWhitelistRepository
import com.cleanvault.wallet.data.service.AssetMarketData
import com.cleanvault.wallet.data.service.PriceFeedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AssetDetailUiState(
    val token: VerifiedToken? = null,
    val marketData: AssetMarketData? = null,
    val userBalance: TokenBalance? = null,
    val isLoading: Boolean = true
)

class AssetDetailViewModel(
    private val tokenId: String,
    private val whitelistRepository: TokenWhitelistRepository,
    private val portfolioRepository: PortfolioRepository,
    private val priceFeedService: PriceFeedService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssetDetailUiState())
    val uiState: StateFlow<AssetDetailUiState> = _uiState.asStateFlow()

    init {
        loadAsset()
    }

    private fun loadAsset() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = whitelistRepository.verifiedTokens.value.find { it.id == tokenId }
                ?: whitelistRepository.loadInitialWhitelist().getOrNull()?.find { it.id == tokenId }

            if (token != null) {
                val market = priceFeedService.getMarketData(token)
                val balance = portfolioRepository.portfolio.value.verifiedBalances.find { it.token.id == tokenId }

                _uiState.value = AssetDetailUiState(
                    token = token,
                    marketData = market,
                    userBalance = balance,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
