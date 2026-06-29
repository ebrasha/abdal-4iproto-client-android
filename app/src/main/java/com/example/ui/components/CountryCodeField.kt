/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : CountryCodeField.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 04:02:16
 * Description : Text field for ISO 3166-1 alpha-2 country codes with live flag preview.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.CountryCatalog

@Composable
fun CountryCodeField(
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null
) {
    val normalizedCode = CountryCatalog.normalizeInput(countryCode)
    val countryName = remember(normalizedCode) {
        if (normalizedCode.length == 2) {
            CountryCatalog.nameForCode(normalizedCode)
        } else {
            ""
        }
    }

    OutlinedTextField(
        value = countryCode,
        onValueChange = { input ->
            onCountryCodeChange(CountryCatalog.normalizeInput(input))
        },
        label = { Text(stringResource(R.string.country)) },
        placeholder = { Text(stringResource(R.string.country_code_hint)) },
        supportingText = {
            when {
                errorText != null -> Text(errorText)
                normalizedCode.length == 2 && countryName.isNotBlank() && countryName != normalizedCode -> Text(countryName)
            }
        },
        isError = isError,
        trailingIcon = {
            if (normalizedCode.isNotBlank()) {
                CountryFlagImage(countryCode = normalizedCode, size = 28.dp)
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
