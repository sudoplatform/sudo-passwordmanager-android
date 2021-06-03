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
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoPasswordManagerClient.getRegistrationStatus] using mocks and spies.
 *
 * @since 2020-10-07
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientGetRegistrationStatusTest : BaseTests() {

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
    fun `getRegistrationStatus() should map registered state from secureVaultClient`() = runBlocking<Unit> {

        passwordManager.getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.REGISTERED

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).isRegistered()
    }

    @Test
    fun `getRegistrationStatus() should map unregistered state from secureVaultClient`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { isRegistered() } doReturn false
        }

        passwordManager.getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.NOT_REGISTERED

        verify(mockSecureVaultClient).isRegistered()
    }

    @Test
    fun `getRegistrationStatus() should map missing key`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn null
        }

        passwordManager.getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.MISSING_SECRET_CODE

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).isRegistered()
    }

    @Test
    fun `getRegistrationStatus() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { isRegistered() } doThrow SudoSecureVaultException("Mock")
        }

        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.getRegistrationStatus()
        }

        verify(mockSecureVaultClient).isRegistered()
    }

    @Test
    fun `getRegistrationStatus() should throw when keyStore throws`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doThrow SudoPasswordManagerException.CryptographyException("mock")
        }

        shouldThrow<SudoPasswordManagerException.CryptographyException> {
            passwordManager.getRegistrationStatus()
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).isRegistered()
    }

    @Test
    fun `getRegistrationStatus() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        shouldThrow<CancellationException> {
            passwordManager.getRegistrationStatus()
        }

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getUserName()
        verify(mockSecureVaultClient).isRegistered()
    }
}
