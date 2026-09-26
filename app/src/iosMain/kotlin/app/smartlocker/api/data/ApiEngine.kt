package app.smartlocker.api.data

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun createApiEngine(): HttpClientEngine = Darwin.create()
