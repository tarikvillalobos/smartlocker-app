package app.smartlocker

import app.smartlocker.auth.domain.LoginChannel
import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import app.smartlocker.shared.presentation.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerMembershipRevocationTest {
    @Test fun preferencesClearRevokedMembershipBeforeLoadingTheRemainingMembership() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        var confirmedProfile: Profile? = null
        val started = CompletableDeferred<Unit>()
        val allowReload = CompletableDeferred<Unit>()
        val repository = object : LockerRepository by demo {
            override suspend fun profile(): Profile = confirmedProfile ?: demo.profile()
            override suspend fun updatePreferences(value: CommunicationPreferences): Profile =
                demo.updatePreferences(value).let { it.copy(memberships = it.memberships.filter { m -> m.id == "office" }) }
                    .also { confirmedProfile = it }
            override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
                if (confirmedProfile != null) {
                    assertEquals("office", locationId)
                    started.complete(Unit)
                    allowReload.await()
                }
                return demo.parcels(locationId, filter, cursor)
            }
        }
        val controller = start(repository, clock)
        assertNotNull(controller.state.value.credential)
        controller.preferences(CommunicationPreferences(sms = false))
        runCurrent()
        assertTrue(started.isCompleted)
        val waiting = controller.state.value
        assertTrue(waiting.busy)
        assertEquals("office", waiting.membershipId)
        assertCleared(waiting)
        assertEquals("Preferências salvas.", waiting.feedback)
        allowReload.complete(Unit)
        val refreshed = controller.state.first { !it.busy }
        assertFalse(refreshed.stale)
        assertTrue(refreshed.parcels.isNotEmpty())
        assertTrue(refreshed.parcels.all { it.locationId == "office" })
        assertEquals("office", refreshed.selected?.locationId)
    }

    @Test fun aFailedReloadCannotRestoreDataFromTheRevokedMembership() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        var revoked = false
        val repository = object : LockerRepository by demo {
            override suspend fun updatePreferences(value: CommunicationPreferences): Profile {
                val profile = demo.updatePreferences(value)
                revoked = true
                return profile.copy(memberships = profile.memberships.filter { it.id == "office" })
            }
            override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
                if (revoked) throw AppFailure(FailureKind.NETWORK, "Sem rede")
                return demo.parcels(locationId, filter, cursor)
            }
        }
        val controller = start(repository, clock)
        controller.preferences(CommunicationPreferences(sms = false))
        val current = controller.state.first { !it.busy && it.feedback != null }
        assertEquals("office", current.membershipId)
        assertCleared(current)
        assertTrue(current.stale)
        assertTrue(current.error!!.contains("operação foi concluída"))
        assertEquals("Preferências salvas.", current.feedback)
    }

    @Test fun refreshClearsRevokedMembershipBeforeAReplacementReadFails() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        var revoked = false
        val started = CompletableDeferred<Unit>()
        val allowFailure = CompletableDeferred<Unit>()
        val repository = object : LockerRepository by demo {
            override suspend fun profile(): Profile = demo.profile().let { profile ->
                if (revoked) profile.copy(memberships = profile.memberships.filter { it.id == "office" }) else profile
            }
            override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
                if (revoked) {
                    assertEquals("office", locationId)
                    started.complete(Unit)
                    allowFailure.await()
                    throw AppFailure(FailureKind.NETWORK, "Sem rede")
                }
                return demo.parcels(locationId, filter, cursor)
            }
        }
        val controller = start(repository, clock)
        assertNotNull(controller.state.value.credential)
        revoked = true
        controller.refresh()
        runCurrent()
        assertTrue(started.isCompleted)
        val waiting = controller.state.value
        assertTrue(waiting.busy)
        assertEquals("office", waiting.membershipId)
        assertEquals(listOf("office"), waiting.profile!!.memberships.map { it.id })
        assertCleared(waiting)
        allowFailure.complete(Unit)
        val failed = controller.state.first { !it.busy }
        assertCleared(failed)
        assertEquals("office", failed.membershipId)
        assertEquals(listOf("office"), failed.profile!!.memberships.map { it.id })
        assertTrue(failed.stale)
