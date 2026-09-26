package app.smartlocker.shared.presentation

import app.smartlocker.auth.domain.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*

enum class Route { HOME, HISTORY, PROFILE, DETAIL, NOTICES, ISSUES, RESIDENTS, CONTACT, LOCATIONS, SUPPORT, LEGAL, DEMO }

data class AppState(
    val initialized: Boolean = false,
    val session: Session? = null,
    val profile: Profile? = null,
    val membershipId: String = "home",
    val route: Route = Route.HOME,
    val filter: ParcelFilter = ParcelFilter.ALL,
    val parcels: List<Parcel> = emptyList(),
    val pending: List<Parcel> = emptyList(),
    val nextCursor: String? = null,
    val selectedId: String? = null,
    val selected: Parcel? = null,
