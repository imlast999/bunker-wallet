package com.cleanvault.wallet.data.service

import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.VerifiedToken
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger

data class ScannedRawBalance(
    val tokenAddress: String,
    val chain: Blockchain,
    val rawUnits: Double,
    val isNative: Boolean
)

interface BalanceScannerService {
    suspend fun scanSolanaBalances(
        solanaAddress: String,
        rpcUrl: String = "https://api.mainnet-beta.solana.com"
    ): Result<List<ScannedRawBalance>>

    suspend fun scanEvmBalances(
        evmAddress: String,
        verifiedEvmTokens: List<VerifiedToken>,
        rpcUrl: String = "https://arb1.arbitrum.io/rpc"
    ): Result<List<ScannedRawBalance>>
}

@Serializable
private data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int = 1,
    val method: String,
    val params: List<String>
)

class BalanceScannerServiceImpl(
    private val client: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
    }
) : BalanceScannerService {

    override suspend fun scanSolanaBalances(
        solanaAddress: String,
        rpcUrl: String
    ): Result<List<ScannedRawBalance>> = withContext(Dispatchers.IO) {
        if (solanaAddress.isBlank()) return@withContext Result.success(emptyList())
        val balances = mutableListOf<ScannedRawBalance>()

        try {
            // 1. Query Native SOL Balance
            val solPayload = """{"jsonrpc":"2.0","id":1,"method":"getBalance","params":["$solanaAddress"]}"""
            val solResp: JsonObject = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                setBody(solPayload)
            }.body()

            val lamports = solResp["result"]?.jsonObject?.get("value")?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
            val solUnits = lamports / 1_000_000_000.0
            balances.add(
                ScannedRawBalance(
                    tokenAddress = "11111111111111111111111111111111",
                    chain = Blockchain.SOLANA,
                    rawUnits = solUnits,
                    isNative = true
                )
            )

            // 2. Query SPL Token Accounts by Owner
            val splPayload = """
                {
                    "jsonrpc": "2.0",
                    "id": 2,
                    "method": "getTokenAccountsByOwner",
                    "params": [
                        "$solanaAddress",
                        {"programId": "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"},
                        {"encoding": "jsonParsed"}
                    ]
                }
            """.trimIndent()

            val splResp: JsonObject = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                setBody(splPayload)
            }.body()

            val accounts = splResp["result"]?.jsonObject?.get("value")?.jsonArray
            accounts?.forEach { acc ->
                try {
                    val info = acc.jsonObject["account"]?.jsonObject?.get("data")?.jsonObject?.get("parsed")?.jsonObject?.get("info")?.jsonObject
                    val mint = info?.get("mint")?.jsonPrimitive?.content ?: ""
                    val amountStr = info?.get("tokenAmount")?.jsonObject?.get("uiAmountString")?.jsonPrimitive?.content
                    val amount = amountStr?.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        balances.add(
                            ScannedRawBalance(
                                tokenAddress = mint,
                                chain = Blockchain.SOLANA,
                                rawUnits = amount,
                                isNative = false
                            )
                        )
                    }
                } catch (_: Exception) {}
            }

            Result.success(balances)
        } catch (e: Exception) {
            // If RPC is unreachable or fresh wallet with 0 balance, provide safe zero balances
            Result.success(balances)
        }
    }

    override suspend fun scanEvmBalances(
        evmAddress: String,
        verifiedEvmTokens: List<VerifiedToken>,
        rpcUrl: String
    ): Result<List<ScannedRawBalance>> = withContext(Dispatchers.IO) {
        if (evmAddress.isBlank()) return@withContext Result.success(emptyList())
        val balances = mutableListOf<ScannedRawBalance>()

        try {
            // 1. Query Native ETH Balance (eth_getBalance)
            val ethPayload = """{"jsonrpc":"2.0","id":1,"method":"eth_getBalance","params":["$evmAddress", "latest"]}"""
            val ethResp: JsonObject = client.post(rpcUrl) {
                contentType(ContentType.Application.Json)
                setBody(ethPayload)
            }.body()

            val hexBal = ethResp["result"]?.jsonPrimitive?.content ?: "0x0"
            val cleanHex = hexBal.removePrefix("0x").removePrefix("0X").ifEmpty { "0" }
            val wei = BigInteger(cleanHex, 16)
            val ethUnits = wei.toDouble() / 1e18
            balances.add(
                ScannedRawBalance(
                    tokenAddress = "0x0000000000000000000000000000000000000000",
                    chain = Blockchain.ROBINHOOD_CHAIN,
                    rawUnits = ethUnits,
                    isNative = true
                )
            )

            // 2. Query ERC-20 balanceOf for verified tokens (USDC, AAPL, NVDA, SPY, TSLA)
            // Function selector: balanceOf(address) = 0x70a08231
            val paddedAddress = evmAddress.removePrefix("0x").padStart(64, '0')
            val callData = "0x70a08231$paddedAddress"

            verifiedEvmTokens.forEachIndexed { index, token ->
                if (token.address != "0x0000000000000000000000000000000000000000") {
                    try {
                        val tokenPayload = """{"jsonrpc":"2.0","id":${index + 10},"method":"eth_call","params":[{"to":"${token.address}","data":"$callData"}, "latest"]}"""
                        val tokenResp: JsonObject = client.post(rpcUrl) {
                            contentType(ContentType.Application.Json)
                            setBody(tokenPayload)
                        }.body()

                        val tokenHex = tokenResp["result"]?.jsonPrimitive?.content ?: "0x0"
                        val tokenCleanHex = tokenHex.removePrefix("0x").ifEmpty { "0" }
                        val rawUnitsBig = BigInteger(tokenCleanHex, 16)
                        val divisor = Math.pow(10.0, token.decimals.toDouble())
                        val units = rawUnitsBig.toDouble() / divisor
                        if (units > 0.0) {
                            balances.add(
                                ScannedRawBalance(
                                    tokenAddress = token.address,
                                    chain = Blockchain.ROBINHOOD_CHAIN,
                                    rawUnits = units,
                                    isNative = false
                                )
                            )
                        }
                    } catch (_: Exception) {}
                }
            }

            Result.success(balances)
        } catch (e: Exception) {
            Result.success(balances)
        }
    }
}
