/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.util

import org.spongycastle.util.encoders.Hex
import java.util.Locale

/**
 * Secret code formatting and parsing.
 *
 * @since 2020-10-12
 */

/** KDK is Key deriving key, AKA secret code */
private const val KDK_LENGTH = 32
private const val USER_PREFIX_LENGTH = 5

internal fun formatSecretCode(secretCode: String): String? {
    if (secretCode.length != KDK_LENGTH + USER_PREFIX_LENGTH) {
        return null
    }
    val uppercase = secretCode.toUpperCase(Locale.ROOT)
    return buildString {
        // 5-6-5-5-5-5-6 pattern - first 5 is user subject from SudoUser, rest is the kdk.
        append(uppercase.substring(0, 5)).append("-")
        append(uppercase.substring(5, 11)).append("-")
        append(uppercase.substring(11, 16)).append("-")
        append(uppercase.substring(16, 21)).append("-")
        append(uppercase.substring(21, 26)).append("-")
        append(uppercase.substring(26, 31)).append("-")
        append(uppercase.substring(31, 37))
    }
}

/**
 * Secret code is the last 32 digits of the code passed in. When the code is created we prepend
 * (e.g. hash of user subject) metadata to the front of the secret code for support purposes.
 */
internal fun parseSecretCode(secretCode: String): ByteArray? {
    val hexString = secretCode.replace(Regex("[-, ]"), "")
    return if (hexString.length >= KDK_LENGTH) {
        val startIndex = hexString.length - KDK_LENGTH
        Hex.decode(hexString.substring(startIndex))
    } else {
        null
    }
}
