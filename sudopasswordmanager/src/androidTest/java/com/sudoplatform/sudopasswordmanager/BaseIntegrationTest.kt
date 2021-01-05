/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sudoplatform.sudoentitlements.SudoEntitlementsClient
import com.sudoplatform.sudokeymanager.KeyManagerFactory
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudoprofiles.ListOption
import com.sudoplatform.sudoprofiles.SudoProfilesClient
import com.sudoplatform.sudouser.SudoUserClient
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import io.kotlintest.shouldBe
import timber.log.Timber

/**
 * Base class of the integration tests of the Sudo Password Manager SDK.
 *
 * @since 2020-09-29
 */
abstract class BaseIntegrationTest {

    private val verbose = true
    private val logLevel = if (verbose) LogLevel.VERBOSE else LogLevel.INFO
    protected val logger = Logger("pwm-test", AndroidUtilsLogDriver(logLevel))

    protected val context: Context = ApplicationProvider.getApplicationContext<Context>()

    protected val userClient by lazy {
        SudoUserClient.builder(context)
            .setNamespace("pwm-client-test")
            .setLogger(logger)
            .build()
    }

    protected val sudoClient by lazy {
        val containerURI = context.cacheDir.toURI()
        SudoProfilesClient.builder(context, userClient, containerURI)
            .setLogger(logger)
            .build()
    }

    protected val keyManager by lazy {
        KeyManagerFactory(context).createAndroidKeyManager()
    }

    protected val entitlementsClient by lazy {
        SudoEntitlementsClient.builder()
            .setContext(context)
            .setLogger(logger)
            .setSudoUserClient(userClient)
            .build()
    }

    private suspend fun registerUser() {
        userClient.isRegistered() shouldBe false

        val privateKey = readTextFile("register_key.private")
        val keyId = readTextFile("register_key.id")

        val authProvider = TESTAuthenticationProvider(
            name = "pwm-client-test",
            privateKey = privateKey,
            publicKey = null,
            keyManager = keyManager,
            keyId = keyId
        )

        userClient.registerWithAuthenticationProvider(authProvider, "pwm-client-test")
    }

    private fun readTextFile(fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use {
            it.readText().trim()
        }
    }

    protected suspend fun signInAndRegisterUser() {
        if (!userClient.isRegistered()) {
            registerUser()
        }
        userClient.isRegistered() shouldBe true
        if (userClient.isSignedIn()) {
            userClient.getRefreshToken()?.let { userClient.refreshTokens(it) }
        } else {
            userClient.signInWithKey()
        }
        userClient.isSignedIn() shouldBe true
        entitlementsClient.redeemEntitlements()
    }

    protected fun clientConfigFilesPresent(): Boolean {
        val configFiles = context.assets.list("")?.filter { fileName ->
            fileName == "sudoplatformconfig.json" ||
                fileName == "register_key.private" ||
                fileName == "register_key.id"
        } ?: emptyList()
        Timber.d("config files present ${configFiles.size}")
        return configFiles.size == 3
    }

    protected suspend fun deleteAllSudos() {
        if (userClient.isRegistered()) {
            sudoClient.listSudos(ListOption.REMOTE_ONLY).forEach {
                try {
                    sudoClient.deleteSudo(it)
                } catch (e: Throwable) {
                    Timber.e(e)
                }
            }
        }
    }
}
