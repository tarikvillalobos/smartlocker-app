package app.smartlocker.config

data class Features(
    val residents: Boolean = true,
    val issues: Boolean = true,
    val manualPickup: Boolean = true,
    val contactEditing: Boolean = true,
)
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
