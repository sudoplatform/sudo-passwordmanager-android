/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.UUID

/**
 * Bank Account details.
 *
 * @since 2021-01-19
 */
@Parcelize
data class VaultBankAccount(
    /** Unique identifier of this [VaultItem] */
    override var id: String = UUID.randomUUID().toString(),

    /** Time created. Unix Time (seconds since epoch) */
    override var createdAt: Date = Date(),

    /** Time updated. Unix Time (seconds since epoch) */
    override var updatedAt: Date = Date(),

    /** The name of this [VaultItem] */
    val name: String,

    /** The type of bank account */
    val accountType: String? = null,

    /** Bank account number */
    val accountNumber: VaultItemValue? = null,

    /** Bank account pin */
    val accountPin: VaultItemValue? = null,

    /** Bank name */
    val bankName: String? = null,

    /** Bank branch address */
    val branchAddress: String? = null,

    /** Bank branch phone number */
    val branchPhone: String? = null,

    /** International Bank Account Number */
    val ibanNumber: String? = null,

    /** Bank Account Routing Number */
    val routingNumber: String? = null,

    /** Bank SWIFT code */
    val swiftCode: String? = null,

    /** Notes about the bank account */
    val notes: VaultItemNote? = null

) : VaultItem, Parcelable
