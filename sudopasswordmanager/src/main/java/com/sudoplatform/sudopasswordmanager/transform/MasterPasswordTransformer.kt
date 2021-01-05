/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager.transform

import java.text.Normalizer

internal data class MasterPasswordTransformer(val userProvidedValue: String) {
    fun standardize(): String {
        return Normalizer.normalize(userProvidedValue.trim(), Normalizer.Form.NFKD)
    }

    fun data(): ByteArray {
        return standardize().toByteArray()
    }
}
