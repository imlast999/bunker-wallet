package com.cleanvault.wallet.ui.screens.explore

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.data.model.AssetCategory
import com.cleanvault.wallet.data.service.AssetMarketData
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
fun ExploreScreen(
    viewModel: ExploreViewModel,
    onNavigateToAssetDetail: (tokenId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .padding(top = 16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Explore Verified RWAs",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Regulated tokenized stocks, index funds & institutional yield",
                fontSize = 12.sp,
                color = VaultTextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categorized Tabs
        CategoryTabsRow(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Asset Market List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.filteredMarketData) { item ->
                MarketDataRowItem(
                    data = item,
                    onClick = { onNavigateToAssetDetail(item.token.id) }
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
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = VaultTextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search AAPL, NVDA, S&P 500, Ondo USDY...",
                    fontSize = 13.sp,
                    color = VaultTextMuted
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(VaultGreen),
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = VaultTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryTabsRow(
    selectedCategory: AssetCategory,
    onCategorySelected: (AssetCategory) -> Unit
) {
    val categories = listOf(
        AssetCategory.US_STOCK,
        AssetCategory.ETF_INDEX,
        AssetCategory.TREASURY_BOND,
        AssetCategory.COMMODITY,
        AssetCategory.YIELD_STABLE,
        AssetCategory.CORE_CRYPTO
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(modifier = Modifier.width(8.dp)) }
        items(categories) { category ->
            val isSelected = category == selectedCategory
            val bg = if (isSelected) VaultGreen else VaultDarkSurface
            val textColor = if (isSelected) VaultBlack else VaultTextSecondary
            val border = if (isSelected) VaultGreen else VaultCardBorder

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .border(1.dp, border, RoundedCornerShape(20.dp))
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = category.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
        item { Spacer(modifier = Modifier.width(8.dp)) }
    }
}

@Composable
fun MarketDataRowItem(
    data: AssetMarketData,
    onClick: () -> Unit
) {
    val token = data.token
    val isPositive = data.change24hPercent >= 0
    val trendColor = if (isPositive) VaultGreen else VaultRed
    val prefix = if (isPositive) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left Column: Avatar & Details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.3f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VaultSurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                if (token.logoUrl.isNotBlank()) {
                    coil.compose.AsyncImage(
                        model = token.logoUrl,
                        contentDescription = token.name,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        text = token.ticker.take(3),
                        fontSize = 12.sp,
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Token",
                        tint = Color(0xFF388BFD),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    ChainPill(token.chain)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${token.name} • ${token.issuer}",
                    fontSize = 11.sp,
                    color = VaultTextSecondary,
                    maxLines = 1
                )
            }
        }

        // Middle: Tiny Sparkline
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(30.dp)
                .padding(horizontal = 4.dp)
        ) {
            SparklineChart(
                points = data.sparklinePoints,
                isPositive = isPositive,
                height = 30.dp,
                showGradientFill = false
            )
        }

        // Right Column: Price & Change
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(0.9f)
        ) {
            Text(
                text = String.format(Locale.US, "$%,.2f", data.currentPriceUsd),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = VaultTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = String.format(Locale.US, "%s%.2f%%", prefix, data.change24hPercent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = trendColor
            )
        }
    }
}
