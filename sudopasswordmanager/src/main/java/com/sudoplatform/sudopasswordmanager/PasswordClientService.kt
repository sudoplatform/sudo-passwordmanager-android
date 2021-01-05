/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudopasswordmanager.crypto.CryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.KeyDerivingKeyStore
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudosecurevault.SudoSecureVaultClient
import com.sudoplatform.sudouser.SudoUserClient

/**
 * Defines the dependencies required for the password manager client. Also moves some state away
 * from the client, e.g. keys and data needs to be namespaced so it doesn't conflict with other
 * users, but client doesn't need to deal with that.
 */
internal interface PasswordClientService {

    /** Provide access to the encrypted vaults */
    val secureVaultClient: SudoSecureVaultClient

    /** Provide access to the user */
    val userClient: SudoUserClient

    /** Provide access to Sudos */
    val profilesClient: SudoProfilesClient

    /** Provide access to entitlements */
    val entitlementsClient: SudoEntitlementsClient

    /** Cryptography operations */
    val cryptoProvider: CryptographyProvider

    /** Loading and saving keys */
    val keyStore: KeyDerivingKeyStore

    /** Gets the key deriving key for the currently signed in user */
    fun getKey(): KeyDerivingKey?

    /** Sets the key for the currently signed in user */
    fun set(key: KeyDerivingKey)

    /** Fetches the "subject" of the currently logged in user's Sudo. */
    fun getUserSubject(): String?
}
