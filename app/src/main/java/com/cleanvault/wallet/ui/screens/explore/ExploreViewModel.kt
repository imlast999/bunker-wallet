package com.cleanvault.wallet.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanvault.wallet.data.model.AssetCategory
import com.cleanvault.wallet.data.model.VerifiedToken
import com.cleanvault.wallet.data.repository.TokenWhitelistRepository
import com.cleanvault.wallet.data.service.AssetMarketData
import com.cleanvault.wallet.data.service.PriceFeedService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExploreUiState(
    val searchQuery: String = "",
    val selectedCategory: AssetCategory = AssetCategory.US_STOCK,
    val tokensMarketData: List<AssetMarketData> = emptyList(),
    val filteredMarketData: List<AssetMarketData> = emptyList(),
    val isLoading: Boolean = false
)

class ExploreViewModel(
    private val whitelistRepository: TokenWhitelistRepository,
    private val priceFeedService: PriceFeedService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState(isLoading = true))
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    init {
        loadMarketData()
    }

    fun loadMarketData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val tokens = whitelistRepository.verifiedTokens.value.ifEmpty {
                whitelistRepository.loadInitialWhitelist().getOrDefault(emptyList())
            }

            val list = tokens.map { token ->
                priceFeedService.getMarketData(token)
            }

            _uiState.value = _uiState.value.copy(
                tokensMarketData = list,
                isLoading = false
            )
            applyFilter()
        }
    }

    fun selectCategory(category: AssetCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilter()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    private fun applyFilter() {
        val current = _uiState.value
        val query = current.searchQuery.trim().lowercase()

        val filtered = current.tokensMarketData.filter { item ->
            val matchCategory = item.token.category == current.selectedCategory
            val matchQuery = query.isEmpty() ||
                    item.token.symbol.lowercase().contains(query) ||
                    item.token.name.lowercase().contains(query) ||
                    item.token.underlyingAsset.lowercase().contains(query)
            matchCategory && matchQuery
        }

        _uiState.value = current.copy(filteredMarketData = filtered)
    }
}
