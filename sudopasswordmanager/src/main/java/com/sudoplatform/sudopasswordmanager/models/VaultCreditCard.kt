/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Date
import java.util.UUID

/**
 * Credit card details.
 *
 * @since 2021-01-19
 */
@Parcelize
data class VaultCreditCard(
    /** Unique identifier of this [VaultItem] */
    override var id: String = UUID.randomUUID().toString(),

    /** Time created. Unix Time (seconds since epoch) */
    override var createdAt: Date = Date(),

    /** Time updated. Unix Time (seconds since epoch) */
    override var updatedAt: Date = Date(),

    /** The name of this [VaultItem] */
    val name: String,

    /** The card holder name */
    var cardName: String? = null,

    /** The type of credit card */
    val cardType: String? = null,

    /** Credit card number */
    val cardNumber: VaultItemValue? = null,

    /** Credit card expiration date */
    val expiresAt: Date? = null,

    /** Security code from the back of the card */
    val securityCode: VaultItemValue? = null,

    /** Notes about the credit card. */
    val notes: VaultItemNote? = null

) : VaultItem, Parcelable
