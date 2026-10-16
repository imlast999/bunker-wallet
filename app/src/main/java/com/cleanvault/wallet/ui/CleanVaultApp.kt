package com.cleanvault.wallet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cleanvault.wallet.CleanVaultApplication
import com.cleanvault.wallet.ui.navigation.Screen
import com.cleanvault.wallet.ui.screens.assetdetail.AssetDetailScreen
import com.cleanvault.wallet.ui.screens.assetdetail.AssetDetailViewModel
import com.cleanvault.wallet.ui.screens.explore.ExploreScreen
import com.cleanvault.wallet.ui.screens.explore.ExploreViewModel
import com.cleanvault.wallet.ui.screens.onboarding.AuthEntryScreen
import com.cleanvault.wallet.ui.screens.onboarding.CreateWalletScreen
import com.cleanvault.wallet.ui.screens.onboarding.ExperienceQuizScreen
import com.cleanvault.wallet.ui.screens.onboarding.ImportWalletScreen
import com.cleanvault.wallet.ui.screens.portfolio.PortfolioScreen
import com.cleanvault.wallet.ui.screens.portfolio.PortfolioViewModel
import com.cleanvault.wallet.ui.screens.security.SecurityScreen
import com.cleanvault.wallet.ui.screens.security.SecurityViewModel
import com.cleanvault.wallet.ui.screens.trade.TradeScreen
import com.cleanvault.wallet.ui.screens.trade.TradeViewModel
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextSecondary

@Composable
fun CleanVaultApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current.applicationContext as CleanVaultApplication
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Portfolio,
        Screen.Explore,
        Screen.Trade,
        Screen.Security
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    val startDestination = if (context.secureStorage.hasVault && context.secureStorage.hasCompletedOnboarding) {
        Screen.Portfolio.route
    } else {
        Screen.AuthEntry.route
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = VaultDarkSurface,
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(0.5.dp, VaultCardBorder, androidx.compose.ui.graphics.RectangleShape)
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                if (screen.icon != null) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Portfolio.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VaultGreen,
                                selectedTextColor = VaultGreen,
                                indicatorColor = Color(0x1A00C805),
                                unselectedIconColor = VaultTextSecondary,
                                unselectedTextColor = VaultTextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VaultBlack)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Portfolio.route) {
                    val viewModel = remember {
                        PortfolioViewModel(
                            walletManager = context.walletManager,
                            portfolioRepository = context.portfolioRepository,
                            whitelistRepository = context.whitelistRepository,
                            scheduledOrderRepository = context.scheduledOrderRepository
                        )
                    }
                    PortfolioScreen(
                        viewModel = viewModel,
                        onNavigateToTrade = { fromTokenId ->
                            navController.navigate(Screen.Trade.createRoute(fromTokenId))
                        },
                        onNavigateToAssetDetail = { tokenId ->
                            navController.navigate(Screen.AssetDetail.createRoute(tokenId))
                        },
                        onNavigateToSecurity = {
                            navController.navigate(Screen.Security.route)
                        }
                    )
                }

                composable(Screen.Explore.route) {
                    val viewModel = remember {
                        ExploreViewModel(
                            whitelistRepository = context.whitelistRepository,
                            priceFeedService = context.priceFeedService
                        )
                    }
                    ExploreScreen(
                        viewModel = viewModel,
                        onNavigateToAssetDetail = { tokenId ->
                            navController.navigate(Screen.AssetDetail.createRoute(tokenId))
                        }
                    )
                }

                composable(
                    route = Screen.Trade.route,
                    arguments = listOf(navArgument("fromToken") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    val fromToken = entry.arguments?.getString("fromToken")
                    val viewModel = remember {
                        TradeViewModel(
                            whitelistRepository = context.whitelistRepository,
                            swapRoutingService = context.swapRoutingService,
                            walletManager = context.walletManager,
                            marketHoursManager = context.marketHoursManager,
                            scheduledOrderRepository = context.scheduledOrderRepository,
                            balanceScannerService = context.balanceScannerService
                        ).apply {
                            setInitialFromToken(fromToken)
                        }
                    }
                    TradeScreen(
                        viewModel = viewModel,
                        onBackClicked = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Portfolio.route)
                            }
                        }
                    )
                }

                composable(Screen.Security.route) {
                    val viewModel = remember {
                        SecurityViewModel(
                            walletManager = context.walletManager
                        )
                    }
                    SecurityScreen(
                        viewModel = viewModel,
                        onBackClicked = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Portfolio.route)
                            }
                        }
                    )
                }

                composable(
                    route = Screen.AssetDetail.route,
                    arguments = listOf(navArgument("tokenId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val tokenId = backStackEntry.arguments?.getString("tokenId") ?: ""
                    val viewModel = remember(tokenId) {
                        AssetDetailViewModel(
                            tokenId = tokenId,
                            whitelistRepository = context.whitelistRepository,
                            portfolioRepository = context.portfolioRepository,
                            priceFeedService = context.priceFeedService
                        )
                    }
                    AssetDetailScreen(
                        viewModel = viewModel,
                        onBackClicked = { navController.popBackStack() },
                        onTradeClicked = { id ->
                            navController.navigate(Screen.Trade.createRoute(id))
                        }
                    )
                }

                // --- ONBOARDING FLOW ---
                composable(Screen.AuthEntry.route) {
                    AuthEntryScreen(
                        onCreateWalletClicked = {
                            navController.navigate(Screen.CreateWallet.route)
                        },
                        onImportWalletClicked = {
                            navController.navigate(Screen.ImportWallet.route)
                        }
                    )
                }

                composable(Screen.CreateWallet.route) {
                    CreateWalletScreen(
                        walletManager = context.walletManager,
                        onBackClicked = { navController.popBackStack() },
                        onWalletCreated = {
                            navController.navigate(Screen.ExperienceQuiz.route)
                        }
                    )
                }

                composable(Screen.ImportWallet.route) {
                    ImportWalletScreen(
                        walletManager = context.walletManager,
                        onBackClicked = { navController.popBackStack() },
                        onWalletImported = {
                            navController.navigate(Screen.ExperienceQuiz.route)
                        }
                    )
                }

                composable(Screen.ExperienceQuiz.route) {
                    ExperienceQuizScreen(
                        secureStorage = context.secureStorage,
                        neonSyncRepository = context.neonSyncRepository,
                        onQuizCompleted = {
                            navController.navigate(Screen.Portfolio.route) {
                                popUpTo(Screen.AuthEntry.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}
