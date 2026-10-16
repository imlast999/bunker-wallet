package com.cleanvault.wallet.ui.screens.trade

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.MarketStatus
import com.cleanvault.wallet.data.model.OrderExecutionMode
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.data.model.SwapExecutionState
import com.cleanvault.wallet.data.model.SwapRoute
import com.cleanvault.wallet.data.model.VerifiedToken
import com.cleanvault.wallet.ui.components.ChainPill
import com.cleanvault.wallet.ui.components.MarketStatusIndicator
import com.cleanvault.wallet.ui.theme.ShieldGold
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultRed
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import java.util.Locale

@Composable
fun TradeScreen(
    viewModel: TradeViewModel,
    onBackClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showFromPicker by remember { mutableStateOf(false) }
    var showToPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = VaultTextPrimary
                    )
                }
                Text(
                    text = "Trade & Route",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
            }

            // Market Status Indicator
            if (uiState.isTargetEquityOrEtf) {
                MarketStatusIndicator(
                    status = uiState.marketStatus,
                    formattedNextOpenTime = uiState.formattedNextOpen,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Swap Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VaultDarkSurface)
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // "FROM" Section
                    TradeAssetInputSection(
                        label = "YOU PAY",
                        token = uiState.fromToken,
                        amount = uiState.inputAmountString,
                        onAmountChanged = { viewModel.onAmountChanged(it) },
                        onTokenClick = { showFromPicker = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Swap direction button
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = VaultCardBorder,
                            thickness = 1.dp
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VaultSurfaceHighlight)
                                .border(1.dp, VaultCardBorder, CircleShape)
                                .clickable { viewModel.swapTokensDirection() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = "Swap Direction",
                                tint = VaultGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // "TO" Section
                    val outputAmountStr = uiState.swapRoute?.let {
                        String.format(Locale.US, "%.4f", it.expectedOutputAmount)
                    } ?: "0.0000"

                    TradeAssetOutputSection(
                        label = "YOU RECEIVE (ESTIMATED)",
                        token = uiState.toToken,
                        amount = outputAmountStr,
                        onTokenClick = { showToPicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DUAL-MODE EXECUTION SELECTOR (If target is Stock/ETF and market is Closed)
            if (uiState.isTargetEquityOrEtf && uiState.marketStatus == MarketStatus.EXTENDED_OR_WEEKEND) {
                DualModeExecutionSelector(
                    selectedMode = uiState.executionMode,
                    formattedNextOpen = uiState.formattedNextOpen,
                    onModeSelected = { viewModel.setExecutionMode(it) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Transparent Fee & Routing Breakdown
            if (uiState.swapRoute != null) {
                TradeRoutingCard(route = uiState.swapRoute!!)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic CTA Button
            TradeActionButton(
                state = uiState.executionState,
                marketStatus = uiState.marketStatus,
                executionMode = uiState.executionMode,
                isTargetEquityOrEtf = uiState.isTargetEquityOrEtf,
                validationError = uiState.validationError,
                fromToken = uiState.fromToken,
                toToken = uiState.toToken,
                onExecute = { viewModel.executeTrade() },
                onReset = { viewModel.resetState() }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Modal Pickers
    if (showFromPicker) {
        TokenSelectionModal(
            title = "Select Source Asset",
            tokens = uiState.availableTokens,
            onSelect = {
                viewModel.onFromTokenSelected(it)
                showFromPicker = false
            },
            onDismiss = { showFromPicker = false }
        )
    }

    if (showToPicker) {
        TokenSelectionModal(
            title = "Select Target Stock / Asset",
            tokens = uiState.availableTokens,
            onSelect = {
                viewModel.onToTokenSelected(it)
                showToPicker = false
            },
            onDismiss = { showToPicker = false }
        )
    }
}

@Composable
private fun DualModeExecutionSelector(
    selectedMode: OrderExecutionMode,
    formattedNextOpen: String,
    onModeSelected: (OrderExecutionMode) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "MARKET IS CURRENTLY CLOSED • CHOOSE EXECUTION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ShieldGold,
            letterSpacing = 0.5.sp
        )

        // Option 1: Schedule for Market Open (Recommended)
        ExecutionModeCard(
            title = "Schedule for Market Open (Recommended)",
            subtitle = "Executes $formattedNextOpen • Zero off-hours spread markup.",
            icon = Icons.Default.CalendarToday,
            iconTint = VaultGreen,
            isSelected = selectedMode == OrderExecutionMode.SCHEDULE_FOR_OPEN,
            badge = "OPTIMAL LIQUIDITY",
            onClick = { onModeSelected(OrderExecutionMode.SCHEDULE_FOR_OPEN) }
        )

        // Option 2: Instant 24/7 Execution (High Spread)
        ExecutionModeCard(
            title = "Instant 24/7 Execution",
            subtitle = "Execute immediately against on-chain AMMs • Warning: Liquidity spread is wider (~1.5% - 2.5%).",
            icon = Icons.Default.Bolt,
            iconTint = ShieldGold,
            isSelected = selectedMode == OrderExecutionMode.INSTANT_OFF_HOURS,
            badge = "HIGH SPREAD",
            onClick = { onModeSelected(OrderExecutionMode.INSTANT_OFF_HOURS) }
        )

        if (selectedMode == OrderExecutionMode.INSTANT_OFF_HOURS) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF261D13))
                    .border(1.dp, Color(0xFF6B4B1B), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ShieldGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Off-hours AMM execution incurs wider pool spreads. For minimal price impact, schedule for market open.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = Color(0xFFE3B341)
                )
            }
        }
    }
}

@Composable
private fun ExecutionModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) VaultGreen else VaultCardBorder
    val bgColor = if (isSelected) Color(0xFF13231B) else VaultDarkSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(VaultSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = VaultTextSecondary
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isSelected) Color(0x3300C805) else Color(0x2630363D))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = badge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) VaultGreen else VaultTextMuted
            )
        }
    }
}

