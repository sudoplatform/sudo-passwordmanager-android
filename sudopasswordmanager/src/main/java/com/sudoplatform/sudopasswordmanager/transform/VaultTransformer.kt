/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.transform

import android.util.Base64
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerClient
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.crypto.CryptographyProvider
import com.sudoplatform.sudopasswordmanager.datastore.VaultBankAccountProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultCreditCardProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultLoginProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultNoteProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultPasswordProxy
import com.sudoplatform.sudopasswordmanager.datastore.VaultSecureFieldProxy
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudopasswordmanager.models.SecureFieldValue
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultItemNote
import com.sudoplatform.sudopasswordmanager.models.VaultItemPassword
import com.sudoplatform.sudopasswordmanager.models.VaultItemValue
import com.sudoplatform.sudopasswordmanager.models.VaultLogin

/**
 * Transform to and from the types returned by the [SudoSecureVaultClient] and the public
 * types of this SDK.
 *
 * @since 2020-10-29
 */
internal class VaultTransformer(
    val crypto: CryptographyProvider,
    val passwordManagerClient: SudoPasswordManagerClient
) {

    // Public to Internal

    /**
     * Converts between the user-facing [VaultLogin] to the internal [VaultLoginProxy]
     * If a secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     * @param login The [VaultLogin] to be converted
     * @param vaultKey The vault key used to encrypt the item
     * Returns the on-disk format of a [VaultLoginProxy]
     */
    fun createVaultLoginProxy(login: VaultLogin, vaultKey: ByteArray): VaultLoginProxy {

        var notes: VaultNoteProxy? = null
        if (login.notes != null) {
            notes = this.createVaultNoteProxy(login.notes, vaultKey)
        }

        var password: VaultPasswordProxy? = null
        if (login.password != null) {
            password = this.createVaultPasswordProxy(login.password, vaultKey)
        }

        return VaultLoginProxy(
            login.createdAt,
            login.id,
            login.name,
            notes,
            login.updatedAt,
            VaultSchema.VaultSchemaV1.VaultItemType.LOGIN,
            password,
            login.url?.toString(),
            login.user
        )
    }

    /**
     * Converts between the user-facing [VaultCreditCard] to the internal [VaultCreditCardProxy]
     * If a secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     *
     * @param creditCard The `VaultCreditCard` to be converted
     * @param vaultKey The vault key used to encrypt the item
     * Returns a [VaultCreditCardProxy]
     */
    fun createVaultCreditCardProxy(creditCard: VaultCreditCard, vaultKey: ByteArray): VaultCreditCardProxy {

        val notes = creditCard.notes?.let {
            createVaultNoteProxy(it, vaultKey)
        }

        val cardNumber = creditCard.cardNumber?.value?.let {
            createVaultSecureField(it, vaultKey)
        }

        val securityCode = creditCard.securityCode?.value?.let {
            createVaultSecureField(it, vaultKey)
        }

        return VaultCreditCardProxy(
            createdAt = creditCard.createdAt,
            id = creditCard.id,
            name = creditCard.name,
            notes = notes,
            updatedAt = creditCard.updatedAt,
            type = VaultSchema.VaultSchemaV1.VaultItemType.CREDIT_CARD,
            cardNumber = cardNumber,
            cardSecurityCode = securityCode,
            cardType = creditCard.cardType,
            cardExpiration = creditCard.expiresAt,
            cardName = creditCard.cardName
        )
    }

    /**
     * Converts between the user-facing [VaultBankAccount] to the internal [VaultBankAccountProxy]
     * If a secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     *
     * @param bankAccount The [VaultBankAccount] to be converted
     * @param vaultKey The vault key used to encrypt the item
     * Returns a [VaultBankAccountProxy]
     */
    fun createVaultBankAccountProxy(bankAccount: VaultBankAccount, vaultKey: ByteArray): VaultBankAccountProxy {

        val notes = bankAccount.notes?.let {
            createVaultNoteProxy(it, vaultKey)
        }

        val accountNumber = bankAccount.accountNumber?.value?.let {
            createVaultSecureField(it, vaultKey)
        }

        val accountPin = bankAccount.accountPin?.value?.let {
            createVaultSecureField(it, vaultKey)
        }

        return VaultBankAccountProxy(
            createdAt = bankAccount.createdAt,
            id = bankAccount.id,
            name = bankAccount.name,
            notes = notes,
            updatedAt = bankAccount.updatedAt,
            type = VaultSchema.VaultSchemaV1.VaultItemType.BANK_ACCOUNT,
            accountNumber = accountNumber,
            accountPin = accountPin,
            accountType = bankAccount.accountType,
            bankName = bankAccount.bankName,
            branchAddress = bankAccount.branchAddress,
            branchPhone = bankAccount.branchPhone,
            ibanNumber = bankAccount.ibanNumber,
            routingNumber = bankAccount.routingNumber,
            swiftCode = bankAccount.swiftCode
        )
    }

    /**
     * Converts between the user-facing [VaultItemNote] to the internal [VaultNoteProxy]
     * If a secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     * @param note The [VaultItemNote] to be converted
     * @param vaultKey The vault key used to encrypt the item
     * Returns the on-disk format of a [VaultNoteProxy]
     */
    fun createVaultNoteProxy(note: VaultItemNote, vaultKey: ByteArray): VaultNoteProxy {
        val secureField = this.createVaultSecureField(note.value, vaultKey)
        return VaultNoteProxy(secureField.secureValue)
    }

    /**
     * Converts between the user-facing [VaultItemPassword] to the internal [VaultPasswordProxy]
     * If a secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     * @param password ]The [VaultItemPassword] to be converted
     * @param vaultKey The vault key used to encrypt the item
     * Returns the on-disk format of a [VaultPasswordProxy]
     */
    fun createVaultPasswordProxy(password: VaultItemPassword, vaultKey: ByteArray): VaultPasswordProxy {
        val secureField = this.createVaultSecureField(password.value, vaultKey)
        return VaultPasswordProxy(secureField.secureValue, password.createdAt, password.replacedAt)
    }

    /**
     * Converts between the user-facing [SecureFieldValue] to the internal [VaultSecureFieldProxy] to be stored in the vault
     * If the secure field was already encrypted, it is left in that state and not decrypted and re-encrypted
     * @param value The [SecureFieldValue] that may need to be encrypted
     * @param vaultKey The vault key used to encrypt the item
     * Returns a [VaultSecureFieldProxy]
     */
    fun createVaultSecureField(value: SecureFieldValue, vaultKey: ByteArray): VaultSecureFieldProxy {
        if (value.cipherText != null) {
            return VaultSecureFieldProxy(value.cipherText)
        } else {
            val data = value.plainText?.toByteArray()
                ?: throw SudoPasswordManagerException.InvalidFormatException("SecureFieldValue.plainText cannot be null")

            val cipherText = Base64.encodeToString(this.crypto.encryptSecureField(data, vaultKey), Base64.NO_WRAP)
            return VaultSecureFieldProxy(cipherText)
        }
    }

    // Internal to Public

    fun createVaultLogin(login: VaultLoginProxy, revealKey: ByteArray): VaultLogin {
        var notes: VaultItemNote? = null
        if (login.notes != null) {
            val note = login.notes!!
            notes = this.createVaultItemNote(note, revealKey)
        }

        var password: VaultItemPassword? = null
        if (login.password != null) {
            val pw = login.password!!
            password = this.createVaultItemPassword(pw, revealKey)
        }

        return VaultLogin(
            id = login.id,
            createdAt = login.createdAt,
            updatedAt = login.updatedAt,
            user = login.user,
            url = login.url,
            name = login.name,
            notes = notes,
            password = password,
            previousPasswords = emptyList()
        )
    }

    fun createVaultCreditCard(creditCard: VaultCreditCardProxy, revealKey: ByteArray): VaultCreditCard {

        val notes = creditCard.notes?.let {
            createVaultItemNote(it, revealKey)
        }

        val cardNumber = creditCard.cardNumber?.let {
            createVaultItemValue(it, revealKey)
        }

        val securityCode = creditCard.cardSecurityCode?.let {
            createVaultItemValue(it, revealKey)
        }

        return VaultCreditCard(
            id = creditCard.id,
            createdAt = creditCard.createdAt,
            updatedAt = creditCard.updatedAt,
            name = creditCard.name,
            cardType = creditCard.cardType,
            cardNumber = cardNumber,
            cardName = creditCard.cardName,
            expiresAt = creditCard.cardExpiration,
            securityCode = securityCode,
            notes = notes
        )
    }

    fun createVaultBankAccount(bankAccount: VaultBankAccountProxy, revealKey: ByteArray): VaultBankAccount {

        val notes = bankAccount.notes?.let {
            createVaultItemNote(it, revealKey)
        }

        val accountNumber = bankAccount.accountNumber?.let {
            createVaultItemValue(it, revealKey)
        }

        val accountPin = bankAccount.accountPin?.let {
            createVaultItemValue(it, revealKey)
        }

        return VaultBankAccount(
            id = bankAccount.id,
            createdAt = bankAccount.createdAt,
            updatedAt = bankAccount.updatedAt,
            name = bankAccount.name,
            accountType = bankAccount.accountType,
            accountNumber = accountNumber,
            accountPin = accountPin,
            bankName = bankAccount.bankName,
            branchAddress = bankAccount.branchAddress,
            branchPhone = bankAccount.branchPhone,
            ibanNumber = bankAccount.ibanNumber,
            routingNumber = bankAccount.routingNumber,
            swiftCode = bankAccount.swiftCode,
            notes = notes
        )
    }

    fun createVaultItemNote(note: VaultNoteProxy, revealKey: ByteArray): VaultItemNote {
        return VaultItemNote(this.createSecureFieldValue(note.secureValue, revealKey))
    }

    fun createVaultItemPassword(password: VaultPasswordProxy, revealKey: ByteArray): VaultItemPassword {
        return VaultItemPassword(this.createSecureFieldValue(password.secureValue, revealKey), password.createdAt, password.replacedAt)
    }

    fun createVaultItemValue(value: VaultSecureFieldProxy, revealKey: ByteArray): VaultItemValue {
        return VaultItemValue(createSecureFieldValue(value.secureValue, revealKey))
    }

    fun createSecureFieldValue(cipherText: String, revealKey: ByteArray): SecureFieldValue {
        val revealFunction: ((String) -> String) = {
            if (passwordManagerClient.isLocked()) {
                throw SudoPasswordManagerException.VaultLockedException("Vaults must be unlocked")
            }
            val cipherData = Base64.decode(cipherText, Base64.DEFAULT)
                ?: throw SudoPasswordManagerException.InvalidFormatException("Base64 decoding of cipherText failed")

            val plainTextData = this.crypto.decryptSecureField(cipherData, revealKey)
            String(plainTextData, Charsets.UTF_8)
        }

        return SecureFieldValue(null, cipherText, revealFunction)
    }
}
