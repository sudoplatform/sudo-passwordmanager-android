/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
import com.sudoplatform.sudopasswordmanager.TestData.SUDO
import com.sudoplatform.sudopasswordmanager.TestData.SUDO_ID
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoPasswordManagerClient.getEntitlementState] using mocks and spies.
 *
 * @since 2020-10-29
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientGetEntitlementsStateTest : BaseTests() {

    @After
    fun fini() {
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockProfilesClient,
            mockCryptographyProvider,
            mockKeyStore,
            mockSecureVaultClient,
            mockEntitlementsClient
        )
    }

    @Test
    fun `getEntitlementState() should call entitlementsClient`() = runBlocking<Unit> {
        SUDO.id = SUDO_ID
        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getEntitlementState().shouldContainExactly(
            EntitlementState(
                name = EntitlementState.Name.MAX_VAULTS_PER_SUDO,
                limit = 2,
                sudoId = SUDO_ID,
                value = 1
            )
        )

        verify(mockEntitlementsClient).getEntitlements()
        verify(mockProfilesClient).listSudos(any())
        verify(mockSecureVaultClient).listVaultsMetadataOnly()
    }

    @Test
    fun `getEntitlementState() should throw when entitlementsClient throws`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlements() } doThrow SudoEntitlementsClient.EntitlementsException.FailedException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.getEntitlementState()
        }

        verify(mockEntitlementsClient).getEntitlements()
    }

    @Test
    fun `getEntitlementState() should throw when secureVaultClient throws`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { listVaultsMetadataOnly() } doThrow SudoSecureVaultException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.getEntitlementState()
        }

        verify(mockEntitlementsClient).getEntitlements()
        verify(mockSecureVaultClient).listVaultsMetadataOnly()
    }

    @Test
    fun `getEntitlementState() should throw when vaults are locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.getEntitlementState()
        }
    }

    @Test
    fun `getEntitlementState() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlements() } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.getEntitlementState()
        }

        verify(mockEntitlementsClient).getEntitlements()
    }
}
