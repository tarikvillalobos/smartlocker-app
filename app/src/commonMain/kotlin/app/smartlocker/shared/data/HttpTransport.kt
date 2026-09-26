package app.smartlocker.shared.data

import app.smartlocker.shared.domain.*
import app.smartlocker.api.data.apiFailure
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/** Transport only. Paths, auth headers and DTOs must come from the external contract. */
class HttpTransport(engine: HttpClientEngine, private val baseUrl: String) : AutoCloseable {
    init {
        require(Url(baseUrl).protocol == URLProtocol.HTTPS) { "Production API must use HTTPS" }
        require(Url(baseUrl).user == null && Url(baseUrl).password == null)
        require(Url(baseUrl).host.isNotBlank() && Url(baseUrl).parameters.isEmpty() && Url(baseUrl).fragment.isEmpty())
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
        require(!documentedPath.contains("://") && !documentedPath.contains('#'))
        require(documentedPath.substringBefore('?').split('/').none { it.decodeURLPart() in setOf(".", "..") })
        try {
            val response = client.request(baseUrl.trimEnd('/') + documentedPath) {
                this.method = method
                accept(ContentType.Application.Json)
                documentedHeaders.forEach { (key, value) -> header(key, value) }
                if (jsonBody != null) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }
            }
            val body = response.bodyAsText()
            if (body.length > 2_000_000) throw AppFailure(FailureKind.UNAVAILABLE, "Resposta da API excedeu o limite permitido.")
            if (response.status.value in 200..299) return body
            throw apiFailure(response.status.value, body)
        } catch (error: CancellationException) {
            throw error
        } catch (error: AppFailure) {
            throw error
        } catch (_: Exception) {
            throw AppFailure(FailureKind.NETWORK, "Não foi possível conectar. Confira sua conexão.")
        }
    }

    override fun close() = client.close()
}
