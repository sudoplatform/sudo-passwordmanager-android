/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue

/**
 * A secure field that contains a value that is held encrypted until it's needed.
 */
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
