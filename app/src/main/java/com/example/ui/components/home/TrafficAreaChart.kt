/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : TrafficAreaChart.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 06:23:01
 * Description : Lightweight smooth dual-series area chart with dashed guides for the traffic card.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChartAxisLabel
import com.example.ui.theme.ChartDownloadGreen
import com.example.ui.theme.ChartGridDash
import com.example.ui.theme.ChartUploadBlue
import com.example.util.BitrateFormatter
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val CHART_HEIGHT_DP = 80
private const val AXIS_TICK_COUNT = 3
private const val LINE_STROKE_DP = 2.5f
private const val DOWNLOAD_FILL_TOP_ALPHA = 0.30f
private const val UPLOAD_FILL_TOP_ALPHA = 0.28f
private const val SERIES_ANIMATION_MS = 420

@Composable
fun TrafficAreaChart(
    downloadSeries: List<Float>,
    uploadSeries: List<Float>,
    modifier: Modifier = Modifier
) {
    val normalizedDownload = remember(downloadSeries) {
        downloadSeries.ifEmpty { emptyList() }
    }
    val normalizedUpload = remember(uploadSeries) {
        uploadSeries.ifEmpty { emptyList() }
    }

    val axisMaxMbps = remember(normalizedDownload, normalizedUpload) {
        niceAxisMaxMbps(
            maxOf(
                normalizedDownload.maxOrNull() ?: 0f,
                normalizedUpload.maxOrNull() ?: 0f
            )
        )
    }

    val yAxisLabels = remember(axisMaxMbps) {
        buildAxisLabels(axisMaxMbps, AXIS_TICK_COUNT)
    }

    val animatedDownload = rememberAnimatedSeries(normalizedDownload)
    val animatedUpload = rememberAnimatedSeries(normalizedUpload)
    val animatedMax = rememberAnimatedScalar(axisMaxMbps)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(44.dp)
                .height(CHART_HEIGHT_DP.dp)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yAxisLabels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = ChartAxisLabel,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(CHART_HEIGHT_DP.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                if (chartWidth <= 0f || chartHeight <= 0f) {
                    return@Canvas
                }

                buildTickFractions(AXIS_TICK_COUNT).forEach { fraction ->
                    val y = chartHeight * fraction
                    drawLine(
                        color = ChartGridDash,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                val downloadPoints = seriesToPoints(
                    series = animatedDownload,
                    axisMaxMbps = animatedMax,
                    width = chartWidth,
                    height = chartHeight
                )
                val uploadPoints = seriesToPoints(
                    series = animatedUpload,
                    axisMaxMbps = animatedMax,
                    width = chartWidth,
                    height = chartHeight
                )

                drawSeriesLayer(
                    points = downloadPoints,
                    chartHeight = chartHeight,
                    lineColor = ChartDownloadGreen,
                    fillTopAlpha = DOWNLOAD_FILL_TOP_ALPHA
                )
                drawSeriesLayer(
                    points = uploadPoints,
                    chartHeight = chartHeight,
                    lineColor = ChartUploadBlue,
                    fillTopAlpha = UPLOAD_FILL_TOP_ALPHA
                )
            }
        }
    }
}

@Composable
private fun rememberAnimatedSeries(target: List<Float>): List<Float> {
    val normalizedTarget = remember(target) {
        if (target.isEmpty()) listOf(0f) else target
    }
    val from = remember { mutableStateListOf<Float>() }
    val to = remember { mutableStateListOf<Float>() }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(normalizedTarget) {
        if (to.isEmpty()) {
            from.clear()
            from.addAll(normalizedTarget)
            to.clear()
            to.addAll(normalizedTarget)
            return@LaunchedEffect
        }
        if (to.size != normalizedTarget.size) {
            from.clear()
            from.addAll(normalizedTarget)
            to.clear()
            to.addAll(normalizedTarget)
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        from.clear()
        from.addAll(to)
        to.clear()
        to.addAll(normalizedTarget)
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(SERIES_ANIMATION_MS))
    }

    val blend = progress.value
    if (from.isEmpty() || to.isEmpty() || from.size != to.size) {
        return normalizedTarget
    }
    return List(to.size) { index ->
        from[index] + (to[index] - from[index]) * blend
    }
}

