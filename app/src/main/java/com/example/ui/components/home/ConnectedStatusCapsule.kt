/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ConnectedStatusCapsule.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : VPN status label shown below the connect button on the home dashboard.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.components.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ConnectGreen
import com.example.vpn.VpnState

@Composable
fun ConnectedStatusCapsule(
    vpnState: VpnState,
    modifier: Modifier = Modifier
) {
    val (label, icon, contentColor) = statusPresentation(vpnState)

    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun statusPresentation(vpnState: VpnState): StatusPresentation {
    return when (vpnState) {
        VpnState.CONNECTED -> StatusPresentation(
            label = stringResource(R.string.status_connected),
            icon = Icons.Default.CheckCircle,
            contentColor = ConnectGreen
        )
        VpnState.CONNECTING -> StatusPresentation(
            label = stringResource(R.string.status_connecting),
            icon = Icons.Default.Sync,
            contentColor = Color(0xFFE65100)
        )
        VpnState.ERROR -> StatusPresentation(
            label = stringResource(R.string.status_error),
            icon = Icons.Default.ErrorOutline,
            contentColor = Color(0xFFC62828)
        )
        VpnState.DISCONNECTED -> StatusPresentation(
            label = stringResource(R.string.status_disconnected),
            icon = Icons.Default.WifiOff,
            contentColor = Color(0xFF546E7A)
        )
    }
}

private data class StatusPresentation(
    val label: String,
    val icon: ImageVector,
    val contentColor: Color
)
