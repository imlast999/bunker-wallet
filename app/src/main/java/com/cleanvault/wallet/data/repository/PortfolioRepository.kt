package com.cleanvault.wallet.data.repository

import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.TokenBalance
import com.cleanvault.wallet.data.model.VerifiedToken
import com.cleanvault.wallet.data.service.BalanceScannerService
import com.cleanvault.wallet.data.service.PriceService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class PortfolioSummary(
    val totalUsdValue: Double = 0.0,
    val change24hUsd: Double = 0.0,
    val change24hPercent: Double = 0.0,
    val verifiedBalances: List<TokenBalance> = emptyList(),
    val sparklinePoints: List<Double> = emptyList(),
    val isLoading: Boolean = false
)

interface PortfolioRepository {
    val portfolio: StateFlow<PortfolioSummary>
    suspend fun refreshBalances(solanaAddress: String, evmAddress: String): Result<PortfolioSummary>
}

class PortfolioRepositoryImpl(
    private val whitelistRepository: TokenWhitelistRepository,
    private val priceService: PriceService,
    private val balanceScannerService: BalanceScannerService
) : PortfolioRepository {

    private val _portfolio = MutableStateFlow(PortfolioSummary(isLoading = true))
    override val portfolio: StateFlow<PortfolioSummary> = _portfolio.asStateFlow()

    override suspend fun refreshBalances(
        solanaAddress: String,
        evmAddress: String
    ): Result<PortfolioSummary> = withContext(Dispatchers.IO) {
        _portfolio.value = _portfolio.value.copy(isLoading = true)

        try {
            val allWhitelisted = whitelistRepository.verifiedTokens.value.ifEmpty {
                whitelistRepository.loadInitialWhitelist().getOrDefault(emptyList())
            }

            // 1. Scan live on-chain balances
            val solanaTokens = allWhitelisted.filter { it.chain == Blockchain.SOLANA }
            val evmTokens = allWhitelisted.filter { it.chain == Blockchain.ROBINHOOD_CHAIN }

            val solanaScanned = balanceScannerService.scanSolanaBalances(solanaAddress).getOrDefault(emptyList())
            val evmScanned = balanceScannerService.scanEvmBalances(evmAddress, evmTokens).getOrDefault(emptyList())
            val allScanned = solanaScanned + evmScanned

            // 2. Fetch live prices from CoinGecko / Pyth
            val priceMap = priceService.getBatchLivePrices(allWhitelisted)

            val verifiedBalances = mutableListOf<TokenBalance>()
            var uncuratedCount = 0
            val uncuratedChains = mutableListOf<Blockchain>()

            // Map each scanned on-chain holding to a verified token
            allScanned.forEach { scanned ->
                val matched = allWhitelisted.find {
                    it.chain == scanned.chain &&
                    (it.address.equals(scanned.tokenAddress, ignoreCase = true) || (scanned.isNative && it.address == "11111111111111111111111111111111"))
                }

                if (matched != null) {
                    val priceData = priceMap[matched.symbol]
                    val price = priceData?.priceUsd ?: 1.0
                    val change = priceData?.change24hPercent ?: 0.0
                    val totalUsd = scanned.rawUnits * price

                    verifiedBalances.add(
                        TokenBalance(
                            token = matched,
                            rawBalance = scanned.rawUnits.toString(),
                            formattedBalance = scanned.rawUnits,
                            usdPrice = price,
                            totalUsdValue = totalUsd,
                            change24hPercent = change
                        )
                    )
                } else {
                    uncuratedCount++
                    if (!uncuratedChains.contains(scanned.chain)) {
                        uncuratedChains.add(scanned.chain)
                    }
                }
            }

            // Always display core assets in portfolio table with live prices even if 0.0 units
            allWhitelisted.forEach { token ->
                val alreadyAdded = verifiedBalances.any { it.token.id == token.id }
                if (!alreadyAdded) {
                    val priceData = priceMap[token.symbol]
                    val price = priceData?.priceUsd ?: 1.0
                    val change = priceData?.change24hPercent ?: 0.0
                    verifiedBalances.add(
                        TokenBalance(
                            token = token,
                            rawBalance = "0.0",
                            formattedBalance = 0.0,
                            usdPrice = price,
                            totalUsdValue = 0.0,
                            change24hPercent = change
                        )
                    )
                }
            }

            // 3. Strict Anti-Rug Whitelist rule: Report any non-whitelisted assets via discreet badge
            whitelistRepository.reportDetectedUncuratedTokens(
                count = uncuratedCount,
                chains = uncuratedChains
            )

            // 4. Calculate total portfolio metrics
            val totalUsd = verifiedBalances.sumOf { it.totalUsdValue }
            val change24hUsd = verifiedBalances.sumOf { (it.totalUsdValue * (it.change24hPercent / 100.0)) }
            val change24hPercent = if (totalUsd > 0) (change24hUsd / totalUsd) * 100.0 else 0.0

            val primaryToken = allWhitelisted.firstOrNull { it.symbol == "SOL" || it.symbol == "SPY" } ?: allWhitelisted.first()
            val sparkline = priceService.getSparkline(primaryToken)

            val summary = PortfolioSummary(
                totalUsdValue = totalUsd,
                change24hUsd = change24hUsd,
                change24hPercent = change24hPercent,
                verifiedBalances = verifiedBalances,
                sparklinePoints = sparkline,
                isLoading = false
            )
            _portfolio.value = summary
            Result.success(summary)
        } catch (e: Exception) {
            _portfolio.value = _portfolio.value.copy(isLoading = false)
            Result.failure(e)
        }
    }
}
