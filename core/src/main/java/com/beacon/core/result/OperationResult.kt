package com.beacon.core.result

/**
 * Lightweight result wrapper for operations that can fail, kept separate from
 * Kotlin's [Result] so domain code can model a user-facing message explicitly.
 */
sealed interface OperationResult<out T> {
    data class Success<out T>(val value: T) : OperationResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : OperationResult<Nothing>
}

inline fun <T> OperationResult<T>.onSuccess(block: (T) -> Unit): OperationResult<T> {
    if (this is OperationResult.Success) block(value)
    return this
}

inline fun <T> OperationResult<T>.onFailure(block: (String, Throwable?) -> Unit): OperationResult<T> {
    if (this is OperationResult.Failure) block(message, cause)
    return this
}
