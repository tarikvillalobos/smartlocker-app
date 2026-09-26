package app.smartlocker.shared.presentation

import app.smartlocker.auth.domain.*
import app.smartlocker.config.*
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*

enum class Route { HOME, HISTORY, PROFILE, DETAIL, NOTICES, ISSUES, RESIDENTS, CONTACT, LOCATIONS, SUPPORT, LEGAL, DEMO }

data class AppState(
    val initialized: Boolean = false,
    val remoteBrand: Brand? = null,
    val session: Session? = null,
    val profile: Profile? = null,
    val membershipId: String = "home",
    val route: Route = Route.HOME,
    val filter: ParcelFilter = ParcelFilter.ALL,
    val parcels: List<Parcel> = emptyList(),
    val pending: List<Parcel> = emptyList(),
    val recent: List<Parcel> = emptyList(),
    val nextCursor: String? = null,
    val selectedId: String? = null,
    val selected: Parcel? = null,
    val credential: PickupCredential? = null,
    val credentialMessage: String? = null,
    val statistics: Statistics? = null,
    val notices: List<DeliveryNotice> = emptyList(),
    val noticeCursor: String? = null,
    val serverUnreadCount: Int? = null,
    val issueCursor: String? = null,
    val issues: List<SupportIssue> = emptyList(),
    val residents: List<Recipient> = emptyList(),
    val challenge: Challenge? = null,
    val contactChallenge: Challenge? = null,
    val contactValue: String = "",
    val contactChannel: LoginChannel = LoginChannel.EMAIL,
    val issueSubmission: Long = 0,
    val busy: Boolean = false,
    val stale: Boolean = false,
    val error: String? = null,
    val feedback: String? = null,
    val now: Long = 0,
    val lastUpdated: Long? = null,
) {
    val membership: Membership? get() = profile?.memberships?.find { it.id == membershipId }
    val unreadCount: Int get() = serverUnreadCount ?: notices.count { !it.read }
    fun features(brand: Brand): Features {
        val allowed = membership?.features ?: if (profile == null) brand.features else Features(false, false, false, false)
        return Features(brand.features.residents && allowed.residents, brand.features.issues && allowed.issues,
            brand.features.manualPickup && allowed.manualPickup, brand.features.contactEditing && allowed.contactEditing)
    }
    fun channels(brand: Brand): Set<String> = when {
        profile != null && membership == null -> emptySet()
        else -> membership?.channels?.intersect(brand.channels) ?: brand.channels
    }
}
