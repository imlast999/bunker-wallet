package com.cleanvault.wallet.data.remote.neon

import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.ScheduledOrderStatus
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
    private val neonClient: NeonDatabaseClient = NeonDatabaseClient()
) : NeonSyncRepository {

    override suspend fun syncUserProfile(
        evmAddress: String,
        solanaAddress: String,
        experienceLevel: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val query = """
            INSERT INTO users (id, evm_address, solana_address, experience_level, updated_at)
            VALUES (gen_random_uuid(), '$evmAddress', '$solanaAddress', '$experienceLevel', NOW())
            ON CONFLICT (evm_address) 
            DO UPDATE SET experience_level = EXCLUDED.experience_level, updated_at = NOW();
        """.trimIndent()

        val result = neonClient.executeQuery(query)
        if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Neon sync failed"))
    }

    override suspend fun syncScheduledOrder(
        order: ScheduledOrder,
        userAddress: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val query = """
            INSERT INTO scheduled_orders (
                id, user_address, from_token, to_token, amount_in, estimated_out, trigger_epoch, status, created_at
            ) VALUES (
                '${order.id}', '$userAddress', '${order.fromTokenSymbol}', '${order.toTokenSymbol}',
                ${order.amountIn}, ${order.estimatedAmountOut}, ${order.targetTriggerTime}, '${order.status.name}', NOW()
            )
            ON CONFLICT (id) DO UPDATE SET status = EXCLUDED.status;
        """.trimIndent()

        val result = neonClient.executeQuery(query)
        if (result.isSuccess) Result.success(true) else Result.failure(result.exceptionOrNull() ?: Exception("Neon sync order failed"))
    }

    override suspend fun fetchScheduledOrders(
        userAddress: String
    ): Result<List<ScheduledOrder>> = withContext(Dispatchers.IO) {
        val query = "SELECT * FROM scheduled_orders WHERE user_address = '$userAddress' ORDER BY trigger_epoch ASC;"
        val result = neonClient.executeQuery(query)
        Result.success(emptyList())
    }
}
