package com.cleanvault.wallet.data.remote.neon

import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.ScheduledOrderStatus
import com.cleanvault.wallet.data.remote.BunkerBackendClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class NeonUserProfile(
    val id: String = UUID.randomUUID().toString(),
    val evmAddress: String,
    val solanaAddress: String,
    val experienceLevel: String, // 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED'
    val createdAt: Long = System.currentTimeMillis()
)

interface NeonSyncRepository {
    suspend fun syncUserProfile(
        evmAddress: String,
        solanaAddress: String,
        experienceLevel: String
    ): Result<Boolean>

    suspend fun syncScheduledOrder(
        order: ScheduledOrder,
        userAddress: String
    ): Result<Boolean>

    suspend fun fetchScheduledOrders(
        userAddress: String
    ): Result<List<ScheduledOrder>>
}

class NeonSyncRepositoryImpl(
    private val backendClient: BunkerBackendClient = BunkerBackendClient()
) : NeonSyncRepository {

    override suspend fun syncUserProfile(
        evmAddress: String,
        solanaAddress: String,
        experienceLevel: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = backendClient.syncUser(
            evmAddress = evmAddress,
            solanaAddress = solanaAddress,
            experienceLevel = experienceLevel
        )
        if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Backend user sync failed"))
        }
    }

    override suspend fun syncScheduledOrder(
        order: ScheduledOrder,
        userAddress: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = backendClient.syncOrder(order, userAddress)
        if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Backend order sync failed"))
        }
    }

    override suspend fun fetchScheduledOrders(
        userAddress: String
    ): Result<List<ScheduledOrder>> = withContext(Dispatchers.IO) {
        val result = backendClient.fetchOrders(userAddress)
        result.map { dtos ->
            dtos.map { dto ->
                ScheduledOrder(
                    id = dto.id,
                    fromTokenSymbol = dto.fromToken,
                    fromTokenAddress = "",
                    fromChain = try { Blockchain.valueOf(dto.chain) } catch (_: Exception) { Blockchain.SOLANA },
                    toTokenSymbol = dto.toToken,
                    toTokenAddress = "",
                    toChain = try { Blockchain.valueOf(dto.chain) } catch (_: Exception) { Blockchain.SOLANA },
                    amountIn = dto.amountIn,
                    estimatedAmountOut = dto.amountIn,
                    targetTriggerTime = dto.triggerEpoch,
                    formattedTargetTime = "Scheduled",
                    status = try { ScheduledOrderStatus.valueOf(dto.status) } catch (_: Exception) { ScheduledOrderStatus.PENDING }
                )
            }
        }
    }
}
