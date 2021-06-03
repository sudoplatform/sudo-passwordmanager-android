/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import io.kotlintest.matchers.string.shouldEndWith
import io.kotlintest.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.robolectric.RobolectricTestRunner

/**
 * Test the operation of [SudoPasswordManagerClient.getSecretCode] using mocks and spies.
 *
 * @since 2020-10-07
 */
@RunWith(RobolectricTestRunner::class)
internal class SudoPasswordManagerClientGetSecretCodeTest : BaseTests() {

    @After
    fun fini() {
        verifyNoMoreInteractions(
            mockContext,
            mockUserClient,
            mockProfilesClient,
            mockCryptographyProvider,
            mockKeyStore,
            mockSecureVaultClient,
            mockVaultStore,
            mockEntitlementsClient
        )
    }

    @Test
    fun `getSecretCode() include key from keystore`() = runBlocking<Unit> {

        passwordManager.getSecretCode() shouldEndWith "616161-61626-26262-63636-36364-646464"

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getSubject()
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `getSecretCode() should return null when missing key`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn null
        }

        passwordManager.getSecretCode() shouldBe null

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getSubject()
        verify(mockUserClient).getUserName()
    }

    @Test
    fun `getSecretCode() should return null key too short`() = runBlocking<Unit> {

        mockKeyStore.stub {
            on { getKey(anyString()) } doReturn "foo".toByteArray()
        }

        passwordManager.getSecretCode() shouldBe null

        verify(mockKeyStore).getKey(anyString())
        verify(mockUserClient).getSubject()
        verify(mockUserClient).getUserName()
    }
}
