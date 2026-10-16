package com.cleanvault.wallet.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.cleanvault.wallet.ui.theme.ArbitrumBlue
import com.cleanvault.wallet.ui.theme.SolanaPurple
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary

@Composable
fun AddressBadgeRow(
    solanaAddress: String,
    evmAddress: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ACTIVE VAULT DEPOSIT ADDRESSES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextMuted,
            letterSpacing = 0.8.sp
        )

        // Solana Address Badge
        SingleAddressItem(
            networkName = "Solana",
            tag = "Deposit SOL or USDC-SPL",
            badgeColor = SolanaPurple,
            fullAddress = solanaAddress,
            onCopy = {
                clipboardManager.setText(AnnotatedString(solanaAddress))
                Toast.makeText(context, "Solana address copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )

        // Robinhood Chain EVM Address Badge
        SingleAddressItem(
            networkName = "Robinhood Chain",
            tag = "Deposit ETH or USDC-ERC20",
            badgeColor = ArbitrumBlue,
            fullAddress = evmAddress,
            onCopy = {
                clipboardManager.setText(AnnotatedString(evmAddress))
                Toast.makeText(context, "Robinhood Chain address copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SingleAddressItem(
    networkName: String,
    tag: String,
    badgeColor: Color,
    fullAddress: String,
    onCopy: () -> Unit
) {
    val truncated = if (fullAddress.length > 12) {
        "${fullAddress.take(6)}...${fullAddress.takeLast(5)}"
    } else {
        fullAddress.ifEmpty { "Generating address..." }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF21262D))
            .clickable { onCopy() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = networkName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultTextPrimary
                )
                Text(
                    text = tag,
                    fontSize = 11.sp,
                    color = VaultTextSecondary
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = truncated,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Address",
                tint = VaultTextSecondary,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
