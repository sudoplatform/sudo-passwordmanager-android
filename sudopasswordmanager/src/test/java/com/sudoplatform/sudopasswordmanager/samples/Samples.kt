/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.samples

import android.content.Context
import android.net.Uri
import com.nhaarman.mockitokotlin2.mock
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus
import com.sudoplatform.sudopasswordmanager.PasswordStrength
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerClient
import com.sudoplatform.sudopasswordmanager.calculateStrengthOfPassword
import com.sudoplatform.sudopasswordmanager.entitlements.Entitlement
import com.sudoplatform.sudopasswordmanager.generatePassword
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.RuntimeException
import java.util.Calendar

/**
 * These are sample snippets of code that are included in the generated documentation. They are
 * placed here in the test code so that at least we know they will compile.
 *
 * @since 2020-09-23
 */
@RunWith(RobolectricTestRunner::class)
@Suppress("UNUSED_VARIABLE")
class Samples {

    @Test
    fun mockTest() {
        // Just to keep junit happy
    }

    fun generatePasswordSample() {
        val myPassword = generatePassword(
            length = 20,
            allowUppercase = true,
            allowLowercase = true,
            allowNumbers = true,
            allowSymbols = true
        )
    }

    private val myPassword = "password1"

    fun calculateStrengthOfPasswordSample() {
        val passwordStrength = calculateStrengthOfPassword(myPassword)
        when (passwordStrength) {
            PasswordStrength.VeryWeak,
            PasswordStrength.Weak -> println("Please choose a stronger password")
            else -> {}
        }
    }

    private val context = mock<Context>()

    fun buildClient() {
        // This is how to construct the SudoPasswordManagerClient

        // Create a logger for any messages or errors
        val logger = Logger("MyApplication", AndroidUtilsLogDriver(LogLevel.INFO))

        // Create an instance of SudoUserClient to perform registration and sign in.
        val sudoUserClient = SudoUserClient.builder(context)
            .setNamespace("com.mycompany.myapplication")
            .setLogger(logger)
            .build()

        // Create an instance of SudoProfilesClient to perform creation, deletion and modification of Sudos.
        val blobURI = Uri.fromFile(context.cacheDir)
        val sudoProfilesClient = SudoProfilesClient.builder(context, sudoUserClient, blobURI)
            .setLogger(logger)
            .build()

        // Create an instance of SudoPasswordManagerClient to manipulate secure vaults of sensitive items
        val sudoPasswordManager = SudoPasswordManagerClient.builder()
            .setContext(context)
            .setSudoUserClient(sudoUserClient)
            .setSudoProfilesClient(sudoProfilesClient)
            .setLogger(logger)
            .build()
    }

    private lateinit var client: SudoPasswordManagerClient

    // This function hides the GlobalScope from the code used in the documentation. The use
    // of GlobalScope is not something that should be recommended in the code samples.
    private fun launch(
        block: suspend CoroutineScope.() -> Unit
    ) = GlobalScope.launch { block.invoke(GlobalScope) }

    fun getSecretCode() {
        val secretCode = client.getSecretCode()
        // Save or copy to the clipboard so the user can save it
    }

    fun isLocked() {
        if (client.isLocked()) {
            // Password manager must be unlocked before use
        }
    }

    fun getRegistrationStatus() {
        launch {
            val status = withContext(Dispatchers.IO) {
                client.getRegistrationStatus()
            }
            when (status) {
                PasswordManagerRegistrationStatus.REGISTERED -> {
                    // Password manager is registered and if it's unlocked it can be used
                }
                PasswordManagerRegistrationStatus.NOT_REGISTERED -> {
                    // Password manager must be registered and unlocked before use
                }
                PasswordManagerRegistrationStatus.MISSING_SECRET_CODE -> {
                    // Password manager must be registered and unlocked with the secret code
                    // to recover the encryption keys before use
                }
            }
        }
    }

    private val masterPassword = ""

    fun register() {
        launch {
            withContext(Dispatchers.IO) {
                client.register(masterPassword)
            }
        }
    }

    fun deregister() {
        launch {
            withContext(Dispatchers.IO) {
                client.deregister()
            }
        }
    }

    fun lock() {
        launch {
            withContext(Dispatchers.IO) {
                client.lock()
            }
        }
    }

    private val secretCode = ""

