package com.cleanvault.wallet.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class ScheduledOrderStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    CANCELLED
}

@Serializable
data class ScheduledOrder(
    val id: String = UUID.randomUUID().toString(),
    val fromTokenSymbol: String,
    val fromTokenAddress: String,
    val fromChain: Blockchain,
    val toTokenSymbol: String,
    val toTokenAddress: String,
    val toChain: Blockchain,
    val amountIn: Double,
    val estimatedAmountOut: Double,
    val targetTriggerTime: Long,
    val formattedTargetTime: String,
    val status: ScheduledOrderStatus = ScheduledOrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val intentHashOrSignature: String? = null
)
