# CleanVault RWA Wallet (Android)

> **Curated, non-custodial multi-chain investment wallet combining the minimalist UX of MyInvestor/Robinhood with the web3 architecture of Phantom.**

CleanVault enables crypto-native users to buy, hold, and manage verified tokenized Stocks, ETFs, and Core Assets (BTC, ETH, SOL, USDC) without exposure to memecoin spam, zero-value airdrops, scams, or malicious approvals.

---

## 🏛️ Architecture Overview

### 1. Non-Custodial Key Derivation & Hardware Security
- **Single BIP-39 Seed Derivation:**
  - **Solana:** `m/44'/501'/0'/0'` (Ed25519 SLIP-0010)
  - **Robinhood Chain (Arbitrum Orbit EVM L2):** `m/44'/60'/0'/0/0` (secp256k1 BIP-44)
- **Local Key Storage:** Android Keystore + EncryptedSharedPreferences (Hardware-backed `AES-256-GCM` values, `AES-256-SIV` keys).
- **Biometric Security:** Android `BiometricPrompt` protects private key and mnemonic revelation. Users can export private keys anytime for import into Phantom or MetaMask.

### 2. Strict Anti-Rug Whitelist Filter ("No Noise" Engine)
- **Zero Memecoin Guarantee:** Non-whitelisted tokens, suspicious approvals, or zero-value spam airdrops are strictly stripped from the portfolio view.
- **Embedded Asset List:** `app/src/main/assets/token_whitelist.json` with cryptographic signature verification for remote updates.
  - **Solana RWAs:** Backed Finance bTokens (`bIB01`, `bCSPX`, `bNVDA`, `bAAPL`), Ondo (`USDY`), Native `SOL`, `USDC`, `USDT`.
  - **Robinhood Chain (EVM):** Official Robinhood Stock Tokens (`AAPL`, `NVDA`, `SPY`, `TSLA`), Native `ETH`, `USDC`.
- **Discreet Uncurated Badge:** If unknown tokens are detected on-chain, a discreet banner appears:
  > *"Non-curated asset detected in balance. Export key to external wallet to view and manage."*

### 3. Multi-Chain Routing & Liquidity Execution
- **Cross-Chain Bridging:** `deBridge DLN` API and `Relay.link` API (e.g. Swapping native SOL on Solana directly into tokenized SPY on Robinhood Chain in one transaction).
- **Native Solana Swaps:** `Jupiter v6` Aggregator API with dynamic slippage.
- **Robinhood Chain DEX:** Uniswap v3 contract bindings on Arbitrum Orbit.
- **Monetization Fee (Platform Markup):** A configurable **0.85%** integrator fee is calculated, displayed transparently, and injected into the swap payload.

### 4. Market Hours Intelligence & Scheduled Execution Engine
- **Session Detection (`MarketHoursManager.kt`):**
  - Monitors NYSE/Nasdaq regular trading hours (`09:30 AM - 04:00 PM ET` in `America/New_York`).
  - Detects official US market holidays (New Year, MLK, Presidents Day, Good Friday, Memorial Day, Juneteenth, July 4, Labor Day, Thanksgiving, Christmas).
  - Pure crypto assets (SOL, ETH, BTC, stables) trade 24/7 (`OPEN`), whereas tokenized stocks and ETFs trigger off-hours intelligence.
- **Dual-Mode Trade Execution:**
  - **`SCHEDULE_FOR_OPEN` (Optimal Liquidity):** Pre-authorizes a non-custodial limit/intent order staged for NYSE market open (Monday at 9:30 AM ET). Zero off-hours AMM spread markup.
  - **`INSTANT_OFF_HOURS` (High Spread):** Executes immediately against secondary 24/7 on-chain AMMs with transparent spread warning (~1.5% - 2.5%).
- **Scheduled Order Queue (`ScheduledOrderRepository.kt`):**
  - Persistent queue with Room-compatible entities and reactive Flow.
  - Visualized as a collapsible status card in `PortfolioScreen` with quick-cancel actions.
