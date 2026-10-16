package com.cleanvault.wallet.ui.screens.security

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.fragment.app.FragmentActivity
import com.cleanvault.wallet.core.crypto.VaultExportBundle
import com.cleanvault.wallet.ui.theme.ArbitrumBlue
import com.cleanvault.wallet.ui.theme.ShieldGold
import com.cleanvault.wallet.ui.theme.SolanaPurple
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultRed
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary

@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    onBackClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

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
                text = "Security & Key Vault",
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
            // Vault Security Enclave Status Card
            HardwareSecurityStatusCard(isBackedUp = uiState.vaultState.isBackedUp)

            // Biometric Authentication Toggle
            BiometricSettingsCard(
                enabled = uiState.vaultState.biometricsActive,
                onToggle = { viewModel.toggleBiometrics(it) }
            )

            // Non-Custodial Key Export Section
            if (!uiState.isExportRevealed) {
                ExportActionCard(
                    onExportClick = {
                        if (activity != null) {
                            viewModel.requestExportWithBiometrics(activity)
                        } else {
                            Toast.makeText(context, "Activity context required for biometrics", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                ExportedKeysView(
                    bundle = uiState.exportBundle!!,
                    onHide = { viewModel.hideExportDetails() }
                )
            }

            // Custom RPC Endpoints
            RpcConfigCard(
                solanaRpc = uiState.solanaRpc,
                rhChainRpc = uiState.robinhoodChainRpc
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun HardwareSecurityStatusCard(isBackedUp: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x2600C805)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = VaultGreen,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Android Keystore Active",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = if (isBackedUp) "Vault backed up & encrypted with AES-256-GCM" else "Backup recommended: Keys stored locally in hardware enclave",
                fontSize = 11.sp,
                color = if (isBackedUp) VaultGreen else ShieldGold
            )
        }
    }
}

@Composable
private fun BiometricSettingsCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = VaultTextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Biometric Lock",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultTextPrimary
                )
                Text(
                    text = "Require fingerprint/face to view keys or sign",
                    fontSize = 11.sp,
                    color = VaultTextSecondary
                )
            }
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VaultBlack,
                checkedTrackColor = VaultGreen,
                uncheckedThumbColor = VaultTextSecondary,
                uncheckedTrackColor = VaultSurfaceHighlight
            )
        )
    }
}

@Composable
private fun ExportActionCard(
    onExportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VaultDarkSurface)
            .border(1.dp, VaultCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = ShieldGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Export Keys & Recovery Phrase",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Exporting your keys allows you to view non-curated tokens, import into Phantom (Solana) or MetaMask (Robinhood Chain), or migrate your vault anytime.",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = VaultTextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onExportClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D))
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                tint = VaultGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Authenticate to Reveal Keys",
                color = VaultTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ExportedKeysView(
    bundle: VaultExportBundle,
    onHide: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF191F26))
            .border(1.dp, ShieldGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SECRET RECOVERY PHRASE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ShieldGold
            )
            IconButton(onClick = onHide) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = "Hide keys",
                    tint = VaultTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Mnemonic tiles
        if (bundle.mnemonic != null) {
            val words = bundle.mnemonic.split(" ")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (chunk in words.chunked(3)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (w in chunk) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VaultDarkSurface)
                                    .border(0.5.dp, VaultCardBorder, RoundedCornerShape(6.dp))
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = w,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = VaultTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Divider(color = VaultCardBorder, thickness = 0.5.dp)

        // Solana Ed25519 Private Key
        KeyExportItem(
            networkTitle = "Solana Private Key (Phantom / Solflare)",
            badgeColor = SolanaPurple,
            privateKey = bundle.solanaPrivateKey,
            onCopy = {
                clipboard.setText(AnnotatedString(bundle.solanaPrivateKey))
                Toast.makeText(context, "Solana private key copied", Toast.LENGTH_SHORT).show()
            }
        )

        Divider(color = VaultCardBorder, thickness = 0.5.dp)

        // Robinhood Chain EVM secp256k1 Key
        KeyExportItem(
            networkTitle = "Robinhood Chain Private Key (MetaMask / Rabby)",
            badgeColor = ArbitrumBlue,
            privateKey = bundle.evmPrivateKey,
            onCopy = {
                clipboard.setText(AnnotatedString(bundle.evmPrivateKey))
                Toast.makeText(context, "Robinhood Chain EVM key copied", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun KeyExportItem(
    networkTitle: String,
    badgeColor: Color,
    privateKey: String,
    onCopy: () -> Unit
) {
    Column {
        Text(
            text = networkTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = badgeColor
        )
        if (networkTitle.contains("Solana", ignoreCase = true)) {
            Text(
                text = "Formato Base58 estándar de 64 bytes (semilla + pubkey). 100% compatible con Phantom y Solflare.",
                fontSize = 10.sp,
                color = VaultTextMuted,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(VaultBlack)
                .clickable { onCopy() }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${privateKey.take(12)}...${privateKey.takeLast(8)}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = VaultTextPrimary
            )
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Key",
                tint = VaultTextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RpcConfigCard(
    solanaRpc: String,
    rhChainRpc: String
) {
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
            text = "NETWORK RPC ENDPOINTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = VaultTextMuted
        )
        Text(
            text = "Solana: $solanaRpc",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = VaultTextSecondary
        )
        Text(
            text = "Robinhood Chain: $rhChainRpc",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = VaultTextSecondary
        )
    }
}
