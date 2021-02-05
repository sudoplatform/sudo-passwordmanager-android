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
 * Test the operation of [SudoPasswordManagerClient.register] using mocks and spies.
 *
 * @since 2020-10-01
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientRegisterTest : BaseTests() {

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
    fun `register() should call keyStore and userClient`() = runBlocking<Unit> {

        passwordManager.register("masterPassword")

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).register(any(), any())
    }

    @Test
    fun `register() should throw when secureVaultClient throws`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { register(any(), any()) } doThrow SudoSecureVaultException("mock")
        }

        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.register("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).register(any(), any())
    }

    @Test
    fun `register() should throw when keyStore throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.register("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `register() should throw when cryptoProvider throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn null
        }

        mockCryptographyProvider.stub {
            on { generateKeyDerivingKey() } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.register("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockCryptographyProvider).generateKeyDerivingKey()
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `register() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn null
        }

        mockCryptographyProvider.stub {
            on { generateKeyDerivingKey() } doThrow CancellationException("Mock Cancellation Exception")
        }

        shouldThrow<CancellationException> {
            passwordManager.register("masterPassword")
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockCryptographyProvider).generateKeyDerivingKey()
        verify(mockUserClient).getUserName()
    }
}