@Composable
private fun TradeAssetInputSection(
    label: String,
    token: VerifiedToken?,
    amount: String,
    onAmountChanged: (String) -> Unit,
    onTokenClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicTextField(
                value = amount,
                onValueChange = onAmountChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(VaultGreen),
                textStyle = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = VaultTextPrimary
                ),
                modifier = Modifier.weight(1f)
            )

            TokenSelectorBadge(
                token = token,
                onClick = onTokenClick
            )
        }
    }
}

@Composable
private fun TradeAssetOutputSection(
    label: String,
    token: VerifiedToken?,
    amount: String,
    onTokenClick: () -> Unit
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = amount,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = VaultGreen,
                modifier = Modifier.weight(1f)
            )

            TokenSelectorBadge(
                token = token,
                onClick = onTokenClick
            )
        }
    }
}

@Composable
private fun TokenSelectorBadge(
    token: VerifiedToken?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(VaultSurfaceHighlight)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (token != null) {
            if (token.logoUrl.isNotBlank()) {
                AsyncImage(
                    model = token.logoUrl,
                    contentDescription = token.name,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = token.ticker,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            ChainPill(token.chain)
        } else {
            Text(
                text = "Select",
                fontSize = 14.sp,
                color = VaultTextSecondary
            )
        }
    }
}

@Composable
private fun TradeRoutingCard(route: SwapRoute) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LIQUIDITY ROUTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextMuted
            )
            Text(
                text = route.provider.displayName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultGreen
            )
        }

        Divider(color = VaultCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

        FeeRow("Bridge Protocol Fee", String.format(Locale.US, "$%,.2f", route.fees.bridgeProtocolFeeUsd))
        FeeRow("Platform Integrator Fee (0.85%)", String.format(Locale.US, "$%,.2f", route.fees.platformFeeUsd), isHighlight = true)

        if (route.fees.offHoursLiquiditySpreadUsd > 0) {
            FeeRow("Off-Hours AMM Spread Markup", String.format(Locale.US, "+$%,.2f", route.fees.offHoursLiquiditySpreadUsd), isWarning = true)
        }

        FeeRow("Network Gas Fee", String.format(Locale.US, "$%,.3f", route.fees.networkGasFeeUsd))
        FeeRow("Min. Received (Slippage 0.5%)", "${route.minimumReceivedAmount} ${route.toToken.symbol}")

        if (route.executionMode == OrderExecutionMode.SCHEDULE_FOR_OPEN && route.formattedTargetTime != null) {
            FeeRow("Execution Timing", route.formattedTargetTime, isHighlight = true)
        } else {
            FeeRow("Estimated Duration", "~${route.estimatedDurationSeconds} seconds")
        }
    }
}

