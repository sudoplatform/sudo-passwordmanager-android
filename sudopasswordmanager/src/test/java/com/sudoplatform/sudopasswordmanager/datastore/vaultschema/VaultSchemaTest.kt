/*
 * Copyright © 2020 - Anonyome Labs, Inc. - All rights reserved
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.datastore.vaultschema

import com.sudoplatform.sudopasswordmanager.BaseTests
import com.sudoplatform.sudopasswordmanager.TestData
import com.sudoplatform.sudopasswordmanager.datastore.VaultProxy
import com.sudoplatform.sudosecurevault.Owner
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Date
import java.util.UUID

/**
 * Test the correct blob format is used by the [VaultSchema].
 *
 * @since 2021-04-20
 */
@RunWith(RobolectricTestRunner::class)
internal class VaultSchemaTest : BaseTests() {

    @Test
    fun `encodes correct vault schema blob format`() {
        val vaultProxy = makeVaultProxy()
        vaultProxy.blobFormat shouldBe VaultSchema.FORMAT_V1
    }

    @Test
    fun `encodes correct date format format`() {
        val encodedVaultProxyBytes = VaultSchema.latest().encodeVaultWithLatestSchema(makeVaultProxy())
        val encodedVaultProxyJson = String(encodedVaultProxyBytes)
        println(encodedVaultProxyJson)
        encodedVaultProxyJson shouldContain """"createdAt":"1970-01-01T00:00:00.00Z""""
    }

    @Test
    fun `decodes correct date format format`() {
        val decodedVaultProxy = VaultSchema.latest().decodeSecureVault(makeVault())
        with(decodedVaultProxy.vaultData.login[0]) {
            createdAt.time shouldBe 0L
            updatedAt.time shouldBe 1618963860340L
            type shouldBe VaultSchema.VaultSchemaV1.VaultItemType.LOGIN
        }
        with(decodedVaultProxy.vaultData.bankAccount[0]) {
            createdAt.time shouldBe 0L
            updatedAt.time shouldBe 1618963860340L
            type shouldBe VaultSchema.VaultSchemaV1.VaultItemType.BANK_ACCOUNT
        }
    }

    private fun makeVault() =
        com.sudoplatform.sudosecurevault.Vault(
            id = UUID.randomUUID().toString(),
            owner = "owner",
            version = 1,
            blobFormat = VaultSchema.latest().format,
            createdAt = Date(0L),
            updatedAt = Date(1L),
            owners = listOf(Owner("ownerId", "issuer")),
            blob = vaultBlobJson.toByteArray()
        )

    private val vaultBlobJson = """
        {
            "bankAccount":[ {
                "id":"id",
                "name":"ANZ Savings",
                "createdAt":"1970-01-01T00:00:00Z",
                "updatedAt":"2021-04-21T00:11:00.340Z",
                "type":"bankAccount"
                }
            ],
            "creditCard":[ {
                "id":"id",
                "name":"Teds Visa",
                "createdAt":"1970-01-01T00:00:00Z",
                "updatedAt":"2021-04-21T00:11:00.340Z",
                "type":"bankAccount"
                }
            ],
            "generatedPassword":[],
            "login":[ {
                "createdAt":"1970-01-01T00:00:00Z",
                "id":"id",
                "name":"Ted Bear",
                "updatedAt":"2021-04-21T00:11:00.340Z",
                "type":"login",
                "user":"tedbear"
                }
            ],
            "schemaVersion":1.0
        }
    """.trimIndent()

    private fun makeVaultProxy() =
        VaultProxy(
            secureVaultId = UUID.randomUUID().toString(),
            version = 1,
            createdAt = Date(0),
            updatedAt = Date(1),
            vaultData = TestData.VAULT_SCHEMA,
            owners = TestData.OWNERS
        )
}
