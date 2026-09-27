package app.smartlocker.api.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiUnitResident(val id: String, val name: String, val isSelf: Boolean)

@Serializable
data class ApiUnitDetail(val id: String, val condominiumId: String, val residents: List<ApiUnitResident>)

@Serializable
data class ApiDelegateInput(val membershipId: String)
