/*
 * Copyright © 2021 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.transform

import com.sudoplatform.sudoentitlements.types.EntitlementsConsumption
import com.sudoplatform.sudopasswordmanager.entitlements.Entitlement
import com.sudoplatform.sudopasswordmanager.util.MAX_VAULTS_PER_SUDO

/**
 * Transform from the Sudo Platform entitlement types to the public types exported by this SDK.
 *
 * @since 2021-01-28
 */
internal object EntitlementTransformer {

    /**
     * Transform from the Sudo Platform entitlement types to the public types exported by this SDK.
     */
    fun transform(entitlementsConsumption: EntitlementsConsumption): List<Entitlement> {
        return entitlementsConsumption.entitlements.entitlements.mapNotNull {
            if (it.name == MAX_VAULTS_PER_SUDO) {
                Entitlement(
                    name = Entitlement.Name.MAX_VAULTS_PER_SUDO,
                    limit = it.value
                )
            } else {
                null
            }
        }
    }
}
