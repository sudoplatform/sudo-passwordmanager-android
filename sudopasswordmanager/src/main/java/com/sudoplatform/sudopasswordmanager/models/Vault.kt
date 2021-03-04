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
 * A password manager vault in which you can store [VaultItem]s such as login credentials,
 * credit cards, and bank accounts
 */
@Parcelize
open class Vault(
    /** Identifier of the vault */
    val id: String,

    /** Identifier(s) of the vault owner(s) */
    val owners: List<VaultOwner>,

    /** When the vault was created */
    val createdAt: Date,

    /** When the vault was last updated. */
    var updatedAt: Date
) : Parcelable
