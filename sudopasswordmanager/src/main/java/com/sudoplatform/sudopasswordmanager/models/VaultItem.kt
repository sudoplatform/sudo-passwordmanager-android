/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.models

import java.util.Date

interface VaultItem {
    /** Unique identifier of this item */
    val id: String

    /** Time created. Unix Time (seconds since epoch) */
    val createdAt: Date

    /** Time created. Unix Time (seconds since epoch) */
    val updatedAt: Date
}
