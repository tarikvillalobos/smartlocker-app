package app.smartlocker.api.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun createApiEngine(): HttpClientEngine = CIO.create()
