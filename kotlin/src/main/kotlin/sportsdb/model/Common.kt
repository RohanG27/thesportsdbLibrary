package sportsdb.model

/** Every field of a record exactly as the API sent it, with blank strings as null. */
public class RawRecord internal constructor(public val fields: Map<String, String?>) {
    public operator fun get(field: String): String? = fields[field]

    override fun equals(other: Any?): Boolean = other is RawRecord && other.fields == fields
    override fun hashCode(): Int = fields.hashCode()
    override fun toString(): String = "RawRecord(${fields.size} fields)"
}

/**
 * Base class of every record the API returns ([Team], [Event], ...).
 *
 * A record's properties are all read from [raw], so two records are equal when the API sent
 * the same fields. Records are created only by the library: constructors are internal, so new
 * properties can be added in later versions without breaking compiled code. To test code
 * that uses them, fake the HTTP layer ([sportsdb.http.HttpTransport]) instead.
 */
public abstract class ApiRecord internal constructor(
    /** Every field as the API sent it. Use it for fields not modelled yet: `team.raw["strKeywords"]`. */
    public val raw: RawRecord,
) {
    final override fun equals(other: Any?): Boolean =
        this === other || (other != null && other.javaClass == javaClass && (other as ApiRecord).raw == raw)

    final override fun hashCode(): Int = 31 * javaClass.hashCode() + raw.hashCode()
}

/** Social and web links. TheSportsDB often gives these without a scheme (`www.facebook.com/Arsenal`). */
public class Socials internal constructor(
    public val website: String?,
    public val facebook: String?,
    public val twitter: String?,
    public val instagram: String?,
    public val youtube: String?,
    public val rss: String?,
) {
    private val all get() = listOf(website, facebook, twitter, instagram, youtube, rss)
    override fun equals(other: Any?): Boolean = other is Socials && other.all == all
    override fun hashCode(): Int = all.hashCode()
    override fun toString(): String =
        "Socials(website=$website, facebook=$facebook, twitter=$twitter, instagram=$instagram, youtube=$youtube, rss=$rss)"
}

/** A league a team plays in: one of `idLeague`/`strLeague` .. `idLeague7`/`strLeague7`. */
public class LeagueRef internal constructor(public val id: Long, public val name: String?) {
    override fun equals(other: Any?): Boolean = other is LeagueRef && other.id == id && other.name == name
    override fun hashCode(): Int = 31 * id.hashCode() + name.hashCode()
    override fun toString(): String = "LeagueRef(id=$id, name=$name)"
}

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

/**
 * A broad reading of a `strStatus` code, so you don't need every sport's codes. Covers the codes
 * in TheSportsDB's data documentation (docs_api_data) for every sport.
 */
public enum class EventStatus {
    NOT_STARTED,
    IN_PLAY,
    FINISHED,
    POSTPONED,

    /** Suspended or interrupted (`SUSP`, `INT`, `INTR`): stopped, and may resume. */
    INTERRUPTED,
    CANCELLED,
    ABANDONED,

    /** Null, or a code this library doesn't know. The raw code is still on the record. */
    UNKNOWN,
    ;

    public companion object {
        private val notStarted = setOf("NS", "TBD", "NOT STARTED", "SCHEDULED")
        private val inPlay = setOf(
            "1H", "2H", "HT", "ET", "BT", "P", "PT", "LIVE", "BREAK", "Q1", "Q2", "Q3", "Q4", "OT",
            "P1", "P2", "P3", "SO", "IN PROGRESS",
        )
        private val finished = setOf(
            "FT", "AET", "PEN", "AOT", "AP", "AWD", "AW", "WO", "FINISHED", "MATCH FINISHED", "FINAL", "ENDED",
        )
        private val postponed = setOf("PST", "POST", "POSTPONED", "DELAYED")
        private val interrupted = setOf("SUSP", "INT", "INTR", "SUSPENDED", "INTERRUPTED")
        private val cancelled = setOf("CANC", "CANCELLED", "CANCELED")
        private val abandoned = setOf("ABD", "ABANDONED")
        private val numbered = Regex("""(IN\d+|S\d)""") // baseball innings, volleyball sets

        /** Classifies a raw `strStatus` code (`NS`, `2H`, `Q3`, `P2`, `IN4`, `S2`, `FT`, `PST`...). */
        public fun of(code: String?): EventStatus {
            val c = code?.trim()?.uppercase() ?: return UNKNOWN
            return when {
                c in notStarted -> NOT_STARTED
                c in finished -> FINISHED
                c in postponed -> POSTPONED
                c in interrupted -> INTERRUPTED
                c in cancelled -> CANCELLED
                c in abandoned -> ABANDONED
                c in inPlay || numbered.matches(c) -> IN_PLAY
                else -> UNKNOWN
            }
        }
    }
}

/**
 * The stage a special `intRound` value stands for. TheSportsDB uses these codes in place of a
 * round number (docs_api_data); any other value is an ordinary round number.
 */
public enum class RoundStage(public val code: Int) {
    QUARTER_FINAL(125),
    SEMI_FINAL(150),
    PLAYOFF(160),
    PLAYOFF_SEMI_FINAL(170),
    PLAYOFF_FINAL(180),
    FINAL(200),
    QUALIFIER(400),
    PRE_SEASON(500),
    ;

    public companion object {
        /** The stage for an `intRound` value, or null for an ordinary round number. */
        public fun of(round: Int?): RoundStage? = entries.firstOrNull { it.code == round }
    }
}
