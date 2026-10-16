package com.cleanvault.wallet.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Blockchain(val displayName: String, val nativeGasSymbol: String, val bip44Path: String) {
    SOLANA(
        displayName = "Solana",
        nativeGasSymbol = "SOL",
        bip44Path = "m/44'/501'/0'/0'"
    ),
    ROBINHOOD_CHAIN(
        displayName = "Robinhood Chain (L2)",
        nativeGasSymbol = "ETH",
        bip44Path = "m/44'/60'/0'/0/0"
    )
}

@Serializable
enum class AssetCategory(val label: String) {
    US_STOCK("US Stocks"),
    ETF_INDEX("ETFs & Indices"),
    TREASURY_BOND("Treasuries & Bonds"),
    COMMODITY("Commodities"),
    CORE_CRYPTO("Core Crypto"),
    STABLECOIN("Cash & Stables"),
    YIELD_STABLE("Yield USD")
}

@Serializable
data class VerifiedToken(
    val id: String,
    val ticker: String = "",
    val symbol: String = "",
    val name: String,
    val chain: Blockchain,
    val contractAddress: String = "",
    val address: String = "",
    val decimals: Int = 6,
    val category: AssetCategory,
    val issuer: String,
    val underlyingAsset: String = "",
    val isVerified: Boolean = true,
    val logoUrl: String = ""
) {
    val displayTicker: String get() = if (ticker.isNotBlank()) ticker else symbol
    val displayAddress: String get() = if (contractAddress.isNotBlank()) contractAddress else address
}

@Serializable
data class TokenBalance(
    val token: VerifiedToken,
    val rawBalance: String,
    val formattedBalance: Double,
    val usdPrice: Double,
    val totalUsdValue: Double,
    val change24hPercent: Double
)

@Serializable
data class NonCuratedAssetNotice(
    val detectedCount: Int,
    val chains: List<Blockchain>,
    val warningMessage: String = "Non-curated asset detected in balance. Export key to external wallet to view and manage."
)

@Serializable
data class WhitelistConfig(
    val version: String,
    val updatedAt: String,
    val signature: String,
    val tokens: List<VerifiedToken>
)
