/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * A password that is held securely in memory until its value is needed.
 */
@Parcelize
data class VaultItemPassword(
    /** The secure value */
    val value: SecureFieldValue,
    /** created */
    val createdAt: Date = Date(),
    /** when this item was replaced */
    val replacedAt: Date? = null
) : Parcelable {
    /**
     * Fetches the password value from the vault store. Passwords are stored in memory as cipher
     * text as an added layer of security.
     *
     * @return The clear text password
     */
    fun getValue(): String {
        return value.reveal()
    }
}
