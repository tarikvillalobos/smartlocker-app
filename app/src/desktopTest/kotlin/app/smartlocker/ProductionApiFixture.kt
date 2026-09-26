package app.smartlocker

import app.smartlocker.api.data.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import kotlinx.serialization.encodeToString
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Clock
import kotlin.time.Instant

/** Stateful HTTP fixture, independent from the demo repository and private contract-test fixtures. */
internal class ProductionApiFixture {
    val requests = CopyOnWriteArrayList<HttpRequestData>()
    @Volatile var unavailable = false
    @Volatile private var manual = false
    private val now = Clock.System.now().toEpochMilliseconds()
    private fun at(offset: Long) = Instant.fromEpochMilliseconds(now + offset).toString()
    private val yes = ApiChannelCapability(true)
