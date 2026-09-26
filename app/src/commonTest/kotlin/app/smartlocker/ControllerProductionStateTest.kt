package app.smartlocker

import app.smartlocker.config.*
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import app.smartlocker.shared.presentation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ControllerProductionStateTest {
    @Test fun globalUnreadCountSurvivesPartialPagesAndAppendingAnotherPage() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        val notice = demo.notifications("home").first()
        val cursors = mutableListOf<String?>()
        val repository = object : LockerRepository by demo {
            override suspend fun noticePage(locationId: String, cursor: String?): NoticePage {
                cursors += cursor
                return if (cursor == null) NoticePage(listOf(notice), "next-notices", 100)
                else NoticePage(listOf(notice.copy(id = "notice-next", read = true)), null, 100)
            }
        }
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.PRODUCTION), repository, clock, backgroundScope)
        val first = controller.state.first { it.profile != null && !it.busy }
        assertEquals(1, first.notices.size)
        assertEquals(100, first.unreadCount)
        controller.moreNotices()
        val second = controller.state.first { it.notices.size == 2 && !it.busy }
        assertEquals(100, second.unreadCount)
        assertNull(second.noticeCursor)
        assertEquals(listOf(null, "next-notices"), cursors)
    }

    @Test fun revokedMembershipCapabilitiesClearDataAndBlockOptionalRoutes() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        var revoked = false
        val repository = object : LockerRepository by demo {
            override suspend fun profile(): Profile = demo.profile().let { profile ->
                if (!revoked) profile else profile.copy(memberships = profile.memberships.map {
                    it.copy(features = Features(residents = false, issues = false, contactEditing = false))
                })
            }
        }
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.PRODUCTION), repository, clock, backgroundScope)
        controller.state.first { it.profile != null && !it.busy }
        controller.navigate(Route.RESIDENTS)
        controller.state.first { it.residents.isNotEmpty() && !it.busy }
        revoked = true
        controller.refresh()
        val current = controller.state.first { !it.busy && it.profile != null && !it.membership!!.features.residents }
        assertTrue(current.residents.isEmpty())
        assertNotEquals(Route.RESIDENTS, current.route)
        for (route in listOf(Route.RESIDENTS, Route.ISSUES, Route.CONTACT, Route.DEMO)) {
            controller.navigate(route)
            assertNotEquals(route, controller.state.value.route)
        }
    }

    @Test fun transientSessionRefreshFailureKeepsAccountButRevocationClearsPersonalData() = runTest {
        val clock = TestClock()
        val demo = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        demo.signIn()
        var failure: FailureKind? = null
        val repository = object : LockerRepository by demo {
            override suspend fun profile(): Profile {
                failure?.let { throw AppFailure(it, "Falha simulada na renovação") }
                return demo.profile()
            }
        }
        val controller = AppController(AppConfiguration(Brands.smartLocker, Environment.PRODUCTION), repository, clock, backgroundScope)
        val signedIn = controller.state.first { it.profile != null && !it.busy }
        failure = FailureKind.NETWORK
        controller.refresh()
        val unavailable = controller.state.first { it.error != null && !it.busy }
        assertEquals(signedIn.session?.userId, unavailable.session?.userId)
