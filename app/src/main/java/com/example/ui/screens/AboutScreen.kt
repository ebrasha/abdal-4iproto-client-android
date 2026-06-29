/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : AboutScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-11 05:27:00
 * Description : About screen showing the app logo, auto-read version, contact details, a donation card,
 *               and a developer biography. The version is read from BuildConfig so it always matches the
 *               source.
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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R

private const val PROGRAMMER_EMAIL = "Prof.Shafiei@Gmail.com"
private const val TELEGRAM_HANDLE = "@ProfShafiei"
private const val TELEGRAM_URL = "https://t.me/ProfShafiei"
private const val GITHUB_HANDLE = "github.com/ebrasha"
private const val GITHUB_URL = "https://github.com/ebrasha"
private const val DONATION_URL = "https://t.me/AbdalDonationBot"

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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.abdal_logo),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Version is read from the build configuration so it always matches the source.
            Text(
                text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.about_tagline),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))

            ContactCard(context)
            Spacer(modifier = Modifier.height(16.dp))

            DonationCard(context)
            Spacer(modifier = Modifier.height(16.dp))

            DeveloperCard()
        }
    }
}

/**
 * Card grouping the programmer's clickable contact details (email, Telegram, GitHub).
 */
@Composable
private fun ContactCard(context: android.content.Context) {
    SectionCard(title = stringResource(R.string.contact_title)) {
        ContactItem(
            icon = Icons.Default.Person,
            title = stringResource(R.string.programmer),
            subtitle = "Ebrahim Shafiei (EbraSha)"
        )
        ContactItem(
            icon = Icons.Default.Email,
            title = stringResource(R.string.email),
            subtitle = PROGRAMMER_EMAIL,
            onClick = { openEmail(context, PROGRAMMER_EMAIL) }
        )
        ContactItem(
            icon = Icons.AutoMirrored.Filled.Send,
            title = stringResource(R.string.telegram),
            subtitle = TELEGRAM_HANDLE,
            onClick = { openUrl(context, TELEGRAM_URL) }
        )
        ContactItem(
            icon = Icons.Default.Code,
            title = stringResource(R.string.github),
            subtitle = GITHUB_HANDLE,
            onClick = { openUrl(context, GITHUB_URL) }
        )
    }
}

/**
 * Card inviting the user to support development, with a Donate button that opens the donation bot.
 */
@Composable
private fun DonationCard(context: android.content.Context) {
    SectionCard(title = stringResource(R.string.donation_title)) {
        Text(
            text = stringResource(R.string.donation_desc),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { openUrl(context, DONATION_URL) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.donate), fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Card with a professional biography of the developer, emphasising key roles for readability.
 */
@Composable
private fun DeveloperCard() {
    val primary = MaterialTheme.colorScheme.primary
    val bio = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primary)) {
            append("Ebrahim Shafiei (EbraSha)")
        }
        append(" — ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Founder & Leader of Abdal Security Group | White Hat Hacker")
        }
        append("\n\n")
        append("I am a dedicated technologist and cybersecurity researcher with a deep-rooted passion for securing digital landscapes. As the ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("Founder and Leader of Abdal Security Group")
        }
        append(", I lead a mission-driven team focused on identifying critical vulnerabilities and fortifying infrastructure against evolving cyber threats.")
        append("\n\n")
        append("My career is defined by a commitment to the principles of ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("ethical hacking")
        }
        append(". As a ")
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append("White Hat Hacker")
        }
        append(", I believe that true security is built through transparency, continuous learning, and a proactive approach to defense. Whether I am analyzing complex binary systems, conducting vulnerability research, or architecting secure software solutions, my objective remains constant: to contribute to a safer, more resilient digital ecosystem.")
        append("\n\n")
        append("Beyond the technical challenges, I value the collaborative nature of the global security community. I am driven by the belief that by sharing knowledge and addressing security gaps at their core, we can empower organizations and protect users worldwide.")
        append("\n\n")
        append("My work spans across international borders, involving high-stakes penetration testing and the development of robust, high-performance security software. I approach every project—from low-level assembly analysis to large-scale AI system design—with precision, discipline, and a relentless curiosity to understand how things work and how to make them better.")
    }

    SectionCard(title = stringResource(R.string.about_developer_title)) {
        Text(
            text = bio,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )
    }
}

/**
 * Reusable rounded card with a section title and arbitrary content.
 */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

/**
 * A single contact row: leading icon, a small label and a larger (optionally clickable) value.
 */
@Composable
private fun ContactItem(
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
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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
 * Opens the given URL in the appropriate app or the default browser.
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
