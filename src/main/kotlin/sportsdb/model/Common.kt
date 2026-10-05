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
