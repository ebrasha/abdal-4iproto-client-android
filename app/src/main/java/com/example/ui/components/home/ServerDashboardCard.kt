/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ServerDashboardCard.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 05:41:55
 * Description : Redesigned home server card with dynamic flag, endpoint, ping, and protocol chips.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ServerEntity
import com.example.ui.components.CountryFlagImage
import com.example.ui.components.DashboardCardShape
import com.example.ui.components.PillShape
import com.example.ui.components.ToggleButtonShape
import com.example.ui.components.shapeClickable
import com.example.ui.theme.PingChipBackground
import com.example.ui.theme.PingChipBackgroundFair
import com.example.ui.theme.PingChipBackgroundPoor
import com.example.ui.theme.PingChipForeground
import com.example.ui.theme.PingChipForegroundFair
import com.example.ui.theme.PingChipForegroundPoor
import com.example.ui.theme.ProtocolChipBackground
import com.example.ui.theme.ProtocolChipForeground
import com.example.ui.theme.ServerAddressText
import com.example.ui.theme.ServerCardBackground
import com.example.ui.theme.ServerChevron
import com.example.ui.theme.ServerFlagFrameBackground
import com.example.ui.theme.ServerNameText
import com.example.ui.theme.ServerToggleBackground
import com.example.ui.theme.ServerToggleIcon
import com.example.util.LatencyQuality
import com.example.util.LatencyState

private val FlagContainerShape = RoundedCornerShape(16.dp)
private val FlagInnerShape = RoundedCornerShape(6.dp)
private const val FlagOuterSizeDp = 56
private const val FlagInnerPaddingDp = 8

private data class PingChipStyle(
    val background: Color,
    val foreground: Color
)

private fun pingChipStyle(quality: LatencyQuality): PingChipStyle =
    when (quality) {
        LatencyQuality.EXCELLENT, LatencyQuality.GOOD -> PingChipStyle(
            background = PingChipBackground,
            foreground = PingChipForeground
        )
        LatencyQuality.FAIR -> PingChipStyle(
            background = PingChipBackgroundFair,
            foreground = PingChipForegroundFair
        )
        LatencyQuality.POOR -> PingChipStyle(
            background = PingChipBackgroundPoor,
            foreground = PingChipForegroundPoor
        )
        LatencyQuality.UNKNOWN -> PingChipStyle(
            background = ServerFlagFrameBackground,
            foreground = ServerAddressText
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDashboardCard(
    server: ServerEntity?,
    displayPort: Int,
    latencyState: LatencyState,
    protocolLabel: String,
    randomPortMode: Boolean,
    onTogglePortMode: () -> Unit,
    onOpenServerManagement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = DashboardCardShape,
        color = ServerCardBackground,
        shadowElevation = 2.dp
    ) {
        if (server == null) {
            Text(
                text = stringResource(R.string.no_server_selected),
                modifier = Modifier.padding(16.dp),
                color = ServerAddressText,
                fontSize = 15.sp
            )
            return@Surface
        }

        val pingStyle = pingChipStyle(latencyState.quality)
        val pingLabel = latencyState.latencyMs?.let { ms ->
            stringResource(R.string.ping_ms, ms)
        } ?: stringResource(R.string.quality_unknown)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(FlagOuterSizeDp.dp)
                    .clip(FlagContainerShape)
                    .background(ServerFlagFrameBackground, FlagContainerShape)
                    .padding(FlagInnerPaddingDp.dp),
                contentAlignment = Alignment.Center
            ) {
                val innerFlagSize = (FlagOuterSizeDp - FlagInnerPaddingDp * 2).dp
                CountryFlagImage(
                    countryCode = server.countryCode,
                    size = innerFlagSize,
                    contentDescription = server.countryCode,
                    modifier = Modifier
                        .size(innerFlagSize)
                        .clip(FlagInnerShape)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = ServerNameText,
                    maxLines = 1
                )

                Text(
                    text = "${server.ip}:$displayPort",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = ServerAddressText,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PingChip(
                        label = pingLabel,
                        style = pingStyle
                    )
                    ProtocolChip(label = protocolLabel)
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(ToggleButtonShape)
                    .background(ServerToggleBackground, ToggleButtonShape)
                    .shapeClickable(ToggleButtonShape, onTogglePortMode),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (randomPortMode) {
                        Icons.Default.Autorenew
                    } else {
                        Icons.Default.ArrowRightAlt
                    },
                    contentDescription = if (randomPortMode) {
                        stringResource(R.string.random_port_mode)
                    } else {
                        stringResource(R.string.fixed_port_mode)
                    },
                    tint = ServerToggleIcon,
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(PillShape)
                    .shapeClickable(PillShape, onOpenServerManagement),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = stringResource(R.string.open_server_list),
                    tint = ServerChevron,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PingChip(
    label: String,
    style: PingChipStyle
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(style.background, PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(style.foreground, CircleShape)
        )
        Text(
            text = label,
            color = style.foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ProtocolChip(label: String) {
    Text(
        text = label,
        modifier = Modifier
            .clip(PillShape)
            .background(ProtocolChipBackground, PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = ProtocolChipForeground,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
        maxLines = 1
    )
}
