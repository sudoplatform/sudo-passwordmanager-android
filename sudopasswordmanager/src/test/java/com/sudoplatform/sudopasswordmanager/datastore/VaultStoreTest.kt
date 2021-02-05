/*
 * Copyright © 2020 - Anonyome Labs, Inc. - All rights reserved
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.datastore

import com.nhaarman.mockitokotlin2.times
import com.sudoplatform.sudopasswordmanager.BaseTests
import com.sudoplatform.sudopasswordmanager.util.SUDO_SERVICE_ISSUER
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.TestData
import com.sudoplatform.sudopasswordmanager.TestData.LOGIN
import com.sudoplatform.sudopasswordmanager.TestData.SECURE_VAULT
import com.sudoplatform.sudopasswordmanager.TestData.SUDO_ID
import com.sudoplatform.sudopasswordmanager.TestData.VAULT_PROXY
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudosecurevault.Owner
import com.sudoplatform.sudosecurevault.Vault
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import java.util.UUID

/**
 * Test the operation of the [DefaultVaultStore].
 *
 * @since 2020-10-08
 */
@RunWith(RobolectricTestRunner::class)
internal class VaultStoreTest : BaseTests() {

    private val store by before {
        DefaultVaultStore()
    }

    private val jsonBlob = """
            { 
                "bankAccount": [],
                "creditCard": [],
                "generatedPassword": [],
                "login": [],
                "revealKey": "key42",
                "schemaVersion": 1.0
            }
        """.trimIndent().toByteArray()

    private val secureVault = Vault(
        id = "id",
        owner = "owner",
        version = 1,
        blobFormat = "blobFormat",
        createdAt = Date(0),
        updatedAt = Date(1),
        owners = listOf(Owner(id = SUDO_ID, issuer = SUDO_SERVICE_ISSUER)),
        blob = jsonBlob
    )

    @Test
    fun `can import, get and delete vaults`() {
        store.listVaults() shouldHaveSize 0
        store.importVault(VAULT_PROXY)
        store.listVaults() shouldHaveSize 1

        val vaultId = VAULT_PROXY.secureVaultId
        val vaultProxy = store.getVault(vaultId)
        vaultProxy shouldNotBe null
        vaultProxy?.secureVaultId shouldBe vaultId

        store.add(LOGIN, vaultId)
        store.removeVaultItem(LOGIN.id, vaultId)
        store.listVaults() shouldHaveSize 1

        store.deleteVault(vaultId)
        store.listVaults() shouldHaveSize 0
    }

    @Test
    fun `removeAll clears the vaults`() {
        store.listVaults() shouldHaveSize 0
        store.importVault(VAULT_PROXY)
        store.listVaults() shouldHaveSize 1
        store.removeAll()
        store.listVaults() shouldHaveSize 0
    }

    @Test
    fun `can import secure vaults`() {
        store.importSecureVaults(listOf(secureVault))
        store.listVaults() shouldHaveSize 1
    }

    @Test
    fun `can update a vault`() {
        store.importSecureVaults(listOf(secureVault))
        store.listVaults() shouldHaveSize 1
        var vaultProxy = store.getVault("id")
        val updatedAt = vaultProxy?.updatedAt

        store.updateVault(SECURE_VAULT)
        vaultProxy = store.getVault("id")
        vaultProxy?.updatedAt?.time ?: 0L shouldBeGreaterThan (updatedAt?.time ?: 0L)
    }

    @Test
    fun `can update a vault item`() {
        store.importSecureVaults(listOf(secureVault))
        store.listVaults() shouldHaveSize 1
        var vaultProxy = store.getVault("id")
        val updatedAt = vaultProxy?.updatedAt

        store.update(LOGIN, "id")
        vaultProxy = store.getVault("id")
        vaultProxy?.updatedAt?.time ?: 0L shouldBeGreaterThan (updatedAt?.time ?: 0L)
    }

    @Test
    fun `methods throw if vault does not exist`() {
        store.listVaults() shouldHaveSize 0

        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            store.add(LOGIN, "42")
        }

        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            store.update(LOGIN, "42")
        }

        shouldThrow<SudoPasswordManagerException.VaultNotFoundException> {
            store.removeVaultItem("42", "42")
        }
    }

    private val MAX_PARALLEL = 200
    private var failureCount = 0

    @Test
    fun `coroutine test`() = runBlocking<Unit> {
        failureCount = 0
        val deferreds = mutableListOf<Deferred<Unit>>()
        for (i in 0..MAX_PARALLEL) {
            val deferred = async<Unit> {
                exerciseVaultStore()
            }
            deferreds.add(deferred)
        }
        deferreds.forEach { it.start() }
        deferreds.forEach { it.await() }
        failureCount shouldBe 0
    }

    @Test
    fun `multithread test`() {
        failureCount = 0
        val threads = mutableListOf<Thread>()
        val runnable = object : Runnable {
            override fun run() {
                exerciseVaultStore()
            }
        }
        for (i in 0..MAX_PARALLEL) {
            threads.add(Thread(runnable))
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        failureCount shouldBe 0
    }

    private fun exerciseVaultStore() {
        try {
            val vaultProxy = makeVaultProxy()
            store.importVault(vaultProxy)
            if (store.getVault(vaultProxy.secureVaultId) == null) {
                throw NullPointerException("getVault returned null")
            }
            store.update(LOGIN, vaultProxy.secureVaultId)
            store.removeVaultItem(LOGIN.id, vaultProxy.secureVaultId)
            store.deleteVault(vaultProxy.secureVaultId)
        } catch (e: Throwable) {
            println("$e")
            failureCount++
        }
    }

    private fun makeVaultProxy() =
        VaultProxy(
            secureVaultId = UUID.randomUUID().toString(),
            version = 1,
            createdAt = Date(0),
            updatedAt = Date(1),
            vaultData = TestData.VAULT_SCHEMA,
            owners = TestData.OWNERS
        )

    @Test
    fun `throws when JSON deserialisation throws`() = runBlocking<Unit> {

        val badVault = com.sudoplatform.sudosecurevault.Vault(
            id = "id",
            owner = "owner",
            version = 1,
            blobFormat = VaultSchema.FORMAT_V1,
            blob = "some bad json".toByteArray(),
            createdAt = Date(0),
            updatedAt = Date(1),
            owners = emptyList()
        )

        shouldThrow<SudoPasswordManagerException.InvalidFormatException> {
            store.importSecureVaults(listOf(badVault))
        }
    }
}
