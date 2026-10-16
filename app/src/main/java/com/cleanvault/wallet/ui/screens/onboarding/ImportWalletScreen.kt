package com.cleanvault.wallet.ui.screens.onboarding

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.core.crypto.WalletManager
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch

@Composable
fun ImportWalletScreen(
    walletManager: WalletManager,
    onBackClicked: () -> Unit,
    onWalletImported: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var mnemonicInput by remember { mutableStateOf("") }
    var solanaKeyInput by remember { mutableStateOf("") }
    var evmKeyInput by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }

    val wordCount = remember(mnemonicInput) {
        mnemonicInput.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Navigation Top
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VaultDarkSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Atrás",
                    tint = VaultTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Importar Bóveda",
                    color = VaultTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restaura tus cuentas existentes",
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tabs: Frase Semilla vs Claves Privadas
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = VaultDarkSurface,
            contentColor = VaultGreen,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = VaultGreen,
                    height = 2.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, VaultCardBorder, RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = {
                    Text(
                        text = "Frase Semilla (12/24)",
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTabIndex == 0) VaultGreen else VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = {
                    Text(
                        text = "Claves Privadas",
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTabIndex == 1) VaultGreen else VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedTabIndex == 0) {
            // Mnemonic Phrase Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Pega o escribe tu frase de recuperación separada por espacios:",
                    color = VaultTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mnemonicInput,
                    onValueChange = { mnemonicInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = {
                        Text(
                            text = "abandon ability able about above absent absorb abstract absurd abuse access accident...",
                            color = VaultTextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultDarkSurface,
                        unfocusedContainerColor = VaultDarkSurface,
                        focusedBorderColor = VaultGreen,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Palabras detectadas: $wordCount",
                        color = if (wordCount == 12 || wordCount == 24) VaultGreen else VaultTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "BIP-39 Estándar",
                        color = VaultTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Raw Private Keys Input
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Solana Private Key (Phantom 64-byte Base58 o JSON array):",
                    color = VaultTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = solanaKeyInput,
                    onValueChange = { solanaKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "5Mii... (Phantom Base58) o [12, 45, ...]",
                            color = VaultTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultDarkSurface,
                        unfocusedContainerColor = VaultDarkSurface,
                        focusedBorderColor = VaultGreen,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Robinhood Chain / Arbitrum EVM Private Key (Hexadecimal):",
                    color = VaultTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = evmKeyInput,
                    onValueChange = { evmKeyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "0x4a1f... o 64 caracteres hex",
                            color = VaultTextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultDarkSurface,
                        unfocusedContainerColor = VaultDarkSurface,
                        focusedBorderColor = VaultGreen,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Import Button
        val canImport = if (selectedTabIndex == 0) {
            wordCount == 12 || wordCount == 24
        } else {
            solanaKeyInput.isNotBlank() && evmKeyInput.isNotBlank()
        }

        Button(
            onClick = {
                if (canImport && !isImporting) {
                    isImporting = true
                    scope.launch {
                        val result = if (selectedTabIndex == 0) {
                            walletManager.importFromMnemonic(mnemonicInput.trim())
                        } else {
                            walletManager.importFromPrivateKeys(solanaKeyInput.trim(), evmKeyInput.trim())
                        }
                        isImporting = false
                        if (result.isSuccess) {
                            Toast.makeText(context, "Bóveda importada exitosamente", Toast.LENGTH_SHORT).show()
                            onWalletImported()
                        } else {
                            Toast.makeText(
                                context,
                                "Error al importar: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            enabled = canImport && !isImporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultGreen,
                disabledContainerColor = VaultDarkSurface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isImporting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (canImport) Color.Black else VaultTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Importar y Continuar",
                        color = if (canImport) Color.Black else VaultTextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
