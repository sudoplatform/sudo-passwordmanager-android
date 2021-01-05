/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.util

import com.sudoplatform.sudosecurevault.Vault

/**
 * Constants defined by the Sudo Platform in various APIs.
 *
 * @since 2020-10-28
 */

/** The audience used when obtaining an ownership proof from the Sudo */
internal const val SECURE_VAULT_AUDIENCE = "sudoplatform.secure-vault.vault"

/** The value of the issuer field of the [Owners] of a [Vault] that holds the Sudo Identifier */
internal const val SUDO_SERVICE_ISSUER = "sudoplatform.sudoservice"

/** Entitlement name in the Sudo Entitlements Service */
internal const val MAX_VAULTS_PER_SUDO = "sudoplatform.vault.vaultMaxPerSudo"
