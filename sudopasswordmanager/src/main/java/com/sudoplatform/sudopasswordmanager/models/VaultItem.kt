/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import java.util.Date

/**
 * Items held in a [Vault] implement this interface.
 */
interface VaultItem {
    /** Unique identifier of this item */
    val id: String

    /** Time created. Unix Time (seconds since epoch) */
    val createdAt: Date

    /** Time updated. Unix Time (seconds since epoch) */
    val updatedAt: Date
}
