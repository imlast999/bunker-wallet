package com.cleanvault.wallet.ui.screens.portfolio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.data.model.ScheduledOrder
import com.cleanvault.wallet.ui.components.AddressBadgeRow
import com.cleanvault.wallet.ui.components.AssetRowItem
import com.cleanvault.wallet.ui.components.BalanceCard
import com.cleanvault.wallet.ui.components.ChainPill
import com.cleanvault.wallet.ui.components.GuardRailBanner
import com.cleanvault.wallet.ui.components.NonCuratedAssetBanner
import com.cleanvault.wallet.ui.components.QuickActionRow
import com.cleanvault.wallet.ui.components.SparklineChart
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
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    onNavigateToTrade: (fromTokenId: String?) -> Unit,
    onNavigateToAssetDetail: (tokenId: String) -> Unit,
    onNavigateToSecurity: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
    ) {
        // Top App Bar
        PortfolioTopBar(
            onSecurityClick = onNavigateToSecurity
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Balance card
            item {
                BalanceCard(
                    totalUsd = summary.totalUsdValue,
                    change24hUsd = summary.change24hUsd,
                    change24hPercent = summary.change24hPercent
                )
            }

            // 24h Portfolio Curve
            item {
                SparklineChart(
                    points = summary.sparklinePoints,
                    isPositive = summary.change24hUsd >= 0,
                    height = 100.dp
                )
            }

            // Quick Actions: [Deposit], [Trade / Swap], [Send]
            item {
                QuickActionRow(
                    onDepositClicked = { /* Opens address copy / QR view */ },
                    onTradeClicked = { onNavigateToTrade(null) },
                    onSendClicked = { /* Opens Send Modal */ }
                )
            }

            // Contextual Guard Rail Banner
            item {
                GuardRailBanner()
            }

            // Core Active Addresses (Solana + Robinhood Chain)
            item {
                AddressBadgeRow(
                    solanaAddress = uiState.solanaAddress,
                    evmAddress = uiState.evmAddress
                )
            }

            // Anti-Rug / Non-Curated Asset Banner (Discreet badge)
            if (uiState.nonCuratedNotice != null) {
                item {
                    NonCuratedAssetBanner(
                        detectedCount = uiState.nonCuratedNotice!!.detectedCount,
                        onExportClicked = onNavigateToSecurity
                    )
                }
            }

            // PENDING SCHEDULED ORDERS SECTION (Collapsible Card)
            if (uiState.pendingScheduledOrders.isNotEmpty()) {
                item {
                    PendingScheduledOrdersCard(
                        orders = uiState.pendingScheduledOrders,
                        onCancelOrder = { viewModel.cancelScheduledOrder(it) }
                    )
                }
            }

            // Owned Verified Assets Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VERIFIED HOLDINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${summary.verifiedBalances.size} Assets",
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                }
            }

            // List of owned verified assets
            items(summary.verifiedBalances) { balance ->
                AssetRowItem(
                    balance = balance,
                    onClick = { onNavigateToAssetDetail(balance.token.id) }
                )
                Divider(color = VaultCardBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PendingScheduledOrdersCard(
    orders: List<ScheduledOrder>,
    onCancelOrder: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF131D18))
            .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        // Card Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x3300C805)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = VaultGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${orders.size} Order${if (orders.size > 1) "s" else ""} Scheduled for Market Open",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Pre-market staged intent • Locked nominal spread",
                        fontSize = 11.sp,
                        color = VaultGreen
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = VaultTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // Expandable List of Orders
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Divider(color = Color(0xFF1E3A2B), thickness = 0.5.dp)

                orders.forEach { order ->
                    ScheduledOrderItemRow(
                        order = order,
                        onCancel = { onCancelOrder(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledOrderItemRow(
    order: ScheduledOrder,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16251E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Buy ${order.toTokenSymbol}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                ChainPill(order.toChain)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${String.format(Locale.US, "$%,.2f", order.amountIn)} ${order.fromTokenSymbol} → ~${String.format(Locale.US, "%.4f", order.estimatedAmountOut)} ${order.toTokenSymbol}",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Target: ${order.formattedTargetTime}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ShieldGold
            )
        }

        TextButton(
            onClick = onCancel,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = VaultRed)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel Order",
                tint = VaultRed,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Cancel",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PortfolioTopBar(
    onSecurityClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(VaultGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = VaultBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column {
                Text(
                    text = "Bunker",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Text(
                    text = "The Cold Vault for RWAs",
                    fontSize = 11.sp,
                    color = VaultGreen
                )
            }
        }

        IconButton(onClick = onSecurityClick) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Security & Settings",
                tint = VaultTextSecondary
            )
        }
    }
}
