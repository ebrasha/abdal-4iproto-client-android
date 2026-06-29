/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ConnectionTimerSection.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Displays the VPN connection uptime timer with segment labels for the home dashboard.
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ConnectGreen
import com.example.ui.theme.DashboardNavy
import com.example.util.ConnectionTimerState

@Composable
fun ConnectionTimerSection(
    timerState: ConnectionTimerState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(ConnectGreen, CircleShape)
            )
            Text(
                text = stringResource(R.string.connection_time),
                modifier = Modifier.padding(start = 8.dp),
                color = DashboardNavy.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top
        ) {
            TimerSegment(
                value = "%02d".format(timerState.hours),
                label = stringResource(R.string.hours_label)
            )
            TimerColon()
            TimerSegment(
                value = "%02d".format(timerState.minutes),
                label = stringResource(R.string.minutes_label)
            )
            TimerColon()
            TimerSegment(
                value = "%02d".format(timerState.seconds),
                label = stringResource(R.string.seconds_label)
            )
        }
    }
}

@Composable
private fun TimerSegment(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            color = DashboardNavy,
            letterSpacing = 1.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TimerColon() {
    Text(
        text = ":",
        fontSize = 44.sp,
        fontWeight = FontWeight.Bold,
        color = DashboardNavy,
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .padding(top = 2.dp)
    )
}
