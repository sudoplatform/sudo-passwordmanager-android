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
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
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
 * Test the operation of [SudoPasswordManagerClient.createVault] using mocks and spies.
 *
 * @since 2020-10-08
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientCreateVaultTest : BaseTests() {

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
    fun `createVault() should call keyStore and secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.createVault("ownershipProof")

        verify(mockProfilesClient).getOwnershipProof(any(), anyString())
        verify(mockSecureVaultClient).createVault(any(), any(), any(), anyString(), anyString())
        verify(mockVaultStore).importVault(any())
    }

    @Test
    fun `createVault() should throw when password manager is locked`() = runBlocking<Unit> {

        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.createVault("ownershipProof")
        }
    }

    @Test
    fun `createVault() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { createVault(any(), any(), any(), anyString(), anyString()) } doThrow SudoSecureVaultException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.createVault("ownershipProof")
        }

        verify(mockProfilesClient).getOwnershipProof(any(), anyString())
        verify(mockSecureVaultClient).createVault(any(), any(), any(), anyString(), anyString())
    }

    @Test
    fun `createVault() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { createVault(any(), any(), any(), anyString(), anyString()) } doThrow CancellationException("Mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.createVault("ownershipProof")
        }

        verify(mockProfilesClient).getOwnershipProof(any(), anyString())
        verify(mockSecureVaultClient).createVault(any(), any(), any(), anyString(), anyString())
    }
}
