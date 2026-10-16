package com.cleanvault.wallet.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class MarketStatus(
    val title: String,
    val description: String,
    val isTradeOptimal: Boolean
) {
    OPEN(
        title = "Market Open",
        description = "NYSE & Nasdaq Regular Hours (09:30 - 16:00 ET). Maximum institutional liquidity and nominal spreads.",
        isTradeOptimal = true
    ),
    EXTENDED_OR_WEEKEND(
        title = "Market Closed",
        description = "Off-hours / Weekend. Primary stock order books closed. On-chain AMM execution subject to wider spread.",
        isTradeOptimal = false
    )
}
