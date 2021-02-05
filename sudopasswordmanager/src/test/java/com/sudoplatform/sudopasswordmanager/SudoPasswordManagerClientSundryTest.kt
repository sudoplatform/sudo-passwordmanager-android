/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.stub
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.MASTER_PASSWORD
import com.sudoplatform.sudopasswordmanager.TestData.OWNERS
import com.sudoplatform.sudopasswordmanager.TestData.VAULT
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_BANK_ACCOUNT_ITEM
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_CREDIT_CARD_ITEM
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_LOGIN_ITEM
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_SCHEMA
import com.sudoplatform.sudopasswordmanager.datastore.VaultBankAccountProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultCreditCardProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultLoginProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
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
 * Test the operation of [SudoPasswordManagerClient.listVaults],
 * [SudoPasswordManagerClient.getVault], [SudoPasswordManagerClient.listVaultItems],
 * [SudoPasswordManagerClient.getVaultItem] using mocks and spies.
 *
 * @since 2020-10-08
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientSundryTest : BaseTests() {

    @Before
    fun init() {
    }

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
    // List Vaults tests
    //

    @Test
    fun `listVaults() should call vaultStore`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { listVaults() } doReturn listOf(VaultProxy(vaultData = VAULT_SCHEMA, owners = OWNERS))
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.listVaults() shouldHaveSize 1

        verify(mockVaultStore).listVaults()
    }

    @Test
    fun `listVaults() should throw when password manager is locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.listVaults()
        }
    }

    @Test
    fun `listVaults() should throw when vaultStore throws`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { listVaults() } doThrow ConcurrentModificationException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.UnknownException> {
            passwordManager.listVaults()
        }

        verify(mockVaultStore).listVaults()
    }

    @Test
    fun `listVaults() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { listVaults() } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.listVaults()
        }

        verify(mockVaultStore).listVaults()
    }

    //
    // Get Vault tests
    //

    @Test
    fun `getVault() should call vaultStore`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn VaultProxy(vaultData = VAULT_SCHEMA, owners = OWNERS)
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getVault("id") shouldNotBe null

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVault() should call vaultStore and handle null`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getVault("id") shouldBe null

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVault() should throw when password manager is locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.getVault("id")
        }
    }

    @Test
    fun `getVault() should throw when vaultStore throws`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow ConcurrentModificationException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.UnknownException> {
            passwordManager.getVault("id")
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVault() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.getVault("id")
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    //
    // List Vault Items tests
    //

    @Test
    fun `listVaultItems() should call vaultStore`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.listVaultItems(VAULT) shouldNotBe null

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `listVaultItems() should call vaultStore and handle null`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.listVaultItems(VAULT) shouldHaveSize 0

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `listVaultItems() should throw when password manager is locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.listVaultItems(VAULT)
        }
    }

    @Test
    fun `listVaultItems() should throw when vaultStore throws`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow ConcurrentModificationException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.UnknownException> {
            passwordManager.listVaultItems(VAULT)
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `listVaultItems() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.listVaultItems(VAULT)
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    //
    // Get Vault Item tests
    //

    @Test
    fun `getVaultItem() should call vaultStore`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getVaultItem("id", VAULT) shouldNotBe null

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVaultItem() should call vaultStore and handle null`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doReturn null
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.getVaultItem("id", VAULT) shouldBe null

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVaultItem() should throw when password manager is locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.getVaultItem("id", VAULT)
        }
    }

    @Test
    fun `getVaultItem() should throw when vaultStore throws`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow ConcurrentModificationException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.UnknownException> {
            passwordManager.getVaultItem("id", VAULT)
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    @Test
    fun `getVaultItem() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { getVault(anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.getVaultItem("id", VAULT)
        }

        verify(mockVaultStore).getVault(VAULT.id)
    }

    //
    // Update Vault Item tests
    //

    @Test
    fun `update() should call vaultStore`() = runBlocking<Unit> {

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        passwordManager.update(VAULT_LOGIN_ITEM, VAULT) shouldNotBe null
        passwordManager.update(VAULT_CREDIT_CARD_ITEM, VAULT) shouldNotBe null
        passwordManager.update(VAULT_BANK_ACCOUNT_ITEM, VAULT) shouldNotBe null

        verify(mockVaultStore).update(any<VaultLoginProxy>(), eq(VAULT.id))
        verify(mockVaultStore).update(any<VaultCreditCardProxy>(), eq(VAULT.id))
        verify(mockVaultStore).update(any<VaultBankAccountProxy>(), eq(VAULT.id))
    }

    @Test
    fun `update() should throw when vault not found`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { update(any<VaultLoginProxy>(), anyString()) } doThrow SudoPasswordManagerException.VaultNotFoundException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            passwordManager.update(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).update(any<VaultLoginProxy>(), eq(VAULT.id))
    }

    @Test
    fun `update() should throw when password manager is locked`() = runBlocking<Unit> {
        shouldThrow<SudoPasswordManagerException.VaultLockedException> {
            passwordManager.update(VAULT_LOGIN_ITEM, VAULT)
        }
    }

    @Test
    fun `update() should throw when vaultStore throws`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { update(any<VaultLoginProxy>(), anyString()) } doThrow ConcurrentModificationException("mock")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<SudoPasswordManagerException.UnknownException> {
            passwordManager.update(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).update(any<VaultLoginProxy>(), eq(VAULT.id))
    }

    @Test
    fun `update() should not block coroutine cancellation exception`() = runBlocking<Unit> {

        mockVaultStore.stub {
            on { update(any<VaultLoginProxy>(), anyString()) } doThrow CancellationException("Mock Cancellation Exception")
        }

        passwordManager.setSessionData(MASTER_PASSWORD.toByteArray(), KEY_VALUE)
        shouldThrow<CancellationException> {
            passwordManager.update(VAULT_LOGIN_ITEM, VAULT)
        }

        verify(mockVaultStore).update(any<VaultLoginProxy>(), eq(VAULT.id))
    }
}
