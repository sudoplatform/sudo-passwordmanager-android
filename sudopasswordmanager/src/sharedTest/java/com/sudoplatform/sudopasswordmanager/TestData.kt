/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import com.sudoplatform.sudoentitlements.types.Entitlement
import com.sudoplatform.sudoentitlements.types.EntitlementConsumption
import com.sudoplatform.sudoentitlements.types.EntitlementsConsumption
import com.sudoplatform.sudoentitlements.types.EntitlementsSet
import com.sudoplatform.sudoentitlements.types.UserEntitlements
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import com.sudoplatform.sudopasswordmanager.datastore.vaultschema.VaultSchema
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultBankAccount
import com.sudoplatform.sudopasswordmanager.models.VaultCreditCard
import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudopasswordmanager.util.MAX_VAULTS_PER_SUDO
import com.sudoplatform.sudopasswordmanager.models.VaultOwner
import com.sudoplatform.sudopasswordmanager.util.SUDO_SERVICE_ISSUER
import com.sudoplatform.sudoprofiles.Sudo
import com.sudoplatform.sudosecurevault.Owner
import com.sudoplatform.sudosecurevault.VaultMetadata
import java.util.Collections
import java.util.Date
import java.util.UUID

/**
 * Data common to many tests.
 *
 * @since 2020-10-08
 */
internal object TestData {

    const val PLAIN_TEXT = "The owl and the pussy cat went to sea in a beautiful pea green boat."
    const val FNAME = "Theodore"
    const val LNAME = "Bear"
    const val NAME = "$FNAME $LNAME"
    const val USER = "tedbear"
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
        blobFormat = VaultSchema.latest().format,
        createdAt = Date(0),
        updatedAt = Date(1),
        owners = SECURE_VAULT_OWNER
    )
    val VAULT = Vault(id = "id", owners = OWNERS, createdAt = Date(0L), updatedAt = Date(0L))
    val VAULT_LOGIN_ITEM = VaultLogin(
        id = "id",
        createdAt = Date(0),
        updatedAt = Date(0),
        name = "Ted Bear",
        user = "tedbear"
    )
    val VAULT_CREDIT_CARD_ITEM = VaultCreditCard(
        id = "id",
        createdAt = Date(0),
        updatedAt = Date(0),
        name = "Ted Bear's Visa",
        cardType = "Visa"
    )
    val VAULT_BANK_ACCOUNT_ITEM = VaultBankAccount(
        id = "id",
        createdAt = Date(0),
        updatedAt = Date(0),
        name = "Ted Bear's Bank Account",
        accountType = "Savings"
    )
    val LOGIN = VaultSchema.VaultSchemaV1.Login(
        id = "id",
        name = "Ted Bear",
        user = "tedbear",
        type = VaultSchema.VaultSchemaV1.VaultItemType.LOGIN,
        notes = null,
        password = null,
        url = null,
        createdAt = Date(0),
        updatedAt = Date(0)
    )
    val CREDIT_CARD = VaultSchema.VaultSchemaV1.CreditCard(
        id = "id",
        name = "Teds Visa",
        cardName = null,
        cardNumber = null,
        cardSecurityCode = null,
        cardType = "VISA",
        type = VaultSchema.VaultSchemaV1.VaultItemType.CREDIT_CARD,
        notes = null,
        cardExpiration = Date(1618963860340L),
        createdAt = Date(0),
        updatedAt = Date(0)
    )
    // These are unmodifiable lists to catch any unintended modification
    val VAULT_SCHEMA = VaultSchema.VaultSchemaV1.Vault(
        bankAccount = Collections.unmodifiableList(mutableListOf()),
        creditCard = Collections.unmodifiableList(mutableListOf(CREDIT_CARD)),
        generatedPassword = Collections.unmodifiableList(mutableListOf()),
        login = Collections.unmodifiableList(mutableListOf(LOGIN)),
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
    val USER_ENTITLEMENTS = UserEntitlements(
        version = 1.0,
        entitlementsSetName = "default",
        entitlements = ENTITLEMENTS.entitlements.toList()
    )
    val ENTITLEMENTS_CONSUMPTION = EntitlementsConsumption(
        entitlements = USER_ENTITLEMENTS,
        consumption = listOf(
            EntitlementConsumption(
                name = MAX_VAULTS_PER_SUDO,
                consumer = null,
                value = 3,
                consumed = 0,
                available = 3,
                firstConsumedAtEpochMs = null,
                lastConsumedAtEpochMs = null
            )
        )
    )
}
