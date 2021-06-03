/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [SudoPasswordManagerClient.reset] using mocks and spies.
 *
 * @since 2020-10-07
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientResetTest : BaseTests() {

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
    fun `reset() should call keyStore and secureVaultClient`() = runBlocking<Unit> {

        passwordManager.reset()

        verify(mockKeyStore).resetKeys()
        verify(mockSecureVaultClient).reset()
        verify(mockVaultStore).removeAll()
    }

    @Test
    fun `reset() should throw when keyStore throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { resetKeys() } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.reset()
        }

        verify(mockKeyStore).resetKeys()
        verify(mockVaultStore).removeAll()
    }

    @Test
    fun `reset() should throw when secureVaultClient reset throws`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { reset() } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.reset()
        }

        verify(mockKeyStore).resetKeys()
        verify(mockSecureVaultClient).reset()
        verify(mockVaultStore).removeAll()
    }
}