- **Visual Market Indicators (`MarketStatusIndicator.kt`):**
  - Live status pills (Green for `Market Open`, Amber for `Market Closed - Opens Mon 9:30 AM ET`) on `TradeScreen` and `AssetDetailScreen`.

### 5. Gas Experience & Deposit Guard Rails
- Both active deposit addresses are clearly shown on the home screen:
  1. **Solana Address** (Deposit SOL or USDC-SPL)
  2. **Robinhood Chain Address** (Deposit ETH or USDC-ERC20)
- Guard rail banner warns: *"Only deposit SOL / USDC to your Solana address and ETH / USDC to your Robinhood Chain address to fund gas and purchases."*

---

## 📂 Project Directory Structure

```
├── app
│   ├── build.gradle.kts
│   ├── src
│   │   └── main
│   │       ├── AndroidManifest.xml
│   │       ├── assets
│   │       │   └── token_whitelist.json
│   │       ├── java/com/cleanvault/wallet
│   │       │   ├── CleanVaultApplication.kt
│   │       │   ├── MainActivity.kt
│   │       │   ├── core
│   │       │   │   ├── crypto
│   │       │   │   │   ├── Bip39Helper.kt
│   │       │   │   │   ├── KeyDerivation.kt
│   │       │   │   │   └── WalletManager.kt
│   │       │   │   └── security
│   │       │   │       ├── BiometricAuthHelper.kt
│   │       │   │       └── SecureStorage.kt
│   │       │   ├── data
│   │       │   │   ├── model
│   │       │   │   │   ├── SwapQuote.kt
│   │       │   │   │   └── Token.kt
│   │       │   │   ├── repository
│   │       │   │   │   ├── PortfolioRepository.kt
│   │       │   │   │   └── TokenWhitelistRepository.kt
│   │       │   │   └── service
│   │       │   │       ├── PriceFeedService.kt
│   │       │   │       └── SwapRoutingService.kt
│   │       │   └── ui
│   │       │       ├── CleanVaultApp.kt
│   │       │       ├── components
│   │       │       │   ├── AddressBadgeRow.kt
│   │       │       │   ├── AssetRowItem.kt
│   │       │       │   ├── BalanceCard.kt
│   │       │       │   ├── GuardRailBanner.kt
│   │       │       │   └── SparklineChart.kt
│   │       │       ├── navigation
│   │       │       │   └── NavRoutes.kt
│   │       │       ├── screens
│   │       │       │   ├── assetdetail (AssetDetailScreen.kt, AssetDetailViewModel.kt)
│   │       │       │   ├── explore (ExploreScreen.kt, ExploreViewModel.kt)
│   │       │       │   ├── portfolio (PortfolioScreen.kt, PortfolioViewModel.kt)
│   │       │       │   ├── security (SecurityScreen.kt, SecurityViewModel.kt)
│   │       │       │   └── trade (TradeScreen.kt, TradeViewModel.kt)
│   │       │       └── theme
│   │       │           ├── Color.kt
│   │       │           ├── Theme.kt
│   │       │           └── Type.kt
│   │       └── res
│   │           └── values (colors.xml, strings.xml, themes.xml)
├── gradle
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Iguana / Jellyfish (or higher)
- JDK 17
- Android SDK 34 (minSdk 26)

### Run Automated Build & Device Deploy (Windows)
Double-click or run from terminal:
```cmd
build_apk.bat
```
This script automatically:
1. Verifies `JAVA_HOME` / JDK availability.
2. Auto-detects Android SDK and configures `local.properties`.
3. Runs `gradlew.bat assembleDebug`.
4. Copies the output APK to `dist\Bunker-Debug.apk`.
5. Detects connected Android devices via ADB and prompts for instant one-click installation and launch.

### Run via Command Line Manually
```bash
./gradlew assembleDebug
```
