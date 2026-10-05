package sportsdb.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A plain HTTP response: what the library needs from a transport. */
public class HttpResponse(
    public val status: Int,
    public val body: String,
    public val headers: Map<String, String> = emptyMap(),
) {
    /** Case-insensitive header lookup. */
    public fun header(name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
}

/**
 * Performs GET requests. The default is [OkHttpTransport]; supply your own to plug in
 * another HTTP stack or to fake the network in tests.
 *
 * Implementations throw [IOException] for network failures and return any HTTP status
 * (including 4xx and 5xx) as an [HttpResponse].
 */
public fun interface HttpTransport {
    public suspend fun get(url: HttpUrl, headers: Map<String, String>): HttpResponse
}

/** [HttpTransport] backed by OkHttp. Share one [OkHttpClient] across your app when you can. */
public class OkHttpTransport(private val client: OkHttpClient = OkHttpClient()) : HttpTransport {
    override suspend fun get(url: HttpUrl, headers: Map<String, String>): HttpResponse {
        val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        val call = client.newCall(request)
        return suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = try {
                        response.use { r ->
                            HttpResponse(r.code, r.body.string(), r.headers.toMap())
                        }
                    } catch (e: IOException) {
                        cont.resumeWithException(e)
                        return
                    }
                    cont.resume(result)
                }
            })
        }
    }
}