@Composable
private fun rememberAnimatedScalar(target: Float): Float {
    val animatable = remember { Animatable(target) }
    LaunchedEffect(target) {
        animatable.animateTo(target, animationSpec = tween(SERIES_ANIMATION_MS))
    }
    return animatable.value
}

private fun buildAxisLabels(axisMaxMbps: Float, tickCount: Int): List<String> {
    return buildTickFractions(tickCount).map { fraction ->
        val mbps = axisMaxMbps * (1f - fraction)
        BitrateFormatter.formatSpeed(mbps * 1_000_000.0)
    }
}

private fun buildTickFractions(tickCount: Int): List<Float> {
    if (tickCount <= 1) {
        return listOf(0f)
    }
    return (0 until tickCount).map { index ->
        index.toFloat() / (tickCount - 1).toFloat()
    }
}

private fun niceAxisMaxMbps(rawMax: Float): Float {
    if (rawMax <= 0f) {
        return 1f
    }
    val magnitude = 10.0.pow(floor(log10(rawMax.toDouble()))).toFloat()
    val normalized = rawMax / magnitude
    val niceNormalized = when {
        normalized <= 1f -> 1f
        normalized <= 2f -> 2f
        normalized <= 5f -> 5f
        else -> 10f
    }
    return niceNormalized * magnitude
}

private fun seriesToPoints(
    series: List<Float>,
    axisMaxMbps: Float,
    width: Float,
    height: Float
): List<Offset> {
    val values = if (series.isEmpty()) listOf(0f) else series
    val max = axisMaxMbps.coerceAtLeast(0.0001f)
    val lastIndex = (values.size - 1).coerceAtLeast(1)
    return values.mapIndexed { index, value ->
        val x = if (values.size == 1) {
            width
        } else {
            width * index.toFloat() / lastIndex.toFloat()
        }
        val normalized = (value / max).coerceIn(0f, 1f)
        val y = height - (normalized * height)
        Offset(x, y)
    }
}

private fun DrawScope.drawSeriesLayer(
    points: List<Offset>,
    chartHeight: Float,
    lineColor: Color,
    fillTopAlpha: Float
) {
    if (points.isEmpty()) {
        return
    }
    val linePath = buildSmoothPath(points)
    val areaPath = Path().apply {
        addPath(linePath)
        lineTo(points.last().x, chartHeight)
        lineTo(points.first().x, chartHeight)
        close()
    }
    drawPath(
        path = areaPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                lineColor.copy(alpha = fillTopAlpha),
                Color.Transparent
            ),
            startY = points.minOf { it.y },
            endY = chartHeight
        )
    )
    drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(
            width = LINE_STROKE_DP.dp.toPx(),
            cap = StrokeCap.Round
        )
    )
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) {
        return path
    }
    if (points.size == 1) {
        path.moveTo(points.first().x, points.first().y)
        return path
    }

    path.moveTo(points.first().x, points.first().y)
    for (index in 0 until points.lastIndex) {
        val p0 = points[getSeriesIndex(index - 1, points.size)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[getSeriesIndex(index + 2, points.size)]

        val control1 = Offset(
            x = p1.x + (p2.x - p0.x) / 6f,
            y = p1.y + (p2.y - p0.y) / 6f
        )
        val control2 = Offset(
            x = p2.x - (p3.x - p1.x) / 6f,
            y = p2.y - (p3.y - p1.y) / 6f
        )
        path.cubicTo(control1.x, control1.y, control2.x, control2.y, p2.x, p2.y)
    }
    return path
}

private fun getSeriesIndex(index: Int, size: Int): Int =
    index.coerceIn(0, size - 1)
