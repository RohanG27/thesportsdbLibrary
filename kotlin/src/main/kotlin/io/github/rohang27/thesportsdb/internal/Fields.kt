package io.github.rohang27.thesportsdb.internal

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import io.github.rohang27.thesportsdb.model.RawRecord
import io.github.rohang27.thesportsdb.model.Socials
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

// Lenient readers for TheSportsDB records. Everything arrives as a string (or null), so
// each reader parses defensively: a malformed value becomes null, never an exception.

internal fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.takeIf { it.isNotBlank() }

internal fun JsonObject.long(key: String): Long? = str(key)?.trim()?.let { it.toLongOrNull() ?: it.toDoubleOrNull()?.toLong() }

/** Ids: like [long], but 0 means "none". */
internal fun JsonObject.id(key: String): Long? = long(key)?.takeIf { it != 0L }

internal fun JsonObject.int(key: String): Int? = str(key)?.trim()?.let { it.toIntOrNull() ?: it.toDoubleOrNull()?.toInt() }

/** Years: like [int], but 0 means "unknown". */
internal fun JsonObject.year(key: String): Int? = int(key)?.takeIf { it > 0 }

internal fun JsonObject.double(key: String): Double? = str(key)?.trim()?.removeSuffix("%")?.toDoubleOrNull()

/** `yes`/`no`, `true`/`false`, `1`/`0`, any case. */
internal fun JsonObject.bool(key: String): Boolean? = when (str(key)?.trim()?.lowercase()) {
    "yes", "true", "1", "y" -> true
    "no", "false", "0", "n" -> false
    else -> null
}

/** `strLocked`: "locked" / "unlocked". */
internal fun JsonObject.locked(): Boolean? = when (str("strLocked")?.trim()?.lowercase()) {
    "locked" -> true
    "unlocked" -> false
    else -> null
}

internal fun JsonObject.date(key: String): LocalDate? = str(key)?.trim()?.take(10)?.let {
    try { LocalDate.parse(it) } catch (_: DateTimeParseException) { null }
}

private val timePattern = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?""")

/** `16:00`, `16:00:00`, `16:00:00+00:00` (any offset is ignored). */
internal fun JsonObject.time(key: String): LocalTime? {
    val m = str(key)?.trim()?.let(timePattern::find) ?: return null
    val (h, min, sec) = m.destructured
    return runCatching { LocalTime.of(h.toInt(), min.toInt(), sec.ifEmpty { "0" }.toInt()) }.getOrNull()
}

/**
 * A UTC timestamp. Accepts `2026-10-10T11:30:00` and `2026-10-10 11:30:00` (no zone: UTC,
 * as measured) and ISO strings with an offset or `Z`.
 */
internal fun JsonObject.instant(key: String): Instant? = str(key)?.trim()?.replace(' ', 'T')?.let { s ->
    try {
        OffsetDateTime.parse(s).toInstant()
    } catch (_: DateTimeParseException) {
        try { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) } catch (_: DateTimeParseException) { null }
    }
}

/** A date-time with no known zone (e.g. `dateUpdated`), as given. */
internal fun JsonObject.localDateTime(key: String): LocalDateTime? = str(key)?.trim()?.replace(' ', 'T')?.let {
    try { LocalDateTime.parse(it) } catch (_: DateTimeParseException) { null }
}

/** A comma-separated list, trimmed, blanks dropped. */
internal fun JsonObject.list(key: String): List<String> =
    str(key)?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

/** Values of `prefix1`..`prefixN` that are present, in order. */
internal fun JsonObject.numbered(prefix: String, count: Int): List<String> =
    (1..count).mapNotNull { str("$prefix$it") }

private val descriptionKey = Regex("^strDescription([A-Z]{2})$")

/** `strDescriptionEN`, `strDescriptionDE`, ... as a map keyed by the two-letter code (`EN`, `DE`). */
internal fun JsonObject.descriptions(): Map<String, String> {
    val record = this
    return buildMap {
        for (k in record.keys) {
            val code = descriptionKey.matchEntire(k)?.groupValues?.get(1) ?: continue
            record.str(k)?.let { put(code, it) }
        }
    }
}

internal fun JsonObject.socials(): Socials = Socials(
    website = str("strWebsite"),
    facebook = str("strFacebook"),
    twitter = str("strTwitter"),
    instagram = str("strInstagram"),
    youtube = str("strYoutube"),
    rss = str("strRSS"),
)

internal fun JsonObject.raw(): RawRecord = RawRecord(mapValues { (k, _) -> str(k) })
