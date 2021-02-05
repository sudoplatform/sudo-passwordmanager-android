/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date
import java.util.UUID

/**
 * Login Credentials with password history.
 */
@Parcelize
data class VaultLogin(
    /** Unique identifier of this item */
    override var id: String = UUID.randomUUID().toString(),

    /** Time created. Unix Time (seconds since epoch) */
    override var createdAt: Date = Date(),

    /** Time updated. Unix Time (seconds since epoch) */
    override var updatedAt: Date = Date(),

    /** Username for the service */
    val user: String? = null,

    /** URL or domain of the service. */
    val url: String? = null,

    /** The name of this item */
    val name: String,

    /** Space to store notes about the service. */
    val notes: VaultItemNote? = null,

    /** Password for the service */
    val password: VaultItemPassword? = null,

    /** A list of previous passwords used for this service.  Currently contains a complete list of all previously saved passwords. */
    val previousPasswords: List<VaultItemPassword> = emptyList()

) : VaultItem, Parcelable
