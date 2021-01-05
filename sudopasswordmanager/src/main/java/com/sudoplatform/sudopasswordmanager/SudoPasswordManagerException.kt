/*
 * Copyright © 2020 Anonyome Labs, Inc. All rights reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.sudoplatform.sudopasswordmanager

/**
 * Defines the exceptions thrown by the methods of the [SudoPasswordManagerClient].
 *
 * @property message Accompanying message for the exception.
 * @property cause The cause for the exception.
 */
sealed class SudoPasswordManagerException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause) {
    class SudoNotFoundException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class UnauthorizedUserException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class CryptographyException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class InvalidPasswordOrMissingSecretCodeException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class InvalidFormatException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class VaultLockedException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class InvalidVaultException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class VaultNotFoundException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class MissingSudoOwnerException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class FailedException(message: String? = null, cause: Throwable? = null) :
        SudoPasswordManagerException(message = message, cause = cause)
    class UnknownException(cause: Throwable) :
        SudoPasswordManagerException(cause = cause)
}
