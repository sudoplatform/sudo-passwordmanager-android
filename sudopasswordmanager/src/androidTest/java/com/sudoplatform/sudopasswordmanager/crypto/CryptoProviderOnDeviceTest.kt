/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.crypto

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.base.Stopwatch
import com.sudoplatform.sudopasswordmanager.AndroidTestData.PLAIN_TEXT
import com.sudoplatform.sudopasswordmanager.BaseIntegrationTest
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

/**
 * Test the operation of the [DefaultCryptographyProvider] on a real device with real crypto.
 *
 * @since 2020-10-09
 */
@RunWith(AndroidJUnit4::class)
class CryptoProviderOnDeviceTest : BaseIntegrationTest() {

    private val cryptoProvider by lazy {
        DefaultCryptographyProvider(keyManager)
    }

    @Before
    fun init() {
        Timber.plant(Timber.DebugTree())
        keyManager.removeAllKeys()
    }

    @After
    fun fini() = runBlocking {
        Timber.uprootAll()
    }

    @Test
    fun shouldBeAbleEncryptThenDecrypt() {

        val secureFieldKeyBytes = cryptoProvider.generateSecureFieldKey()
        secureFieldKeyBytes.size shouldBe DefaultCryptographyProvider.KEY_SIZE_BYTES

        val encryptedSecureField = cryptoProvider.encryptSecureField(PLAIN_TEXT.toByteArray(), secureFieldKeyBytes)

        val decryptedSecureField = cryptoProvider.decryptSecureField(encryptedSecureField, secureFieldKeyBytes)
        val decryptedClearText = String(decryptedSecureField, Charsets.UTF_8)
        decryptedClearText shouldBe PLAIN_TEXT
    }

    @Test
    @Ignore // Enable when you want to examine peformance
    fun bulkDecryptionShouldBeFast() {

        val secureFields = mutableListOf<ByteArray>()
        val secureFieldKeyBytes = cryptoProvider.generateSecureFieldKey()

        val iterations = 1_000
        for (i in 1..iterations) {
            secureFields.add(cryptoProvider.encryptSecureField(PLAIN_TEXT.toByteArray(), secureFieldKeyBytes))
        }

        val stopwatch = Stopwatch.createStarted()
        secureFields.forEach { encryptedSecureField ->
            val decryptedSecureField = cryptoProvider.decryptSecureField(encryptedSecureField, secureFieldKeyBytes)
            val decryptedClearText = String(decryptedSecureField, Charsets.UTF_8)
            decryptedClearText shouldBe PLAIN_TEXT
        }
        stopwatch.stop()

        println("Decrypting of $iterations secure fields took $stopwatch on ${Build.MANUFACTURER} ${Build.MODEL}")
    }
}
