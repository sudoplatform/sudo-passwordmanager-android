/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import com.sudoplatform.sudoentitlements.types.Entitlement
import com.sudoplatform.sudoentitlements.types.EntitlementsSet
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudopasswordmanager.util.MAX_VAULTS_PER_SUDO
import com.sudoplatform.sudopasswordmanager.models.VaultOwner
import com.sudoplatform.sudopasswordmanager.util.SUDO_SERVICE_ISSUER
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudosecurevault.Owner
import com.sudoplatform.sudosecurevault.VaultMetadata
import java.util.Date
import java.util.UUID

/**
 * Data common to many tests.
 *
 * @since 2020-10-08
 */
internal object TestData {

    val SUDO_ID = UUID.randomUUID().toString()
    val SUDO = Sudo("Mr", "Theodore", "Bear", "Shopping", null, null)
    const val OWNERSHIP_PROOF = "ownershipProof42"
    val MASTER_PASSWORD = "masterPassword"
    val KEY_VALUE: KeyDerivingKey = "aaaabbbbccccdddd".substring(0, 16).toByteArray()
    const val USER_ID = "slartibartfast"
    val USER_SUBJECT = UUID.randomUUID().toString()
    val OWNERS = listOf(VaultOwner(SUDO_ID, SUDO_SERVICE_ISSUER))
    val SECURE_VAULT_OWNER = listOf(Owner(SUDO_ID, SUDO_SERVICE_ISSUER))
    val SECURE_VAULT = VaultMetadata(
        id = "id",
        owner = "owner",
        version = 1,
        blobFormat = VaultSchema.FORMAT_V1,
        createdAt = Date(0),
        updatedAt = Date(1),
        owners = SECURE_VAULT_OWNER
    )
    val VAULT = Vault(id = "id", owners = OWNERS, createdAt = Date(0L), updatedAt = Date(0L))
    val VAULT_ITEM = VaultLogin(
        id = "id",
        createdAt = Date(0),
        updatedAt = Date(0),
        name = "Ted Bear",
        user = "tedbear"
    )
    val LOGIN = VaultSchema.VaultSchemaV1.Login(
        id = "id",
        name = "Ted Bear",
        user = "tedbear",
        type = VaultSchema.VaultSchemaV1.LoginType.LOGIN,
        notes = null,
        password = null,
        url = null,
        createdAt = Date(0),
        updatedAt = Date(0)
    )
    val VAULT_SCHEMA = VaultSchema.VaultSchemaV1.Vault(
        bankAccount = mutableListOf(),
        creditCard = mutableListOf(),
        generatedPassword = mutableListOf(),
        login = mutableListOf(LOGIN),
        schemaVersion = 1.0
    )
    val VAULT_PROXY = VaultProxy(
        secureVaultId = "id",
        version = 1,
        createdAt = Date(0),
        updatedAt = Date(1),
        vaultData = VAULT_SCHEMA,
        owners = OWNERS
    )
    val ENTITLEMENTS = EntitlementsSet(
        name = "name",
        description = "description",
        version = 1.0,
        entitlements = setOf(
            Entitlement(
                name = MAX_VAULTS_PER_SUDO,
                value = 2,
                description = "Maximum vaults per Sudo"
            )
        ),
        createdAt = Date(0),
        updatedAt = Date(1)
    )
}
