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
import java.text.DateFormat
import java.time.Instant
import java.util.Date

/**
 * Convert a [Date] to and from an ISO-8601 string representation when the date is being
 * serialised and deserialised to/from JSON. This is the format expected for dates by the
 * iOS implementation of the Password Manager SDK.
 *
 * @since 2021-04-21
 */
internal class DateTypeConverter : JsonSerializer<Date>, JsonDeserializer<Date> {

    override fun serialize(src: Date, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        var dateTimeString = Instant.ofEpochMilli(src.time ?: 0L).toString()
        if (dateTimeString.contains(Regex(":[0-9][0-9]Z"))) {
            // Date time string lacks the milliseconds part probably because it's zero.
            // It must be added otherwise iOS refuses to parse it.
            dateTimeString = dateTimeString.replace("Z", ".00Z")
        }
        return JsonPrimitive(dateTimeString)
    }

    @Throws(JsonParseException::class)
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Date {
        val dateTimeString = json.asString
        return parseIso8601DateTime(dateTimeString)
            ?: parseLegacyDateTime(dateTimeString)
            ?: Date(0L)
    }

    private fun parseIso8601DateTime(s: String): Date? {
        return try {
            Date(Instant.parse(s).toEpochMilli())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * If we failed to deserialise the date as ISO 8601, try using the native legacy java.util.Date
     * format in case this is a vault that was created before the serialised format was changed
     * to be ISO-8601.
     */
    private fun parseLegacyDateTime(s: String): Date? {
        return try {
            DateFormat.getDateTimeInstance().parse(s)
        } catch (e: Exception) {
            null
        }
    }
}
