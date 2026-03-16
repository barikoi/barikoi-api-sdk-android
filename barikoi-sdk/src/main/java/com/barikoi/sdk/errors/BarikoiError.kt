package com.barikoi.sdk.errors

/**
 * Sealed class representing all possible errors from the Barikoi SDK.
 *
 * Typical usage:
 * ```kotlin
 * client.reverseGeocode(lat, lon)
 *     .onSuccess { place -> ... }
 *     .onFailure { error ->
 *         when {
 *             error.isNetworkError -> showOfflineMessage()
 *             error.isAuthError    -> promptReEnterApiKey()
 *             else                 -> showGenericError(error.message)
 *         }
 *     }
 * ```
 */
sealed class BarikoiError : Exception() {

    /** Network-related errors (no internet, timeout, DNS failure, etc.). */
    data class NetworkError(
        override val message: String,
        override val cause: Throwable? = null
    ) : BarikoiError()

    /** Generic HTTP error for status codes not covered by the subclasses below. */
    data class HttpError(val code: Int, override val message: String) : BarikoiError()

    /** HTTP 401 / 403 – API key is invalid, missing, or access is forbidden. */
    data class UnauthorizedError(
        override val message: String = "Invalid or missing API key"
    ) : BarikoiError()

    /** HTTP 402 – API quota or billing limit exceeded. */
    data class QuotaExceededError(
        override val message: String = "API quota exceeded"
    ) : BarikoiError()

    /** HTTP 429 – too many requests in a short period. */
    data class RateLimitError(
        override val message: String = "Too many requests. Please try again later."
    ) : BarikoiError()

    /** HTTP 400 – invalid or missing request parameters. */
    data class BadRequestError(
        override val message: String = "Invalid request parameters"
    ) : BarikoiError()

    /**
     * Invalid arguments passed by the caller (e.g. blank query, out-of-range coordinates,
     * empty waypoints list). This is a **client-side** programming error, not a network error.
     */
    data class ValidationError(
        override val message: String
    ) : BarikoiError()

    /** The response was received but could not be parsed or was missing expected fields. */
    data class ParseError(
        override val message: String,
        override val cause: Throwable? = null
    ) : BarikoiError()

    /** HTTP 503 – server is temporarily unavailable (maintenance / overloaded). */
    data class ServiceUnavailableError(
        override val message: String = "Service temporarily unavailable. Please try again later."
    ) : BarikoiError()

    /** An unexpected error not covered by any of the above categories. */
    data class UnknownError(
        override val message: String = "An unknown error occurred",
        override val cause: Throwable? = null
    ) : BarikoiError()

    // ─── Convenience helpers ──────────────────────────────────────────────────

    /** `true` if the failure was caused by connectivity (no internet, timeout, etc.). */
    val isNetworkError: Boolean get() = this is NetworkError

    /** `true` if the failure was caused by an invalid or missing API key. */
    val isAuthError: Boolean get() = this is UnauthorizedError

    /** `true` if the app has exceeded its API quota or billing limit. */
    val isQuotaError: Boolean get() = this is QuotaExceededError

    /** `true` if the app is sending too many requests too quickly. */
    val isRateLimitError: Boolean get() = this is RateLimitError

    /** `true` if the server is temporarily unavailable (HTTP 503). */
    val isServiceUnavailable: Boolean get() = this is ServiceUnavailableError

    /** `true` if the caller passed invalid arguments (blank query, bad coordinates, etc.). */
    val isValidationError: Boolean get() = this is ValidationError

    /** `true` if the response was received but could not be parsed. */
    val isParseError: Boolean get() = this is ParseError

    /** `true` if the server returned HTTP 404. */
    val isNotFound: Boolean get() = this is HttpError && code == 404

    /** `true` for any 4xx client-side error (including local validation errors). */
    val isClientError: Boolean
        get() = this is UnauthorizedError ||
                this is QuotaExceededError ||
                this is RateLimitError ||
                this is BadRequestError ||
                this is ValidationError ||
                (this is HttpError && code in 400..499)

    /** `true` for any 5xx server-side error. */
    val isServerError: Boolean
        get() = this is ServiceUnavailableError ||
                (this is HttpError && code in 500..599)
}
