/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.datastore

import com.google.gson.JsonParseException
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerException
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudosecurevault.Vault
import com.sudoplatform.sudosecurevault.VaultMetadata
import java.lang.Exception
import java.util.Date

/**
 * Default implementation of [VaultStore] that keeps the vaults in memory.
 */
internal class DefaultVaultStore : VaultStore {

    /** A collection of secure vaults that have been decoded */
    private var vaults: MutableMap<String, VaultProxy> = mutableMapOf()

    /**
     * Imports an array of secure [Vault]s fetched from from the secure vault service
     * Intended to be used during unlock when all the vaults are downloaded.
     */
    override fun importSecureVaults(vaults: List<Vault>) {
        var error: Exception? = null
        vaults.forEach {
            try {
                val schema = VaultSchema.fromRaw(it.blobFormat)
                val parsed = schema.decodeSecureVault(it)
                synchronized(vaults) {
                    this.vaults[it.id] = parsed
                }
            } catch (e: JsonParseException) {
                // Record the error and carry on importing the rest of the vaults
                error = e
            }
        }
        if (error != null) {
            throw SudoPasswordManagerException.InvalidFormatException("Failed to import a vault", error)
        }
    }

    /** Imports a [VaultProxy] into the store. Intended to be used when a new vault is created */
    override fun importVault(vault: VaultProxy) {
        synchronized(vaults) {
            vaults.put(vault.secureVaultId, defensiveCopyOf(vault))
        }
    }

    // Defensive copy of the vault
    private fun defensiveCopyOf(vault: VaultProxy): VaultProxy {
        val logins = mutableListOf<VaultSchema.VaultSchemaV1.Login>().apply {
            addAll(vault.vaultData.login)
        }
        val cards = mutableListOf<VaultSchema.VaultSchemaV1.CreditCard>().apply {
            addAll(vault.vaultData.creditCard)
        }
        val banks = mutableListOf<VaultSchema.VaultSchemaV1.BankAccount>().apply {
            addAll(vault.vaultData.bankAccount)
        }
        val vaultData = vault.vaultData.copy(
            bankAccount = banks,
            creditCard = cards,
            login = logins
        )
        return vault.copy(vaultData = vaultData)
    }

    override fun updateVault(metadata: VaultMetadata) {
        synchronized(vaults) {
            val proxy = this.getVault(metadata.id) ?: return
            proxy.updatedAt = metadata.updatedAt
            vaults.put(metadata.id, proxy)
        }
    }

    /** Remove all [Vault]s from the store. */
    override fun removeAll() {
        synchronized(vaults) {
            vaults.clear()
        }
    }

    /** Lists all [Vault]s in the store */
    override fun listVaults(): List<VaultProxy> {
        synchronized(vaults) {
            return vaults.values.toList()
        }
    }

    override fun getVault(id: String): VaultProxy? {
        synchronized(vaults) {
            return vaults[id]
        }
    }

    override fun deleteVault(id: String) {
        synchronized(vaults) {
            vaults.remove(id)
        }
    }

    // Vault Items

    override fun add(login: VaultLoginProxy, id: String) {
        synchronized(vaults) {
            val vault = vaults[id] ?: throw SudoPasswordManagerException.VaultNotFoundException()
            vault.vaultData.login.add(login)
            vaults[id] = vault
        }
    }

    override fun add(creditCard: VaultCreditCardProxy, id: String) {
        synchronized(vaults) {
            val vault = vaults[id] ?: throw SudoPasswordManagerException.VaultNotFoundException()
            vault.vaultData.creditCard.add(creditCard)
            vaults[id] = vault
        }
    }

    override fun add(bankAccount: VaultBankAccountProxy, id: String) {
        synchronized(vaults) {
            val vault = vaults[id] ?: throw SudoPasswordManagerException.VaultNotFoundException()
            vault.vaultData.bankAccount.add(bankAccount)
            vaults[id] = vault
        }
    }

    override fun update(login: VaultLoginProxy, vaultId: String) {
        synchronized(vaults) {
            val vault = vaults[vaultId] ?: throw SudoPasswordManagerException.VaultNotFoundException()

            val mutableLogin = login
            mutableLogin.updatedAt = Date()

            vault.vaultData.login.removeAll { it.id == login.id }
            vault.vaultData.login.add(mutableLogin)
            vaults[vaultId] = vault
        }
    }

    override fun update(creditCard: VaultCreditCardProxy, vaultId: String) {
        synchronized(vaults) {
            val vault = vaults[vaultId] ?: throw SudoPasswordManagerException.VaultNotFoundException()

            val mutableCreditCard = creditCard
            mutableCreditCard.updatedAt = Date()

            vault.vaultData.creditCard.removeAll { it.id == creditCard.id }
            vault.vaultData.creditCard.add(mutableCreditCard)
            vaults[vaultId] = vault
        }
    }

    override fun update(bankAccount: VaultBankAccountProxy, vaultId: String) {
        synchronized(vaults) {
            val vault = vaults[vaultId] ?: throw SudoPasswordManagerException.VaultNotFoundException()

            val mutableBankAccount = bankAccount
            mutableBankAccount.updatedAt = Date()

            vault.vaultData.bankAccount.removeAll { it.id == bankAccount.id }
            vault.vaultData.bankAccount.add(mutableBankAccount)
            vaults[vaultId] = vault
        }
    }

    override fun removeVaultItem(itemId: String, vaultId: String) {
        synchronized(vaults) {
            val vault = vaults[vaultId] ?: throw SudoPasswordManagerException.VaultNotFoundException()
            with(vault.vaultData) {
                login.removeAll { it.id == itemId }
                creditCard.removeAll { it.id == itemId }
                bankAccount.removeAll { it.id == itemId }
            }
            vaults[vaultId] = vault
        }
    }

    enum class VaultEncodingError {
        INVALID_SCHEMA
    }
}
