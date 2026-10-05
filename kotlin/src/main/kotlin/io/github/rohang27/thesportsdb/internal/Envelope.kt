package io.github.rohang27.thesportsdb.internal

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import io.github.rohang27.thesportsdb.ApiMessageException
import io.github.rohang27.thesportsdb.InvalidApiKeyException
import io.github.rohang27.thesportsdb.ResponseParseException

internal val json = Json { isLenient = true }

private const val NO_DATA = "No data found"
private const val INVALID_KEY = "Invalid Premium API key"

/**
 * Pulls the records out of a TheSportsDB response.
 *
 * Every response is one JSON object with one key whose value is an array of records. The
 * key name differs by endpoint and version, so [expectedKey] is tried first and any other
 * array-valued key is accepted as a fallback. All of these mean "no results":
 * an empty body, `{"key": null}`, `{"Message": "No data found"}`.
 */
internal fun parseRecords(body: String, expectedKey: String, displayUrl: String): List<JsonObject> {
    if (body.isBlank()) return emptyList()
    val root = try {
        json.parseToJsonElement(body)
    } catch (e: SerializationException) {
        throw ResponseParseException("Response from $displayUrl is not JSON: ${body.take(120)}", e)
    }
    if (root !is JsonObject) throw ResponseParseException("Response from $displayUrl is not a JSON object")

    (root["Message"] as? JsonPrimitive)?.contentOrNull?.let { message ->
        when {
            message.startsWith(NO_DATA, ignoreCase = true) -> return emptyList()
            message.startsWith(INVALID_KEY, ignoreCase = true) ->
                throw InvalidApiKeyException("TheSportsDB rejected the API key ($displayUrl): $message")
            root.size == 1 -> throw ApiMessageException("TheSportsDB said: $message ($displayUrl)", message)
        }
    }

    val value = root[expectedKey] ?: root.values.firstOrNull { it is JsonArray } ?: JsonNull
    return when (value) {
        is JsonNull -> emptyList()
        is JsonArray -> value.mapNotNull { (it as? JsonObject)?.let(::normalizeRecord) }
        is JsonObject -> listOf(normalizeRecord(value))
        // A rejected parameter comes back as text in place of the records:
        // {"seasons":"Invalid League ID passed"}, {"events":"Invalid League ID or no round passed"}.
        is JsonPrimitive -> throw ApiMessageException("TheSportsDB said: ${value.content} ($displayUrl)", value.content)
    }
}

/** Turns blank strings into nulls, so models only ever see "a value" or "no value". */
internal fun normalizeRecord(obj: JsonObject): JsonObject = JsonObject(
    obj.mapValues { (_, v) -> if (v is JsonPrimitive && v.isString && v.content.isBlank()) JsonNull else v },
)
