/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : CountryCatalog.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:25:30
 * Description : ISO 3166-1 alpha-2 country code helpers for flag lookup and validation.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

import java.util.Locale

object CountryCatalog {

    data class CountryEntry(val code: String, val name: String)

    val allCountries: List<CountryEntry> by lazy {
        Locale.getISOCountries()
            .map { code ->
                CountryEntry(
                    code = code.uppercase(Locale.US),
                    name = Locale("", code).getDisplayCountry(Locale.ENGLISH)
                )
            }
            .filter { it.name.isNotBlank() }
            .sortedBy { it.name.lowercase(Locale.US) }
    }

    fun nameForCode(code: String): String {
        if (code.isBlank()) {
            return ""
        }
        return allCountries.find { it.code.equals(code, ignoreCase = true) }?.name
            ?: code.uppercase(Locale.US)
    }

    /** Keeps only Latin letters, uppercases, and limits input to two characters. */
    fun normalizeInput(raw: String): String =
        raw.filter { it.isLetter() }.uppercase(Locale.US).take(2)

    /** Returns true when the code is a known ISO 3166-1 alpha-2 country code. */
    fun isValidIsoCode(code: String): Boolean {
        if (code.length != 2) {
            return false
        }
        return allCountries.any { it.code.equals(code, ignoreCase = true) }
    }
}
