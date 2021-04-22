/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.core.content.res.ResourcesCompat
import com.apollographql.apollo.exception.ApolloNetworkException
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudokeymanager.KeyManagerException
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudopasswordmanager.datastore.DefaultVaultStore
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultStore
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudopasswordmanager.entitlements.Entitlement
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudopasswordmanager.models.VaultOwner
import com.sudoplatform.sudopasswordmanager.transform.EntitlementTransformer
import com.sudoplatform.sudopasswordmanager.transform.MasterPasswordTransformer
import com.sudoplatform.sudopasswordmanager.transform.VaultTransformer
import com.sudoplatform.sudopasswordmanager.util.SECURE_VAULT_AUDIENCE
import com.sudoplatform.sudopasswordmanager.util.SUDO_SERVICE_ISSUER
import com.sudoplatform.sudopasswordmanager.util.formatSecretCode
import com.sudoplatform.sudopasswordmanager.util.parseSecretCode
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudoprofiles.exceptions.SudoProfileException
import com.sudoplatform.sudosecurevault.VaultMetadata
import com.sudoplatform.sudosecurevault.exceptions.SudoSecureVaultException
import com.sudoplatform.sudouser.exceptions.AuthenticationException
import org.spongycastle.util.encoders.Hex
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.CancellationException

typealias KeyDerivingKey = ByteArray

private data class SessionData(
    val masterPassword: ByteArray,
    val keyDerivingKey: KeyDerivingKey
)

/**
 * The default implementation of [SudoPasswordManagerClient] provided by this SDK.
 */
