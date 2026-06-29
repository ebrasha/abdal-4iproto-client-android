/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : CountryFlagIcon.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:25:30
 * Description : Resolves ISO country codes to flagpack-compose ImageVector assets with a safe fallback.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import flagpack.icons.FlagIcons
import flagpack.icons.medium.*

private val isoToFlag: Map<String, ImageVector> = mapOf(
    "AF" to FlagIcons.Medium.Afghanistan,
    "AL" to FlagIcons.Medium.Albania,
    "DZ" to FlagIcons.Medium.Algeria,
    "AR" to FlagIcons.Medium.Argentina,
    "AM" to FlagIcons.Medium.Armenia,
    "AU" to FlagIcons.Medium.Australia,
    "AT" to FlagIcons.Medium.Austria,
    "AZ" to FlagIcons.Medium.Azerbaijan,
    "BH" to FlagIcons.Medium.Bahrain,
    "BD" to FlagIcons.Medium.Bangladesh,
    "BY" to FlagIcons.Medium.Belarus,
    "BE" to FlagIcons.Medium.Belgium,
    "BR" to FlagIcons.Medium.Brazil,
    "BG" to FlagIcons.Medium.Bulgaria,
    "CA" to FlagIcons.Medium.Canada,
    "CL" to FlagIcons.Medium.Chile,
    "CN" to FlagIcons.Medium.China,
    "CO" to FlagIcons.Medium.Colombia,
    "HR" to FlagIcons.Medium.Croatia,
    "CY" to FlagIcons.Medium.Cyprus,
    "CZ" to FlagIcons.Medium.CzechRepublic,
    "DK" to FlagIcons.Medium.Denmark,
    "EG" to FlagIcons.Medium.Egypt,
    "EE" to FlagIcons.Medium.Estonia,
    "FI" to FlagIcons.Medium.Finland,
    "FR" to FlagIcons.Medium.France,
    "GE" to FlagIcons.Medium.Georgia,
    "DE" to FlagIcons.Medium.Germany,
    "GR" to FlagIcons.Medium.Greece,
    "HK" to FlagIcons.Medium.HongKong,
    "HU" to FlagIcons.Medium.Hungary,
    "IS" to FlagIcons.Medium.Iceland,
    "IN" to FlagIcons.Medium.India,
    "ID" to FlagIcons.Medium.Indonesia,
    "IR" to FlagIcons.Medium.Iran,
    "IQ" to FlagIcons.Medium.Iraq,
    "IE" to FlagIcons.Medium.Ireland,
    "IL" to FlagIcons.Medium.Israel,
    "IT" to FlagIcons.Medium.Italy,
    "JP" to FlagIcons.Medium.Japan,
    "JO" to FlagIcons.Medium.Jordan,
    "KZ" to FlagIcons.Medium.Kazakhstan,
    "KE" to FlagIcons.Medium.Kenya,
    "KW" to FlagIcons.Medium.Kuwait,
    "LV" to FlagIcons.Medium.Latvia,
    "LB" to FlagIcons.Medium.Lebanon,
    "LT" to FlagIcons.Medium.Lithuania,
    "LU" to FlagIcons.Medium.Luxembourg,
    "MY" to FlagIcons.Medium.Malaysia,
    "MX" to FlagIcons.Medium.Mexico,
    "MA" to FlagIcons.Medium.Morocco,
    "NL" to FlagIcons.Medium.Netherlands,
    "NZ" to FlagIcons.Medium.NewZealand,
    "NG" to FlagIcons.Medium.Nigeria,
    "NO" to FlagIcons.Medium.Norway,
    "OM" to FlagIcons.Medium.Oman,
    "PK" to FlagIcons.Medium.Pakistan,
    "PS" to FlagIcons.Medium.Palestine,
    "PH" to FlagIcons.Medium.Philippines,
    "PL" to FlagIcons.Medium.Poland,
    "PT" to FlagIcons.Medium.Portugal,
    "QA" to FlagIcons.Medium.Qatar,
    "RO" to FlagIcons.Medium.Romania,
    "RU" to FlagIcons.Medium.Russia,
    "SA" to FlagIcons.Medium.SaudiArabia,
    "RS" to FlagIcons.Medium.Serbia,
    "SG" to FlagIcons.Medium.Singapore,
    "SK" to FlagIcons.Medium.Slovakia,
    "SI" to FlagIcons.Medium.Slovenia,
    "ZA" to FlagIcons.Medium.SouthAfrica,
    "KR" to FlagIcons.Medium.SouthKorea,
    "ES" to FlagIcons.Medium.Spain,
    "SE" to FlagIcons.Medium.Sweden,
    "CH" to FlagIcons.Medium.Switzerland,
    "SY" to FlagIcons.Medium.Syria,
    "TW" to FlagIcons.Medium.Taiwan,
    "TH" to FlagIcons.Medium.Thailand,
    "TR" to FlagIcons.Medium.Turkey,
    "UA" to FlagIcons.Medium.Ukraine,
    "AE" to FlagIcons.Medium.UnitedArabEmirates,
    "GB" to FlagIcons.Medium.UnitedKingdom,
    "US" to FlagIcons.Medium.UnitedStates,
    "VN" to FlagIcons.Medium.Vietnam
)

fun resolveFlagIcon(isoCode: String): ImageVector? {
    if (isoCode.isBlank()) {
        return null
    }
    return isoToFlag[isoCode.uppercase()]
}

@Composable
fun CountryFlagImage(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    contentDescription: String? = null
) {
    val normalizedCode = countryCode.trim().uppercase()
    key(normalizedCode) {
        val icon = resolveFlagIcon(normalizedCode)
        if (icon != null) {
            Image(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = modifier.size(size)
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = normalizedCode.take(2),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
