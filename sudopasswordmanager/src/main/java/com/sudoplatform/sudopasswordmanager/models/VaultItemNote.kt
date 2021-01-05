/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

@Parcelize
data class SecureFieldValue(
    val plainText: String? = null,
    val cipherText: String? = null,
    val revealFunction: @RawValue ((String) -> String)? = null
) : Parcelable {
    fun reveal(): String {
        if (plainText != null) {
            return plainText
        }

        if (cipherText != null && revealFunction != null) {
            return revealFunction.invoke(cipherText)
        }

        return ""
    }
}

@Parcelize
data class VaultItemNote(val value: SecureFieldValue) : Parcelable {

    /**
     * Fetches the password value from the vault store. Notes are stored in memory as cipher text as an added layer of security.
     *
     * @return The clear text note
     * @throws An error if the note cannot be displayed, e.g. if the password manager is locked.
     */
    fun getValue(): String {
        return value.reveal()
    }
}
