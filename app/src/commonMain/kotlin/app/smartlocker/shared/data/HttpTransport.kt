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
    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        followRedirects = false
        expectSuccess = false
    }

    suspend fun execute(
        documentedPath: String,
        method: HttpMethod,
        documentedHeaders: Map<String, String> = emptyMap(),
        jsonBody: String? = null,
    ): String {
        require(documentedPath.startsWith("/") && !documentedPath.startsWith("//"))
        require(!documentedPath.contains("://") && !documentedPath.contains(".."))
        try {
