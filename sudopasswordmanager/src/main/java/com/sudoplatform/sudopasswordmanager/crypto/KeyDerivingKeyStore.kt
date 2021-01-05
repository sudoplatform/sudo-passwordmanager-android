/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.crypto

import com.sudoplatform.sudokeymanager.KeyManagerException
import com.sudoplatform.sudokeymanager.KeyManagerInterface
import com.sudoplatform.sudopasswordmanager.KeyDerivingKey
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException

/**
 * Providers of key management operations to this SDK implement this interface.
 */
interface KeyDerivingKeyStore {
    /**
     * Gets a key with the given name from the key store.
     *
     * @param name The name of the key to get.
     * @return the key or null if the key was not found.
     */
    fun getKey(name: String): KeyDerivingKey?

    /**
     * Adds a key to the key store.
     *
     * @param key The key to add to the store.
     * @param name The name of the key.
     */
    fun add(key: KeyDerivingKey, name: String)

    /**
     * Clears all the keys from the key store.
     */
    fun resetKeys()
}

/**
 * The default implementation of [KeyDerivingKeyStore] used in this SDK.
 */
internal class DefaultKeyDerivingKeyStore(private val keyManager: KeyManagerInterface) : KeyDerivingKeyStore {

    override fun getKey(name: String): KeyDerivingKey? {
        try {
            return keyManager.getSymmetricKeyData(name)
        } catch (e: KeyManagerException) {
            throw SudoPasswordManagerException.CryptographyException(
                "getSymmetricKeyData failed",
                e
            )
        }
    }

    override fun add(key: KeyDerivingKey, name: String) {
        try {
            if (keyManager.getSymmetricKeyData(name) != null) {
                keyManager.deleteSymmetricKey(name)
            }
            keyManager.addSymmetricKey(key, name)
        } catch (e: KeyManagerException) {
            throw SudoPasswordManagerException.CryptographyException("addSymmetricKey failed", e)
        }
    }

    override fun resetKeys() {
        try {
            keyManager.removeAllKeys()
        } catch (e: KeyManagerException) {
            throw SudoPasswordManagerException.CryptographyException("removeAllKeys failed", e)
        }
    }
}
