package app.smartlocker

import app.smartlocker.config.Brands
import app.smartlocker.demo.data.DemoRepository
import app.smartlocker.parcels.domain.*
import app.smartlocker.shared.domain.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UseCaseTest {
    @Test fun refusesCredentialsForAnotherParcelOrMembership() = runTest {
        val clock = TestClock()
        val repository = DemoRepository(MemoryStorage(), MemorySecure(), Brands.smartLocker, clock, 0)
        repository.signIn()
        val parcel = repository.parcel("home", "demo-0")
        val wrongResponse = object : LockerRepository by repository {
            override suspend fun credential(locationId: String, parcelId: String): PickupCredential =
                repository.credential(locationId, "demo-1")
        }
        assertFailsWith<AppFailure> { LoadPickupCredential(wrongResponse, clock)("home", parcel) }
