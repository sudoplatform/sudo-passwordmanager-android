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
 * The default implementation of the [PasswordClientService] for this SDK.
 */
internal class DefaultPasswordClientService(
    override val cryptoProvider: CryptographyProvider,
    override val keyStore: KeyDerivingKeyStore,
    override val secureVaultClient: SudoSecureVaultClient,
    override val userClient: SudoUserClient,
    override val profilesClient: SudoProfilesClient,
    override val entitlementsClient: SudoEntitlementsClient
) : PasswordClientService {

    private val kdkName: String
        get() {
            val userId = userClient.getUserName()
                ?: throw SudoPasswordManagerException.UnauthorizedUserException("Sudo User must be registered and signed in")
            return "kdk-$userId"
        }

    override fun getKey(): KeyDerivingKey? {
        return keyStore.getKey(kdkName)
    }

    override fun set(key: KeyDerivingKey) {
        keyStore.add(key, kdkName)
    }

    override fun getUserSubject(): String? {
        return try {
            userClient.getSubject()
        } catch (e: Throwable) {
            throw SudoPasswordManagerException.UnauthorizedUserException("Failed to get Sudo User subject", e)
        }
    }
}
