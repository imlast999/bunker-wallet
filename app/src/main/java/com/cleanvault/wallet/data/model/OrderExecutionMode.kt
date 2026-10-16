package com.cleanvault.wallet.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class OrderExecutionMode(
    val title: String,
    val subtitle: String,
    val estimatedSlippagePercent: Double,
    val spreadWarning: String? = null
) {
    REGULAR_HOURS_INSTANT(
        title = "Instant Execution",
        subtitle = "Optimal market liquidity • Instant settlement",
        estimatedSlippagePercent = 0.15,
        spreadWarning = null
    ),
    SCHEDULE_FOR_OPEN(
        title = "Schedule for Market Open",
        subtitle = "Executes at 9:30 AM ET • Zero off-hours spread markup",
        estimatedSlippagePercent = 0.10,
        spreadWarning = null
    ),
    INSTANT_OFF_HOURS(
        title = "Instant 24/7 Execution",
        subtitle = "Executes now via on-chain AMMs • Off-hours liquidity spread",
        estimatedSlippagePercent = 1.85,
        spreadWarning = "Off-hours synthetic spread is currently ~1.5% - 2.5% higher than regular market hours."
    )
}
