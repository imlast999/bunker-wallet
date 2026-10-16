package com.cleanvault.wallet.ui.screens.trade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cleanvault.wallet.core.crypto.WalletManager
import com.cleanvault.wallet.core.market.MarketHoursManager
import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.MarketStatus
import com.cleanvault.wallet.data.model.OrderExecutionMode
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.ScheduledOrderStatus
import com.cleanvault.wallet.data.model.SwapExecutionState
import com.cleanvault.wallet.data.model.SwapRoute
import com.cleanvault.wallet.data.model.VerifiedToken
import com.cleanvault.wallet.data.repository.ScheduledOrderRepository
import com.cleanvault.wallet.data.repository.TokenWhitelistRepository
import com.cleanvault.wallet.data.service.BalanceScannerService
import com.cleanvault.wallet.data.service.SwapRoutingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TradeUiState(
    val availableTokens: List<VerifiedToken> = emptyList(),
    val fromToken: VerifiedToken? = null,
    val toToken: VerifiedToken? = null,
    val inputAmountString: String = "1.0",
    val tokenInBalance: Double = 0.0,
    val nativeGasBalance: Double = 0.0,
    val validationError: String? = null,
    val swapRoute: SwapRoute? = null,
    val executionState: SwapExecutionState = SwapExecutionState.Idle,
    val isCrossChain: Boolean = false,
    val marketStatus: MarketStatus = MarketStatus.OPEN,
    val formattedNextOpen: String = "",
    val executionMode: OrderExecutionMode = OrderExecutionMode.REGULAR_HOURS_INSTANT,
    val isTargetEquityOrEtf: Boolean = false
)

