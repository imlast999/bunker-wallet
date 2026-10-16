package com.cleanvault.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.data.model.MarketStatus
import com.cleanvault.wallet.ui.theme.ShieldGold
import com.cleanvault.wallet.ui.theme.VaultGreen

@Composable
fun MarketStatusIndicator(
    status: MarketStatus,
    formattedNextOpenTime: String = "",
    modifier: Modifier = Modifier
) {
    val isOpen = status == MarketStatus.OPEN
    val dotColor = if (isOpen) VaultGreen else ShieldGold
    val bgColor = if (isOpen) Color(0x2600C805) else Color(0x26D29922)
    val borderColor = if (isOpen) Color(0x4D00C805) else Color(0x4DD29922)
    val textLabel = if (isOpen) {
        "Market Open (NYSE/Nasdaq)"
    } else {
        if (formattedNextOpenTime.isNotBlank()) "Market Closed (Opens $formattedNextOpenTime)" else "Market Closed"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = textLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor
        )
    }
}
