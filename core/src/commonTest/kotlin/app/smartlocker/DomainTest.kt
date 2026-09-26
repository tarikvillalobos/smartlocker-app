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
