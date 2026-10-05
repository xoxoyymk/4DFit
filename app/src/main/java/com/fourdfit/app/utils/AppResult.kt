package com.fourdfit.app.utils

sealed interface AppResult<out T> {
    data class Success<T>(
        val data: T,
    ) : AppResult<T>

    data class Error(
        val message: String,
        val code: Int? = null,
    ) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
    }

val AppResult<*>.errorMessage: String? get() = (this as? AppResult.Error)?.message
