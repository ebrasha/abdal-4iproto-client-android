/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : AdvancedSettingsScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 19:49:24
 * Description : Advanced settings with flat bordered cards and instant-save toggles.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.AppViewModel
import com.example.ui.components.SettingsCard
import com.example.util.PingIntervalConfig
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val loggingEnabled by viewModel.loggingEnabled.collectAsStateWithLifecycle()
    val pingMeasurementEnabled by viewModel.pingMeasurementEnabled.collectAsStateWithLifecycle()
    val pingInterval by viewModel.pingIntervalSeconds.collectAsStateWithLifecycle()
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val fakeIpEnabled by viewModel.fakeIpEnabled.collectAsStateWithLifecycle()
    val soundsEnabled by viewModel.soundsEnabled.collectAsStateWithLifecycle()
    val whitelistIps by viewModel.whitelistIps.collectAsStateWithLifecycle()
    val pingSliderSteps =
        ((PingIntervalConfig.MAX_SECONDS - PingIntervalConfig.MIN_SECONDS) / PingIntervalConfig.STEP_SECONDS) - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advanced_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsCard(
                title = stringResource(R.string.app_logging_title),
                description = stringResource(R.string.app_logging_desc)
            ) {
                SettingsSwitchRow(
                    checked = loggingEnabled,
                    onCheckedChange = { viewModel.setLoggingEnabled(it) }
                )
            }

            SettingsCard(
                title = stringResource(R.string.ping_measurement_title),
                description = stringResource(R.string.ping_measurement_desc)
            ) {
                SettingsSwitchRow(
                    checked = pingMeasurementEnabled,
                    onCheckedChange = { viewModel.setPingMeasurementEnabled(it) }
                )
                if (pingMeasurementEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.ping_interval_value, pingInterval),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = pingInterval.toFloat(),
                        onValueChange = { value ->
                            viewModel.setPingIntervalSeconds(PingIntervalConfig.coerce(value.roundToInt()))
                        },
                        valueRange = PingIntervalConfig.MIN_SECONDS.toFloat()..PingIntervalConfig.MAX_SECONDS.toFloat(),
                        steps = pingSliderSteps.coerceAtLeast(0),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SettingsCard(
                title = stringResource(R.string.kill_switch),
                description = stringResource(R.string.kill_switch_desc)
            ) {
                SettingsSwitchRow(
                    checked = killSwitchEnabled,
                    onCheckedChange = { viewModel.setKillSwitch(it) }
                )
            }

            SettingsCard(
                title = stringResource(R.string.fake_ip_dns),
                description = stringResource(R.string.fake_ip_dns_desc)
            ) {
                SettingsSwitchRow(
                    checked = fakeIpEnabled,
                    onCheckedChange = { viewModel.setFakeIp(it) }
                )
            }

            SettingsCard(
                title = stringResource(R.string.app_sounds_title),
                description = stringResource(R.string.app_sounds_desc)
            ) {
                SettingsSwitchRow(
                    checked = soundsEnabled,
                    onCheckedChange = { viewModel.setSoundsEnabled(it) }
                )
            }

            SettingsCard(
                title = stringResource(R.string.whitelist_ips_title),
                description = stringResource(R.string.whitelist_ips_desc)
            ) {
                OutlinedTextField(
                    value = whitelistIps,
                    onValueChange = { viewModel.setWhitelistIps(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("e.g. 192.168.1.0/24, 10.0.0.1") },
                    maxLines = 10
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
