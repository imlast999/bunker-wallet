package com.cleanvault.wallet.data.remote.neon

import com.cleanvault.wallet.data.remote.BunkerBackendClient

/**
 * NeonDatabaseClient
 *
 * Deprecated: In accordance with production security standards, direct database connections
 * and raw SQL strings have been completely removed from client mobile code.
 * All backend operations now flow securely through [BunkerBackendClient] and the Cloudflare Edge Worker.
 */
@Deprecated(
    message = "Use BunkerBackendClient instead to prevent storing database credentials on the client device.",
    replaceWith = ReplaceWith("BunkerBackendClient", "com.cleanvault.wallet.data.remote.BunkerBackendClient")
)
class NeonDatabaseClient(
    val backendClient: BunkerBackendClient = BunkerBackendClient()
)
