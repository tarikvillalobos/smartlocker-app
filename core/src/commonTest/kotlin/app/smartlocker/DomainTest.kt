package app.smartlocker

import app.smartlocker.auth.domain.InputValidation
import app.smartlocker.parcels.domain.*
import kotlin.test.*

class DomainTest {
    @Test fun validatesCpfCheckDigitsAndRejectsRepeatedNumbers() {
        assertTrue(InputValidation.cpf("529.982.247-25"))
        assertFalse(InputValidation.cpf("529.982.247-26"))
        assertFalse(InputValidation.cpf("11111111111"))
        assertFalse(InputValidation.cpf(""))
    }
    @Test fun validatesBrazilianPhonesAndEmail() {
        assertTrue(InputValidation.phone("(11) 98765-4321"))
        assertTrue(InputValidation.phone("+55 11 98765-4321"))
        assertTrue(InputValidation.phone("(55) 98765-4321"))
        assertFalse(InputValidation.phone("1112345678"))
        assertTrue(InputValidation.email("ana@example.test"))
        assertFalse(InputValidation.email("ana@"))
    }
    private fun parcel(id: String, arrival: Long, manual: Long? = null, physical: Long? = null) =
        Parcel(id, "ana", "home", "Correios", null, "Portaria", "Rua", "1", "M", arrival,
            null, 10_000, manualAt = manual, collectedAt = physical)

    @Test fun separatesManualPickupAndValidPhysicalMetrics() {
        val items = listOf(parcel("a", 100, physical = 1100), parcel("b", 200, physical = 3200),
            parcel("manual", 100, manual = 500), parcel("invalid", 500, physical = 100),
            parcel("outside", -100, physical = 400), parcel("pending", 800))
        val stats = calculateStatistics(items, 0, 5000)
        assertEquals(5, stats.total)
        assertEquals(2000, stats.averageMillis)
        assertEquals(1, items.filtered(ParcelFilter.WAITING).size)
        assertEquals(ParcelStatus.MANUAL, items[2].status)
        assertEquals(5, items.filtered(ParcelFilter.COLLECTED).size)
        assertNull(calculateStatistics(listOf(items[2]), 0, 5000).averageMillis)
    }
    @Test fun hidesStaleExpiredAndConsumedCredentials() {
        val code = PickupCredential("p", "123456", "DEMO", 100_000, 1000, CredentialStatus.ACTIVE)
        assertTrue(code.canDisplay(1001, true))
