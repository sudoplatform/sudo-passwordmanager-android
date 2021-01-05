/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.sudoplatform.sudopasswordmanager.util

import com.sudoplatform.sudosecurevault.Owner
import com.sudoplatform.sudosecurevault.Vault

/**
 * Extension functions for handling the [SudoSecureVaultClient].
 *
 * @since 2020-10-29
 */

internal fun Vault.getOwningSudoId(): String? {
    return owners.getOwningSudoId()
}

internal fun List<Owner>.getOwningSudoId(): String? {
    return firstOrNull { it.issuer == SUDO_SERVICE_ISSUER }?.id
}
