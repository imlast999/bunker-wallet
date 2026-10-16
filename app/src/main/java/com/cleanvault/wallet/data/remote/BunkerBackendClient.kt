package com.cleanvault.wallet.data.remote

import com.cleanvault.wallet.BuildConfig
import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.ScheduledOrderStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UserSyncRequest(
    @SerialName("evm_address") val evmAddress: String,
    @SerialName("solana_address") val solanaAddress: String,
    @SerialName("experience_level") val experienceLevel: String
)

@Serializable
data class UserProfileDto(
    val id: String,
    @SerialName("evm_address") val evmAddress: String,
    @SerialName("solana_address") val solanaAddress: String,
    @SerialName("experience_level") val experienceLevel: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("last_active") val lastActive: String? = null
)

@Serializable
data class UserSyncResponse(
    val success: Boolean,
    val user: UserProfileDto? = null,
    val error: String? = null
)

@Serializable
data class OrderSyncRequest(
    val id: String,
    @SerialName("user_address") val userAddress: String,
    @SerialName("from_token") val fromToken: String,
    @SerialName("to_token") val toToken: String,
    val chain: String,
    @SerialName("amount_in") val amountIn: Double,
    val status: String,
    @SerialName("trigger_epoch") val triggerEpoch: Long,
    @SerialName("intent_hash") val intentHash: String? = null
)

@Serializable
data class ScheduledOrderDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("from_token") val fromToken: String,
    @SerialName("to_token") val toToken: String,
    val chain: String,
    @SerialName("amount_in") val amountIn: Double,
    val status: String,
    @SerialName("trigger_epoch") val triggerEpoch: Long,
    @SerialName("intent_hash") val intentHash: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OrderSyncResponse(
    val success: Boolean,
    val order: ScheduledOrderDto? = null,
    val error: String? = null
)

@Serializable
data class OrdersListResponse(
    val success: Boolean,
    val count: Int = 0,
    val orders: List<ScheduledOrderDto> = emptyList(),
    val error: String? = null
)

/**
 * BunkerBackendClient
 *
 * Lightweight, zero-credential client connecting to the Bunker Cloudflare Edge Worker
 * backed by Neon Serverless Postgres.
 *
 * SECURITY INVARIANT:
 * Zero private keys, seed phrases, or database connection strings are present in this code.
 * Only public wallet addresses, user UI preferences, and non-custodial order intents are exchanged.
 */
class BunkerBackendClient(
    val baseUrl: String = try {
        BuildConfig.BACKEND_API_URL.ifBlank { "https://bunker-api.workers.dev" }
    } catch (_: Exception) {
        "https://bunker-api.workers.dev"
    },
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }
) {

    /**
     * Synchronizes wallet profile and onboarding experience level (Principiante, Intermedio, Avanzado).
     */
    suspend fun syncUser(
        evmAddress: String,
        solanaAddress: String,
        experienceLevel: String
    ): Result<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val response: UserSyncResponse = client.post("$baseUrl/api/users/sync") {
                contentType(ContentType.Application.Json)
                setBody(
                    UserSyncRequest(
                        evmAddress = evmAddress,
                        solanaAddress = solanaAddress,
                        experienceLevel = experienceLevel
                    )
                )
            }.body()

            if (response.success && response.user != null) {
                Result.success(response.user)
            } else {
                Result.failure(Exception(response.error ?: "Edge worker sync failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves or cancels scheduled order intent on the backend.
     */
    suspend fun syncOrder(
        order: ScheduledOrder,
        userAddress: String
    ): Result<ScheduledOrderDto> = withContext(Dispatchers.IO) {
        try {
            val response: OrderSyncResponse = client.post("$baseUrl/api/orders") {
                contentType(ContentType.Application.Json)
                setBody(
                    OrderSyncRequest(
                        id = order.id,
                        userAddress = userAddress,
                        fromToken = order.fromTokenSymbol,
                        toToken = order.toTokenSymbol,
                        chain = order.fromChain.name,
                        amountIn = order.amountIn,
                        status = order.status.name,
                        triggerEpoch = order.targetTriggerTime,
                        intentHash = null
                    )
                )
            }.body()

            if (response.success && response.order != null) {
                Result.success(response.order)
            } else {
                Result.failure(Exception(response.error ?: "Order sync failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves active/pending scheduled orders for a wallet.
     */
    suspend fun fetchOrders(
        userAddress: String,
        statusFilter: String? = null
    ): Result<List<ScheduledOrderDto>> = withContext(Dispatchers.IO) {
        try {
            val response: OrdersListResponse = client.get("$baseUrl/api/orders") {
                parameter("address", userAddress)
                if (statusFilter != null) {
                    parameter("status", statusFilter)
                }
            }.body()

            if (response.success) {
                Result.success(response.orders)
            } else {
                Result.failure(Exception(response.error ?: "Failed to fetch orders"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
