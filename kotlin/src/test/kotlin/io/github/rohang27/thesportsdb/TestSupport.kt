package io.github.rohang27.thesportsdb

import okhttp3.HttpUrl
import io.github.rohang27.thesportsdb.http.HttpResponse
import io.github.rohang27.thesportsdb.http.HttpTransport

fun fixture(path: String): String =
    requireNotNull(object {}.javaClass.getResource("/fixtures/$path")) { "missing fixture $path" }.readText()

/** Records every request and answers from a queue (or with [default]). */
class FakeTransport(var default: () -> HttpResponse = { HttpResponse(200, "{}") }) : HttpTransport {
    val requests = mutableListOf<Pair<HttpUrl, Map<String, String>>>()
    val queue = ArrayDeque<() -> HttpResponse>()

    val lastUrl: HttpUrl get() = requests.last().first

    fun respond(body: String, status: Int = 200, headers: Map<String, String> = emptyMap()) = apply {
        queue.addLast { HttpResponse(status, body, headers) }
    }

    fun fail(e: java.io.IOException) = apply { queue.addLast { throw e } }

    override suspend fun get(url: HttpUrl, headers: Map<String, String>): HttpResponse {
        requests += url to headers
        return (queue.removeFirstOrNull() ?: default)()
    }
}

fun client(transport: FakeTransport, key: String = SportsDbConfig.FREE_API_KEY, configure: SportsDbConfig.() -> Unit = {}) =
    SportsDbClient {
        apiKey = key
        this.transport = transport
        requestsPerMinute = 0
        retryBackoff = kotlin.time.Duration.ZERO
        configure()
    }

/** The part of a URL after the key: `lookupteam.php?id=133604`. */
val HttpUrl.v1Call: String get() = pathSegments.last() + (encodedQuery?.let { "?$it" } ?: "")
