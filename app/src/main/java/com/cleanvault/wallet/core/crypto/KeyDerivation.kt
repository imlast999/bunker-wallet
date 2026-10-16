package com.cleanvault.wallet.core.crypto

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Arrays

object Base58 {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val INDEXES = IntArray(128) { -1 }.apply {
        for (i in ALPHABET.indices) {
            this[ALPHABET[i].code] = i
        }
    }

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) {
            zeros++
        }
        val encoded = CharArray(input.size * 2)
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < input.size) {
            val decodedByte = divmod(input, inputStart, 256, 58)
            if (input[inputStart].toInt() == 0) {
                inputStart++
            }
            encoded[--outputStart] = ALPHABET[decodedByte.toInt()]
        }
        while (outputStart < encoded.size && encoded[outputStart] == ALPHABET[0]) {
            outputStart++
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ALPHABET[0]
        }
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray {
        if (input.isEmpty()) return ByteArray(0)
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.code < 128) INDEXES[c.code] else -1
            if (digit < 0) {
                throw IllegalArgumentException("Illegal character '$c' at index $i in Base58 string")
            }
            input58[i] = digit.toByte()
        }
        var zeros = 0
        while (zeros < input58.size && input58[zeros].toInt() == 0) {
            zeros++
        }
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            val decodedByte = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart].toInt() == 0) {
                inputStart++
            }
            decoded[--outputStart] = decodedByte
        }
        while (outputStart < decoded.size && decoded[outputStart].toInt() == 0) {
            outputStart++
        }
        val result = ByteArray(zeros + (decoded.size - outputStart))
        System.arraycopy(decoded, outputStart, result, zeros, decoded.size - outputStart)
        return result
    }

    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}

data class DerivedKeypair(
    val address: String,
    val privateKeyHexOrBase58: String,
    val publicKeyBytes: ByteArray,
    val derivationPath: String
)

/**
 * Key derivation engine for multi-chain support:
 * - Solana: m/44'/501'/0'/0' (Ed25519 SLIP-0010 + 64-byte keypair Base58 for 100% Phantom compatibility)
 * - Robinhood Chain (EVM): m/44'/60'/0'/0/0 (secp256k1 BIP-44)
 */
object KeyDerivation {

    private const val ED25519_CURVE = "ed25519 seed"

    fun deriveSolanaKey(seed: ByteArray): DerivedKeypair {
        val path = "m/44'/501'/0'/0'"
        // SLIP-0010 Hardened derivation for Ed25519
        var i = hmacSha512(ED25519_CURVE.toByteArray(Charsets.UTF_8), seed)
        var key = Arrays.copyOfRange(i, 0, 32)
        var chainCode = Arrays.copyOfRange(i, 32, 64)

        // Derivation indices: 44', 501', 0', 0'
        val indices = longArrayOf(44L or 0x80000000L, 501L or 0x80000000L, 0L or 0x80000000L, 0L or 0x80000000L)
        for (index in indices) {
            val data = ByteArray(37)
            data[0] = 0x00
            System.arraycopy(key, 0, data, 1, 32)
            ByteBuffer.wrap(data, 33, 4).putInt(index.toInt())

            i = hmacSha512(chainCode, data)
            key = Arrays.copyOfRange(i, 0, 32)
            chainCode = Arrays.copyOfRange(i, 32, 64)
        }

        // True Ed25519 curve point multiplication via BouncyCastle
        val ed25519PrivParams = Ed25519PrivateKeyParameters(key, 0)
        val ed25519PubParams = ed25519PrivParams.generatePublicKey()
        val pubKeyBytes = ed25519PubParams.encoded

        // Standard Solana 64-byte secret key (32 bytes private seed + 32 bytes public key)
        val fullKeyPair = ByteArray(64)
        System.arraycopy(key, 0, fullKeyPair, 0, 32)
        System.arraycopy(pubKeyBytes, 0, fullKeyPair, 32, 32)

        val solanaAddress = Base58.encode(pubKeyBytes)
        val privateKeyBase58 = Base58.encode(fullKeyPair)

        return DerivedKeypair(
            address = solanaAddress,
            privateKeyHexOrBase58 = privateKeyBase58,
            publicKeyBytes = pubKeyBytes,
            derivationPath = path
        )
    }

