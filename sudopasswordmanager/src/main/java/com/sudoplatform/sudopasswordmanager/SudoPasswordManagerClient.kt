/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudoconfigmanager.DefaultSudoConfigManager
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudopasswordmanager.crypto.CryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.DefaultCryptographyProvider
import com.sudoplatform.sudopasswordmanager.crypto.DefaultKeyDerivingKeyStore
import com.sudoplatform.sudopasswordmanager.crypto.KeyDerivingKeyStore
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import com.sudoplatform.sudopasswordmanager.logging.LogConstants
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudosecurevault.SudoSecureVaultClient
import com.sudoplatform.sudouser.SudoUserClient
import java.io.FileNotFoundException
import java.util.Objects

/**
 * The registration status of the [SudoPasswordManagerClient].
 */
enum class PasswordManagerRegistrationStatus {
    /** Returning user, known device.  User can interact with vaults after unlocking with master password. */
    REGISTERED,

    /** New User, new device. This is a first time user.  User must choose a master password and register. */
    NOT_REGISTERED,

    /** Returning user, new device. Password manager requires the secret code which was generated when they first registered. */
    MISSING_SECRET_CODE
}

/**
 * Interface encapsulating a library for interacting with the Sudo Platform Password Manager service.
 * @sample com.sudoplatform.sudopasswordmanager.samples.Samples.buildClient
 */
interface SudoPasswordManagerClient {

    companion object {
        /** Create a [Builder] for [SudoPasswordManagerClient]. */
        @JvmStatic
        fun builder() = Builder()
    }

    /**
     * Builder used to construct the [SudoPasswordManagerClient].
     */
    class Builder internal constructor() {
        private var context: Context? = null
        private var sudoUserClient: SudoUserClient? = null
        private var sudoProfilesClient: SudoProfilesClient? = null
        private var sudoSecureVaultClient: SudoSecureVaultClient? = null
        private var cryptoProvider: CryptographyProvider? = null
        private var keyStore: KeyDerivingKeyStore? = null
        private var logger: Logger = Logger(LogConstants.SUDOLOG_TAG, AndroidUtilsLogDriver(LogLevel.INFO))

        /**
         * Provide the application context (required input).
         */
        fun setContext(context: Context) = also {
            it.context = context
        }

        /**
         * Provide the implementation of the [SudoUserClient] used to perform
         * sign in and ownership operations (required input).
         */
        fun setSudoUserClient(sudoUserClient: SudoUserClient) = also {
            it.sudoUserClient = sudoUserClient
        }

        /**
         * Provide the implementation of the [SudoProfilesClient] used to perform
         * ownership proof lifecycle operations (required input).
         */
        fun setSudoProfilesClient(sudoProfilesClient: SudoProfilesClient) = also {
            it.sudoProfilesClient = sudoProfilesClient
        }

        /**
         * Provide the implementation of the [SudoSecureVaultClient] used for vault management
         * operations (optional input). If a value is not supplied a default implementation
         * will be used.
         */
        fun setSudoSecureVaultClient(vaultClient: SudoSecureVaultClient) = also {
            it.sudoSecureVaultClient = vaultClient
        }

        /**
         * Provide the implementation of the [CryptographyProvider] used for cryptographic
         * operations (optional input). If a value is not supplied a default implementation
         * will be used.
         */
        fun setCryptographyProvider(cryptographyProvider: CryptographyProvider) = also {
            it.cryptoProvider = cryptographyProvider
        }

        /**
         * Provide the implementation of the [KeyDerivingKeyStore] used for key management and
         * cryptographic operations (optional input). If a value is not supplied a default
         * implementation will be used.
         */
        fun setKeyStore(keyStore: KeyDerivingKeyStore) = also {
            it.keyStore = keyStore
        }

        /**
         * Provide the implementation of the [Logger] used for logging errors (optional input).
         * If a value is not supplied a default implementation will be used.
         */
        fun setLogger(logger: Logger) = also {
            it.logger = logger
        }

        /**
         * Construct the [SudoPasswordManagerClient]. Will throw a [NullPointerException] if
         * the [context] or [sudoUserClient] have not been provided.
         */
        @Throws(NullPointerException::class)
        fun build(): SudoPasswordManagerClient {
            Objects.requireNonNull(context, "Context must be provided.")
            Objects.requireNonNull(sudoUserClient, "SudoUserClient must be provided.")
            Objects.requireNonNull(sudoProfilesClient, "SudoProfilesClient must be provided.")

            val secureVaultConfig by lazy {
                DefaultSudoConfigManager(context!!, logger).getConfigSet("secureVaultService")
                    ?: throw FileNotFoundException("SudoSecureVaultService config stanza is missing from sudoplatformconfig.json")
            }

            val secureVaultClient = sudoSecureVaultClient
                ?: SudoSecureVaultClient.builder(context!!, sudoUserClient!!)
                        .setLogger(logger)
                        .setConfig(secureVaultConfig)
                        .build()

            val keyManager by lazy {
                KeyManagerFactory(context!!).createAndroidKeyManager()
            }

            val entitlementsClient = SudoEntitlementsClient.builder()
                .setContext(context!!)
                .setSudoUserClient(sudoUserClient!!)
                .setLogger(logger)
                .build()

            val passwordClientService = DefaultPasswordClientService(
                cryptoProvider = cryptoProvider ?: DefaultCryptographyProvider(keyManager),
                keyStore = keyStore ?: DefaultKeyDerivingKeyStore(keyManager),
                userClient = sudoUserClient!!,
                secureVaultClient = secureVaultClient,
                profilesClient = sudoProfilesClient!!,
                entitlementsClient = entitlementsClient
            )
            return DefaultPasswordManagerClient(passwordClientService, logger)
        }
    }

