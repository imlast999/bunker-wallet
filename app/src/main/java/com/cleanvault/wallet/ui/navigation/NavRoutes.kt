package com.cleanvault.wallet.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Portfolio : Screen("portfolio", "Portfolio", Icons.Default.AccountBalanceWallet)
    object Explore : Screen("explore", "Explore", Icons.Default.Search)
    object Trade : Screen("trade?fromToken={fromToken}", "Trade", Icons.Default.SwapHoriz) {
        fun createRoute(fromToken: String? = null): String {
            return if (fromToken != null) "trade?fromToken=$fromToken" else "trade"
        }
    }
    object Security : Screen("security", "Security", Icons.Default.Lock)
    object AssetDetail : Screen("asset_detail/{tokenId}", "Asset Detail") {
        fun createRoute(tokenId: String): String = "asset_detail/$tokenId"
    }
    
    // Onboarding
    object AuthEntry : Screen("auth_entry", "Welcome")
    object CreateWallet : Screen("create_wallet", "Create Wallet")
    object ImportWallet : Screen("import_wallet", "Import Wallet")
    object ExperienceQuiz : Screen("experience_quiz", "Experience")
}
