package sportsdb

import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Base class for every error this library throws.
 *
 * Messages never contain an API key: v1 URLs (which carry the key in their path) are
 * redacted before they are put into a message.
 */
public sealed class SportsDbException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** The API rejected the key (HTTP 400 `Invalid Premium API key`). Only `123` and paid keys work. */
public class InvalidApiKeyException(message: String) : SportsDbException(message)

/** A v2 endpoint was called with the free key. v2 accepts premium keys only. */
public class PremiumRequiredException(message: String) : SportsDbException(message)

/**
 * The rate limit was exceeded (HTTP 429) and retries were used up or disabled.
 * [retryAfter] is how long the server asked to wait, when it said.
 */
public class RateLimitException(message: String, public val retryAfter: Duration?) : SportsDbException(message) {
    /** Java: [retryAfter] as a `java.time.Duration`. */
    public fun retryAfterDuration(): java.time.Duration? = retryAfter?.toJavaDuration()
}

/** Any other non-2xx HTTP response. [bodySnippet] is the start of the response body. */
public class HttpStatusException(message: String, public val status: Int, public val bodySnippet: String) :
    SportsDbException(message)

/** The response body could not be understood as a TheSportsDB envelope. */
public class ResponseParseException(message: String, cause: Throwable? = null) : SportsDbException(message, cause)

/** The API answered with a `{"Message": ...}` body this library does not recognise. */
public class ApiMessageException(message: String, public val apiMessage: String) : SportsDbException(message)

/** The request failed before an HTTP response arrived (DNS, connection, timeout...). */
public class NetworkException(message: String, cause: Throwable) : SportsDbException(message, cause)
