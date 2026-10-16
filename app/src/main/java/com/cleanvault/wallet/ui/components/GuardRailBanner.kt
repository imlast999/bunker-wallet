package com.cleanvault.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.ui.theme.ShieldGold
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary

@Composable
fun GuardRailBanner(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131F2A))
            .border(1.dp, Color(0xFF1F3D5C), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Guard rail info",
            tint = Color(0xFF58A6FF),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Only deposit SOL / USDC to your Solana address and ETH / USDC to your Robinhood Chain address to fund gas and purchases.",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Color(0xFFC9D1D9)
        )
    }
}

@Composable
fun NonCuratedAssetBanner(
    detectedCount: Int,
    onExportClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF261D13))
            .border(1.dp, Color(0xFF6B4B1B), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Anti-rug shield",
                tint = ShieldGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "CleanVault Anti-Rug Shield Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ShieldGold
                )
                Text(
                    text = "Non-curated asset detected in balance. Export key to external wallet to view and manage.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = VaultTextSecondary
                )
            }
        }

        TextButton(
            onClick = onExportClicked,
            colors = ButtonDefaults.textButtonColors(contentColor = VaultGreen)
        ) {
            Text(
                text = "Export Key",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
