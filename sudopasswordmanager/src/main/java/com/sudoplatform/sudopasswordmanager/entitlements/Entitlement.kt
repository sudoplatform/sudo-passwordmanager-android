/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.entitlements

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Data to represent the entitlements for the Sudo Password Manager
 *
 * @since 2021-01-28
 */
@Parcelize
data class Entitlement(
    /** Name of the Entitlement */
    val name: Name = Name.MAX_VAULTS_PER_SUDO,

    /** The entitlement's limit */
    val limit: Int
) : Parcelable {
    /** Enum that represents the different types of entitlements available */
    enum class Name {
        /** The maximum number of vaults entitled per Sudo */
        MAX_VAULTS_PER_SUDO
    }
}
