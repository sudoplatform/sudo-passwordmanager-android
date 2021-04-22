/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.datastore.vaultschema

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.util.Locale

/**
 * Serialise and deserialise a [VaultItemType] so that it is handled in a case insensitive manner.
 *
 * @since 2021-04-21
 */
internal class VaultItemTypeConverter :
    JsonSerializer<VaultSchema.VaultSchemaV1.VaultItemType>,
    JsonDeserializer<VaultSchema.VaultSchemaV1.VaultItemType> {

    companion object {
        private const val BANK_ACCOUNT_ALIAS = "bankAccount"
        private const val CREDIT_CARD_ALIAS = "creditCard"
        private const val GENERATED_PASSWORD_ALIAS = "generatedPassword"
        private const val LOGIN_ALIAS = "login"
        private const val UNKNOWN_ALIAS = "unknown"

        private val fromSerialisedForm = mapOf(
            BANK_ACCOUNT_ALIAS to VaultSchema.VaultSchemaV1.VaultItemType.BANK_ACCOUNT,
            CREDIT_CARD_ALIAS to VaultSchema.VaultSchemaV1.VaultItemType.CREDIT_CARD,
            GENERATED_PASSWORD_ALIAS to VaultSchema.VaultSchemaV1.VaultItemType.GENERATED_PASSWORD,
            LOGIN_ALIAS to VaultSchema.VaultSchemaV1.VaultItemType.LOGIN
        )

        private val toSerialisedForm = mapOf(
            VaultSchema.VaultSchemaV1.VaultItemType.BANK_ACCOUNT to BANK_ACCOUNT_ALIAS,
            VaultSchema.VaultSchemaV1.VaultItemType.CREDIT_CARD to CREDIT_CARD_ALIAS,
            VaultSchema.VaultSchemaV1.VaultItemType.GENERATED_PASSWORD to GENERATED_PASSWORD_ALIAS,
            VaultSchema.VaultSchemaV1.VaultItemType.LOGIN to LOGIN_ALIAS
        )
    }

    override fun serialize(
        src: VaultSchema.VaultSchemaV1.VaultItemType,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(toSerialisedForm[src] ?: UNKNOWN_ALIAS)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): VaultSchema.VaultSchemaV1.VaultItemType {

        val name = json.asString.trim().toLower()
        for (vaultItemType in VaultSchema.VaultSchemaV1.VaultItemType.values()) {
            if (vaultItemType.name.equals(other = name, ignoreCase = true)) {
                return vaultItemType
            }
        }
        for ((alias, vaultItemType) in fromSerialisedForm) {
            if (alias.equals(other = name, ignoreCase = true)) {
                return vaultItemType
            }
        }
        return VaultSchema.VaultSchemaV1.VaultItemType.UNKNOWN
    }

    private fun String.toLower(): String = this.toLowerCase(Locale.ENGLISH)
}
