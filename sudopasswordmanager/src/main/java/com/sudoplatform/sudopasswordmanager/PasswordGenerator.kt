/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import com.nulabinc.zxcvbn.Zxcvbn
import kotlin.random.asKotlinRandom

private const val MIN_PASSWORD_LENGTH = 6
private const val DEFAULT_PASSWORD_LENGTH = 20

/**
 * Generate a password
 * @param length The number of characters in the generated password. Must be >= 6
 * @param allowUppercase Whether the password should contain at least one uppercase letter (O and I excluded)
 * @param allowLowercase Whether the password should contain at least one lowercase letter (o and l excluded)
 * @param allowNumbers Whether the password should contain at least one number (0 and 1 excluded)
 * @param allowSymbols Whether the password should contain at least one of the symbols "!?@*._-"
 * @return The generated password
 * @sample com.sudoplatform.sudopasswordmanager.samples.Samples.generatePasswordSample
 */
fun generatePassword(
    length: Int = DEFAULT_PASSWORD_LENGTH,
    allowUppercase: Boolean = true,
    allowLowercase: Boolean = true,
    allowNumbers: Boolean = true,
    allowSymbols: Boolean = true
): String {
    val secureRandom = java.security.SecureRandom().asKotlinRandom()

    // default to all characters allowed if all are toggled off
    var allAllowed = false
    if (!allowUppercase && !allowLowercase && !allowNumbers && !allowSymbols) {
        allAllowed = true
    }

    // add possible characters excluding ambiguous characters `oO0lI1`
    val uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    val lowercase = "abcdefghijkmnpqrstuvwxyz"
    val numbers = "23456789"
    val symbols = "!?@*._-"

    val allPossibleCharacters = StringBuilder()
    val password = StringBuilder()
    if (allowUppercase || allAllowed) {
        allPossibleCharacters.append(uppercase)
        // add one character from this set to ensure there's at least one
        password.append(uppercase.random(secureRandom))
    }
    if (allowLowercase || allAllowed) {
        allPossibleCharacters.append(lowercase)
        // add one character from this set to ensure there's at least one
        password.append(lowercase.random(secureRandom))
    }
    if (allowNumbers || allAllowed) {
        allPossibleCharacters.append(numbers)
        // add one character from this set to ensure there's at least one
        password.append(numbers.random(secureRandom))
    }
    if (allowSymbols || allAllowed) {
        allPossibleCharacters.append(symbols)
        // add one character from this set to ensure there's at least one
        password.append(symbols.random(secureRandom))
    }

    // restrict length to greater than or equal to 6
    val finalLength = length.coerceAtLeast(MIN_PASSWORD_LENGTH)

    // generate password
    for (i in 0 until finalLength - password.length) {
        val character = allPossibleCharacters.random(secureRandom)
        password.append(character)
    }
    // shuffle the password so it doesn't always start with uppercase->lowercase->etc..
    val charArray = password.toString().toMutableList()
    charArray.shuffle(secureRandom)
    return charArray.joinToString("")
}

/** The calculated strength of a password */
enum class PasswordStrength {
    VeryWeak,
    Weak,
    Moderate,
    Strong,
    VeryStrong,
}

/**
 * Calculate the strength of a password
 * @param password The password string to calculate
 * @return A [PasswordStrength] indicating the strength of the given password
 * @sample com.sudoplatform.sudopasswordmanager.samples.Samples.calculateStrengthOfPasswordSample
 */
fun calculateStrengthOfPassword(password: String): PasswordStrength {
    val zxc = Zxcvbn()
    return when (zxc.measure(password).score) {
        0 -> PasswordStrength.VeryWeak
        1 -> PasswordStrength.Weak
        2 -> PasswordStrength.Moderate
        3 -> PasswordStrength.Strong
        4 -> PasswordStrength.VeryStrong
        else -> PasswordStrength.VeryWeak
    }
}
