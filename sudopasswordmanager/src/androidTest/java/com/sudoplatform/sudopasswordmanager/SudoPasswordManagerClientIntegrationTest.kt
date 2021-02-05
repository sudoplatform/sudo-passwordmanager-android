/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudopasswordmanager.crypto.DefaultCryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.DefaultKeyDerivingKeyStore
import com.sudoplatform.sudopasswordmanager.entitlements.Entitlement
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
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

    @Test
    fun operationsOnLockedVaultShouldNotThrow() = runBlocking<Unit> {

        // Can only run if client config files are present
        assumeTrue(clientConfigFilesPresent())

        with(passwordManagerClient) {
            reset()

            signInAndRegisterUser()

            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.NOT_REGISTERED

            register(masterPassword)
            getRegistrationStatus() shouldBe PasswordManagerRegistrationStatus.REGISTERED

            isLocked() shouldBe true
            getEntitlementState() shouldHaveSize 0
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
            val sudo = sudoClient.createSudo(TestData.SUDO)
            sudo shouldNotBe null
            sudo.id shouldNotBe null

            // Check the entitlements
            val entitlements = getEntitlementState()
            entitlements shouldHaveSize 1
            with(entitlements[0]) {
                name shouldBe Entitlement.Name.MAX_VAULTS_PER_SUDO
                limit shouldBeGreaterThan 0
                value shouldBe 0
            }

            // Create a vault for the Sudo
            var vault = createVault(sudo.id!!)
            vault.id shouldNotBe null
            vault.createdAt.time shouldBeGreaterThan 0L
            vault.updatedAt.time shouldBeGreaterThan 0L
            val vaultId = vault.id
            getEntitlementState()[0].value shouldBe 1

            // Add a login, credit card, and a bank account to the vault
            var password = VaultItemPassword(SecureFieldValue(TestData.PLAIN_TEXT))
            var note = VaultItemNote(SecureFieldValue(TestData.PLAIN_TEXT))
            val loginId = UUID.randomUUID().toString()
            var login = VaultLogin(
                id = loginId,
                name = TestData.NAME,
                user = TestData.USER,
                password = password,
                notes = note
            )
            add(login, vault)

            val cardNumber = VaultItemValue(SecureFieldValue(TestData.PLAIN_TEXT))
            val securityCode = VaultItemValue(SecureFieldValue(TestData.PLAIN_TEXT))
            val creditCardId = UUID.randomUUID().toString()
            val creditCard = VaultCreditCard(
                id = creditCardId,
                name = TestData.NAME,
                notes = note,
                cardName = TestData.NAME,
                cardNumber = cardNumber,
                cardType = "Visa",
                securityCode = securityCode
            )
            add(creditCard, vault)
            update(vault)

            val bankAccountNumber = VaultItemValue(SecureFieldValue(TestData.PLAIN_TEXT))
            val bankAccountPin = VaultItemValue(SecureFieldValue(TestData.PLAIN_TEXT))
            val bankAccountId = UUID.randomUUID().toString()
            val bankAccount = VaultBankAccount(
                id = bankAccountId,
                name = TestData.NAME,
                notes = note,
                accountNumber = bankAccountNumber,
                accountType = "Savings",
                accountPin = bankAccountPin
            )
            add(bankAccount, vault)
            update(vault)

            // Fetch the vault items and check them
            val items = listVaultItems(vault)
            items shouldHaveSize 3
            items.forEach {
                if (it is VaultLogin) {
                    it.id shouldBe loginId
                    it.createdAt.time shouldBeGreaterThan 0L
                    it.updatedAt.time shouldBeGreaterThan 0L
                } else if (it is VaultCreditCard) {
                    it.id shouldBe creditCardId
                    it.createdAt.time shouldBeGreaterThan 0L
                    it.updatedAt.time shouldBeGreaterThan 0L
                } else if (it is VaultBankAccount) {
                    it.id shouldBe bankAccountId
                    it.createdAt.time shouldBeGreaterThan 0L
                    it.updatedAt.time shouldBeGreaterThan 0L
                }
            }
            var fetchedItem = getVaultItem(loginId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe login.name
            fetchedItem.password?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.url shouldBe login.url
            fetchedItem.user shouldBe login.user
            fetchedItem.previousPasswords shouldBe login.previousPasswords

            fetchedItem = getVaultItem(creditCardId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultCreditCard) shouldBe true
            fetchedItem as VaultCreditCard
            fetchedItem.name shouldBe creditCard.name
            fetchedItem.cardName shouldBe TestData.NAME
            fetchedItem.cardNumber?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.securityCode?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT

            fetchedItem = getVaultItem(bankAccountId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultBankAccount) shouldBe true
            fetchedItem as VaultBankAccount
            fetchedItem.name shouldBe bankAccount.name
            fetchedItem.accountNumber?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.accountPin?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT
            // Update the vault item
            val updatedItem = VaultLogin(
                id = loginId,
                name = TestData.NAME + "_updated",
                user = TestData.USER + "_updated",
                password = password,
                notes = note
            )
            update(updatedItem, vault)
            listVaultItems(vault) shouldHaveSize 3
            fetchedItem = getVaultItem(loginId, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe login.name + "_updated"
            fetchedItem.password?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.url shouldBe login.url
            fetchedItem.user shouldBe login.user + "_updated"
            fetchedItem.previousPasswords shouldBe login.previousPasswords

            // Add another login to the vault
            password = VaultItemPassword(SecureFieldValue(TestData.PLAIN_TEXT))
            note = VaultItemNote(SecureFieldValue(TestData.PLAIN_TEXT))
            val loginId2 = UUID.randomUUID().toString()
            login = VaultLogin(
                id = loginId2,
                name = TestData.NAME,
                user = TestData.USER,
                password = password,
                notes = note
            )
            add(login, vault)
            update(vault)

            // Check there are four vault items
            listVaultItems(vault) shouldHaveSize 4

            // Delete the first vault item
            removeVaultItem(loginId, vault)
            listVaultItems(vault) shouldHaveSize 3
            getVaultItem(loginId, vault) shouldBe null
            fetchedItem = getVaultItem(loginId2, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe login.name
            fetchedItem.password?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.url shouldBe login.url
            fetchedItem.user shouldBe login.user

            // Change the master password and check the vault can still be read
            changeMasterPassword(masterPassword, newMasterPassword)
            listVaults() shouldHaveSize 1
            vault = listVaults().first()
            fetchedItem = getVaultItem(loginId2, vault)
            fetchedItem shouldNotBe null
            (fetchedItem is VaultLogin) shouldBe true
            fetchedItem as VaultLogin
            fetchedItem.name shouldBe login.name
            fetchedItem.password?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.notes?.getValue() shouldBe TestData.PLAIN_TEXT
            fetchedItem.url shouldBe login.url
            fetchedItem.user shouldBe login.user

            // Delete the credit card item
            removeVaultItem(creditCardId, vault)
            listVaultItems(vault) shouldHaveSize 2
            getVaultItem(creditCardId, vault) shouldBe null

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

            val sudo = sudoClient.createSudo(TestData.SUDO)

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

            val sudo = sudoClient.createSudo(TestData.SUDO)

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