    /**
     * Checks if the password manager is registered.
     *
     * @return The registration status
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun getRegistrationStatus(): PasswordManagerRegistrationStatus

    /**
     * Registers with the password manager service. On successful return
     * the password manager will be locked. If the user is already registered
     * the [SudoPasswordManagerClient.PasswordManagerException.AlreadyRegisteredException]
     * will be thrown.
     *
     * @param masterPassword The master password that will be used to secure vaults.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun register(masterPassword: String)

    /**
     * Deregisters with the password manager. This will delete all the vaults.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun deregister()

    /**
     * Returns the secret code needed to bootstrap a new device.
     * This is part of a rescue kit and should be backed up in a secure location.
     */
    fun getSecretCode(): String?

    /**
     * Locks the password manager. If the password manager hasn't been registered, or the secret
     * code is missing, this function does nothing.
     */
    suspend fun lock()

    /**
     * Unlocks the password manager.
     *
     * @param masterPassword Master password of the password manager
     * @param secretCode The secret code to unlock on a new device. Can be null if password
     * manager has been used on this device in the past.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun unlock(masterPassword: String, secretCode: String? = null)

    /**
     * Resets the client and removes any encryption keys and data. You should backup the secret
     * code before calling this function. All vault data will be irretrievably lost if the key
     * is lost.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun reset()

    /**
     * Checks if the vault is locked or not
     *
     * @return True if the vault is unlocked, otherwise false.
     */
    fun isLocked(): Boolean

    /**
     * Creates a new vault on the service. Requires password manager to be registered and unlocked.
     *
     * @param sudoId Identifier of the [Sudo] to associate with the vault.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun createVault(sudoId: String): Vault

    /**
     * Fetches all vaults. Requires password manager to be registered and unlocked.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun listVaults(): List<Vault>

    /**
     * Fetches the vault with the specified identifier. Requires password manager to be registered and unlocked.
     *
     * @param id Identifier of the vault
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun getVault(id: String): Vault?

    /**
     * Updates a vault. Requires password manager to be registered and unlocked.
     *
     * @param vault The vault to update
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun update(vault: Vault)

    /**
     * Deletes a vault. Does not require the password manager to be unlocked.
     *
     * @param id The identifier of the vault to delete
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun deleteVault(id: String)

    /**
     * Change the master password. Requires password manager to be registered and unlocked.
     *
     * @param currentPassword The current password.
     * @param newPassword The new password.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun changeMasterPassword(currentPassword: String, newPassword: String)

    /**
     * Adds a new item to the vault. Requires password manager to be registered and unlocked.
     *
     * @param item The [VaultItem] item to add.
     * @param vault [Vault] to add item to.
     * @return The identifier of the new item that was added
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun add(item: VaultItem, vault: Vault): String

    /**
     * Fetches the full list of credentials stored in the vault. Requires password manager to be registered and unlocked.
     *
     * @param vault [Vault] to list items from
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun listVaultItems(vault: Vault): List<VaultItem>

    /**
     * Fetches a single [VaultItem] if the [VaultItem] with the identifier can be found.
     * Requires password manager to be registered and unlocked.
     *
     * @param id Identifier of the [VaultItem] to search for
     * @param vault [Vault] to get the item from
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun getVaultItem(id: String, vault: Vault): VaultItem?

    /**
     * Update a [VaultItem] in a [Vault]. Requires password manager to be registered and unlocked.
     *
     * @param item The [VaultItem] to update
     * @param vault [Vault] to update item in.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun update(item: VaultItem, vault: Vault)

    /**
     * Remove the [VaultItem] with the specified identifier from the [Vault].
     * Requires password manager to be registered and unlocked.
     *
     * @param id Identifier of the [VaultItem] to remove.
     * @param vault [Vault] to remove item from.
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun removeVaultItem(id: String, vault: Vault)

    /**
     * Creates a Rescue Kit PDF with the user's secret code.
     *
     * @param template [ByteArray] of a template image for the Rescue Kit PDF. This can be null.
     * If it is null a default template will be used.
     * @return A Rescue Kit PDF with the secret code
     */
    suspend fun renderRescueKit(context: Context, template: ByteArray? = null): PdfDocument

    /**
     * Fetches the current [EntitlementState].
     */
    @Throws(SudoPasswordManagerException::class)
    suspend fun getEntitlementState(): List<EntitlementState>
}
