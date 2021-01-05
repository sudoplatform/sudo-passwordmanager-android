/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.samples

import android.content.Context
import com.nhaarman.mockitokotlin2.mock
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudopasswordmanager.PasswordStrength
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerClient
import com.sudoplatform.sudopasswordmanager.calculateStrengthOfPassword
import com.sudoplatform.sudopasswordmanager.generatePassword
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI

/**
 * These are sample snippets of code that are included in the generated documentation. They are
 * placed here in the test code so that at least we know they will compile.
 *
 * @since 2020-09-23
 */
@RunWith(RobolectricTestRunner::class)
@Suppress("UNUSED_VARIABLE")
class Samples {

    @Test
    fun mockTest() {
        // Just to keep junit happy
    }

    fun generatePasswordSample() {
        val myPassword = generatePassword(
            length = 20,
            allowUppercase = true,
            allowLowercase = true,
            allowNumbers = true,
            allowSymbols = true
        )
    }

    private val myPassword = "password1"

    fun calculateStrengthOfPasswordSample() {
        val passwordStrength = calculateStrengthOfPassword(myPassword)
        when (passwordStrength) {
            PasswordStrength.VeryWeak,
            PasswordStrength.Weak -> println("Please choose a stronger password")
            else -> {}
        }
    }

    private val context = mock<Context>()

    fun buildClient() {
        // This is how to construct the SudoPasswordManagerClient

        // Create a logger for any messages or errors
        val logger = Logger("MyApplication", AndroidUtilsLogDriver(LogLevel.INFO))

        // Create an instance of SudoUserClient to perform registration and sign in.
        val sudoUserClient = SudoUserClient.builder(context)
            .setNamespace("com.mycompany.myapplication")
            .setLogger(logger)
            .build()

        // Create an instance of SudoProfilesClient to perform creation, deletion and modification of Sudos.
        val blobURI = URI(context.cacheDir.path)
        val sudoProfilesClient = SudoProfilesClient
            .builder(context, sudoUserClient, blobURI)
            .setLogger(logger)
            .build()

        // Create an instance of SudoPasswordManagerClient to manipulate secure vaults of sensitive items
        val sudoPasswordManager = SudoPasswordManagerClient.builder()
            .setContext(context)
            .setSudoUserClient(sudoUserClient)
            .setSudoProfilesClient(sudoProfilesClient)
            .setLogger(logger)
            .build()
    }
}