internal class DefaultPasswordManagerClient(
    private val service: PasswordClientService,
    private val logger: Logger,
    /** Internal handling of vaults */
    @VisibleForTesting
    private val vaultStore: VaultStore = DefaultVaultStore()
) : SudoPasswordManagerClient {

    companion object {
        /** Exception messages */
        private const val VAULT_NOT_FOUND = "Vault not found"
        private const val VAULTS_MUST_BE_UNLOCKED = "Vaults must be unlocked"
        private const val VAULT_MISSING_SECURE_KEY = "Vault is missing the secure key field. Vault is corrupt."
        private const val NO_ENTITLEMENTS_FROM_SERVER = "Unable to fetch entitlements from server"
        private const val UNSUPPORTED_VAULT_ITEM_TYPE = "Vault item is unsupported."
    }

    // Logged in/out session date
    private var sessionData: SessionData? = null

    private val vaultFactory by lazy {
        VaultTransformer(service.cryptoProvider, this)
    }

    override suspend fun getRegistrationStatus(): PasswordManagerRegistrationStatus {
        try {
            if (service.secureVaultClient.isRegistered()) {
                if (service.getKey() != null) {
                    return PasswordManagerRegistrationStatus.REGISTERED
                } else {
                    return PasswordManagerRegistrationStatus.MISSING_SECRET_CODE
                }
            }
            return PasswordManagerRegistrationStatus.NOT_REGISTERED
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun register(masterPassword: String) {
        try {
            val passwordData = MasterPasswordTransformer(masterPassword).data()

            // Attempt to register. This assumes registration status is NOT_REGISTERED and relies on the client to do the right thing
            // if we attempt to register twice with the same credentials.

            // We need to store the key before we register in case registration is successful but we never received
            // the response (e.g. loss of network, app crash). On retry we should get a response that indicates registration
            // was successful, so we need to make sure we still have the key deriving key.
            //
            // This also covers the case where the registration request never made it to the service before this was interrupted
            // by fetching the key previously created.
            val key: KeyDerivingKey
            val existingKey = service.getKey()
            if (existingKey != null) {
                key = existingKey
            } else {
                key = service.cryptoProvider.generateKeyDerivingKey()
                service.set(key)
            }
            val username = service.secureVaultClient.register(key, passwordData)
            logger.debug("SudoSecureVaultClient registered username=$username")
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun deregister() {
        try {
            lock()
            service.secureVaultClient.deregister()
            reset()
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override fun getSecretCode(): String? {
        // convert data to hex string
        val hexString = service.getKey()?.let { Hex.toHexString(it) }
        // return formatted string with prefix
        return formatSecretCode(calculateSecretCodeSubscriberPrefix() + hexString)
    }

    /**
     * Calculates the secret code subscriber prefix. This is the first 5 characters of the user
     * subscriber id hashed with sha1. This could fail (unlikely), but shouldn't stop the secret
     * code from being exported so as a fallback a string of all zeros is returned.
     */
    private fun calculateSecretCodeSubscriberPrefix(): String {

        val fallbackPrefix = "00000"
        val prefixLength = fallbackPrefix.length

        val subject = service.getUserSubject()
            ?: run {
                logger.debug("Failed to get user subject when generating secret code")
                return fallbackPrefix
            }

        val sha1Hasher = MessageDigest.getInstance("SHA-1")
        val subjectHash = Hex.toHexString(sha1Hasher.digest(subject.toByteArray(Charsets.UTF_8)))
        if (subjectHash.length >= prefixLength) {
            return subjectHash.substring(0, prefixLength)
        } else {
            return fallbackPrefix
        }
    }

    override suspend fun lock() {
        sessionData = null
        vaultStore.removeAll()
    }

    override suspend fun unlock(masterPassword: String, secretCode: String?) {
        try {
            val passwordData = MasterPasswordTransformer(masterPassword).data()

            val kdk = try {
                service.getKey()
            } catch (e: Throwable) {
                // This is being treated as an unrecoverable case, however we could check the secret code passed in and see
                // if that will let us unlock the vault
                throw SudoPasswordManagerException.InvalidPasswordOrMissingSecretCodeException("Secret code missing")
            }

            val secret = secretCode?.let { parseSecretCode(it) }

            // Key can come from different places
            if (kdk == null && secret == null) {
                // No cached KDK or secret key provided
                throw SudoPasswordManagerException.InvalidPasswordOrMissingSecretCodeException(
                    "Secret code and secret key missing"
                )
            } else if (kdk != null && secret == null) {
                // Cached KDK.  Should be most common case
                validateCredentials(kdk, passwordData)
            } else if (kdk == null && secret != null) {
                // No cached KDK, user provided it
                // validate the kdk the user passed in
                validateCredentials(secret, passwordData)
            } else if (kdk != null && secret != null) {
                // Cached KDK and user passed a new one in
                validateCredentials(kdk, passwordData)
            }
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun reset() {
        try {
            lock()
            service.keyStore.resetKeys()
            service.secureVaultClient.reset()
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    // Handles the work of unlocking the vault behind the public "unlock" function after the password and key have been fetched and validated
    private suspend fun validateCredentials(key: KeyDerivingKey, passwordData: ByteArray) {
        // "unlock" the vault by downloading a list of vault which validates the key and password
        vaultStore.importSecureVaults(
            service.secureVaultClient.listVaults(key, passwordData)
        )
        try {
            service.set(key)
        } catch (e: Exception) {
            // This is probably because the key already exists in the keystore. Just log the error and attempt to move on.
            logger.debug("error $e")
        } finally {
            setSessionData(passwordData, key)
        }
    }

    @VisibleForTesting
    internal fun setSessionData(masterPassword: ByteArray, keyDerivingKey: KeyDerivingKey) {
        // Web will encrypt master password, secret code, and store as the session key
        // Not sure we need to be that granular on mobile.
        sessionData = SessionData(masterPassword, keyDerivingKey)
    }

    override fun isLocked(): Boolean {
        return sessionData == null
    }

    override suspend fun createVault(sudoId: String): Vault {
        val sessionKey = sessionData
            ?: throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)

        try {
            // Retrieve the ownership proof used to map a Sudo to a vault
            val ownershipProof = getOwnershipProof(sudoId)

            val vaultData = VaultSchema.VaultSchemaV1.Vault(
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                mutableListOf(),
                0.0
            )
            val newVaultOwners = listOf(VaultOwner(id = sudoId, issuer = SUDO_SERVICE_ISSUER))
            val newVault = VaultProxy(vaultData = vaultData, owners = newVaultOwners)

            val blob = VaultSchema.latest().encodeVaultWithLatestSchema(newVault)

            val vaultMetadata = service.secureVaultClient.createVault(
                sessionKey.keyDerivingKey,
                sessionKey.masterPassword,
                blob,
                newVault.blobFormat,
                ownershipProof
            )

            // Create a vault proxy from the meta data returned along with the vault data we created earlier.
            val vaultToImport = VaultProxy(
                vaultMetadata.id,
                newVault.blobFormat,
                vaultMetadata.createdAt,
                vaultMetadata.updatedAt,
                vaultMetadata.version,
                newVault.vaultData,
                newVaultOwners
            )

            // Save the newly created vault to the store
            vaultStore.importVault(vaultToImport)

            // Create a user visible object and return
            return Vault(vaultMetadata.id, newVaultOwners, vaultMetadata.createdAt, vaultMetadata.updatedAt)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    @Throws(SudoProfileException.SudoNotFoundException::class)
    private suspend fun getOwnershipProof(sudoId: String): String {
        return service.profilesClient.getOwnershipProof(Sudo(sudoId), SECURE_VAULT_AUDIENCE)
    }

    override suspend fun listVaults(): List<Vault> {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }
            return vaultStore.listVaults().map {
                Vault(it.secureVaultId, it.owners, it.createdAt, it.updatedAt)
            }
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun getVault(id: String): Vault? {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }
            val vaultProxy = vaultStore.getVault(id)
                ?: return null
            return Vault(vaultProxy.secureVaultId, vaultProxy.owners, vaultProxy.createdAt, vaultProxy.updatedAt)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun update(vault: Vault) {
        try {
            val sessionData = this.sessionData
                ?: throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)

            val vaultProxy = vaultStore.getVault(vault.id)
                ?: throw SudoPasswordManagerException.VaultNotFoundException(VAULT_NOT_FOUND)

            val vaultData = VaultSchema.latest().encodeVaultWithLatestSchema(vaultProxy)

            val vaultMetadata = service.secureVaultClient.updateVault(
                sessionData.keyDerivingKey,
                sessionData.masterPassword,
                vaultProxy.secureVaultId,
                vaultProxy.version,
                vaultData,
                vaultProxy.blobFormat.toString()
            )

            // Update the vault data version
            vaultProxy.version = vaultMetadata.version

            vaultStore.updateVault(vaultMetadata)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun deleteVault(id: String) {
        try {
            service.secureVaultClient.deleteVault(id)
                ?: throw SudoPasswordManagerException.VaultNotFoundException(VAULT_NOT_FOUND)
            vaultStore.deleteVault(id)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun changeMasterPassword(currentPassword: String, newPassword: String) {
        try {
            val session = sessionData
                ?: throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)

            service.secureVaultClient.changeVaultPassword(
                session.keyDerivingKey,
                currentPassword.toByteArray(),
                newPassword.toByteArray()
            )
            sessionData = session.copy(masterPassword = newPassword.toByteArray())

            // Re-import the vaults to refresh the version numbers after the change password operation
            vaultStore.removeAll()
            vaultStore.importSecureVaults(
                service.secureVaultClient.listVaults(session.keyDerivingKey, newPassword.toByteArray())
            )
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun add(item: VaultItem, vault: Vault): String {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }

            val vaultKey = this.sessionData?.keyDerivingKey
            if (vaultKey == null) {
                logger.debug(VAULT_MISSING_SECURE_KEY)
                throw SudoPasswordManagerException.InvalidVaultException(VAULT_MISSING_SECURE_KEY)
            }

            // Add the new item to the vault store under the vault's key
            val itemProxyId: String
            if (item is VaultLogin) {
                val loginProxy = vaultFactory.createVaultLoginProxy(item, vaultKey)
                vaultStore.add(loginProxy, vault.id)
                itemProxyId = loginProxy.id
            } else if (item is VaultCreditCard) {
                val creditCardProxy = vaultFactory.createVaultCreditCardProxy(item, vaultKey)
                vaultStore.add(creditCardProxy, vault.id)
                itemProxyId = creditCardProxy.id
            } else if (item is VaultBankAccount) {
                val bankAccountProxy = vaultFactory.createVaultBankAccountProxy(item, vaultKey)
                vaultStore.add(bankAccountProxy, vault.id)
                itemProxyId = bankAccountProxy.id
            } else {
                throw SudoPasswordManagerException.InvalidFormatException(UNSUPPORTED_VAULT_ITEM_TYPE)
            }

            // Update the vault
            update(vault)

            return itemProxyId
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun listVaultItems(vault: Vault): List<VaultItem> {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }

            val internalVault = vaultStore.getVault(vault.id)
                ?: return emptyList()

            val vaultKey = this.sessionData?.keyDerivingKey
            if (vaultKey == null) {
                logger.debug(VAULT_MISSING_SECURE_KEY)
                throw SudoPasswordManagerException.InvalidVaultException(VAULT_MISSING_SECURE_KEY)
            }

            with(internalVault.vaultData) {
                val logins: List<VaultItem> = login.map {
                    vaultFactory.createVaultLogin(it, vaultKey)
                }
                val creditCards: List<VaultItem> = creditCard.map {
                    vaultFactory.createVaultCreditCard(it, vaultKey)
                }
                val bankAccounts: List<VaultItem> = bankAccount.map {
                    vaultFactory.createVaultBankAccount(it, vaultKey)
                }
                return logins + creditCards + bankAccounts
            }
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun getVaultItem(id: String, vault: Vault): VaultItem? {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }

            val internalVault = vaultStore.getVault(vault.id)
                ?: return null

            val vaultKey = this.sessionData?.keyDerivingKey
            if (vaultKey == null) {
                logger.debug(VAULT_MISSING_SECURE_KEY)
                throw SudoPasswordManagerException.InvalidVaultException(VAULT_MISSING_SECURE_KEY)
            }

            val loginProxy = internalVault.vaultData.login.firstOrNull { it.id == id }
            if (loginProxy != null) {
                return vaultFactory.createVaultLogin(loginProxy, vaultKey)
            }
            val creditCardProxy = internalVault.vaultData.creditCard.firstOrNull { it.id == id }
            if (creditCardProxy != null) {
                return vaultFactory.createVaultCreditCard(creditCardProxy, vaultKey)
            }
            val bankAccountProxy = internalVault.vaultData.bankAccount.firstOrNull { it.id == id }
            if (bankAccountProxy != null) {
                return vaultFactory.createVaultBankAccount(bankAccountProxy, vaultKey)
            }

            return null
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun update(item: VaultItem, vault: Vault) {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }

            val vaultKey = this.sessionData?.keyDerivingKey
            if (vaultKey == null) {
                logger.debug(VAULT_MISSING_SECURE_KEY)
                throw SudoPasswordManagerException.InvalidVaultException(VAULT_MISSING_SECURE_KEY)
            }

            if (item is VaultLogin) {
                vaultStore.update(vaultFactory.createVaultLoginProxy(item, vaultKey), vault.id)
            } else if (item is VaultCreditCard) {
                vaultStore.update(vaultFactory.createVaultCreditCardProxy(item, vaultKey), vault.id)
            } else if (item is VaultBankAccount) {
                vaultStore.update(vaultFactory.createVaultBankAccountProxy(item, vaultKey), vault.id)
            } else {
                throw SudoPasswordManagerException.InvalidFormatException(UNSUPPORTED_VAULT_ITEM_TYPE)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun removeVaultItem(id: String, vault: Vault) {
        try {
            if (isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException(VAULTS_MUST_BE_UNLOCKED)
            }

            vaultStore.removeVaultItem(id, vault.id)
            update(vault)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    /**
     * Interpret an exception from the secure vault client or vault store and map it to an exception
     * declared in this SDK's API that the caller is expecting.
     *
     * @param exception The exception from the secure value client.
     * @return The exception mapped to [SudoPasswordManagerException]
     * or [CancellationException]
     */
    private fun interpretException(exception: Throwable?): Throwable {
        return when (exception) {
            is CancellationException, // Never wrap or reinterpret Kotlin coroutines cancellation exception
            is SudoPasswordManagerException -> exception
            is IOException -> SudoPasswordManagerException.FailedException(
                "IO or network failure",
                exception
            )
            is KeyManagerException -> SudoPasswordManagerException.CryptographyException(
                "Cryptography failure",
                exception
            )
            is SudoProfileException.SudoNotFoundException -> SudoPasswordManagerException.SudoNotFoundException(
                "Sudo not found",
                exception
            )
            is ApolloNetworkException -> {
                if (exception.isNestedCauseNotSignedInException())
                    SudoPasswordManagerException.UnauthorizedUserException("User is not signed in", exception)
                else
                    SudoPasswordManagerException.FailedException("Network failure", exception)
            }
            is SudoSecureVaultException.NotSignedInException,
            is SudoSecureVaultException.NotRegisteredException,
            is SudoSecureVaultException.NotAuthorizedException ->
                SudoPasswordManagerException.UnauthorizedUserException("Secure Vault unauthorized error", exception)
            is SudoSecureVaultException -> SudoPasswordManagerException.FailedException("Secure vault client error", exception)
            is SudoEntitlementsClient.EntitlementsException -> SudoPasswordManagerException.FailedException(
                "Entitlements client error",
                exception
            )
            null -> SudoPasswordManagerException.FailedException("Failed with no error returned")
            else -> SudoPasswordManagerException.UnknownException(exception)
        }
    }

    private fun Throwable?.isNestedCauseNotSignedInException(): Boolean {
        if (this == null) {
            return false
        }
        if (cause is AuthenticationException.NotSignedInException) {
            return true
        }
        return cause.isNestedCauseNotSignedInException()
    }

    override suspend fun renderRescueKit(context: Context, template: ByteArray?): PdfDocument {
        // create a PdfDocument with the rescue kit template image
        val document = PdfDocument()
        // use template image if available
        val image = template?.let {
            try {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            } catch (e: IOException) {
                rescueKitTemplateBitmap(context)
            }
        } ?: run {
            rescueKitTemplateBitmap(context)
        }
        val pageInfo = PdfDocument.PageInfo.Builder(image.width, image.height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawBitmap(image, 0.0f, 0.0f, null)
        // draw the secret code text onto the pdf
        val textPaint = TextPaint()
        textPaint.textSize = context.resources.getDimension(R.dimen.secret_code_text_size)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = ResourcesCompat.getFont(context, R.font.courier_prime_bold)
        canvas.drawText(
            getSecretCode() ?: "",
            image.width / 2f,
            image.height - (image.height / 5.1f),
            textPaint
        )
        document.finishPage(page)
        return document
    }

    private fun rescueKitTemplateBitmap(context: Context): Bitmap {
        return try {
            context.assets.open("RescueKit_${Locale.getDefault()}.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: IOException) {
            context.assets.open("RescueKit_en_US.png").use { BitmapFactory.decodeStream(it) }
        }
    }

    override suspend fun getEntitlement(): List<Entitlement> {
        try {
            return EntitlementTransformer.transform(
                service.entitlementsClient.getEntitlementsConsumption()
            )
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    override suspend fun getEntitlementState(): List<EntitlementState> {
        try {
            // Get entitlement
            val entitlement = getEntitlement()

            // Get Vault metadata
            val vaultMetadata = service.secureVaultClient.listVaultsMetadataOnly()

            // Get Sudos
            val sudoList = service.profilesClient.listSudos(ListOption.REMOTE_ONLY)

            return calculateEntitlementStates(sudoList, entitlement, vaultMetadata)
        } catch (e: Throwable) {
            logger.debug("error $e")
            throw interpretException(e)
        }
    }

    private fun calculateEntitlementStates(
        sudos: List<Sudo>,
        entitlements: List<Entitlement>,
        vaultMetadata: List<VaultMetadata>
    ): List<EntitlementState> {

        val maxVaultsPerSudoEntitlement = entitlements.firstOrNull { it.name == Entitlement.Name.MAX_VAULTS_PER_SUDO }?.limit ?: 0

        // Create a histogram of each sudo's vault count.
        val vaultsWithSudoId = vaultMetadata.filter { it.sudoId() != null }
        val vaultsGroupedBySudoId = vaultsWithSudoId.groupBy { it.sudoId() }

        val states: MutableList<EntitlementState> = mutableListOf()
        for (sudo in sudos) {
            val id = sudo.id ?: continue
            val vaultCount = vaultsGroupedBySudoId[id]?.count() ?: 0
            val entitlementState = EntitlementState(Entitlement.Name.MAX_VAULTS_PER_SUDO, id, maxVaultsPerSudoEntitlement, vaultCount)
            states.add(entitlementState)
        }

        return states
    }
}

private fun VaultMetadata.sudoId(): String? {
    return this.owners.firstOrNull { it.issuer == SUDO_SERVICE_ISSUER }?.id
}
