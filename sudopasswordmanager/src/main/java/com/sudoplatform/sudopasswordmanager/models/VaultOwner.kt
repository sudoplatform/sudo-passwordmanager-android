/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * An owner of a vault. The ownership is granted by a service known as the issuer.
 *
 * @since 2020-11-02
 */
@Parcelize
data class VaultOwner(
    /** Identifier of the owner */
    val id: String,
    /** Identifier of the issuer that grants ownership */
    val issuer: String
) : Parcelable
