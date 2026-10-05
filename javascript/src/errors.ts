/**
 * Every error the library throws extends {@link SportsDbError}. Messages never contain an API
 * key: v1 URLs (which carry the key in their path) are shown with *** in its place.
 */
export class SportsDbError extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options);
    this.name = new.target.name;
  }
}

/** The API rejected the key (HTTP 400 "Invalid Premium API key"). Only 123, 3 and paid keys work. */
export class InvalidApiKeyError extends SportsDbError {}

/** A v2 endpoint was called with a free key. v2 accepts premium keys only. Thrown before any request. */
export class PremiumRequiredError extends SportsDbError {}

/** HTTP 429 after the retry was used up or disabled. */
export class RateLimitError extends SportsDbError {
  /** Milliseconds the server asked to wait (from Retry-After), if it said. */
  readonly retryAfterMs: number | null;
  constructor(message: string, retryAfterMs: number | null) {
    super(message);
    this.retryAfterMs = retryAfterMs;
  }
}

/** Any other non-2xx HTTP response, after retries for 5xx. */
export class HttpStatusError extends SportsDbError {
  readonly status: number;
  readonly bodySnippet: string;
  constructor(message: string, status: number, bodySnippet: string) {
    super(message);
    this.status = status;
    this.bodySnippet = bodySnippet;
  }
}

/** The body was not a TheSportsDB envelope (e.g. an HTML error page). */
export class ResponseParseError extends SportsDbError {}

/**
 * The API answered with a message instead of data: an unrecognised {"Message": ...} body, or a
 * rejected parameter, which v1 reports as text where the records belong:
 * {"seasons": "Invalid League ID passed"}.
 */
export class ApiMessageError extends SportsDbError {
  readonly apiMessage: string;
  constructor(message: string, apiMessage: string) {
    super(message);
    this.apiMessage = apiMessage;
  }
}

/** No HTTP response arrived (DNS, connection, timeout), after retries. */
export class NetworkError extends SportsDbError {}
