package com.cleanvault.wallet.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.ScheduledOrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room-compatible Entity definition for Scheduled Orders
 */
data class ScheduledOrderEntity(
    val id: String,
    val fromToken: String,
    val fromChain: String,
    val toToken: String,
    val toChain: String,
    val amountIn: Double,
    val estimatedAmountOut: Double,
    val targetTriggerTime: Long,
    val formattedTargetTime: String,
    val status: String,
    val createdAt: Long,
    val intentHash: String?
)

/**
 * Interface contract matching Room DAO pattern for reactive streams
 */
interface ScheduledOrderDao {
    suspend fun insert(order: ScheduledOrderEntity)
    suspend fun updateStatus(id: String, status: String)
    suspend fun delete(id: String)
    suspend fun getAll(): List<ScheduledOrderEntity>
}

interface ScheduledOrderRepository {
    val pendingOrders: StateFlow<List<ScheduledOrder>>
    val allOrders: StateFlow<List<ScheduledOrder>>

    suspend fun queueOrder(order: ScheduledOrder): Result<ScheduledOrder>
    suspend fun cancelOrder(orderId: String): Result<Boolean>
    suspend fun updateOrderStatus(orderId: String, status: ScheduledOrderStatus): Result<Boolean>
}

/**
 * Persistent Scheduled Orders Repository.
 * Employs local persistent storage + in-memory reactive Flow to provide instant
 * updates across PortfolioScreen, TradeScreen, and Background WorkManager triggers.
 */
class ScheduledOrderRepositoryImpl(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) : ScheduledOrderRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("bunker_scheduled_orders", Context.MODE_PRIVATE)

    private val _allOrders = MutableStateFlow<List<ScheduledOrder>>(emptyList())
    override val allOrders: StateFlow<List<ScheduledOrder>> = _allOrders.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<ScheduledOrder>>(emptyList())
    override val pendingOrders: StateFlow<List<ScheduledOrder>> = _pendingOrders.asStateFlow()

    init {
        loadPersistedOrders()
    }

    private fun loadPersistedOrders() {
        val serialized = prefs.getString("orders_list", null)
        if (!serialized.isNullOrEmpty()) {
            try {
                val list = json.decodeFromString<List<ScheduledOrder>>(serialized)
                _allOrders.value = list
                _pendingOrders.value = list.filter { it.status == ScheduledOrderStatus.PENDING }
                return
            } catch (_: Exception) { }
        }

        // Initial sample pre-staged order to immediately demonstrate UI capability
        val sampleOrder = ScheduledOrder(
            id = "sample-scheduled-1",
            fromTokenSymbol = "USDC",
            fromTokenAddress = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v",
            fromChain = Blockchain.SOLANA,
            toTokenSymbol = "SPY",
            toTokenAddress = "0x3333334444445555556666667777778888889999",
            toChain = Blockchain.ROBINHOOD_CHAIN,
            amountIn = 250.0,
            estimatedAmountOut = 0.4820,
            targetTriggerTime = System.currentTimeMillis() + 86400000L,
            formattedTargetTime = "Monday at 9:30 AM ET",
            status = ScheduledOrderStatus.PENDING
        )
        val initialList = listOf(sampleOrder)
        _allOrders.value = initialList
        _pendingOrders.value = initialList
        saveOrders(initialList)
    }

    override suspend fun queueOrder(order: ScheduledOrder): Result<ScheduledOrder> = withContext(Dispatchers.IO) {
        try {
            val updated = _allOrders.value.toMutableList()
            updated.add(0, order)
            _allOrders.value = updated
            _pendingOrders.value = updated.filter { it.status == ScheduledOrderStatus.PENDING }
            saveOrders(updated)
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelOrder(orderId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val updated = _allOrders.value.map {
                if (it.id == orderId) it.copy(status = ScheduledOrderStatus.CANCELLED) else it
            }
            _allOrders.value = updated
            _pendingOrders.value = updated.filter { it.status == ScheduledOrderStatus.PENDING }
            saveOrders(updated)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrderStatus(
        orderId: String,
        status: ScheduledOrderStatus
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val updated = _allOrders.value.map {
                if (it.id == orderId) it.copy(status = status) else it
            }
            _allOrders.value = updated
            _pendingOrders.value = updated.filter { it.status == ScheduledOrderStatus.PENDING }
            saveOrders(updated)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveOrders(orders: List<ScheduledOrder>) {
        val serialized = json.encodeToString(orders)
        prefs.edit().putString("orders_list", serialized).apply()
    }
}
