/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : StatsSummaryCard.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 07:27:52
 * Description : Redesigned three-column stats card for latency, peak speed, and data usage.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.StatsCardBackground
import com.example.ui.theme.StatsDataAccent
import com.example.ui.theme.StatsDataIconBackground
import com.example.ui.theme.StatsDivider
import com.example.ui.theme.StatsLatencyAccent
import com.example.ui.theme.StatsLatencyIconBackground
import com.example.ui.theme.StatsLatencyStatusFair
import com.example.ui.theme.StatsLatencyStatusPoor
import com.example.ui.theme.StatsPeakAccent
import com.example.ui.theme.StatsPeakIconBackground
import com.example.ui.theme.StatsSubtitleNeutral
import com.example.ui.theme.StatsTitleText
import com.example.ui.theme.StatsUnitText
import com.example.ui.theme.StatsValueText
import com.example.util.BitrateFormatter
import com.example.util.LatencyQuality
import com.example.util.LatencyState
import com.example.util.TrafficLiveStats

private val StatsIconSize = 36.dp
private val StatsIconGlyphSize = 18.dp
private val StatsIconTextGap = 4.dp
private val StatsDividerHorizontalPadding = 3.dp

@Composable
fun StatsSummaryCard(
    latencyState: LatencyState,
    liveStats: TrafficLiveStats,
    modifier: Modifier = Modifier
) {
    val latencyTitle = stringResource(R.string.latency)
    val peakTitle = stringResource(R.string.peak_speed)
    val dataTitle = stringResource(R.string.data_used)

    val latencyValue = latencyState.latencyMs?.toString()
        ?: stringResource(R.string.quality_unknown)
    val latencyUnit = if (latencyState.latencyMs != null) {
        stringResource(R.string.stat_unit_ms)
    } else {
        ""
    }

    val peakFormatted = BitrateFormatter.formatSpeed(liveStats.peakSpeedMbps * 1_000_000.0)
    val (peakValue, peakUnit) = splitValueAndUnit(peakFormatted)

    val dataFormatted = BitrateFormatter.formatDataSize(liveStats.dataUsedTodayBytes)
    val (dataValue, dataUnit) = splitValueAndUnit(dataFormatted)

    HomeDashboardCard(
        color = StatsCardBackground,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatsColumn(
                icon = Icons.Outlined.AccessTime,
                iconBackground = StatsLatencyIconBackground,
                iconTint = StatsLatencyAccent,
                title = latencyTitle,
                value = latencyValue,
                unit = latencyUnit,
                statusText = qualityLabel(latencyState.quality),
                statusColor = latencyStatusColor(latencyState.quality),
                modifier = Modifier.weight(1f)
            )

            StatsVerticalDivider()

            StatsColumn(
                icon = Icons.Outlined.Speed,
                iconBackground = StatsPeakIconBackground,
                iconTint = StatsPeakAccent,
                title = peakTitle,
                value = peakValue,
                unit = peakUnit,
                statusText = peakStatusLabel(liveStats.peakSpeedMbps),
                statusColor = StatsPeakAccent,
                modifier = Modifier.weight(1f)
            )

            StatsVerticalDivider()

            StatsColumn(
                icon = Icons.Outlined.PieChart,
                iconBackground = StatsDataIconBackground,
                iconTint = StatsDataAccent,
                title = dataTitle,
                value = dataValue,
                unit = dataUnit,
                statusText = stringResource(R.string.today),
                statusColor = StatsSubtitleNeutral,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatsVerticalDivider() {
    VerticalDivider(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = StatsDividerHorizontalPadding),
        thickness = 1.dp,
        color = StatsDivider
    )
}

@Composable
private fun StatsColumn(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    value: String,
    unit: String,
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(StatsIconSize)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(StatsIconGlyphSize)
            )
        }

        Spacer(modifier = Modifier.width(StatsIconTextGap))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = StatsTitleText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            StatValueRow(
                value = value,
                unit = unit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp)
            )
            Text(
                text = statusText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}

@Composable
private fun StatValueRow(
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = StatsValueText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (unit.isNotEmpty()) {
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = StatsUnitText,
                maxLines = 1,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp)
            )
        }
    }
}

@Composable
private fun qualityLabel(quality: LatencyQuality): String =
    when (quality) {
        LatencyQuality.EXCELLENT -> stringResource(R.string.quality_excellent)
        LatencyQuality.GOOD -> stringResource(R.string.quality_good)
        LatencyQuality.FAIR -> stringResource(R.string.quality_fair)
        LatencyQuality.POOR -> stringResource(R.string.quality_poor)
        LatencyQuality.UNKNOWN -> stringResource(R.string.quality_unknown)
    }

@Composable
private fun peakStatusLabel(peakMbps: Double): String =
    when {
        peakMbps <= 0.0 -> stringResource(R.string.quality_unknown)
        peakMbps >= 100.0 -> stringResource(R.string.quality_excellent)
        peakMbps >= 50.0 -> stringResource(R.string.quality_good)
        peakMbps >= 10.0 -> stringResource(R.string.quality_fair)
        else -> stringResource(R.string.quality_poor)
    }

private fun latencyStatusColor(quality: LatencyQuality): Color =
    when (quality) {
        LatencyQuality.EXCELLENT, LatencyQuality.GOOD -> StatsLatencyAccent
        LatencyQuality.FAIR -> StatsLatencyStatusFair
        LatencyQuality.POOR -> StatsLatencyStatusPoor
        LatencyQuality.UNKNOWN -> StatsSubtitleNeutral
    }

private fun splitValueAndUnit(formatted: String): Pair<String, String> {
    val trimmed = formatted.trim()
    val separatorIndex = trimmed.indexOf(' ')
    return if (separatorIndex > 0) {
        trimmed.substring(0, separatorIndex) to trimmed.substring(separatorIndex + 1)
    } else {
        trimmed to ""
    }
}
