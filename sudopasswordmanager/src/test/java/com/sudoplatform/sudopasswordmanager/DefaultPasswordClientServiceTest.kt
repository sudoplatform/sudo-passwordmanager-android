/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import com.sudoplatform.sudopasswordmanager.TestData.KEY_VALUE
import com.sudoplatform.sudopasswordmanager.TestData.USER_ID
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [DefaultPasswordClientService] using mocks and spies.
 *
 * @since 2020-09-29
 */
@RunWith(RobolectricTestRunner::class)
internal class DefaultPasswordClientServiceTest : BaseTests() {

    @After
    fun fini() {
        verifyNoMoreInteractions(
            mockCryptographyProvider,
            mockKeyStore,
            mockUserClient,
            mockSecureVaultClient
        )
    }

    @Test
    fun `getKey() should return key from keyStore`() {
        // when
        val key = passwordClientService.getKey()
        key shouldNotBe null
        key shouldBe KEY_VALUE

        // then
        verify(mockKeyStore).getKey("kdk-$USER_ID")
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `setKey() should set key on keyStore`() {
        // when
        passwordClientService.set(KEY_VALUE)

        // then
        verify(mockKeyStore).add(KEY_VALUE, "kdk-$USER_ID")
        verify(mockUserClient).getUserName()
    }
}
