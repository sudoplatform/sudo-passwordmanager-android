/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudopasswordmanager.crypto.DefaultCryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.DefaultKeyDerivingKeyStore
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudosecurevault.SudoSecureVaultClient
import io.kotlintest.fail
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.numerics.shouldBeGreaterThan
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.UUID

/**
 * Test the operation of the [SudoPasswordManagerClient].
 *
 * @since 2020-09-29
 */
@RunWith(AndroidJUnit4::class)
class SudoPasswordManagerClientIntegrationTest : BaseIntegrationTest() {

    private val masterPassword = UUID.randomUUID().toString()
    private val newMasterPassword = UUID.randomUUID().toString()
    private val rescueKitFile = File(context.applicationContext.cacheDir, "RescueKit.pdf")

    private lateinit var passwordManagerClient: SudoPasswordManagerClient

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())

        if (clientConfigFilesPresent()) {
            passwordManagerClient = SudoPasswordManagerClient.builder()
                .setContext(context)
                .setSudoUserClient(userClient)
                .setSudoProfilesClient(sudoClient)
                .setLogger(logger)
                .build()
        }

        // remove rescue kit pdf from cache dir
        if (Files.exists(rescueKitFile.toPath())) {
            rescueKitFile.delete()
        }
    }

    @After
    fun fini() = runBlocking<Unit> {
        if (clientConfigFilesPresent()) {
            deleteAllSudos()
            passwordManagerClient.reset()
        }
        Timber.uprootAll()
    }

    @Test
    fun shouldThrowIfRequiredItemsNotProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        // All required items not provided
        shouldThrow<NullPointerException> {
            SudoPasswordManagerClient.builder().build()
        }

        // Context not provided
        shouldThrow<NullPointerException> {
            SudoPasswordManagerClient.builder()
                .setSudoUserClient(userClient)
                .build()
        }

        // SudoUserClient not provided
        shouldThrow<NullPointerException> {
            SudoPasswordManagerClient.builder()
                .setContext(context)
                .build()
        }
    }

    @Test
    fun shouldNotThrowIfTheRequiredItemsAreProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        SudoPasswordManagerClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .setSudoProfilesClient(sudoClient)
            .build()
    }

    @Test
    fun shouldNotThrowIfAllItemsAreProvidedToBuilder() {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        val secureVaultClient = SudoSecureVaultClient.builder(context, userClient).build()
        val cryptoProvider = DefaultCryptographyProvider(keyManager)
        val keyStore = DefaultKeyDerivingKeyStore(keyManager)

        SudoPasswordManagerClient.builder()
            .setContext(context)
            .setSudoUserClient(userClient)
            .setSudoProfilesClient(sudoClient)
            .setSudoSecureVaultClient(secureVaultClient)
            .setCryptographyProvider(cryptoProvider)
            .setKeyStore(keyStore)
            .setLogger(logger)
            .build()
    }

    @Test
    fun registerShouldChangeRegistrationState() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()

            signInAndRegisterUser()

            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.NOT_REGISTERED

            register(masterPassword)
            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.REGISTERED

            isLocked() shouldBe true
            shouldThrow<SudoPasswordManagerException.VaultLockedException> {
                listVaults()
            }

            reset()
            shouldThrow<SudoPasswordManagerException.UnauthorizedUserException> {
                getRegistrationStatus()
            }
        }
    }

    @Test
    fun operationsOnLockedVaultShouldThrow() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()

            signInAndRegisterUser()

            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.NOT_REGISTERED

            register(masterPassword)
            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.REGISTERED

            isLocked() shouldBe true
            shouldThrow<SudoPasswordManagerException.VaultLockedException> {
                listVaults()
            }
            shouldThrow<SudoPasswordManagerException.VaultLockedException> {
                getVault("vaultId")
            }
            shouldThrow<SudoPasswordManagerException.VaultLockedException> {
                createVault("sudoId")
            }
        }
    }

    /**
     * Test the happy path of password manager operations, which is the normal flow a
     * user would be expected to exercise.
     */
    @Test
    fun completeFlowShouldSucceed() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()

            // Register and sign in the user
            signInAndRegisterUser()
            userClient.isSignedIn() shouldBe true
            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.NOT_REGISTERED

            // Register with the password manager
            register(masterPassword)
            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.REGISTERED
            isLocked() shouldBe true
            getSecretCode().isNullOrBlank() shouldBe false

            // Unlock the password manager
            unlock(masterPassword)
            isLocked() shouldBe false

            // Create a Sudo that will be the vault owner
            val sudo = sudoClient.createSudo(AndroidTestData.SUDO)
            sudo shouldNotBe null
            sudo.id shouldNotBe null

            // Check the entitlements
            val entitlements = getEntitlementState()
            entitlements shouldHaveSize 1
            with(entitlements[0]) {
                name shouldBe EntitlementState.Name.MAX_VAULTS_PER_SUDO
                limit shouldBeGreaterThan 0
                value shouldBe 0
            }

            // Create a vault for the Sudo
            val vault = createVault(sudo.id!!)
            vault.id shouldNotBe null
            vault.createdAt.time shouldBeGreaterThan 0L
            vault.updatedAt.time shouldBeGreaterThan 0L
            val vaultId = vault.id
            getEntitlementState()[0].value shouldBe 1

            // Add a login to the vault
            var password = VaultItemPassword(SecureFieldValue(AndroidTestData.PLAIN_TEXT))
            var note = VaultItemNote(SecureFieldValue(AndroidTestData.PLAIN_TEXT))
            val itemId = UUID.randomUUID().toString()
            var item = VaultLogin(
                id = itemId,
                name = AndroidTestData.NAME,
                user = AndroidTestData.USER,
                password = password,
                notes = note
            )
            add(item, vault)
            update(vault)

            // Fetch the vault item and check it
            val items = listVaultItems(vault)
            items shouldHaveSize 1
            with(items[0]) {
                id shouldBe itemId
                createdAt.time shouldBeGreaterThan 0L
                updatedAt.time shouldBeGreaterThan 0L
            }
            var fetchedItem = getVaultItem(itemId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe item.name
            fetchedItem.password?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.url shouldBe item.url
            fetchedItem.user shouldBe item.user
            fetchedItem.previousPasswords shouldBe item.previousPasswords

            // Update the vault item
            val updatedItem = VaultLogin(
                id = itemId,
                name = AndroidTestData.NAME + "_updated",
                user = AndroidTestData.USER + "_updated",
                password = password,
                notes = note
            )
            update(updatedItem, vault)
            listVaultItems(vault) shouldHaveSize 1
            fetchedItem = getVaultItem(itemId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe item.name + "_updated"
            fetchedItem.password?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.url shouldBe item.url
            fetchedItem.user shouldBe item.user + "_updated"
            fetchedItem.previousPasswords shouldBe item.previousPasswords

            // Add another login to the vault
            password = VaultItemPassword(SecureFieldValue(AndroidTestData.PLAIN_TEXT))
            note = VaultItemNote(SecureFieldValue(AndroidTestData.PLAIN_TEXT))
            val itemId2 = UUID.randomUUID().toString()
            item = VaultLogin(
                id = itemId2,
                name = AndroidTestData.NAME,
                user = AndroidTestData.USER,
                password = password,
                notes = note
            )
            add(item, vault)
            update(vault)

            // Check there are two vault items
            listVaultItems(vault) shouldHaveSize 2

            // Delete the first vault item
            removeVaultItem(itemId, vault)
            listVaultItems(vault) shouldHaveSize 1
            getVaultItem(itemId, vault) shouldBe null
            fetchedItem = getVaultItem(itemId2, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe item.name
            fetchedItem.password?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.url shouldBe item.url
            fetchedItem.user shouldBe item.user

            // Change the master password and check the vault can still be read
            changeMasterPassword(masterPassword, newMasterPassword)
            listVaults() shouldHaveSize 1
            fetchedItem = getVaultItem(itemId2, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe item.name
            fetchedItem.password?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe AndroidTestData.PLAIN_TEXT
            fetchedItem.url shouldBe item.url
            fetchedItem.user shouldBe item.user

            // Delete the vault
            deleteVault(vaultId)
            listVaults() shouldHaveSize 0
            getEntitlementState()[0].value shouldBe 0

            lock()
            isLocked() shouldBe true
        }
    }

    @Test
    fun getSecretCode() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            getSecretCode() shouldBe null

            register(masterPassword)
            val secretCode = getSecretCode()
            secretCode shouldNotBe null
            secretCode!!.length shouldBe 43 // 32 + 5 user prefix + 6 hyphens
            val hyphenPositions = arrayOf(5, 12, 18, 24, 30, 36)
            hyphenPositions.forEach { pos ->
                secretCode[pos] shouldBe '-'
            }
        }
    }

    @Test
    fun unlockShouldSucceedWithSecretCode() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)
            val secretCode = getSecretCode()
            secretCode shouldNotBe null
            unlock(masterPassword, secretCode)

            isLocked() shouldBe false
        }
    }

    @Test
    fun lockShouldSucceedAfterSuccessfulUnlock() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)

            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)
            isLocked() shouldBe false

            lock()
            isLocked() shouldBe true
        }
    }

    @Test
    fun createVaultShouldSucceedWithUnlockedVault() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)
            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)
            isLocked() shouldBe false

            val sudo = sudoClient.createSudo(AndroidTestData.SUDO)

            shouldNotThrow<SudoPasswordManagerException.VaultLockedException> {
                createVault(sudo.id!!)
            }
        }
    }

    @Test
    fun listVaultsShouldSucceedAfterVaultCreation() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)
            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)
            isLocked() shouldBe false

            val sudo = sudoClient.createSudo(AndroidTestData.SUDO)

            shouldNotThrow<SudoPasswordManagerException.VaultLockedException> {
                createVault(sudo.id!!)
            }

            shouldNotThrow<SudoPasswordManagerException.VaultLockedException> {
                listVaults() shouldHaveSize 1
            }
        }
    }

    @Test
    fun generatePDFWithoutTemplate() = runBlocking<Unit> {
        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)
            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)

            withContext(Dispatchers.IO) { // Check it can run as a suspend function
                val doc = renderRescueKit(context)
                try {
                    FileOutputStream(rescueKitFile).use {
                        doc.writeTo(it)
                    }
                } catch (e: Exception) {
                    fail("Failed to write pdf to cache directory: $e")
                }
                doc.close()
            }
            Files.exists(rescueKitFile.toPath()) shouldBe true
        }
    }

    @Test
    fun generatePDFWithTemplate() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)
            val inputStream = context.assets.open("test_image.png")
            val byteArray = ByteArray(inputStream.available())
            inputStream.read(byteArray)
            inputStream.close()
            val doc = renderRescueKit(context, byteArray)
            try {
                FileOutputStream(rescueKitFile).use {
                    doc.writeTo(it)
                }
            } catch (e: Exception) {
                fail("Failed too write pdf to cache directory: $e")
            }
            doc.close()
            Files.exists(rescueKitFile.toPath()) shouldBe true
        }
    }

    @Test
    fun resetShouldAlsoLock() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)

            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)

            isLocked() shouldBe false

            reset()
            isLocked() shouldBe true
        }
    }

    @Test
    fun deregisterShouldAlsoResetAndLock() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()
            signInAndRegisterUser()

            register(masterPassword)

            val secretCode = getSecretCode()
            unlock(masterPassword, secretCode)

            isLocked() shouldBe false

            deregister()
            isLocked() shouldBe true
            userClient.isSignedIn() shouldBe false
        }
    }
}
