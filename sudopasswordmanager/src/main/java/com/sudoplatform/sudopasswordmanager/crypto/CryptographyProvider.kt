/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.crypto

import com.sudoplatform.sudokeymanager.KeyManagerInterface
import com.sudoplatform.sudopasswordmanager.KeyDerivingKey
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException

/**
 * Providers of cryptographic operations to this SDK implement this interface.
 */
interface CryptographyProvider {

    /** Generates a 128 bit key deriving key */
    fun generateKeyDerivingKey(): KeyDerivingKey

    /**
     * Generates a vault key, used to encrypt secret data within the vault. This is used for
     * memory hygiene in web in order to keep things like passwords encrypted until ready for
     * display.
     */
    fun generateSecureFieldKey(): ByteArray

    /**
     * Encrypts a secure field
     *
     * @param data The data of the secure field to encrypt.
     * @param key The encryption key.
     * @return The encrypted data
     */
    fun encryptSecureField(data: ByteArray, key: ByteArray): ByteArray

    /**
     * Decrypts a secure field
     *
     * @param data The data of the secure field to decrypt.
     * @param key The decryption key.
     * @return the decrypted data
     */
    fun decryptSecureField(data: ByteArray, key: ByteArray): ByteArray
}

internal class DefaultCryptographyProvider(val keyManager: KeyManagerInterface) : CryptographyProvider {

    companion object {
        const val KEY_SIZE_BITS = 128
        const val KEY_SIZE_BYTES = KEY_SIZE_BITS / 8
        const val BLOCK_SIZE_AES = 16
    }

    override fun generateKeyDerivingKey(): KeyDerivingKey {
        return keyManager.createRandomData(KEY_SIZE_BYTES)
    }

    override fun generateSecureFieldKey(): ByteArray {
        return keyManager.createRandomData(KEY_SIZE_BYTES)
    }

    override fun encryptSecureField(data: ByteArray, key: ByteArray): ByteArray {
        val iv = keyManager.createRandomData(BLOCK_SIZE_AES)
        val encryptedData = keyManager.encryptWithSymmetricKey(key, data, iv)
        return iv + encryptedData
    }

    override fun decryptSecureField(data: ByteArray, key: ByteArray): ByteArray {
        val ivLength = BLOCK_SIZE_AES

        if (data.size <= ivLength) {
            throw SudoPasswordManagerException.CryptographyException("Secure field contains too little data")
        }

        val iv = data.copyOfRange(0, ivLength)
        val cipherText = data.copyOfRange(ivLength, data.size)
        return keyManager.decryptWithSymmetricKey(key, cipherText, iv)
    }
}
