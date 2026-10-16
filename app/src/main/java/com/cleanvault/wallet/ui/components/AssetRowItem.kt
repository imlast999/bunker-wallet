package com.cleanvault.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.data.model.Blockchain
import com.cleanvault.wallet.data.model.TokenBalance
import com.cleanvault.wallet.ui.theme.ArbitrumBlue
import com.cleanvault.wallet.ui.theme.SolanaPurple
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultRed
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import java.util.Locale

@Composable
fun AssetRowItem(
    balance: TokenBalance,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val token = balance.token
    val isPositive = balance.change24hPercent >= 0
    val trendColor = if (isPositive) VaultGreen else VaultRed
    val prefix = if (isPositive) "+" else ""

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Avatar + Names + Verification
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (token.logoUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = token.logoUrl,
                        contentDescription = token.name,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = token.ticker.take(3),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = token.ticker,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    if (token.isVerified) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Token",
                            tint = Color(0xFF388BFD),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    ChainPill(token.chain)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${token.name} • ${token.issuer}",
                    fontSize = 12.sp,
                    color = VaultTextSecondary,
                    maxLines = 1
                )
            }
        }

        // Right: Balances and 24h change
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = String.format(Locale.US, "$%,.2f", balance.totalUsdValue),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.US, "%.4f %s", balance.formattedBalance, token.symbol),
                    fontSize = 11.sp,
                    color = VaultTextMuted
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format(Locale.US, "%s%.2f%%", prefix, balance.change24hPercent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
fun ChainPill(chain: Blockchain) {
    val (bgColor, textColor, label) = when (chain) {
        Blockchain.SOLANA -> Triple(Color(0x269945FF), SolanaPurple, "SOL")
        Blockchain.ROBINHOOD_CHAIN -> Triple(Color(0x2628A0F0), ArbitrumBlue, "RH")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