@Composable
private fun FeeRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isWarning: Boolean = false
) {
    val valueColor = when {
        isWarning -> ShieldGold
        isHighlight -> VaultGreen
        else -> VaultTextPrimary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isHighlight || isWarning) VaultTextPrimary else VaultTextSecondary,
            fontWeight = if (isHighlight || isWarning) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = valueColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TradeActionButton(
    state: SwapExecutionState,
    marketStatus: MarketStatus,
    executionMode: OrderExecutionMode,
    isTargetEquityOrEtf: Boolean,
    validationError: String?,
    fromToken: VerifiedToken?,
    toToken: VerifiedToken?,
    onExecute: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val buttonText = when {
        isTargetEquityOrEtf && marketStatus == MarketStatus.EXTENDED_OR_WEEKEND && executionMode == OrderExecutionMode.SCHEDULE_FOR_OPEN -> {
            "Schedule for Market Open"
        }
        isTargetEquityOrEtf && marketStatus == MarketStatus.EXTENDED_OR_WEEKEND && executionMode == OrderExecutionMode.INSTANT_OFF_HOURS -> {
            "Buy Now with Slippage Alert"
        }
        else -> "Execute Swap"
    }

    val buttonContainerColor = when {
        isTargetEquityOrEtf && marketStatus == MarketStatus.EXTENDED_OR_WEEKEND && executionMode == OrderExecutionMode.INSTANT_OFF_HOURS -> {
            Color(0xFFE3B341) // Warning Amber
        }
        else -> VaultGreen
    }

    when (state) {
        is SwapExecutionState.Idle, is SwapExecutionState.Quoting -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = VaultSurfaceHighlight)
            ) {
                if (state is SwapExecutionState.Quoting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VaultGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Calculating Route & Liquidity...", color = VaultTextSecondary)
                } else {
                    Text("Enter Amount to Trade", color = VaultTextMuted)
                }
            }
        }
        is SwapExecutionState.QuoteReady -> {
            if (validationError != null) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Color(0xFF2C1517),
                        disabledContentColor = VaultRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = VaultRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = validationError,
                        color = VaultRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onExecute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonContainerColor)
                ) {
                    Text(
                        text = buttonText,
                        color = VaultBlack,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        is SwapExecutionState.Signing -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = VaultSurfaceHighlight)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VaultGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Signing Non-Custodial Intent...", color = VaultTextPrimary)
            }
        }
        is SwapExecutionState.Broadcasting -> {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(disabledContainerColor = VaultSurfaceHighlight)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = VaultGreen)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Broadcasting On-Chain Swap...", color = VaultTextPrimary)
            }
        }
        is SwapExecutionState.Scheduled -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF13231B))
                    .border(1.dp, VaultGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = VaultGreen,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Order Successfully Scheduled",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Executes ${state.order.formattedTargetTime} when NYSE/Nasdaq books open for optimal liquidity.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = VaultTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultGreen)
                ) {
                    Text("View in Portfolio", color = VaultBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
        is SwapExecutionState.Success -> {
            val activeChain = toToken?.chain ?: fromToken?.chain ?: Blockchain.SOLANA
            val truncatedHash = if (state.txHash.length > 14) {
                "${state.txHash.take(6)}...${state.txHash.takeLast(6)}"
            } else {
                state.txHash
            }
            val displaySummary = if (state.swappedSummary.isNotBlank()) {
                state.swappedSummary
            } else {
                "Operación confirmada con éxito"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0E2A1A))
                    .border(1.dp, VaultGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = VaultGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Transacción Confirmada",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displaySummary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = VaultGreen,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Red: ",
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ChainPill(activeChain)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dedicated Hash Display Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(VaultDarkSurface)
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HASH / ID DE TRANSACCIÓN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextMuted
                        )
                        Text(
                            text = truncatedHash,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultTextPrimary
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.txHash))
                            Toast.makeText(context, "ID de Transacción copiado", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar ID de Transacción",
                            tint = VaultGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Copy ID & View Explorer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(state.txHash))
                            Toast.makeText(context, "ID de Transacción copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceHighlight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = VaultTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copiar ID",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultTextPrimary
                        )
                    }

                    Button(
                        onClick = {
                            if (state.explorerUrl.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(state.explorerUrl))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = VaultBlack,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ver en Explorador",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultBlack
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Nueva Operación",
                        fontSize = 13.sp,
                        color = VaultTextSecondary
                    )
                }
            }
        }
        is SwapExecutionState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2C1517))
                    .border(1.dp, VaultRed.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = VaultRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trade Failed",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.message,
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceHighlight)
                ) {
                    Text("Try Again", color = VaultTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun TokenSelectionModal(
    title: String,
    tokens: List<VerifiedToken>,
    onSelect: (VerifiedToken) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(VaultDarkSurface)
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            tokens.forEach { token ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelect(token) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (token.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = token.logoUrl,
                                contentDescription = token.name,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(VaultSurfaceHighlight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = token.ticker.take(3),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = token.ticker,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VaultTextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF388BFD),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                ChainPill(token.chain)
                            }
                            Text(
                                text = "${token.name} • ${token.issuer}",
                                fontSize = 11.sp,
                                color = VaultTextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                Divider(color = VaultCardBorder.copy(alpha = 0.3f), thickness = 0.5.dp)
            }
        }
    }
}
