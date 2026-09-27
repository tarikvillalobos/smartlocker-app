package app.smartlocker.config

data class Features(
    val residents: Boolean = true,
    val issues: Boolean = true,
    val manualPickup: Boolean = true,
    val contactEditing: Boolean = true,
)
enum class BrandMark { PARCEL, MONOGRAM }

data class Brand(
    val id: String,
    val name: String,
    val monogram: String,
    val primary: Long,
    val dark: Long,
    val supportEmail: String?,
    val termsUrl: String?,
    val privacyUrl: String?,
    val channels: Set<String>,
    val features: Features = Features(),
    val headline: String = "Suas encomendas, sempre à mão.",
    val introduction: String = "Receba o código de retirada assim que o pacote chegar ao armário.",
    val bodyFont: String = "jakarta",
    val headingFont: String = "sora",
    val applicationId: String = "app.smartlocker.demo",
    val mark: BrandMark = BrandMark.PARCEL,
    val authMethods: Set<String> = setOf("otp"),
)

object Brands {
    val smartLocker = Brand(
        "smartlocker", "SmartLocker", "SL", 0xFF007A5E, 0xFF08393B,
        null, null, null, setOf("app", "sms", "whatsapp"),
    )
    val aurora = Brand(
        "aurora", "Aurora Lockers", "AL", 0xFF6652B8, 0xFF292344,
        null, null, null, setOf("app", "sms"),
        features = Features(residents = false),
        headline = "Sua entrega. Seu tempo.",
        applicationId = "app.aurora.lockers.demo",
        mark = BrandMark.MONOGRAM,
    )
    val all = listOf(smartLocker, aurora)
}

enum class Environment { DEMO, PRODUCTION }
data class AppConfiguration(
    val brand: Brand,
    val environment: Environment,
    val apiBaseUrl: String? = null,
)
