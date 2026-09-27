package app.smartlocker.api.data

import app.smartlocker.auth.domain.*
import app.smartlocker.config.Brand
import app.smartlocker.config.Features
import app.smartlocker.parcels.domain.*
import app.smartlocker.profile.domain.*
import app.smartlocker.shared.domain.*
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Uses only endpoints described by the agreed app API contract. */
class ApiLockerRepository(private val session: ApiSessionClient) : LockerRepository {
    private val cacheMutex = Mutex()
    private var cacheUser: String? = null
    private val memberships = mutableMapOf<String, ApiMembership>()
    private val reviewedParcels = mutableMapOf<Pair<String, String>, Parcel>()

    suspend fun configuration(): ApiBrandConfiguration = session.configuration()
    override suspend fun brandConfiguration(): Brand = configuration().toDomain(session.brand)

    override suspend fun requestLogin(request: LoginRequest): Challenge = session.requestLogin(request)
    override suspend fun loginWithPassword(identifier: String, password: String): Session {
        val result = session.loginWithPassword(identifier, password)
        clearCache()
        return result
    }
    override suspend fun resendLogin(challengeId: String, request: LoginRequest): Challenge =
        session.resendLogin(identifier(challengeId))
    override suspend fun verifyLogin(challengeId: String, code: String): Session {
        val result = session.verifyLogin(identifier(challengeId), otp(code))
        clearCache()
        return result
    }
    override suspend fun restoreSession(): Session? = session.restoreSession()
    override suspend fun logout() {
        clearCache()
        session.logout()
    }
    override fun close() = session.close()

    override suspend fun profile(): Profile {
        val user = currentUser()
        val value = request<ApiProfile>(user, "/me")
        requireResponse(value.id == user)
        val list = request<ApiMembershipList>(user, "/me/memberships")
        return profile(value, list.items, user)
    }

    override suspend fun parcels(locationId: String, filter: ParcelFilter, cursor: String?): ParcelPage {
        val user = currentUser()
        val membership = membership(locationId, user)
        val status = when (filter) {
            ParcelFilter.ALL -> "all"
            ParcelFilter.WAITING -> "waiting"
            ParcelFilter.COLLECTED -> "collected"
        }
        val path = pagePath("${membershipPath(locationId)}/parcels", cursor) + "&status=$status"
        val response = request<ApiParcelPage>(user, path)
        requireResponse(response.items.all { it.membershipId == locationId })
        val page = response.toDomain()
        requireResponse(page.items.all { when (filter) {
            ParcelFilter.ALL -> true
            ParcelFilter.WAITING -> it.status == ParcelStatus.WAITING
            ParcelFilter.COLLECTED -> it.status != ParcelStatus.WAITING
        } })
        validateCursor(cursor, page.nextCursor)
        val global = configuration().capabilities
        val items = page.items.map { restrictActions(it, membership.capabilities, global) }
        cache(user) { items.forEach { reviewedParcels[locationId to it.id] = it } }
        return page.copy(items = items)
    }

    override suspend fun parcel(locationId: String, id: String): Parcel {
        val user = currentUser()
        val membership = membership(locationId, user)
        val response = request<ApiParcel>(user, parcelPath(locationId, id))
        return parcel(response, locationId, id, membership.capabilities, user)
    }

    override suspend fun statistics(locationId: String): Statistics {
        val user = currentUser()
        membership(locationId, user)
        // Omitting both dates requests the server's complete rolling 30-day window.
        return request<ApiParcelMetrics>(user, "${membershipPath(locationId)}/parcel-metrics").toDomain()
    }

    override suspend fun credential(locationId: String, parcelId: String): PickupCredential {
        val user = currentUser()
        membership(locationId, user)
        val response = request<ApiPickupCredential>(user, "${parcelPath(locationId, parcelId)}/pickup-credential")
        requireResponse(response.membershipId == locationId && response.parcelId == parcelId)
        return response.toDomain()
    }

    override suspend fun markCollected(locationId: String, parcelId: String): Parcel =
        manualPickup(locationId, parcelId, undo = false)

    override suspend fun undoManual(locationId: String, parcelId: String): Parcel =
        manualPickup(locationId, parcelId, undo = true)

    private suspend fun manualPickup(locationId: String, parcelId: String, undo: Boolean): Parcel {
        val user = currentUser()
        val membership = membership(locationId, user)
        val reviewed = restrictActions(reviewedParcel(locationId, parcelId, user),
            membership.capabilities, configuration().capabilities)
        val allowed = if (undo) reviewed.canUndo && reviewed.status == ParcelStatus.MANUAL
            else reviewed.canMarkManually && reviewed.status == ParcelStatus.WAITING
        if (!allowed) throw AppFailure(FailureKind.CONFLICT, "Esta ação não está disponível. Atualize a encomenda.")
        val version = reviewed.version
        requireResponse(version != null && Regex("[1-9][0-9]*").matches(version))
        val response = request<ApiParcel>(user, "${parcelPath(locationId, parcelId)}/manual-pickup",
            if (undo) HttpMethod.Delete else HttpMethod.Post,
            headers = mapOf("If-Match" to "\"$version\""))
        // A version conflict is surfaced unchanged; never fetch a newer version and retry a command.
        requireResponse(if (undo) response.status == "waiting" else response.status == "manual")
        return parcel(response, locationId, parcelId, membership.capabilities, user)
    }

