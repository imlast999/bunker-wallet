package com.cleanvault.wallet

import android.app.Application
import com.cleanvault.wallet.core.crypto.WalletManager
import com.cleanvault.wallet.core.market.MarketHoursManager
import com.cleanvault.wallet.core.security.BiometricAuthHelper
import com.cleanvault.wallet.core.security.SecureStorage
import com.cleanvault.wallet.data.repository.PortfolioRepository
import com.cleanvault.wallet.data.repository.PortfolioRepositoryImpl
import com.cleanvault.wallet.data.repository.ScheduledOrderRepository
import com.cleanvault.wallet.data.repository.ScheduledOrderRepositoryImpl
import com.cleanvault.wallet.data.repository.TokenWhitelistRepository
import com.cleanvault.wallet.data.repository.TokenWhitelistRepositoryImpl
import com.cleanvault.wallet.data.service.PriceFeedService
import com.cleanvault.wallet.data.service.PriceFeedServiceImpl
import com.cleanvault.wallet.data.service.SwapRoutingService
import com.cleanvault.wallet.data.service.SwapRoutingServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CleanVaultApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var secureStorage: SecureStorage
        private set
    lateinit var biometricAuthHelper: BiometricAuthHelper
        private set
    lateinit var walletManager: WalletManager
        private set
    lateinit var whitelistRepository: TokenWhitelistRepository
        private set
    lateinit var priceService: com.cleanvault.wallet.data.service.PriceService
        private set
    lateinit var balanceScannerService: com.cleanvault.wallet.data.service.BalanceScannerService
        private set
    lateinit var priceFeedService: PriceFeedService
        private set
    lateinit var portfolioRepository: PortfolioRepository
        private set
    lateinit var swapRoutingService: SwapRoutingService
        private set
    lateinit var marketHoursManager: MarketHoursManager
        private set
    lateinit var scheduledOrderRepository: ScheduledOrderRepository
        private set
    lateinit var neonSyncRepository: com.cleanvault.wallet.data.remote.neon.NeonSyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        secureStorage = SecureStorage(this)
        biometricAuthHelper = BiometricAuthHelper(this)
        walletManager = WalletManager(this, secureStorage, biometricAuthHelper)
        whitelistRepository = TokenWhitelistRepositoryImpl(this)
        priceService = com.cleanvault.wallet.data.service.PriceServiceImpl()
        balanceScannerService = com.cleanvault.wallet.data.service.BalanceScannerServiceImpl()
        priceFeedService = PriceFeedServiceImpl(priceService)
        portfolioRepository = PortfolioRepositoryImpl(whitelistRepository, priceService, balanceScannerService)
        swapRoutingService = SwapRoutingServiceImpl()
        marketHoursManager = MarketHoursManager()
        scheduledOrderRepository = ScheduledOrderRepositoryImpl(this)
        neonSyncRepository = com.cleanvault.wallet.data.remote.neon.NeonSyncRepositoryImpl()

        appScope.launch(Dispatchers.IO) {
            whitelistRepository.loadInitialWhitelist()
        }
    }
}
