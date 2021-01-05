/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.stub
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudologging.LogDriverInterface
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudopasswordmanager.TestData.ENTITLEMENTS
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.OWNERSHIP_PROOF
import com.sudoplatform.sudopasswordmanager.TestData.SECURE_VAULT
import com.sudoplatform.sudopasswordmanager.TestData.SUDO
import com.sudoplatform.sudopasswordmanager.TestData.USER_ID
import com.sudoplatform.sudopasswordmanager.TestData.USER_SUBJECT
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_PROXY
import com.sudoplatform.sudopasswordmanager.crypto.CryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.KeyDerivingKeyStore
import com.sudoplatform.sudopasswordmanager.datastore.VaultStore
import com.sudoplatform.sudopasswordmanager.rules.ActualPropertyResetter
import com.sudoplatform.sudopasswordmanager.rules.PropertyResetRule
import com.sudoplatform.sudopasswordmanager.rules.PropertyResetter
import com.sudoplatform.sudopasswordmanager.rules.TimberLogRule
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudosecurevault.SudoSecureVaultClient
import com.sudoplatform.sudouser.SudoUserClient
import org.junit.Rule
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString

/**
 * Base class that sets up:
 * - [TimberLogRule]
 * - [PropertyResetRule]
 *
 * And provides convenient access to the [PropertyResetRule.before] via [PropertyResetter.before].
 */
internal abstract class BaseTests : PropertyResetter by ActualPropertyResetter() {
    @Rule @JvmField val timberLogRule = TimberLogRule()

    protected val mockContext by before {
        mock<Context>()
    }

    protected val mockLogDriver by before {
        mock<LogDriverInterface>().stub {
            on { logLevel } doReturn LogLevel.VERBOSE
        }
    }

    protected val mockLogger by before {
        Logger("mock", mockLogDriver)
    }

    protected val mockUserClient by before {
        mock<SudoUserClient>().stub {
            on { getUserName() } doReturn USER_ID
            on { getSubject() } doReturn USER_SUBJECT
        }
    }

    protected val mockProfilesClient by before {
        mock<SudoProfilesClient>().stub {
            onBlocking { createSudo(any()) } doReturn SUDO
            onBlocking { listSudos(any()) } doReturn listOf(SUDO)
            onBlocking { getOwnershipProof(any(), anyString()) } doReturn OWNERSHIP_PROOF
        }
    }

    protected val mockCryptographyProvider by before {
        mock<CryptographyProvider>().stub {
            on { generateSecureFieldKey() } doReturn KEY_VALUE
        }
    }

    protected val mockKeyStore by before {
        mock<KeyDerivingKeyStore>().stub {
            on { getKey(ArgumentMatchers.anyString()) } doReturn KEY_VALUE
        }
    }

    protected val mockSecureVaultClient by before {
        mock<SudoSecureVaultClient>().stub {
            onBlocking { isRegistered() } doReturn true
            onBlocking { createVault(any(), any(), any(), anyString(), anyString()) } doReturn SECURE_VAULT
            onBlocking { listVaults(any(), any()) } doReturn emptyList()
            onBlocking { listVaultsMetadataOnly() } doReturn listOf(SECURE_VAULT)
            onBlocking { updateVault(any(), any(), anyString(), any(), any(), anyString()) } doReturn SECURE_VAULT
        }
    }

    protected val mockVaultStore by before {
        mock<VaultStore>().stub {
            on { listVaults() } doReturn emptyList()
            on { getVault(anyString()) } doReturn VAULT_PROXY
        }
    }

    protected val mockEntitlementsClient by before {
        mock<SudoEntitlementsClient>().stub {
            onBlocking { redeemEntitlements() } doReturn ENTITLEMENTS
            onBlocking { getEntitlements() } doReturn ENTITLEMENTS
        }
    }

    protected val passwordClientService by before {
        DefaultPasswordClientService(
            cryptoProvider = mockCryptographyProvider,
            keyStore = mockKeyStore,
            userClient = mockUserClient,
            secureVaultClient = mockSecureVaultClient,
            profilesClient = mockProfilesClient,
            entitlementsClient = mockEntitlementsClient
        )
    }

    protected val passwordManager by before {
        DefaultPasswordManagerClient(
            service = passwordClientService,
            logger = mockLogger,
            vaultStore = mockVaultStore
        )
    }
}
