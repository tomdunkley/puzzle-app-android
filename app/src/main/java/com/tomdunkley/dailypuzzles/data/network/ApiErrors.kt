package com.tomdunkley.dailypuzzles.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class ErrorDetailDto(val detail: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/** True for connectivity failures (no signal, DNS failure, timeout, etc) -- anything
 * that never got an HTTP response at all. HttpException (a real response, just a
 * non-2xx one) is deliberately excluded.
 */
fun Throwable.isOffline(): Boolean = this is IOException

/** Extracts FastAPI's default `{"detail": "..."}` error body, if present, falling back
 * to a generic message. Use this instead of `Throwable.message` anywhere a Retrofit/
 * OkHttp call result is surfaced to the user.
 */
fun Throwable.toUserMessage(fallback: String = "Something went wrong"): String {
    if (isOffline()) return "No internet connection. Check your connection and try again."
    val httpException = this as? HttpException ?: return message ?: fallback
    val body = httpException.response()?.errorBody()?.string() ?: return fallback
    return runCatching { errorJson.decodeFromString<ErrorDetailDto>(body) }
        .getOrNull()?.detail ?: fallback
}
