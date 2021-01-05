/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
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
 * Test the operation of [SudoPasswordManagerClient.unlock] using mocks and spies.
 *
 * @since 2020-10-07
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientUnlockTest : BaseTests() {

    @After
    fun fini() {
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockProfilesClient,
            mockCryptographyProvider,
            mockKeyStore,
            mockSecureVaultClient
        )
    }

    @Test
    fun `unlock() should call keyStore and secureVaultClient when secretCode is null`() = runBlocking<Unit> {

        passwordManager.unlock("masterPassword")

        verify(mockKeyStore).getKey(anyString())
        verify(mockKeyStore).add(any(), anyString())
        verify(mockUserClient, times(2)).getUserName()
        verify(mockSecureVaultClient).listVaults(any(), any())
    }

    @Test
    fun `unlock() should call keyStore and secureVaultClient when secretCode is not null`() = runBlocking<Unit> {

        passwordManager.unlock("masterPassword", "secretCode")

        verify(mockKeyStore).getKey(anyString())
        verify(mockKeyStore).add(any(), anyString())
        verify(mockUserClient, times(2)).getUserName()
        verify(mockSecureVaultClient).listVaults(any(), any())
    }

    @Test
    fun `unlock() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { listVaults(any(), any()) } doThrow SudoSecureVaultException("mock")
        }

        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.unlock("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).listVaults(any(), any())
    }

    @Test
    fun `unlock() should throw when keyStore throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.InvalidPasswordOrMissingSecretCodeException> {
            passwordManager.unlock("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `unlock() should throw when cryptoProvider throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn null
        }

        mockCryptographyProvider.stub {
            on { generateKeyDerivingKey() } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.InvalidPasswordOrMissingSecretCodeException> {
            passwordManager.unlock("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `unlock() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { listVaults(any(), any()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        shouldThrow<CancellationException> {
            passwordManager.unlock("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).listVaults(any(), any())
    }
}
