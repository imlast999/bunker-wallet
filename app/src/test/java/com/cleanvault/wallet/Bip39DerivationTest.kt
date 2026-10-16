package com.cleanvault.wallet

import com.cleanvault.wallet.core.crypto.Bip39Helper
import com.cleanvault.wallet.core.crypto.KeyDerivation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Bip39DerivationTest {

    @Test
    fun testMnemonicGenerationAndWordCount() {
        val mnemonic = Bip39Helper.generateMnemonic()
        val words = mnemonic.trim().split(Regex("\\s+"))
        assertEquals("Mnemonic must consist of exactly 12 words", 12, words.size)
        assertTrue("Mnemonic validation must pass", Bip39Helper.validateMnemonic(mnemonic))
    }

    @Test
    fun testKnownMnemonicDerivation() {
        // Standard test vector: 12 words
        val mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        val seed = Bip39Helper.mnemonicToSeed(mnemonic)
        assertEquals("PBKDF2 HMAC-SHA512 seed must be 64 bytes (512 bits)", 64, seed.size)

        // Test Robinhood Chain (EVM standard m/44'/60'/0'/0/0)
        val evmKey = KeyDerivation.deriveRobinhoodChainKey(seed)
        assertNotNull(evmKey.address)
        assertTrue("EVM address must start with 0x", evmKey.address.startsWith("0x"))
        assertEquals("EVM address length must be 42 characters", 42, evmKey.address.length)
        // Standard BIP-44 address for 'abandon ... about' on Ethereum
        // m/44'/60'/0'/0/0 -> 0x9858EfFD232B4033E47d90003D41EC34EcaEda94
        assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", evmKey.address)

        // Test Solana SLIP-0010 Ed25519 derivation
        val solanaKey = KeyDerivation.deriveSolanaKey(seed)
        assertNotNull(solanaKey.address)
        assertTrue("Solana address must be valid Base58", solanaKey.address.isNotBlank())
        assertEquals("Solana derivation path must be m/44'/501'/0'/0'", "m/44'/501'/0'/0'", solanaKey.derivationPath)
    }
}
