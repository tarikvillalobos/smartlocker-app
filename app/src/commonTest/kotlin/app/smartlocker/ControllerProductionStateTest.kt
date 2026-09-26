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
