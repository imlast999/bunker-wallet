package com.cleanvault.wallet.data.service

import com.cleanvault.wallet.data.model.VerifiedToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PricePoint(val timestamp: Long, val price: Double)

data class AssetMarketData(
    val token: VerifiedToken,
    val currentPriceUsd: Double,
    val change24hPercent: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24hUsd: Double,
    val marketCapUsd: Double,
    val sparklinePoints: List<Double>,
    val historyPoints: List<PricePoint>
)

interface PriceFeedService {
    suspend fun getMarketData(token: VerifiedToken): AssetMarketData
    suspend fun getPortfolioSparkline(): List<Double>
}

class PriceFeedServiceImpl(
    private val priceService: PriceService = PriceServiceImpl()
) : PriceFeedService {

    override suspend fun getMarketData(token: VerifiedToken): AssetMarketData = withContext(Dispatchers.IO) {
        val live = priceService.getLivePrice(token)
        val basePrice = live.priceUsd
        val change = live.change24hPercent

        val sparkline = priceService.getSparkline(token)
        val history = sparkline.mapIndexed { index, price ->
            PricePoint(System.currentTimeMillis() - ((sparkline.size - index) * 3600_000L), price)
        }

        AssetMarketData(
            token = token,
            currentPriceUsd = basePrice,
            change24hPercent = change,
            high24h = basePrice * (1.0 + kotlin.math.abs(change) / 100.0 * 0.6),
            low24h = basePrice * (1.0 - kotlin.math.abs(change) / 100.0 * 0.6),
            volume24hUsd = 45_200_000.0,
            marketCapUsd = 2_850_000_000.0,
            sparklinePoints = sparkline,
            historyPoints = history
        )
    }

    override suspend fun getPortfolioSparkline(): List<Double> = withContext(Dispatchers.Default) {
        listOf(
            12450.0, 12480.0, 12510.0, 12490.0, 12580.0,
            12620.0, 12590.0, 12670.0, 12720.0, 12790.0,
            12850.0, 12820.0, 12900.0, 12960.0, 13045.50
        )
    }

    private fun generateSparkline(basePrice: Double, isPositive: Boolean): List<Double> {
        val trend = if (isPositive) 0.003 else -0.002
        val list = mutableListOf<Double>()
        var current = basePrice * 0.97
        for (i in 0 until 20) {
            val noise = (Math.random() - 0.48) * (basePrice * 0.008)
            current += (basePrice * trend) + noise
            list.add(current)
        }
        list.add(basePrice)
        return list
    }
}