class TradeViewModel(
    private val whitelistRepository: TokenWhitelistRepository,
    private val swapRoutingService: SwapRoutingService,
    private val walletManager: WalletManager,
    private val marketHoursManager: MarketHoursManager = MarketHoursManager(),
    private val scheduledOrderRepository: ScheduledOrderRepository? = null,
    private val balanceScannerService: BalanceScannerService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TradeUiState())
    val uiState: StateFlow<TradeUiState> = _uiState.asStateFlow()

    private var quoteJob: Job? = null

    init {
        initDefaultTokens()
    }

    private fun initDefaultTokens() {
        viewModelScope.launch {
            val tokens = whitelistRepository.verifiedTokens.value.ifEmpty {
                whitelistRepository.loadInitialWhitelist().getOrDefault(emptyList())
            }

            val defaultFrom = tokens.find { it.symbol == "SOL" || it.symbol == "USDC" } ?: tokens.firstOrNull()
            val defaultTo = tokens.find { it.symbol == "SPY" } ?: tokens.lastOrNull()

            _uiState.value = _uiState.value.copy(
                availableTokens = tokens,
                fromToken = defaultFrom,
                toToken = defaultTo,
                isCrossChain = defaultFrom?.chain != defaultTo?.chain
            )
            updateMarketContext()
            requestQuote()
        }
    }

    fun setInitialFromToken(tokenId: String?) {
        if (tokenId == null) return
        val token = _uiState.value.availableTokens.find { it.id == tokenId } ?: return
        _uiState.value = _uiState.value.copy(
            fromToken = token,
            isCrossChain = token.chain != _uiState.value.toToken?.chain
        )
        requestQuote()
    }

    fun onFromTokenSelected(token: VerifiedToken) {
        _uiState.value = _uiState.value.copy(
            fromToken = token,
            isCrossChain = token.chain != _uiState.value.toToken?.chain
        )
        requestQuote()
    }

    fun onToTokenSelected(token: VerifiedToken) {
        _uiState.value = _uiState.value.copy(
            toToken = token,
            isCrossChain = _uiState.value.fromToken?.chain != token.chain
        )
        updateMarketContext()
        requestQuote()
    }

    fun onAmountChanged(amountStr: String) {
        _uiState.value = _uiState.value.copy(inputAmountString = amountStr)
        requestQuote()
    }

    fun setExecutionMode(mode: OrderExecutionMode) {
        _uiState.value = _uiState.value.copy(executionMode = mode)
        requestQuote()
    }

    fun swapTokensDirection() {
        val currentFrom = _uiState.value.fromToken
        val currentTo = _uiState.value.toToken
        _uiState.value = _uiState.value.copy(
            fromToken = currentTo,
            toToken = currentFrom,
            isCrossChain = currentTo?.chain != currentFrom?.chain
        )
        updateMarketContext()
        requestQuote()
    }

    private fun updateMarketContext() {
        val target = _uiState.value.toToken ?: return
        val status = marketHoursManager.getCurrentMarketStatus(target.category)
        val formattedNextOpen = marketHoursManager.getFormattedNextOpenTime()
        val isTargetEquityOrEtf = target.category.name.contains("STOCK") || target.category.name.contains("ETF")

        // Default to SCHEDULE_FOR_OPEN if off-hours, or REGULAR_HOURS_INSTANT if open
        val defaultMode = if (isTargetEquityOrEtf && status == MarketStatus.EXTENDED_OR_WEEKEND) {
            OrderExecutionMode.SCHEDULE_FOR_OPEN
        } else {
            OrderExecutionMode.REGULAR_HOURS_INSTANT
        }

        _uiState.value = _uiState.value.copy(
            marketStatus = status,
            formattedNextOpen = formattedNextOpen,
            isTargetEquityOrEtf = isTargetEquityOrEtf,
            executionMode = if (isTargetEquityOrEtf && status == MarketStatus.EXTENDED_OR_WEEKEND) {
                if (_uiState.value.executionMode == OrderExecutionMode.REGULAR_HOURS_INSTANT) defaultMode else _uiState.value.executionMode
            } else {
                OrderExecutionMode.REGULAR_HOURS_INSTANT
            }
        )
    }

    fun requestQuote() {
        quoteJob?.cancel()
        val from = _uiState.value.fromToken ?: return
        val to = _uiState.value.toToken ?: return
        val amount = _uiState.value.inputAmountString.toDoubleOrNull() ?: 0.0

        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(swapRoute = null, validationError = null)
            return
        }

        validateBalances(from, amount)

        quoteJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(executionState = SwapExecutionState.Quoting)
            delay(250) // Debounce

            val userAddress = walletManager.getAddressForChain(from.chain)
            val nextOpenEpoch = marketHoursManager.getNextMarketOpenEpochMillis()
            val formattedNextOpen = _uiState.value.formattedNextOpen

            val result = swapRoutingService.getSwapQuote(
                fromToken = from,
                toToken = to,
                amount = amount,
                userAddress = userAddress,
                executionMode = _uiState.value.executionMode,
                targetExecutionTime = nextOpenEpoch,
                formattedTargetTime = formattedNextOpen
            )

            result.onSuccess { route ->
                _uiState.value = _uiState.value.copy(
                    swapRoute = route,
                    executionState = SwapExecutionState.QuoteReady(route)
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    executionState = SwapExecutionState.Error(error.message ?: "Failed to fetch routing quote")
                )
            }
        }
    }

    private fun validateBalances(from: VerifiedToken, amount: Double) {
        if (balanceScannerService == null) return
        viewModelScope.launch {
            try {
                val solAddr = walletManager.getAddressForChain(Blockchain.SOLANA)
                val evmAddr = walletManager.getAddressForChain(Blockchain.ROBINHOOD_CHAIN)

                var tokenBal = 0.0
                var gasBal = 0.0

                if (from.chain == Blockchain.SOLANA) {
                    val solBalances = balanceScannerService.scanSolanaBalances(solAddr).getOrDefault(emptyList())
                    val solNative = solBalances.find { it.isNative }?.rawUnits ?: 0.0
                    gasBal = solNative
                    tokenBal = if (from.symbol == "SOL") {
                        solNative
                    } else {
                        solBalances.find {
                            it.tokenAddress.equals(from.address, ignoreCase = true) ||
                            it.tokenAddress.equals(from.contractAddress, ignoreCase = true)
                        }?.rawUnits ?: 0.0
                    }
                } else {
                    val evmBalances = balanceScannerService.scanEvmBalances(evmAddr, listOf(from)).getOrDefault(emptyList())
                    val ethNative = evmBalances.find { it.isNative }?.rawUnits ?: 0.0
                    gasBal = ethNative
                    tokenBal = if (from.symbol == "ETH") {
                        ethNative
                    } else {
                        evmBalances.find {
                            it.tokenAddress.equals(from.address, ignoreCase = true) ||
                            it.tokenAddress.equals(from.contractAddress, ignoreCase = true)
                        }?.rawUnits ?: 0.0
                    }
                }

                val error = when {
                    amount <= 0.0 -> null
                    tokenBal < amount -> "Saldo insuficiente de ${from.symbol}"
                    from.chain == Blockchain.SOLANA && gasBal < 0.002 -> "Saldo de SOL insuficiente para pagar el gas"
                    from.chain == Blockchain.ROBINHOOD_CHAIN && gasBal < 0.0003 -> "Saldo de ETH insuficiente para pagar el gas"
                    else -> null
                }

                _uiState.value = _uiState.value.copy(
                    tokenInBalance = tokenBal,
                    nativeGasBalance = gasBal,
                    validationError = error
                )
            } catch (_: Exception) {}
        }
    }

    fun executeTrade() {
        val route = _uiState.value.swapRoute ?: return
        val currentError = _uiState.value.validationError
        if (currentError != null) {
            _uiState.value = _uiState.value.copy(
                executionState = SwapExecutionState.Error(currentError)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(executionState = SwapExecutionState.Signing)
            delay(500)

            val solPrivKey = walletManager.getSolanaPrivateKey() ?: ""
            val evmPrivKey = walletManager.getEvmPrivateKey() ?: ""

            if (route.executionMode == OrderExecutionMode.SCHEDULE_FOR_OPEN) {
                // Execute intent queue
                val result = swapRoutingService.executeSwap(
                    route = route,
                    userSolanaPrivateKey = solPrivKey,
                    userEvmPrivateKey = evmPrivKey,
                    scheduledOrderRepository = scheduledOrderRepository
                )
                result.onSuccess { orderTag ->
                    val scheduledOrder = ScheduledOrder(
                        fromTokenSymbol = route.fromToken.symbol,
                        fromTokenAddress = route.fromToken.address,
                        fromChain = route.fromToken.chain,
                        toTokenSymbol = route.toToken.symbol,
                        toTokenAddress = route.toToken.address,
                        toChain = route.toToken.chain,
                        amountIn = route.inputAmount,
                        estimatedAmountOut = route.expectedOutputAmount,
                        targetTriggerTime = route.targetExecutionTime ?: System.currentTimeMillis(),
                        formattedTargetTime = route.formattedTargetTime ?: _uiState.value.formattedNextOpen,
                        status = ScheduledOrderStatus.PENDING
                    )
                    _uiState.value = _uiState.value.copy(
                        executionState = SwapExecutionState.Scheduled(scheduledOrder)
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        executionState = SwapExecutionState.Error(err.message ?: "Failed to schedule order")
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(executionState = SwapExecutionState.Broadcasting())
                val result = swapRoutingService.executeSwap(
                    route = route,
                    userSolanaPrivateKey = solPrivKey,
                    userEvmPrivateKey = evmPrivKey
                )
                result.onSuccess { txHash ->
                    val explorer = if (route.fromToken.chain == Blockchain.SOLANA) {
                        "https://solscan.io/tx/$txHash"
                    } else {
                        "https://explorer.robinhood.com/tx/$txHash"
                    }
                    val summary = "Comprado ${route.expectedOutputAmount} ${route.toToken.symbol} con ${route.inputAmount} ${route.fromToken.symbol}"
                    _uiState.value = _uiState.value.copy(
                        executionState = SwapExecutionState.Success(
                            txHash = txHash,
                            explorerUrl = explorer,
                            swappedSummary = summary
                        )
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        executionState = SwapExecutionState.Error(error.message ?: "Execution failed")
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(executionState = SwapExecutionState.Idle)
        requestQuote()
    }
}
