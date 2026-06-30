/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TrafficChartCard.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 06:23:01
 * Description : Home traffic chart card with dynamic download/upload headers and smooth area chart.
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ChartCardBackground
import com.example.ui.theme.ChartDownloadGreen
import com.example.ui.theme.ChartHeaderLabel
import com.example.ui.theme.ChartIconForeground
import com.example.ui.theme.ChartUploadBlue
import com.example.util.BitrateFormatter
import com.example.util.TrafficChartSeries
import com.example.util.TrafficLiveStats

@Composable
fun TrafficChartCard(
    liveStats: TrafficLiveStats,
    chartSeries: TrafficChartSeries,
    modifier: Modifier = Modifier
) {
    val downloadLabel = stringResource(R.string.download)
    val uploadLabel = stringResource(R.string.upload)
    val downloadValue = remember(liveStats.downloadMbps) {
        BitrateFormatter.formatSpeed(liveStats.downloadMbps * 1_000_000.0)
    }
    val uploadValue = remember(liveStats.uploadMbps) {
        BitrateFormatter.formatSpeed(liveStats.uploadMbps * 1_000_000.0)
    }

    HomeDashboardCard(
        color = ChartCardBackground,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrafficMetricHeader(
                    label = downloadLabel,
                    value = downloadValue,
                    circleColor = ChartDownloadGreen,
                    icon = Icons.Rounded.ArrowDownward,
                    valueColor = ChartDownloadGreen,
                    alignEnd = false
                )
                TrafficMetricHeader(
                    label = uploadLabel,
                    value = uploadValue,
                    circleColor = ChartUploadBlue,
                    icon = Icons.Rounded.ArrowUpward,
                    valueColor = ChartUploadBlue,
                    alignEnd = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TrafficAreaChart(
                downloadSeries = chartSeries.downloadHistory,
                uploadSeries = chartSeries.uploadHistory
            )
        }
    }
}

@Composable
private fun TrafficMetricHeader(
    label: String,
    value: String,
    circleColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    valueColor: androidx.compose.ui.graphics.Color,
    alignEnd: Boolean
) {
    if (alignEnd) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                TrafficMetricTexts(label = label, value = value, valueColor = valueColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            TrafficMetricIconCircle(label = label, circleColor = circleColor, icon = icon)
        }
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrafficMetricIconCircle(label = label, circleColor = circleColor, icon = icon)
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.Start) {
                TrafficMetricTexts(label = label, value = value, valueColor = valueColor)
            }
        }
    }
}

@Composable
private fun TrafficMetricTexts(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    Text(
        text = label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = ChartHeaderLabel
    )
    Text(
        text = value,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = valueColor
    )
}

@Composable
private fun TrafficMetricIconCircle(
    label: String,
    circleColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(circleColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ChartIconForeground,
            modifier = Modifier.size(22.dp)
        )
    }
}
