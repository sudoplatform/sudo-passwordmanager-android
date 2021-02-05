/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.datastore

import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudopasswordmanager.models.VaultOwner
import com.sudoplatform.sudosecurevault.Vault
import com.sudoplatform.sudosecurevault.VaultMetadata
import java.util.Date

// Some of these are their own types, but others are type alias to the internal vault json.
internal typealias VaultLoginProxy = VaultSchema.VaultSchemaV1.Login
internal typealias VaultNoteProxy = VaultSchema.VaultSchemaV1.SecureField
internal typealias VaultPasswordProxy = VaultSchema.VaultSchemaV1.PasswordField
internal typealias VaultSecureFieldProxy = VaultSchema.VaultSchemaV1.SecureField
internal typealias VaultCreditCardProxy = VaultSchema.VaultSchemaV1.CreditCard
internal typealias VaultBankAccountProxy = VaultSchema.VaultSchemaV1.BankAccount

internal data class VaultProxy(
    /** Unique identifier of the vault storage record on the service */
    val secureVaultId: String = "",
    /** Blob format specifier. */
    val blobFormat: VaultSchema = VaultSchema.latest(),
    /** Date/time at which the vault was created. */
    val createdAt: Date = Date(),
    /** Date/time at which the vault was last updated. */
    var updatedAt: Date = Date(),
    /** Version from the SecureVault metadata */
    var version: Int = 1,
    /** The 'vault' */
    val vaultData: VaultSchema.VaultSchemaV1.Vault,
    /** The identifier(s) of the owner(s) of the vault */
    val owners: List<VaultOwner>
)

internal interface VaultStore {
    /**
     * Imports an array of secure [Vault]s fetched from from the secure vault service
     * Intended to be used during unlock when all the vaults are downloaded.
     */
    fun importSecureVaults(vaults: List<Vault>)

    /** Imports a [VaultProxy] into the store. Intended to be used when a new vault is created */
    fun importVault(vault: VaultProxy)

    fun updateVault(metadata: VaultMetadata)

    /** Remove all [Vault]s from the store. */
    fun removeAll()

    /** Lists all [Vault]s in the store */
    fun listVaults(): List<VaultProxy>

    fun getVault(id: String): VaultProxy?

    fun deleteVault(id: String)

    fun add(login: VaultLoginProxy, id: String)
    fun add(creditCard: VaultCreditCardProxy, id: String)
    fun add(bankAccount: VaultBankAccountProxy, id: String)

    fun update(login: VaultLoginProxy, vaultId: String)
    fun update(creditCard: VaultCreditCardProxy, vaultId: String)
    fun update(bankAccount: VaultBankAccountProxy, vaultId: String)

    fun removeVaultItem(itemId: String, vaultId: String)
}
