/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ConnectButtonSection.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Large circular connect/disconnect button with animated glow for the home dashboard.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.shapeClickable
import com.example.ui.theme.ConnectGreen
import com.example.ui.theme.ConnectGreenGlow
import com.example.vpn.VpnState

@Composable
fun ConnectButtonSection(
    vpnState: VpnState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.DISCONNECTED -> Color(0xFF616161)
            VpnState.CONNECTING -> Color(0xFFD4A373)
            VpnState.CONNECTED -> ConnectGreen
            VpnState.ERROR -> Color(0xFFF44336)
        },
        animationSpec = tween(500),
        label = "connectButtonColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "connectGlow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "connectGlowPhase"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .drawBehind {
                if (vpnState == VpnState.CONNECTED) {
                    val layers = 4
                    for (layer in 0 until layers) {
                        val progress = (glowPhase + layer * 0.18f) % 1f
                        val radius = (size.minDimension / 2f) + (24.dp.toPx() * progress)
                        val alpha = (1f - progress) * 0.35f
                        drawCircle(
                            color = ConnectGreenGlow.copy(alpha = alpha),
                            radius = radius
                        )
                    }
                }
            }
            .clip(CircleShape)
            .background(buttonColor)
            .shapeClickable(CircleShape, onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = stringResource(R.string.connect),
            modifier = Modifier.size(88.dp),
            tint = Color.White
        )
    }
}
