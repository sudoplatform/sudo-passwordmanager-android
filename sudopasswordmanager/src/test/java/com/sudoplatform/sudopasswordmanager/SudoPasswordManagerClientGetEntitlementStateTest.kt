/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
import com.sudoplatform.sudopasswordmanager.TestData.SUDO
import com.sudoplatform.sudopasswordmanager.TestData.SUDO_ID
import com.sudoplatform.sudopasswordmanager.entitlements.Entitlement
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.shouldNotBe
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
internal class SudoPasswordManagerClientGetEntitlementStateTest : BaseTests() {

    @After
    fun fini() {
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockProfilesClient,
            mockCryptographyProvider,
            mockKeyStore,
            mockSecureVaultClient,
            mockVaultStore,
            mockEntitlementsClient
        )
    }

    @Test
    fun `getEntitlementState() should call entitlementsClient`() = runBlocking<Unit> {
        SUDO.id = SUDO_ID
        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getEntitlementState().shouldContainExactly(
            EntitlementState(
                name = Entitlement.Name.MAX_VAULTS_PER_SUDO,
                limit = 2,
                sudoId = SUDO_ID,
                value = 1
            )
        )

        verify(mockEntitlementsClient).getEntitlementsConsumption()
        verify(mockProfilesClient).listSudos(any())
        verify(mockSecureVaultClient).listVaultsMetadataOnly()
    }

    @Test
    fun `getEntitlementState() should throw when entitlementsClient throws`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlementsConsumption() } doThrow SudoEntitlementsClient.EntitlementsException.FailedException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.getEntitlementState()
        }

        verify(mockEntitlementsClient).getEntitlementsConsumption()
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

        verify(mockEntitlementsClient).getEntitlementsConsumption()
        verify(mockSecureVaultClient).listVaultsMetadataOnly()
    }

    @Test
    fun `getEntitlementState() should not throw when vaults are locked`() = runBlocking<Unit> {
        passwordManager.getEntitlementState() shouldNotBe null

        verify(mockEntitlementsClient).getEntitlementsConsumption()
        verify(mockProfilesClient).listSudos(any())
        verify(mockSecureVaultClient).listVaultsMetadataOnly()
    }

    @Test
    fun `getEntitlementState() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlementsConsumption() } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.getEntitlementState()
        }

        verify(mockEntitlementsClient).getEntitlementsConsumption()
    }
}
