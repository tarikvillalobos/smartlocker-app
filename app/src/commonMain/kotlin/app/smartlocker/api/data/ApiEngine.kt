package app.smartlocker.api.data

import io.ktor.client.engine.HttpClientEngine

expect fun createApiEngine(): HttpClientEngine

