/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.datastore.vaultschema

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import com.sudoplatform.sudopasswordmanager.models.VaultOwner
import com.sudoplatform.sudosecurevault.Owner
import com.sudoplatform.sudosecurevault.Vault
import kotlinx.parcelize.Parcelize
import java.nio.charset.Charset
import java.util.Date

internal sealed class VaultSchema {

    companion object {

        @VisibleForTesting
        const val FORMAT_V1 = "com.sudoplatform.passwordmanager.vault.v1"

        fun fromRaw(blobFormat: String): VaultSchema {
            return when (blobFormat) {
                // There may be other formats in the future. If so handle them here.
                FORMAT_V1 -> V1()
                else -> latest()
            }
        }

        fun latest(): VaultSchema {
            return V1()
        }
    }

    class V1 : VaultSchema()

    fun decodeSecureVault(vault: Vault): VaultProxy {
        when (this) {
            is V1 -> {
                val data = VaultSchemaV1().decode(vault.blob)

                return VaultProxy(
                    vault.id,
                    latest(),
                    vault.createdAt,
                    vault.updatedAt,
                    vault.version,
                    data,
                    vault.owners.toVaultOwners()
                )
            }
        }
    }

    private fun List<Owner>.toVaultOwners(): List<VaultOwner> {
        return map {
            VaultOwner(it.id, it.issuer)
        }
    }

    fun encodeVaultWithLatestSchema(vault: VaultProxy): ByteArray {
        val vaultData = vault.vaultData
        // vaultData.updatedAt = Date()

        val blob = VaultSchemaV1().encode(vaultData)
        return blob
    }

    class VaultSchemaV1 {

        @Keep
        @Parcelize
        data class Vault(
            var bankAccount: MutableList<BankAccount>,
            var creditCard: MutableList<CreditCard>,
            var generatedPassword: MutableList<GeneratedPassword>,
            var login: MutableList<Login>,
            var schemaVersion: Double
        ) : Parcelable

        @Keep
        @Parcelize
        data class BankAccount(
            var createdAt: Date,
            var id: String,
            var name: String,
            var notes: SecureField?,
            var updatedAt: Date,
            var type: BankAccountType,
            var accountNumber: SecureField?,
            var accountPin: SecureField?,
            var accountType: String?,
            var bankName: String?,
            var branchAddress: String?,
            var branchPhone: String?,
            var ibanNumber: String?,
            var routingNumber: String?,
            var swiftCode: String?
        ) : Parcelable

        @Keep
        @Parcelize
        data class SecureField(var secureValue: String) : Parcelable

        @Keep
        enum class BankAccountType {
            BANK_ACCOUNT
        }

        @Keep
        @Parcelize
        data class CreditCard(
            var createdAt: Date,
            var id: String,
            var name: String,
            var notes: SecureField?,
            var updatedAt: Date,
            var type: CreditCardType,
            var cardExpiration: Date?,
            var cardName: String?,
            var cardNumber: SecureField?,
            var cardSecurityCode: SecureField?,
            var cardType: String?
        ) : Parcelable

        @Keep
        enum class CreditCardType {
            CREDIT_CARD
        }

        @Keep
        @Parcelize
        data class GeneratedPassword(
            var createdAt: Date,
            var id: String,
            var name: String,
            var notes: SecureField?,
            var updatedAt: Date,
            var type: GeneratedPasswordType,
            var password: PasswordField,
            var url: String?
        ) : Parcelable

        @Keep
        @Parcelize
        data class PasswordField(
            var secureValue: String,
            var createdAt: Date,
            var replacedAt: Date?
        ) : Parcelable

        @Keep
        enum class GeneratedPasswordType {
            GENERATED_PASSWORD
        }

        @Keep
        @Parcelize
        data class Login(
            var createdAt: Date,
            var id: String,
            var name: String,
            var notes: SecureField?,
            var updatedAt: Date,
            var type: LoginType,
            var password: PasswordField?,
            var url: String?,
            var user: String?
        ) : Parcelable

        @Keep
        enum class LoginType {
            LOGIN
        }

        @Keep
        @Parcelize
        data class VaultItem(
            val createdAt: Date,
            val id: String,
            val notes: Notes?,
            val password: Password?,
            val previousPasswords: List<Password>,
            val type: VaultType,
            val updatedAt: Date,
            val url: String?,
            val user: String?
        ) : Parcelable

        @Keep
        @Parcelize
        data class Notes(
            val updatedAt: Date,
            val value: VaultSecureField
        ) : Parcelable

        @Keep
        @Parcelize
        data class VaultSecureField(val value: String) : Parcelable

        @Keep
        @Parcelize
        data class Password(
            val createdAt: Date,
            val replacedAt: Date?,
            val value: VaultSecureField
        ) : Parcelable

        @Keep
        enum class VaultType {
            LOGIN;
        }

        private val gson by lazy { Gson() }

        fun encode(vault: VaultSchemaV1.Vault): ByteArray {
            val json = gson.toJson(vault, Vault::class.java)
            return json.toByteArray()
        }

        fun decode(data: ByteArray): Vault {
            val text = String(data, Charset.defaultCharset())
            return gson.fromJson(text, Vault::class.java)
        }
    }
}
