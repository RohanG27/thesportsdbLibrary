package sportsdb.model

/**
 * Every field of a record exactly as the API sent it (blank strings as null).
 * Use it for fields this library does not model yet: `team.raw["strKeywords"]`.
 */
public class RawRecord(public val fields: Map<String, String?>) {
    public operator fun get(field: String): String? = fields[field]

    override fun equals(other: Any?): Boolean = other is RawRecord && other.fields == fields
    override fun hashCode(): Int = fields.hashCode()
    override fun toString(): String = "RawRecord(${fields.size} fields)"
}

/** Social and web links. TheSportsDB often gives these without a scheme (`www.facebook.com/Arsenal`). */
public data class Socials(
    val website: String?,
    val facebook: String?,
    val twitter: String?,
    val instagram: String?,
    val youtube: String?,
    val rss: String?,
)

/** A league a team plays in: one of `idLeague`/`strLeague` .. `idLeague7`/`strLeague7`. */
public data class LeagueRef(val id: Long, val name: String?)

/** Image sizes TheSportsDB serves for any `r2.thesportsdb.com` image by appending a path suffix. */
public enum class ImageSize(internal val suffix: String) {
    /** About 70% of the original's bytes. */
    MEDIUM("/medium"),

    /** About 35%. Good for cards. */
    SMALL("/small"),

    /** About 10%. Good for lists and badges in tables. */
    TINY("/tiny"),
}

/**
 * The same image at a smaller [size]. Only TheSportsDB-hosted images are resized; other
 * URLs are returned unchanged. Image fields are nullable, so call it with `?.`:
 *
 * ```
 * team.badge?.sized(ImageSize.TINY)
 * ```
 */
public fun String.sized(size: ImageSize): String {
    val isSportsDb = startsWith("https://r2.thesportsdb.com/") || startsWith("https://www.thesportsdb.com/images/media/")
    if (!isSportsDb) return this
    val bare = ImageSize.entries.fold(this) { url, s -> url.removeSuffix(s.suffix) }
    return bare + size.suffix
}

/** A broad reading of a `strStatus` code, so you don't need to know every sport's codes. */
public enum class EventStatus {
    NOT_STARTED,
    IN_PLAY,
    FINISHED,
    POSTPONED,
    CANCELLED,
    ABANDONED,

    /** Null, or a code this library doesn't know. The raw code is still on the record. */
    UNKNOWN,
    ;

    public companion object {
        private val finished = setOf(
            "FT", "AET", "PEN", "AOT", "AP", "FINISHED", "MATCH FINISHED", "FINAL", "ENDED", "AWD", "WO",
        )
        private val inPlay = setOf(
            "1H", "2H", "HT", "ET", "BT", "P", "LIVE", "INT", "BREAK", "Q1", "Q2", "Q3", "Q4", "OT",
            "P1", "P2", "P3", "SO", "IN PROGRESS",
        )
        private val notStarted = setOf("NS", "TBD", "NOT STARTED", "SCHEDULED")
        private val postponed = setOf("PST", "POSTPONED", "DELAYED", "SUSP", "INTERRUPTED")
        private val cancelled = setOf("CANC", "CANCELLED", "CANCELED")
        private val abandoned = setOf("ABD", "ABANDONED")

        /** Classifies a raw `strStatus` code (`NS`, `2H`, `Q3`, `P2`, `IN4`, `FT`, `PST`...). */
        public fun of(code: String?): EventStatus {
            val c = code?.trim()?.uppercase() ?: return UNKNOWN
            return when {
                c in notStarted -> NOT_STARTED
                c in finished -> FINISHED
                c in postponed -> POSTPONED
                c in cancelled -> CANCELLED
                c in abandoned -> ABANDONED
                c in inPlay -> IN_PLAY
                c.matches(Regex("""IN\d+""")) -> IN_PLAY // baseball innings
                else -> UNKNOWN
            }
        }
    }
}
