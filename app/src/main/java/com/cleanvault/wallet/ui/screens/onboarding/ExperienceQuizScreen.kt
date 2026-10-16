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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanvault.wallet.core.security.SecureStorage
import com.cleanvault.wallet.data.remote.neon.NeonSyncRepository
import com.cleanvault.wallet.ui.theme.VaultBlack
import com.cleanvault.wallet.ui.theme.VaultCardBorder
import com.cleanvault.wallet.ui.theme.VaultDarkSurface
import com.cleanvault.wallet.ui.theme.VaultGreen
import com.cleanvault.wallet.ui.theme.VaultGreenSubtle
import com.cleanvault.wallet.ui.theme.VaultSurfaceHighlight
import com.cleanvault.wallet.ui.theme.VaultTextMuted
import com.cleanvault.wallet.ui.theme.VaultTextPrimary
import com.cleanvault.wallet.ui.theme.VaultTextSecondary
import kotlinx.coroutines.launch

enum class ExperienceLevel(
    val id: String,
    val title: String,
    val badge: String,
    val description: String,
    val adaptationNote: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    BEGINNER(
        id = "BEGINNER",
        title = "Principiante",
        badge = "Modo Guiado",
        description = "Es mi primera vez o sé muy poco.",
        adaptationNote = "Activa explicaciones sobre RWAs, slippage, comisiones de gas (SOL/ETH) y protección antispam.",
        icon = Icons.Default.School
    ),
    INTERMEDIATE(
        id = "INTERMEDIATE",
        title = "Intermedio",
        badge = "Modo Inversor",
        description = "Conozco wallets y he comprado criptomonedas.",
        adaptationNote = "Alertas equilibradas de colateral RWA, horarios de mercado bursátil (NYSE) y órdenes programadas.",
        icon = Icons.Default.TrendingUp
    ),
    ADVANCED(
        id = "ADVANCED",
        title = "Avanzado / Degen",
        badge = "Modo Pro",
        description = "Uso DeFi a diario y entiendo contratos inteligentes.",
        adaptationNote = "Interfaz minimalista pro, direcciones de contrato directas, enlaces a exploradores y sin tooltips básicos.",
        icon = Icons.Default.ElectricBolt
    )
}

@Composable
fun ExperienceQuizScreen(
    secureStorage: SecureStorage,
    neonSyncRepository: NeonSyncRepository,
    onQuizCompleted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedLevel by remember { mutableStateOf(ExperienceLevel.BEGINNER) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBlack)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Step Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(VaultGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PASO 2 DE 2 • PERSONALIZACIÓN",
                    color = VaultGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "¿Cuál es tu nivel de experiencia en Cripto y Web3?",
                color = VaultTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Adaptaremos la interfaz, los avisos de riesgo y la información técnica a tu perfil de inversor.",
                color = VaultTextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Options Cards
            ExperienceLevel.values().forEach { level ->
                val isSelected = selectedLevel == level
                ExperienceLevelCard(
                    level = level,
                    isSelected = isSelected,
                    onClick = { selectedLevel = level }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // Bottom Continue Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    if (!isSubmitting) {
                        isSubmitting = true
                        scope.launch {
                            // 1. Save locally in encrypted storage
                            secureStorage.userExperienceLevel = selectedLevel.id
                            secureStorage.hasCompletedOnboarding = true

                            // 2. Sync public profile to Neon PostgreSQL (non-custodial: only public addresses & level)
                            val evmAddr = secureStorage.getEvmAddress() ?: ""
                            val solAddr = secureStorage.getSolanaAddress() ?: ""
                            if (evmAddr.isNotBlank() && solAddr.isNotBlank()) {
                                try {
                                    neonSyncRepository.syncUserProfile(
                                        evmAddress = evmAddr,
                                        solanaAddress = solAddr,
                                        experienceLevel = selectedLevel.id
                                    )
                                } catch (e: Exception) {
                                    // Fail open: local state is source of truth
                                }
                            }

                            isSubmitting = false
                            onQuizCompleted()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultGreen),
                shape = RoundedCornerShape(14.dp),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Entrar a mi Bóveda Bunker",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceLevelCard(
    level: ExperienceLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) VaultDarkSurface else VaultSurfaceHighlight.copy(alpha = 0.5f))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) VaultGreen else VaultCardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) VaultGreenSubtle else VaultSurfaceHighlight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = level.icon,
                            contentDescription = null,
                            tint = if (isSelected) VaultGreen else VaultTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = level.title,
                            color = VaultTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = level.description,
                            color = VaultTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) VaultGreenSubtle else VaultDarkSurface)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = level.badge,
                        color = if (isSelected) VaultGreen else VaultTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Adaptation Note
            Text(
                text = level.adaptationNote,
                color = if (isSelected) VaultTextPrimary.copy(alpha = 0.9f) else VaultTextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
