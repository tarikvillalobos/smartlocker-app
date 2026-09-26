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