    override suspend fun updatePreferences(value: CommunicationPreferences): Profile {
        val user = currentUser()
        val body = buildJsonObject {
            put("inApp", value.inApp)
            put("sms", value.sms)
            put("whatsapp", value.whatsapp)
        }.toString()
        val response = request<ApiProfile>(user, "/me/preferences", HttpMethod.Patch, body)
        return profile(response, response.memberships, user)
    }

    override suspend fun requestContactChange(contact: String, channel: LoginChannel): Challenge {
        val user = currentUser()
        if (!configuration().capabilities.features.contactEditing) unavailableFeature()
        val body = buildJsonObject {
            put("contact", normalizedApiContact(contact, channel))
            put("channel", channel.apiValue())
        }.toString()
        return request<ApiChallenge>(user, "/me/contact-challenges", HttpMethod.Post, body)
            .toDomain(expectedPurpose = "contact_change")
    }

    override suspend fun resendContactChange(challengeId: String, contact: String, channel: LoginChannel): Challenge {
        val user = currentUser()
        // Contact/channel come from the original challenge on the server, never from the edited form.
        return request<ApiChallenge>(user, "/me/contact-challenges/${identifier(challengeId)}/resend", HttpMethod.Post)
            .toDomain(expectedPurpose = "contact_change")
    }

    override suspend fun verifyContactChange(challengeId: String, code: String): Profile {
        val user = currentUser()
        val body = buildJsonObject { put("code", otp(code)) }.toString()
        val response = request<ApiProfile>(user, "/me/contact-challenges/${identifier(challengeId)}/verify", HttpMethod.Post, body)
        return profile(response, response.memberships, user)
    }

    override suspend fun notifications(locationId: String): List<DeliveryNotice> = noticePage(locationId).items

    override suspend fun noticePage(locationId: String, cursor: String?): NoticePage {
        val user = currentUser()
        membership(locationId, user)
        val response = request<ApiNoticePage>(user, pagePath("${membershipPath(locationId)}/notifications", cursor))
        requireResponse(response.items.all { it.membershipId == locationId })
        val page = response.toDomain()
        requireResponse(page.unreadCount >= page.items.count { !it.read })
        validateCursor(cursor, page.nextCursor)
        return page
    }

    override suspend fun markNoticeRead(locationId: String, id: String) {
        val user = currentUser()
        membership(locationId, user)
        val response = request<ApiDeliveryNotice>(user,
            "${membershipPath(locationId)}/notifications/${identifier(id)}/read", HttpMethod.Put)
        requireResponse(response.membershipId == locationId && response.id == id)
        requireResponse(response.toDomain().read)
    }

    override suspend fun reportIssue(locationId: String, parcelId: String, message: String): SupportIssue {
        val user = currentUser()
        val membership = membership(locationId, user)
        if (!membership.capabilities.features.supportIssues || !configuration().capabilities.features.supportIssues) unavailableFeature()
        val parcel = reviewedParcel(locationId, parcelId, user)
        if (!parcel.canReportIssue) unavailableFeature()
        val normalized = message.trim()
        if (normalized.length !in 10..2000)
            throw AppFailure(FailureKind.VALIDATION, "Descreva o problema em 10 a 2.000 caracteres.")
        val body = buildJsonObject { put("parcelId", identifier(parcelId)); put("message", normalized) }.toString()
        val response = request<ApiSupportIssue>(user, "${membershipPath(locationId)}/issues", HttpMethod.Post, body)
        requireResponse(response.membershipId == locationId && response.parcelId == parcelId)
        return response.toDomain()
    }

    override suspend fun issues(locationId: String): List<SupportIssue> = issuePage(locationId).items

    override suspend fun issuePage(locationId: String, cursor: String?): IssuePage {
        val user = currentUser()
        val membership = membership(locationId, user)
        if (!membership.capabilities.features.supportIssues || !configuration().capabilities.features.supportIssues) unavailableFeature()
        val response = request<ApiIssuePage>(user, pagePath("${membershipPath(locationId)}/issues", cursor))
        requireResponse(response.items.all { it.membershipId == locationId })
        val page = response.toDomain()
        validateCursor(cursor, page.nextCursor)
        return page
    }

    override suspend fun recipients(locationId: String): List<Recipient> {
        val user = currentUser()
        val membership = membership(locationId, user)
        if (!membership.capabilities.features.recipients || !configuration().capabilities.features.recipients) unavailableFeature()
        val response = request<ApiRecipientList>(user, "${membershipPath(locationId)}/recipients")
        val items = response.items.map { it.toDomain() }
        requireResponse(items.map { it.id }.distinct().size == items.size)
        return items
    }

