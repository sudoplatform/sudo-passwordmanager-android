/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PasswordGeneratorUnitTests {

    @Before
    fun setup() {
    }

    @Test
    fun testDefaultParameters() {
        val password = generatePassword()
        // check length
        assertEquals(password.length, 20)
        // check for one of each character type
        assertTrue(password.contains(Regex("[A-Z]")))
        assertTrue(password.contains(Regex("[a-z]")))
        assertTrue(password.contains(Regex("[2-9]")))
        assertTrue(password.contains(Regex("[!?@*._-]")))
    }

    @Test
    fun testSetLength() {
        val password = generatePassword(25)
        assertEquals(password.length, 25)
    }

    @Test
    fun testMinLength() {
        val password = generatePassword(0)
        assertEquals(password.length, 6)
    }

    @Test
    fun testExcludeUppercase() {
        val password = generatePassword(10, false)
        assertFalse(password.contains(Regex("[A-Z]")))
    }

    @Test
    fun testExcludeLowercase() {
        val password = generatePassword(10, true, false)
        assertFalse(password.contains(Regex("[a-z]")))
    }

    @Test
    fun testExcludeNumbers() {
        val password = generatePassword(10, true, true, false)
        assertFalse(password.contains(Regex("[2-9]")))
    }

    @Test
    fun testExcludeSymbols() {
        val password = generatePassword(10, true, true, true, false)
        assertFalse(password.contains(Regex("[!?@*._-]")))
    }

    @Test
    fun testExcludeAll() {
        // excluding all should enable all
        val password = generatePassword(10, false, false, false, false)
        assertTrue(password.contains(Regex("[A-Z]")))
        assertTrue(password.contains(Regex("[a-z]")))
        assertTrue(password.contains(Regex("[2-9]")))
        assertTrue(password.contains(Regex("[!?@*._-]")))
    }

    @Test
    fun testNoAmbiguousCharacters() {
        for (i in 0..20) {
            val password = generatePassword(50)
            assertFalse(password.contains(Regex("[oO0lI1]")))
        }
    }

    @Test
    fun testVeryWeakPassword() {
        val password = "password"
        val strength = calculateStrengthOfPassword(password)
        assertEquals(strength, PasswordStrength.VeryWeak)
    }

    @Test
    fun testWeakPassword() {
        val password = "mypassword"
        val strength = calculateStrengthOfPassword(password)
        assertEquals(strength, PasswordStrength.Weak)
    }

    @Test
    fun testModeratePassword() {
        val password = "ModeratePassword1"
        val strength = calculateStrengthOfPassword(password)
        assertEquals(strength, PasswordStrength.Moderate)
    }

    @Test
    fun testStrongPassword() {
        val password = "StrongPassword1!"
        val strength = calculateStrengthOfPassword(password)
        assertEquals(strength, PasswordStrength.Strong)
    }

    @Test
    fun testVeryStrongPassword() {
        val password = "MyVeryStrongPassword123!?"
        val strength = calculateStrengthOfPassword(password)
        assertEquals(strength, PasswordStrength.VeryStrong)
    }
}