    fun unlock() {
        launch {
            withContext(Dispatchers.IO) {
                val status = client.getRegistrationStatus()
                if (status == PasswordManagerRegistrationStatus.MISSING_SECRET_CODE) {
                    // Password manager must be unlocked with the secret code
                    // to recover the encryption keys before use
                    client.unlock(masterPassword, secretCode)
                } else {
                    client.unlock(masterPassword)
                }
            }
        }
    }

    fun reset() {
        launch {
            withContext(Dispatchers.IO) {
                client.reset()
            }
        }
    }

    private val sudoId = ""
    private lateinit var vault: Vault

    fun createVault() {
        launch {
            vault = withContext(Dispatchers.IO) {
                client.createVault(sudoId)
            }
        }
    }

    fun listVaults() {
        launch {
            val vaults = withContext(Dispatchers.IO) {
                client.listVaults()
            }
        }
    }

    private val vaultId = ""

    fun getVault() {
        launch {
            val vault = withContext(Dispatchers.IO) {
                client.getVault(vaultId)
            }
        }
    }

    fun update() {
        launch {
            withContext(Dispatchers.IO) {
                // Add or modify something in a vault and then call update to save the mods
                client.update(vault)
            }
        }
    }

    fun deleteVault() {
        launch {
            withContext(Dispatchers.IO) {
                client.deleteVault(vaultId)
            }
        }
    }

    private val newMasterPassword = ""

    fun changeMasterPassword() {
        launch {
            withContext(Dispatchers.IO) {
                client.changeMasterPassword(masterPassword, newMasterPassword)
            }
        }
    }

    private val loginPassword = ""
    private val loginNotes = ""
    private val cardNumber = ""
    private val securityCode = ""
    private val cardNotes = ""

    fun add() {
        launch {
            withContext(Dispatchers.IO) {
                val login = VaultLogin(
                    name = "Daily News",
                    url = "http://brisbanetimes.com.au",
                    user = "tedbear",
                    password = VaultItemPassword(SecureFieldValue(loginPassword)),
                    notes = VaultItemNote(SecureFieldValue(loginNotes))
                )
                client.add(login, vault)
                val expiry = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2024)
                    set(Calendar.MONTH, 0) // January
                }
                val creditCard = VaultCreditCard(
                    name = "My Visa",
                    cardName = "Ted Bear",
                    cardType = "Visa",
                    cardNumber = VaultItemValue(SecureFieldValue(cardNumber)),
                    securityCode = VaultItemValue(SecureFieldValue(securityCode)),
                    notes = VaultItemNote(SecureFieldValue(cardNotes)),
                    expiresAt = expiry.time
                )
                client.add(creditCard, vault)
                client.update(vault)
            }
        }
    }

    fun listVaultItems() {
        launch {
            // Find all my credit card details
            val creditCards = withContext(Dispatchers.IO) {
                client.listVaultItems(vault).filter { it is VaultCreditCard }
            }
        }
    }

    private val visaCardId = ""

    fun getVaultItem() {
        launch {
            val visaCardDetails = withContext(Dispatchers.IO) {
                client.getVaultItem(visaCardId, vault)
            } ?: throw RuntimeException("Visa card missing")
        }
    }

    fun updateVaultItem() {
        launch {
            withContext(Dispatchers.IO) {
                val creditCard = client.getVaultItem(visaCardId, vault) as? VaultCreditCard
                    ?: return@withContext
                // Update the card expiry
                val expiry = Calendar.getInstance().apply {
                    set(Calendar.YEAR, 2026)
                    set(Calendar.MONTH, 5) // June
                }
                client.update(creditCard.copy(expiresAt = expiry.time), vault)
            }
        }
    }

    fun removeVaultItem() {
        launch {
            withContext(Dispatchers.IO) {
                client.removeVaultItem(visaCardId, vault)
            }
        }
    }

    fun renderRescueKit() {
        launch {
            withContext(Dispatchers.IO) {
                val pdfDocument = client.renderRescueKit(context)
                // Share the PDF document so the user can save it somewhere safe
            }
        }
    }

    fun getEntitlement() {
        launch {
            val vaultsAllowed = withContext(Dispatchers.IO) {
                client.getEntitlement().find {
                    it.name == Entitlement.Name.MAX_VAULTS_PER_SUDO
                }
                    ?.limit
                    ?: 0
            }
        }
    }

    fun getEntitlementState() {
        launch {
            val vaultsRemaining = withContext(Dispatchers.IO) {
                client.getEntitlementState().find {
                    it.name == Entitlement.Name.MAX_VAULTS_PER_SUDO
                }
                    ?.value
                    ?: 0
            }
        }
    }
}
