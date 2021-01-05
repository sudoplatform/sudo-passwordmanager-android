/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.transform

import android.content.Context
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sudoplatform.sudopasswordmanager.AndroidTestData
import com.sudoplatform.sudopasswordmanager.BaseIntegrationTest
import com.sudoplatform.sudopasswordmanager.PasswordManagerRegistrationStatus
import com.sudoplatform.sudopasswordmanager.SudoPasswordManagerClient
import com.sudoplatform.sudopasswordmanager.crypto.DefaultCryptographyProvider
import com.sudoplatform.sudopasswordmanager.entitlements.EntitlementState
import com.sudoplatform.sudopasswordmanager.models.Vault
import com.sudoplatform.sudopasswordmanager.models.VaultItem
import io.kotlintest.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test the operation of [VaultTransformer] that it can transform from
 * internal to public types and back again.
 *
 * @since 2020-11-05
 */
@RunWith(AndroidJUnit4::class)
class VaultTransformerTest : BaseIntegrationTest() {

    private val cryptoProvider by lazy {
        DefaultCryptographyProvider(keyManager)
    }

    // I tried to use mock<SudoPasswordManagerClient> to do this, just like in the
    // Robolectric tests. However, I couldn't because I needed some special version
    // of mockito-android to do this and when I try and build it fails in the jefifier
    // with a complaint about not supporting min sdk of less than 26. So here we are with
    // an old school manual version.
    private val mockPasswordManager = object : SudoPasswordManagerClient {

        override fun isLocked(): Boolean {
            return false
        }

        override suspend fun getRegistrationStatus(): PasswordManagerRegistrationStatus {
            TODO("Not yet implemented")
        }

        override suspend fun register(masterPassword: String) {
            TODO("Not yet implemented")
        }

        override suspend fun deregister() {
            TODO("Not yet implemented")
        }

        override fun getSecretCode(): String? {
            TODO("Not yet implemented")
        }

        override suspend fun lock() {
            TODO("Not yet implemented")
        }

        override suspend fun unlock(masterPassword: String, secretCode: String?) {
            TODO("Not yet implemented")
        }

        override suspend fun reset() {
            TODO("Not yet implemented")
        }

        override suspend fun createVault(sudoId: String): Vault {
            TODO("Not yet implemented")
        }

        override suspend fun listVaults(): List<Vault> {
            TODO("Not yet implemented")
        }

        override suspend fun getVault(id: String): Vault? {
            TODO("Not yet implemented")
        }

        override suspend fun update(vault: Vault) {
            TODO("Not yet implemented")
        }

        override suspend fun update(item: VaultItem, vault: Vault) {
            TODO("Not yet implemented")
        }

        override suspend fun deleteVault(id: String) {
            TODO("Not yet implemented")
        }

        override suspend fun changeMasterPassword(currentPassword: String, newPassword: String) {
            TODO("Not yet implemented")
        }

        override suspend fun add(item: VaultItem, vault: Vault): String {
            TODO("Not yet implemented")
        }

        override suspend fun listVaultItems(vault: Vault): List<VaultItem> {
            TODO("Not yet implemented")
        }

        override suspend fun getVaultItem(id: String, vault: Vault): VaultItem? {
            TODO("Not yet implemented")
        }

        override suspend fun removeVaultItem(id: String, vault: Vault) {
            TODO("Not yet implemented")
        }

        override suspend fun renderRescueKit(context: Context, template: ByteArray?): PdfDocument {
            TODO("Not yet implemented")
        }

        override suspend fun getEntitlementState(): List<EntitlementState> {
            TODO("Not yet implemented")
        }
    }

    private val vaultTransformer by lazy {
        VaultTransformer(cryptoProvider, mockPasswordManager)
    }

    @Before
    fun init() {
        keyManager.removeAllKeys()
    }

    @Test
    fun shouldBeAbleTransformToAndFromLogin() {
        val vaultKey = cryptoProvider.generateKeyDerivingKey()
        val loginProxy = vaultTransformer.createVaultLoginProxy(AndroidTestData.VAULT_LOGIN, vaultKey)
        val login = vaultTransformer.createVaultLogin(loginProxy, vaultKey)
        login.name shouldBe AndroidTestData.VAULT_LOGIN.name
        login.user shouldBe AndroidTestData.VAULT_LOGIN.user
        login.password?.getValue() shouldBe AndroidTestData.VAULT_LOGIN.password?.getValue()
        login.notes?.getValue() shouldBe AndroidTestData.VAULT_LOGIN.notes?.getValue()
    }
}
