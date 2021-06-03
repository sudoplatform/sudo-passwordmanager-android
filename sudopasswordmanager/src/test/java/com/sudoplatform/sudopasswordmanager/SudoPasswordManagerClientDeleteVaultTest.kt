/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudopasswordmanager.TestData.SECURE_VAULT
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoPasswordManagerClient.deleteVault] using mocks and spies.
 *
 * @since 2020-10-08
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientDeleteVaultTest : BaseTests() {

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
    fun `deleteVault() should call secureVaultClient`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { deleteVault(anyString()) } doReturn SECURE_VAULT
        }

        passwordManager.deleteVault("id")

        verify(mockSecureVaultClient).deleteVault(anyString())
        verify(mockVaultStore).deleteVault(anyString())
    }

    @Test
    fun `deleteVault() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { deleteVault(anyString()) } doThrow SudoSecureVaultException("mock")
        }

        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.deleteVault("id")
        }

        verify(mockSecureVaultClient).deleteVault(anyString())
    }

    @Test
    fun `deleteVault() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { deleteVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        shouldThrow<CancellationException> {
            passwordManager.deleteVault("id")
        }

        verify(mockSecureVaultClient).deleteVault(anyString())
    }
}
