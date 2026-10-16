package com.cleanvault.wallet.ui.screens.assetdetail

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.ui.components.ChainPill
import com.cleanvault.wallet.ui.components.SparklineChart
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
fun AssetDetailScreen(
    viewModel: AssetDetailViewModel,
    onBackClicked: () -> Unit,
    onTradeClicked: (tokenId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (uiState.isLoading || uiState.token == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = VaultGreen)
        }
        return
    }

    val token = uiState.token!!
    val market = uiState.marketData
    val balance = uiState.userBalance
    val isPositive = (market?.change24hPercent ?: 0.0) >= 0
    val trendColor = if (isPositive) VaultGreen else VaultRed
    val prefix = if (isPositive) "+" else ""

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = VaultTextPrimary
                )
            }
            Text(
                text = "${token.symbol} • ${token.name}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Price & 24h performance
            Column {
                Text(
                    text = String.format(Locale.US, "$%,.2f", market?.currentPriceUsd ?: 0.0),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.US, "%s%.2f%% Today", prefix, market?.change24hPercent ?: 0.0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = trendColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ChainPill(token.chain)
                    }

                    if (token.category.name.contains("STOCK") || token.category.name.contains("ETF")) {
                        val marketHoursManager = com.cleanvault.wallet.core.market.MarketHoursManager()
                        val status = marketHoursManager.getCurrentMarketStatus(token.category)
                        val formattedNextOpen = marketHoursManager.getFormattedNextOpenTime()
                        com.cleanvault.wallet.ui.components.MarketStatusIndicator(
                            status = status,
                            formattedNextOpenTime = formattedNextOpen
                        )
                    }
                }
            }

            // High-resolution Sparkline
            if (market?.sparklinePoints?.isNotEmpty() == true) {
                SparklineChart(
                    points = market.sparklinePoints,
                    isPositive = isPositive,
                    height = 140.dp
                )
            }

            // Quick Trade Action CTA
            Button(
                onClick = { onTradeClicked(token.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = VaultBlack
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Trade ${token.symbol}",
                    color = VaultBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // User Holdings Card (if any)
            if (balance != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(VaultDarkSurface)
                        .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "YOUR POSITION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Shares / Units Owned",
                            fontSize = 13.sp,
                            color = VaultTextSecondary
                        )
                        Text(
                            text = String.format(Locale.US, "%.4f %s", balance.formattedBalance, token.symbol),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultTextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Market Valuation",
                            fontSize = 13.sp,
                            color = VaultTextSecondary
                        )
                        Text(
                            text = String.format(Locale.US, "$%,.2f", balance.totalUsdValue),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VaultTextPrimary
                        )
                    }
                }
            }

            // Regulated Institutional Proof of Reserve Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(VaultDarkSurface)
                    .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF388BFD),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VERIFIED UNDERLYING ASSET",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388BFD)
                    )
                }

                Divider(color = VaultCardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)

                InfoRow("Regulated Issuer", token.issuer)
                InfoRow("Underlying Collateral", token.underlyingAsset)
                InfoRow("Settlement Chain", token.chain.displayName)

                // Contract Address
                Column {
                    Text(
                        text = "Token Contract Address",
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VaultSurfaceHighlight)
                            .clickable {
                                clipboard.setText(AnnotatedString(token.address))
                                Toast.makeText(context, "Contract address copied", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${token.address.take(12)}...${token.address.takeLast(10)}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = VaultTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = VaultTextSecondary
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = VaultTextPrimary
        )
    }
}
