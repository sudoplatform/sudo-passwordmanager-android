/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoPasswordManagerClient.deregister] using mocks and spies.
 *
 * @since 2020-11-02
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientDeregisterTest : BaseTests() {

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
    fun `deregister() should lock, reset and call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.deregister()

        verify(mockKeyStore).resetKeys()
        verify(mockSecureVaultClient).deregister()
        verify(mockSecureVaultClient).reset()
        verify(mockVaultStore, atLeastOnce()).removeAll()
    }

    @Test
    fun `deregister() should throw when secureVaultClient throws`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { deregister() } doThrow SudoSecureVaultException("mock")
        }

        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.deregister()
        }

        verify(mockSecureVaultClient).deregister()
        verify(mockVaultStore, atLeastOnce()).removeAll()
    }

    @Test
    fun `deregister() should throw when keyStore throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { resetKeys() } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.deregister()
        }

        verify(mockKeyStore).resetKeys()
        verify(mockVaultStore, atLeastOnce()).removeAll()
        verify(mockSecureVaultClient).deregister()
    }

    @Test
    fun `deregister() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { resetKeys() } doThrow CancellationException("Mock Cancellation Exception")
        }

        shouldThrow<CancellationException> {
            passwordManager.deregister()
        }

        verify(mockKeyStore).resetKeys()
        verify(mockVaultStore, atLeastOnce()).removeAll()
        verify(mockSecureVaultClient).deregister()
    }
}
