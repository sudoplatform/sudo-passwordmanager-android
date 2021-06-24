/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.util

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.Test

/**
 * Test the correct operation of the methods that format and parse the secret code.
 *
 * @since 2020-10-12
 */
class SecretCodeTest {

    @Test
    fun `formatSecretCode() should return null if secret code is not 37 chars in length`() {
        formatSecretCode("") shouldBe null
        formatSecretCode("a") shouldBe null
        formatSecretCode("123456789012345678901234567890123") shouldBe null
    }

    @Test
    fun `formatSecretCode() should return correctly formatted code`() {
        val secretCode = formatSecretCode("abc00aAAAbBBBcCCCdDDDeEEEfFFFgGGGhHHH")
        // 5-6-5-5-5-5-6 pattern - first 5 is user sub from SudoUser, rest is the kdk.
        secretCode shouldBe "ABC00-AAAABB-BBCCC-CDDDD-EEEEF-FFFGG-GGHHHH"
    }

    @Test
    fun `parseSecretCode() should use the last 32 characters`() {
        val secretCodeString = "aaaaa-000000-00000-00000-00000-00000-000000"
        val secretCode = parseSecretCode(secretCodeString)
        secretCode shouldNotBe null
        secretCode!!.size shouldBe 16
        val zero: Byte = 0
        for (element in secretCode) {
            element shouldBe zero
        }
    }

    @Test
    fun `parseSecretCode() should ignore whitespaces and dashes`() {
        val secretCodeDashes = parseSecretCode("000000-00000-00000-00000-00000-000000")

        val secretCodeSpaces = parseSecretCode(" 000000 00000 00000 00000 00000 000000 ")
        val secretCodeNoDashes = parseSecretCode("00000000000000000000000000000000")
        val secretCodeSpacesAndDashes = parseSecretCode("   000000 - 00000 - 00000 - 00000 - 00000 - 000000   ")

        secretCodeDashes shouldBe secretCodeSpaces
        secretCodeDashes shouldBe secretCodeNoDashes
        secretCodeDashes shouldBe secretCodeSpacesAndDashes
    }
}
