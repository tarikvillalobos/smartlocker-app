package app.smartlocker.api.data

import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.config.Brands
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.CommunicationPreferences
import app.smartlocker.shared.data.HttpTransport
import app.smartlocker.shared.domain.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*
import kotlin.time.Instant

/** Contract tests use the actual session client, headers, transport, DTOs and repository. */
class ApiLockerRepositoryTest {
    @Test fun profileIntersectsCapabilitiesAndRejectsAnotherUser() = runTest {
        val public = configuration.copy(capabilities = capabilities.copy(features = capabilities.features.copy(recipients = false)))
        withRepository(public) { request ->
            assertEquals("/v1/me", request.url.encodedPath)
            json(profile)
        }.useSuspend { fixture ->
            val value = fixture.repo.profile()
            assertEquals("member-1", value.memberships.single().id)
            assertEquals("America/Sao_Paulo", value.memberships.single().timeZone)
            assertFalse(value.memberships.single().features.residents)
            assertEquals(setOf("app", "sms", "email"), value.memberships.single().channels)
            assertEquals(1, fixture.requests.count { it.url.encodedPath == "/v1/me/memberships" })
            fixture.repo.brandConfiguration()
            assertEquals(1, fixture.requests.count { it.url.encodedPath == "/v1/configuration" })
        }
        withRepository { json(profile.copy(id = "another-user")) }.useSuspend {
            assertEquals(FailureKind.UNAVAILABLE, assertFailsWith<AppFailure> { it.repo.profile() }.kind)
            assertFalse(it.requests.any { request -> request.url.encodedPath == "/v1/me/memberships" })
