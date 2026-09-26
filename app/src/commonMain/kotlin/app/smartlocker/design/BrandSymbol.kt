package app.smartlocker.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import app.smartlocker.config.Brand
import app.smartlocker.config.BrandMark

/** Central registry for vector marks and brand monograms. */
@Composable
fun BrandSymbol(brand: Brand) {
    when (brand.mark) {
        BrandMark.PARCEL -> AppIcon(Symbol.PARCEL)
        BrandMark.MONOGRAM -> Text(brand.monogram, style = MaterialTheme.typography.titleMedium)
    }
}
