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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.core.crypto.Bip39Helper
import com.cleanvault.wallet.core.crypto.WalletManager
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultRed
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch

@Composable
fun CreateWalletScreen(
    walletManager: WalletManager,
    onBackClicked: () -> Unit,
    onWalletCreated: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var mnemonicWords by rememberSaveable {
        mutableStateOf(Bip39Helper.generateMnemonic(12).split(" "))
    }
    var isConfirmed by rememberSaveable { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var areWordsHidden by rememberSaveable { mutableStateOf(false) }

    val fullMnemonicString = remember(mnemonicWords) { mnemonicWords.joinToString(" ") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
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
                    text = "Crea tu Bóveda Fría",
                    color = VaultTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paso 1 de 2: Respaldo Secreto",
                    color = VaultGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Instructions
        Text(
            text = "Escribe estas 12 palabras en un lugar físico seguro. Bunker no custodia tus claves; sin estas palabras, nadie podrá recuperar tus fondos.",
            color = VaultTextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Actions Bar for Mnemonic (Copy / Hide)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Frase Semilla (BIP-39)",
                color = VaultTextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row {
                IconButton(
                    onClick = { areWordsHidden = !areWordsHidden },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (areWordsHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Ocultar palabras",
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(fullMnemonicString))
                        Toast.makeText(context, "Frase copiada al portapapeles", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar frase",
                        tint = VaultGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 12 Words Grid Layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(VaultDarkSurface)
                .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            val chunks = mnemonicWords.chunked(3)
            chunks.forEachIndexed { rowIndex, rowWords ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowWords.forEachIndexed { colIndex, word ->
                        val wordIndex = rowIndex * 3 + colIndex + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VaultSurfaceHighlight)
                                .border(0.5.dp, VaultCardBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$wordIndex.",
                                    color = VaultTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = if (areWordsHidden) "••••••" else word,
                                    color = VaultTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
                if (rowIndex < chunks.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Security Warning Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x15FF5252))
                .border(1.dp, Color(0x33FF5252), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = VaultRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Bunker es una bóveda 100% no custodia. El equipo nunca te pedirá esta frase y no puede recuperar fondos si la pierdes.",
                    color = VaultTextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Checkbox confirmation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { isConfirmed = !isConfirmed }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isConfirmed,
                onCheckedChange = { isConfirmed = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = VaultGreen,
                    uncheckedColor = VaultTextSecondary,
                    checkmarkColor = Color.Black
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "He anotado mi frase secreta y entiendo mi responsabilidad de custodia.",
                color = if (isConfirmed) VaultTextPrimary else VaultTextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isConfirmed) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = {
                if (isConfirmed && !isCreating) {
                    isCreating = true
                    scope.launch {
                        val result = walletManager.importFromMnemonic(fullMnemonicString)
                        isCreating = false
                        if (result.isSuccess) {
                            onWalletCreated()
                        } else {
                            Toast.makeText(
                                context,
                                "Error al inicializar la bóveda: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            },
            enabled = isConfirmed && !isCreating,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VaultGreen,
                disabledContainerColor = VaultDarkSurface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isCreating) {
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
                        tint = if (isConfirmed) Color.Black else VaultTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Crear Bóveda y Continuar",
                        color = if (isConfirmed) Color.Black else VaultTextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
