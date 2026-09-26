package app.smartlocker.api.data

import io.ktor.client.engine.HttpClientEngine

expect fun createApiEngine(): HttpClientEngine

fun configuredApiEndpoint(value: String?): String? = runCatching {
    if (value.isNullOrBlank()) return@runCatching null
    val url = io.ktor.http.Url(value)
    require(url.protocol == io.ktor.http.URLProtocol.HTTPS && url.host.isNotBlank())
    require(url.user == null && url.password == null && url.parameters.isEmpty() && url.fragment.isEmpty())
    require(value.none(Char::isWhitespace))
    url.toString().trimEnd('/')
}.getOrNull()
