package app.smartlocker.shared.data

import app.smartlocker.shared.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** Transport only. Paths, auth headers and DTOs must come from the external contract. */
class HttpTransport(engine: HttpClientEngine, private val baseUrl: String) : AutoCloseable {
    init {
        require(Url(baseUrl).protocol == URLProtocol.HTTPS) { "Production API must use HTTPS" }
        require(Url(baseUrl).user == null && Url(baseUrl).password == null)
    }
