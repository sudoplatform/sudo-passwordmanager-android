/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.entitlements

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Data to represent the current state of the Entitlements for the Sudo Password Manager
 *
 * @since 2020-10-22
 */
@Parcelize
data class EntitlementState(
    /** Name of the Entitlement */
    val name: Entitlement.Name = Entitlement.Name.MAX_VAULTS_PER_SUDO,

    /** The id of the sudo owning the entitlement **/
    val sudoId: String,

    /** The entitlement's limit */
    val limit: Int,

    /** The current value for the entitlement */
    val value: Int
) : Parcelable
