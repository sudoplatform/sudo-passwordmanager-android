/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

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
 * Test the operation of [SudoPasswordManagerClient.getEntitlement] using mocks and spies.
 *
 * @since 2021-01-28
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientGetEntitlementTest : BaseTests() {

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
    fun `getEntitlement() should call entitlementsClient`() = runBlocking<Unit> {
        SUDO.id = SUDO_ID
        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getEntitlement().shouldContainExactly(
            Entitlement(
                name = Entitlement.Name.MAX_VAULTS_PER_SUDO,
                limit = 2
            )
        )

        verify(mockEntitlementsClient).getEntitlementsConsumption()
    }

    @Test
    fun `getEntitlement() should throw when entitlementsClient throws`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlementsConsumption() } doThrow SudoEntitlementsClient.EntitlementsException.FailedException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.getEntitlement()
        }

        verify(mockEntitlementsClient).getEntitlementsConsumption()
    }

    @Test
    fun `getEntitlement() should not throw when vaults are locked`() = runBlocking<Unit> {
        passwordManager.getEntitlement() shouldNotBe null

        verify(mockEntitlementsClient).getEntitlementsConsumption()
    }

    @Test
    fun `getEntitlement() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockEntitlementsClient.stub {
            onBlocking { getEntitlementsConsumption() } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.getEntitlement()
        }

        verify(mockEntitlementsClient).getEntitlementsConsumption()
    }
}
