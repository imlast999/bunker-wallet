package com.cleanvault.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultGreenSubtle
import com.cleanvault.wallet.ui.theme.VaultRed
import com.cleanvault.wallet.ui.theme.VaultRedSubtle
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import java.util.Locale

@Composable
fun BalanceCard(
    totalUsd: Double,
    change24hUsd: Double,
    change24hPercent: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = change24hUsd >= 0
    val trendColor = if (isPositive) VaultGreen else VaultRed
    val trendBg = if (isPositive) VaultGreenSubtle else VaultRedSubtle
    val prefix = if (isPositive) "+" else ""

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Total Investing Balance",
            fontSize = 13.sp,
            color = VaultTextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "$%,.2f", totalUsd),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextPrimary,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(trendBg)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = String.format(Locale.US, "%s$%,.2f (%s%.2f%%) Today", prefix, change24hUsd, prefix, change24hPercent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = trendColor
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionRow(
    onDepositClicked: () -> Unit,
    onTradeClicked: () -> Unit,
    onSendClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionButton(
            label = "Deposit",
            icon = Icons.Default.CallReceived,
            onClick = onDepositClicked,
            modifier = Modifier.weight(1f)
        )
        ActionButton(
            label = "Trade / Swap",
            icon = Icons.Default.SwapHoriz,
            onClick = onTradeClicked,
            isPrimary = true,
            modifier = Modifier.weight(1.2f)
        )
        ActionButton(
            label = "Send",
            icon = Icons.Default.CallMade,
            onClick = onSendClicked,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val bgColor = if (isPrimary) VaultGreen else VaultDarkSurface
    val contentColor = if (isPrimary) Color(0xFF0D1117) else VaultTextPrimary
    val borderColor = if (isPrimary) VaultGreen else Color(0xFF30363D)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = contentColor
        )
    }
}
