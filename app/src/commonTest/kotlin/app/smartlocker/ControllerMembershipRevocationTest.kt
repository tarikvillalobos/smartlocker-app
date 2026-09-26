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
