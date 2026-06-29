/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : LogEntryCard.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:49:24
 * Description : Card row for a single structured tunnel log entry with level capsule and copy action.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LogCardBackground
import com.example.ui.theme.LogErrorBackground
import com.example.ui.theme.LogErrorForeground
import com.example.ui.theme.LogInfoBackground
import com.example.ui.theme.LogInfoForeground
import com.example.ui.theme.LogMessageText
import com.example.ui.theme.LogMetaText
import com.example.ui.theme.LogWarnBackground
import com.example.ui.theme.LogWarnForeground
import com.example.util.LogEntry
import com.example.util.LogLevel
import com.example.util.TunnelLogger

@Composable
fun LogEntryCard(
    entry: LogEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val (capsuleBackground, capsuleForeground, levelLabel) = levelStyle(entry.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        colors = CardDefaults.cardColors(containerColor = LogCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = levelLabel,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(capsuleBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = capsuleForeground,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = entry.message,
                    color = LogMessageText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Text(
                    text = "${entry.timestamp} · ${entry.tag}",
                    color = LogMetaText,
                    fontSize = 10.sp
                )
            }

            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(TunnelLogger.dumpEntry(entry)))
                    Toast.makeText(
                        context,
                        context.getString(R.string.log_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy_log_entry),
                    tint = LogMetaText
                )
            }
        }
    }
}

@Composable
private fun levelStyle(level: LogLevel): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, String> {
    return when (level) {
        LogLevel.INFO -> Triple(
            LogInfoBackground,
            LogInfoForeground,
            stringResource(R.string.log_level_info)
        )
        LogLevel.WARN -> Triple(
            LogWarnBackground,
            LogWarnForeground,
            stringResource(R.string.log_level_warn)
        )
        LogLevel.ERROR -> Triple(
            LogErrorBackground,
            LogErrorForeground,
            stringResource(R.string.log_level_error)
        )
    }
}
