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
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
import com.sudoplatform.sudopasswordmanager.TestData.VAULT
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_ITEM
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CancellationException

/**
 * Test the operation of [SudoPasswordManagerClient.updateVault] using mocks and spies.
 *
 * @since 2020-10-08
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientUpdateVaultTest : BaseTests() {

    @Before
    fun init() { }

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

    //
    // Update Vault tests
    //

    @Test
    fun `updateVault() should call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.update(VAULT)

        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `updateVault() should throw when password manager is locked`() = runBlocking<Unit> {

        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.update(VAULT)
        }
    }

    @Test
    fun `updateVault() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { updateVault(any(), any(), anyString(), any(), any(), anyString()) } doThrow SudoSecureVaultException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.update(VAULT)
        }

        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `updateVault() should throw when vault store says not found`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            passwordManager.update(VAULT)
        }
    }

    @Test
    fun `updateVault() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.update(VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
    }

    //
    // Add to Vault tests
    //

    @Test
    fun `add() should call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.add(VAULT_ITEM, VAULT)

        verify(mockVaultStore, times(1)).getVault(anyString())
        verify(mockVaultStore).add(any(), anyString())
        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `add() should throw when password manager is locked`() = runBlocking<Unit> {

        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.add(VAULT_ITEM, VAULT)
        }
    }

    @Test
    fun `add() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { updateVault(any(), any(), anyString(), any(), any(), anyString()) } doThrow SudoSecureVaultException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.add(VAULT_ITEM, VAULT)
        }

        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `add() should throw when vault store says not found`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            passwordManager.add(VAULT_ITEM, VAULT)
        }
    }

    @Test
    fun `add() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.add(VAULT_ITEM, VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
    }

    //
    // Remove from Vault tests
    //

    @Test
    fun `remove() should call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.removeVaultItem("id", VAULT)

        verify(mockVaultStore).getVault(anyString())
        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `remove() should throw when password manager is locked`() = runBlocking<Unit> {

        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.removeVaultItem("id", VAULT)
        }
    }

    @Test
    fun `remove() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { updateVault(any(), any(), anyString(), any(), any(), anyString()) } doThrow SudoSecureVaultException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.removeVaultItem("id", VAULT)
        }

        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `remove() should throw when vault store says not found`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            passwordManager.removeVaultItem("id", VAULT)
        }
    }

    @Test
    fun `remove() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.removeVaultItem("id", VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
    }
}
