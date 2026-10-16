package com.cleanvault.wallet

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.cleanvault.wallet.ui.CleanVaultApp
import com.cleanvault.wallet.ui.theme.CleanVaultTheme
import com.cleanvault.wallet.ui.theme.VaultBlack

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VaultBlack
                ) {
                    CleanVaultApp()
                }
            }
        }
    }
}
