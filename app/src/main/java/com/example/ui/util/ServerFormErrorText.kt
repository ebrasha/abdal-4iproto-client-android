/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ServerFormErrorText.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:49:24
 * Description : Maps server form validation error keys to localized user-facing strings.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.util.ServerFormErrorKey

@Composable
fun resolveServerFormError(key: String?): String? = when (key) {
    ServerFormErrorKey.NAME_REQUIRED -> stringResource(R.string.error_name_required)
    ServerFormErrorKey.COUNTRY_REQUIRED -> stringResource(R.string.error_country_required)
    ServerFormErrorKey.COUNTRY_INVALID -> stringResource(R.string.error_country_invalid)
    ServerFormErrorKey.IP_REQUIRED -> stringResource(R.string.error_ip_required)
    ServerFormErrorKey.IP_INVALID -> stringResource(R.string.error_ip_invalid)
    ServerFormErrorKey.PORTS_REQUIRED -> stringResource(R.string.error_ports_required)
    ServerFormErrorKey.PORTS_INVALID -> stringResource(R.string.error_ports_invalid)
    ServerFormErrorKey.USERNAME_REQUIRED -> stringResource(R.string.error_username_required)
    ServerFormErrorKey.PASSWORD_REQUIRED -> stringResource(R.string.error_password_required)
    else -> null
}