    /**
     * Resolves and imports Solana private keys in any standard format:
     * 1. 64-byte Base58 keypair (Standard Phantom / Solflare export)
     * 2. 32-byte Base58 private seed
     * 3. Byte array JSON string (e.g. [12, 45, 67, ...]) from Solana CLI id.json
     */
    fun deriveSolanaFromPrivateKey(input: String): DerivedKeypair {
        val trimmed = input.trim()
        val rawBytes: ByteArray = when {
            trimmed.startsWith("[") && trimmed.endsWith("]") -> {
                // Parse JSON array of byte integers
                trimmed.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().toInt().toByte() }
                    .toByteArray()
            }
            else -> {
                Base58.decode(trimmed)
            }
        }

        val seed32 = when (rawBytes.size) {
            64 -> Arrays.copyOfRange(rawBytes, 0, 32)
            32 -> rawBytes
            else -> throw IllegalArgumentException("Invalid Solana private key length: ${rawBytes.size} bytes. Expected 32 or 64 bytes.")
        }

        val ed25519Priv = Ed25519PrivateKeyParameters(seed32, 0)
        val pubBytes = ed25519Priv.generatePublicKey().encoded

        val full64 = ByteArray(64)
        System.arraycopy(seed32, 0, full64, 0, 32)
        System.arraycopy(pubBytes, 0, full64, 32, 32)

        return DerivedKeypair(
            address = Base58.encode(pubBytes),
            privateKeyHexOrBase58 = Base58.encode(full64),
            publicKeyBytes = pubBytes,
            derivationPath = "imported"
        )
    }

    fun deriveRobinhoodChainKey(seed: ByteArray): DerivedKeypair {
        val path = "m/44'/60'/0'/0/0"
        val master = Bip32ECKeyPair.generateKeyPair(seed)
        val derivationPath = intArrayOf(
            44 or Bip32ECKeyPair.HARDENED_BIT,
            60 or Bip32ECKeyPair.HARDENED_BIT,
            0 or Bip32ECKeyPair.HARDENED_BIT,
            0,
            0
        )
        val childKeyPair = Bip32ECKeyPair.deriveKeyPair(master, derivationPath)
        val address = Keys.toChecksumAddress(Keys.getAddress(childKeyPair))
        val privateKeyHex = "0x" + childKeyPair.privateKey.toString(16).padStart(64, '0')

        return DerivedKeypair(
            address = address,
            privateKeyHexOrBase58 = privateKeyHex,
            publicKeyBytes = childKeyPair.publicKey.toByteArray(),
            derivationPath = path
        )
    }

    /**
     * Imports an EVM private key (hex with or without 0x prefix) and computes checksum address.
     */
    fun deriveEvmFromPrivateKey(hexKey: String): DerivedKeypair {
        val cleanHex = hexKey.trim().removePrefix("0x").removePrefix("0X")
        val privBigInt = BigInteger(cleanHex, 16)
        val ecKeyPair = ECKeyPair.create(privBigInt)
        val address = Keys.toChecksumAddress(Keys.getAddress(ecKeyPair))
        val formattedHex = "0x" + cleanHex.padStart(64, '0')

        return DerivedKeypair(
            address = address,
            privateKeyHexOrBase58 = formattedHex,
            publicKeyBytes = ecKeyPair.publicKey.toByteArray(),
            derivationPath = "imported"
        )
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = HMac(org.bouncycastle.crypto.digests.SHA512Digest())
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val out = ByteArray(64)
        hmac.doFinal(out, 0)
        return out
    }
}

object MessageDigestHelper {
    fun sha256(input: ByteArray): ByteArray {
        val digest = SHA256Digest()
        digest.update(input, 0, input.size)
        val out = ByteArray(digest.digestSize)
        digest.doFinal(out, 0)
        return out
    }
}
