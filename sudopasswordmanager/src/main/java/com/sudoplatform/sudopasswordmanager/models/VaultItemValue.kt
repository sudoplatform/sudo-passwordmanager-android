/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A secured value, such as a note, credit card number or security code.
 *
 * @since 2021-01-19
 */
@Parcelize
data class VaultItemValue(val value: SecureFieldValue) : Parcelable {

    /**
     * Fetches the value from the vault store. Vault item secure values are
     * stored in memory as cipher text as an added layer of security.
     *
     * @return The clear text of the value
     * @throws An error if the value cannot be displayed, e.g. if the password manager is locked.
     */
    fun getValue(): String {
        return value.reveal()
    }
}

/**
 * A note attached to a [VaultItem].
 */
typealias VaultItemNote = VaultItemValue
