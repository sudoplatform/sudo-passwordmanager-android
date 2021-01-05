/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudoconfigmanager.DefaultSudoConfigManager
import com.sudoplatform.sudopasswordmanager.util.SECURE_VAULT_AUDIENCE
import com.sudoplatform.sudopasswordmanager.util.SUDO_SERVICE_ISSUER
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudosecurevault.SudoSecureVaultClient
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.UUID

private const val KEY_SIZE_BITS = 256
private const val IS_ID_637_FIXED = false

/**
 * Test the operation of the [SudoSecureVaultClient].
 *
 * @since 2020-10-20
 */
@RunWith(AndroidJUnit4::class)
class SudoSecureVaultClientTest : BaseIntegrationTest() {

    private val masterPassword = UUID.randomUUID().toString().toByteArray()
    private val newMasterPassword = UUID.randomUUID().toString().toByteArray()
    private lateinit var keyDerivingKey: ByteArray
    private lateinit var secureVaultClient: SudoSecureVaultClient

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())

        if (clientConfigFilesPresent()) {

            val configManager = DefaultSudoConfigManager(context, logger)
            val secureVaultConfig = configManager.getConfigSet("secureVaultService")
                ?: throw FileNotFoundException("SudoSecureVaultService config stanza is missing from sudoplatformconfig.json")

            secureVaultClient = SudoSecureVaultClient.builder(context, userClient)
                .setLogger(logger)
                .setConfig(secureVaultConfig)
                .build()

            keyDerivingKey = keyManager.createRandomData(KEY_SIZE_BITS)
        }
    }

    @After
    fun fini() = runBlocking<Unit> {
        if (clientConfigFilesPresent()) {
            deleteAllSudos()
            secureVaultClient.reset()
            userClient.deregister()
        }
        Timber.uprootAll()
    }

    @Test
    fun registerShouldChangeRegistrationState() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(secureVaultClient) {
            reset()
            signInAndRegisterUser()

            isRegistered() shouldBe false

            register(keyDerivingKey, masterPassword)
            isRegistered() shouldBe true

            getInitializationData() shouldNotBe null
        }
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

    @Test
    fun completeFlowShouldSucceed() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(secureVaultClient) {
            reset()
            signInAndRegisterUser()

            isRegistered() shouldBe false

            register(keyDerivingKey, masterPassword)
            isRegistered() shouldBe true

            listVaultsMetadataOnly() shouldHaveSize 0
            listVaults(keyDerivingKey, masterPassword) shouldHaveSize 0

            val sudo = sudoClient.createSudo(AndroidTestData.SUDO)
            val ownershipProof = getOwnershipProof(sudo)

            val vaultMetadata = createVault(keyDerivingKey, masterPassword, jsonBlob, "json", ownershipProof)
            with(vaultMetadata) {
                id shouldNotBe ""
                blobFormat shouldBe "json"
                owners shouldHaveSize 2
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 1
            }

            val vaults = listVaults(keyDerivingKey, masterPassword)
            vaults shouldHaveSize 1
            with(vaults[0]) {
                id shouldBe vaultMetadata.id
                blobFormat shouldBe vaultMetadata.blobFormat
                blob shouldBe jsonBlob
                owners shouldHaveSize 2
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 1
            }

            val vaultsMetadata = listVaultsMetadataOnly()
            vaultsMetadata shouldHaveSize 1
            with(vaultsMetadata[0]) {
                id shouldBe vaultMetadata.id
                blobFormat shouldBe vaultMetadata.blobFormat
                owners shouldHaveSize 2
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 1
            }

            var vault = getVault(keyDerivingKey, masterPassword, vaultMetadata.id)
            vault shouldNotBe null
            with(vault!!) {
                id shouldBe vaultMetadata.id
                blobFormat shouldBe vaultMetadata.blobFormat
                blob shouldBe jsonBlob
                owners shouldHaveSize 2
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 1
            }
            getVault(keyDerivingKey, masterPassword, "bogusVaultId") shouldBe null

            changeVaultPassword(keyDerivingKey, masterPassword, newMasterPassword)

            vault = getVault(keyDerivingKey, newMasterPassword, vaultMetadata.id)
            vault shouldNotBe null
            with(vault!!) {
                id shouldBe vaultMetadata.id
                blobFormat shouldBe vaultMetadata.blobFormat
                blob shouldBe jsonBlob
                owners shouldHaveSize 2
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 2
            }

            val deletedVault = deleteVault(vaultMetadata.id)
            deletedVault shouldNotBe null
            with(deletedVault!!) {
                id shouldNotBe ""
                blobFormat shouldBe "json"
                owners shouldHaveSize 2
                val sudoOwner = owners.find { it.issuer == SUDO_SERVICE_ISSUER }
                sudoOwner shouldNotBe null
                sudoOwner!!.id shouldBe sudo.id
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
                version shouldBe 2
            }
            deleteVault("bogusVaultId") shouldBe null

            listVaults(keyDerivingKey, newMasterPassword) shouldHaveSize 0
            listVaultsMetadataOnly() shouldHaveSize 0

            // This method below doesn't throw when given a bad password. Enable when ID-637 is fixed.
            if (IS_ID_637_FIXED) {
                shouldThrow<Throwable> {
                    listVaults(keyDerivingKey, "badpassword".toByteArray())
                }
            }
        }
    }

    private suspend fun getOwnershipProof(sudo: Sudo): String {
        return sudoClient.getOwnershipProof(Sudo(sudo.id), SECURE_VAULT_AUDIENCE)
    }
}
