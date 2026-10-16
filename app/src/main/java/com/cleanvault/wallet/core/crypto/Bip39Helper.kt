package com.cleanvault.wallet.core.crypto

import org.web3j.crypto.MnemonicUtils
import java.security.SecureRandom

/**
 * Clean BIP-39 Mnemonic Generator and Validator using standard 2048-word English dictionary.
 * Ensures 100% interoperability with Phantom, MetaMask, Solflare, and Rabby.
 */
object Bip39Helper {

    fun generateMnemonic(wordCount: Int = 12): String {
        require(wordCount == 12 || wordCount == 24) { "Mnemonic must be 12 or 24 words" }
        val entropyBytes = if (wordCount == 12) 16 else 32
        val entropy = ByteArray(entropyBytes)
        SecureRandom().nextBytes(entropy)
        return MnemonicUtils.generateMnemonic(entropy)
    }

    fun validateMnemonic(mnemonic: String): Boolean {
        val clean = mnemonic.trim().lowercase().replace("\\s+".toRegex(), " ")
        val words = clean.split(" ")
        if (words.size != 12 && words.size != 24) return false
        return try {
            MnemonicUtils.validateMnemonic(clean)
        } catch (e: Exception) {
            false
        }
    }

    fun mnemonicToSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val clean = mnemonic.trim().lowercase().replace("\\s+".toRegex(), " ")
        return MnemonicUtils.generateSeed(clean, passphrase)
    }
}
