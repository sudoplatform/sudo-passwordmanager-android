/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
import com.sudoplatform.sudopasswordmanager.TestData.VAULT
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_BANK_ACCOUNT_ITEM
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_CREDIT_CARD_ITEM
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_LOGIN_ITEM
import com.sudoplatform.sudopasswordmanager.datastore.VaultBankAccountProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultCreditCardProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultLoginProxy
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
            mockSecureVaultClient,
            mockVaultStore,
            mockEntitlementsClient
        )
    }

    //
    // Update Vault tests
    //

    @Test
    fun `updateVault() should call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.update(VAULT)

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).updateVault(any())
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

        verify(mockVaultStore).getVault(anyString())
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

        verify(mockVaultStore).getVault(anyString())
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
        passwordManager.add(VAULT_LOGIN_ITEM, VAULT)
        passwordManager.add(VAULT_CREDIT_CARD_ITEM, VAULT)
        passwordManager.add(VAULT_BANK_ACCOUNT_ITEM, VAULT)

        verify(mockVaultStore, times(3)).getVault(anyString())
        verify(mockVaultStore).add(any<VaultLoginProxy>(), anyString())
        verify(mockVaultStore).add(any<VaultCreditCardProxy>(), anyString())
        verify(mockVaultStore).add(any<VaultBankAccountProxy>(), anyString())
        verify(mockVaultStore, times(3)).updateVault(any())
        verify(mockSecureVaultClient, times(3)).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `add() should throw when password manager is locked`() = runBlocking<Unit> {

        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.add(VAULT_LOGIN_ITEM, VAULT)
        }
    }

    @Test
    fun `add() should throw when secureVaultClient returns error`() = runBlocking<Unit> {

        mockSecureVaultClient.stub {
            onBlocking { updateVault(any(), any(), anyString(), any(), any(), anyString()) } doThrow SudoSecureVaultException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.FailedException> {
            passwordManager.add(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).add(any<VaultLoginProxy>(), anyString())
        verify(mockSecureVaultClient).updateVault(any(), any(), anyString(), any(), any(), anyString())
    }

    @Test
    fun `add() should throw when vault store says not found`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            passwordManager.add(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).add(any<VaultLoginProxy>(), anyString())
    }

    @Test
    fun `add() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.add(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).add(any<VaultLoginProxy>(), anyString())
    }

    //
    // Remove from Vault tests
    //

    @Test
    fun `remove() should call secureVaultClient`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.removeVaultItem("id", VAULT)

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).removeVaultItem(anyString(), anyString())
        verify(mockVaultStore).updateVault(any())
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

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).removeVaultItem(anyString(), anyString())
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

        verify(mockVaultStore).getVault(anyString())
        verify(mockVaultStore).removeVaultItem(anyString(), anyString())
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
        verify(mockVaultStore).removeVaultItem(anyString(), anyString())
    }
}
