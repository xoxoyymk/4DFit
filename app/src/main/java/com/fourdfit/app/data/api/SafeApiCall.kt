package com.fourdfit.app.data.api

import com.fourdfit.app.utils.AppResult
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Wraps an API call and converts failures into user-readable [AppResult.Error]s. */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        AppResult.Error(parseHttpError(e), e.code())
    } catch (e: IOException) {
        AppResult.Error("Can't reach the server. Check your connection and try again.")
    } catch (e: Exception) {
        AppResult.Error(e.message ?: "Something went wrong. Try again.")
    }

private fun parseHttpError(e: HttpException): String {
    val serverMessage =
        runCatching {
            val body =
                e
                    .response()
                    ?.errorBody()
                    ?.string()
                    .orEmpty()
            JsonParser
                .parseString(body)
                .asJsonObject
                .get("message")
                ?.asString
        }.getOrNull()
    if (!serverMessage.isNullOrBlank()) return serverMessage
    return when (e.code()) {
        400 -> "Some details look incorrect. Check the form and try again."
        401 -> "Your session has expired. Sign in again."
        403 -> "You don't have access to this."
        404 -> "We couldn't find that."
        409 -> "An account with this email already exists."
        429 -> "Too many attempts. Wait a moment and try again."
        in 500..599 -> "The server is having trouble. Try again soon."
        else -> "Request failed (${e.code()})."
    }
}
