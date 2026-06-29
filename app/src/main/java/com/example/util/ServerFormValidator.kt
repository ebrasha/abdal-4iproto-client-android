/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ServerFormValidator.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:48:20
 * Description : Field-level validation for add/edit server forms.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.util

data class ServerFormErrors(
    val name: String? = null,
    val countryCode: String? = null,
    val ip: String? = null,
    val ports: String? = null,
    val username: String? = null,
    val password: String? = null
) {
    val hasErrors: Boolean =
        name != null ||
            countryCode != null ||
            ip != null ||
            ports != null ||
            username != null ||
            password != null
}

object ServerFormValidator {

    fun validate(
        name: String,
        countryCode: String,
        ip: String,
        ports: String,
        username: String,
        password: String
    ): ServerFormErrors {
        val normalizedCountry = CountryCatalog.normalizeInput(countryCode)
        return ServerFormErrors(
            name = when {
                name.trim().isEmpty() -> ServerFormErrorKey.NAME_REQUIRED
                else -> null
            },
            countryCode = when {
                countryCode.trim().isEmpty() -> ServerFormErrorKey.COUNTRY_REQUIRED
                !CountryCatalog.isValidIsoCode(normalizedCountry) -> ServerFormErrorKey.COUNTRY_INVALID
                else -> null
            },
            ip = when {
                ip.trim().isEmpty() -> ServerFormErrorKey.IP_REQUIRED
                !HostValidator.isValidHostOrIp(ip) -> ServerFormErrorKey.IP_INVALID
                else -> null
            },
            ports = when {
                ports.trim().isEmpty() -> ServerFormErrorKey.PORTS_REQUIRED
                !PortParser.isValidPortList(ports) -> ServerFormErrorKey.PORTS_INVALID
                else -> null
            },
            username = when {
                username.trim().isEmpty() -> ServerFormErrorKey.USERNAME_REQUIRED
                else -> null
            },
            password = when {
                password.isEmpty() -> ServerFormErrorKey.PASSWORD_REQUIRED
                else -> null
            }
        )
    }
}

object ServerFormErrorKey {
    const val NAME_REQUIRED = "name_required"
    const val COUNTRY_REQUIRED = "country_required"
    const val COUNTRY_INVALID = "country_invalid"
    const val IP_REQUIRED = "ip_required"
    const val IP_INVALID = "ip_invalid"
    const val PORTS_REQUIRED = "ports_required"
    const val PORTS_INVALID = "ports_invalid"
    const val USERNAME_REQUIRED = "username_required"
    const val PASSWORD_REQUIRED = "password_required"
}
