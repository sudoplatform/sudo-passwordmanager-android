/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import com.sudoplatform.sudopasswordmanager.models.VaultLogin
import com.sudoplatform.sudoprofiles.Sudo
import java.util.Date

/**
 * Data used in Android Connected Device tests.
 *
 * @since 2020-10-14
 */
object AndroidTestData {

    const val PLAIN_TEXT = "The owl and the pussy cat went to sea in a beautiful pea green boat."
    const val FNAME = "Theodore"
    const val LNAME = "Bear"
    const val NAME = "$FNAME $LNAME"
    const val USER = "tedbear"
    val SUDO = Sudo("Mr", FNAME, LNAME, "Shopping", null, null)
    val VAULT_LOGIN = VaultLogin(
        id = "id",
        createdAt = Date(0),
        updatedAt = Date(0),
        name = "Ted Bear",
        user = "tedbear"
    )
}
