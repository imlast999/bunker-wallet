package com.cleanvault.wallet.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class RoutingProvider(val displayName: String) {
    DEBRIDGE_DLN("deBridge DLN Cross-Chain"),
    RELAY_LINK("Relay.link Cross-Chain Bridge"),
    JUPITER_V6("Jupiter v6 Aggregator (Solana)"),
    ROBINHOOD_DEX("Robinhood Chain Uniswap v3 Router"),
    JUPITER_LIMIT_STAGED("Jupiter Limit / Pre-Market Staged Intent"),
    ROBINHOOD_ESCROW_INTENT("Robinhood Chain EIP-712 Open Session Escrow")
}

@Serializable
data class FeeBreakdown(
    val networkGasFeeUsd: Double,
    val bridgeProtocolFeeUsd: Double,
    val platformFeePercent: Double = 0.85, // 0.85% platform integrator fee
    val platformFeeUsd: Double,
    val platformFeeRecipient: String,
    val estimatedTotalFeesUsd: Double,
    val offHoursLiquiditySpreadUsd: Double = 0.0
)

@Serializable
data class SwapRoute(
    val provider: RoutingProvider,
    val fromToken: VerifiedToken,
    val toToken: VerifiedToken,
    val inputAmount: Double,
    val expectedOutputAmount: Double,
    val minimumReceivedAmount: Double,
    val priceImpactPercent: Double,
    val fees: FeeBreakdown,
    val estimatedDurationSeconds: Int,
    val routeSteps: List<String>,
    val txPayloadHexOrBase64: String,
    val executionMode: OrderExecutionMode = OrderExecutionMode.REGULAR_HOURS_INSTANT,
    val targetExecutionTime: Long? = null,
    val formattedTargetTime: String? = null
)

sealed class SwapExecutionState {
    object Idle : SwapExecutionState()
    object Quoting : SwapExecutionState()
    data class QuoteReady(val route: SwapRoute) : SwapExecutionState()
    object Signing : SwapExecutionState()
    data class Broadcasting(val txHash: String? = null) : SwapExecutionState()
    data class Success(val txHash: String, val explorerUrl: String, val swappedSummary: String = "") : SwapExecutionState()
    data class Scheduled(val order: ScheduledOrder) : SwapExecutionState()
    data class Error(val message: String) : SwapExecutionState()
}