    private suspend fun profile(value: ApiProfile, values: List<ApiMembership>, user: String): Profile {
        requireResponse(value.id == user)
        val global = configuration().capabilities
        val mapped = values.map { membership ->
            val result = membership.toDomain()
            result.copy(features = intersect(result.features, global.features.toDomain()),
                channels = membership.capabilities.channels.availableChannels().intersect(global.channels.availableChannels()))
        }
        requireResponse(mapped.map { it.id }.distinct().size == mapped.size)
        val result = value.toDomain(mapped)
        cache(user) {
            memberships.clear()
            values.forEach { memberships[it.id] = it }
            val valid = memberships.keys
            reviewedParcels.keys.filter { it.first !in valid }.forEach(reviewedParcels::remove)
        }
        return result
    }

    private suspend fun membership(id: String, user: String): ApiMembership {
        identifier(id)
        cache(user) { memberships[id] }?.let { return it }
        val response = request<ApiMembershipList>(user, "/me/memberships")
        val mapped = response.items.map { it.toDomain() }
        requireResponse(mapped.map { it.id }.distinct().size == mapped.size)
        cache(user) {
            memberships.clear()
            response.items.forEach { memberships[it.id] = it }
        }
        return response.items.singleOrNull { it.id == id }
            ?: throw AppFailure(FailureKind.DENIED, "Você não tem acesso a este local.")
    }

    private suspend fun parcel(value: ApiParcel, locationId: String, id: String, membership: ApiCapabilities, user: String): Parcel {
        requireResponse(value.membershipId == locationId && value.id == id)
        val result = restrictActions(value.toDomain(), membership, configuration().capabilities)
        cache(user) { reviewedParcels[locationId to id] = result }
        return result
    }

    private suspend fun reviewedParcel(locationId: String, id: String, user: String): Parcel =
        cache(user) { reviewedParcels[locationId to id] } ?: parcel(locationId, id).also { checkUser(user) }

    private fun restrictActions(value: Parcel, membership: ApiCapabilities, global: ApiCapabilities): Parcel = value.copy(
        canMarkManually = value.canMarkManually && membership.features.manualPickup && global.features.manualPickup,
        canUndo = value.canUndo && membership.features.undoManualPickup && global.features.undoManualPickup,
        canReportIssue = value.canReportIssue && membership.features.supportIssues && global.features.supportIssues,
    )

    private suspend inline fun <reified T> request(
        user: String, path: String, method: HttpMethod = HttpMethod.Get, body: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): T {
        checkUser(user)
        val bodyText = session.request(path, method, body, headers)
        checkUser(user)
        return decodeApi(bodyText)
    }

    private suspend fun <T> cache(user: String, block: () -> T): T = cacheMutex.withLock {
        checkUser(user)
        if (cacheUser != user) { memberships.clear(); reviewedParcels.clear(); cacheUser = user }
        block()
    }
    private suspend fun clearCache() = cacheMutex.withLock {
        memberships.clear(); reviewedParcels.clear(); cacheUser = null
    }
    private fun currentUser(): String = session.currentSession()?.userId
        ?: throw AppFailure(FailureKind.EXPIRED_SESSION, "Sua sessão expirou. Entre novamente.")
    private fun checkUser(user: String) {
        if (session.currentSession()?.userId != user)
            throw AppFailure(FailureKind.EXPIRED_SESSION, "Sua sessão mudou. Entre novamente.")
    }
    private fun membershipPath(id: String) = "/memberships/${identifier(id)}"
    private fun parcelPath(membershipId: String, id: String) = "${membershipPath(membershipId)}/parcels/${identifier(id)}"
    private fun identifier(value: String): String {
        if (!Regex("[A-Za-z0-9_-]{1,128}").matches(value))
            throw AppFailure(FailureKind.VALIDATION, "Identificador inválido.")
        return value
    }
    private fun pagePath(path: String, cursor: String?): String {
        if (cursor != null && (cursor.isEmpty() || cursor.length > 2048))
            throw AppFailure(FailureKind.VALIDATION, "A página solicitada não é válida. Atualize a lista.")
        return "$path?limit=20" + (cursor?.let { "&cursor=${it.encodeURLParameter()}" } ?: "")
    }
    private fun validateCursor(current: String?, next: String?) {
        requireResponse(next == null || (next.isNotEmpty() && next.length <= 2048 && next != current))
    }
    private fun otp(value: String): String = value.also(::validateApiOtp)
    private fun intersect(a: Features, b: Features) = Features(a.residents && b.residents, a.issues && b.issues,
        a.manualPickup && b.manualPickup, a.contactEditing && b.contactEditing)
    private fun requireResponse(valid: Boolean) { if (!valid) invalidResponse() }
    private fun invalidResponse(): Nothing = throw AppFailure(FailureKind.UNAVAILABLE, "O serviço retornou dados inválidos. Tente atualizar.")
    private fun unavailableFeature(): Nothing = throw AppFailure(FailureKind.DENIED, "Este recurso não está disponível para o local.")
}
