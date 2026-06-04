/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : AboutScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 19:49:03
 * Description : About screen with clickable programmer contact information (email and Telegram).
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Marketing version shown only on the About header (MAJOR.MINOR).
private const val APP_DISPLAY_VERSION = "5.2"

private const val PROGRAMMER_EMAIL = "Prof.Shafiei@Gmail.com"
private const val TELEGRAM_HANDLE = "@ProfShafiei"
private const val TELEGRAM_URL = "https://t.me/ProfShafiei"
private const val GITHUB_HANDLE = "github.com/ebrasha"
private const val GITHUB_URL = "https://github.com/ebrasha"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${stringResource(R.string.app_name)} v$APP_DISPLAY_VERSION",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))

            AboutItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.programmer),
                subtitle = "Ebrahim Shafiei (EbraSha)"
            )
            Spacer(modifier = Modifier.height(24.dp))

            AboutItem(
                icon = Icons.Default.Email,
                title = stringResource(R.string.email),
                subtitle = PROGRAMMER_EMAIL,
                onClick = { openEmail(context, PROGRAMMER_EMAIL) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            AboutItem(
                icon = Icons.AutoMirrored.Filled.Send,
                title = stringResource(R.string.telegram),
                subtitle = TELEGRAM_HANDLE,
                onClick = { openUrl(context, TELEGRAM_URL) }
            )
            Spacer(modifier = Modifier.height(24.dp))

            AboutItem(
                icon = Icons.Default.Code,
                title = stringResource(R.string.github),
                subtitle = GITHUB_HANDLE,
                onClick = { openUrl(context, GITHUB_URL) }
            )
            Spacer(modifier = Modifier.height(32.dp))

            BiographyCard()
        }
    }
}

/**
 * Displays a short professional biography of the programmer.
 */
@Composable
private fun BiographyCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.about_bio_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.about_bio),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun AboutItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(vertical = 8.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Opens the device email composer addressed to the given recipient.
 */
private fun openEmail(context: android.content.Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$email")
    }
    launchSafely(context, intent)
}

/**
 * Opens the given URL in Telegram or the default browser.
 */
private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    launchSafely(context, intent)
}

private fun launchSafely(context: android.content.Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to handle this action", Toast.LENGTH_SHORT).show()
    }
}
