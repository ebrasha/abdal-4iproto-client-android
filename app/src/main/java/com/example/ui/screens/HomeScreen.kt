/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : HomeScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:02:25
 * Description : Main tunnel dashboard with connect control, traffic chart, and runtime stats.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.screens

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.AppViewModel
import com.example.ui.components.home.ConnectButtonSection
import com.example.ui.components.home.ConnectedStatusCapsule
import com.example.ui.components.home.ConnectionTimerSection
import com.example.ui.components.home.ServerDashboardCard
import com.example.ui.components.home.StatsSummaryCard
import com.example.ui.components.home.TrafficChartCard
import com.example.ui.theme.DashboardBackground
import com.example.vpn.VpnState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onServerManagementClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    onPerAppSplitTunClick: () -> Unit,
    onUdpTunnelClick: () -> Unit
) {
    val context = LocalContext.current
    val servers by viewModel.allServers.collectAsStateWithLifecycle()
    val vpnState by viewModel.vpnState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val connectionTimer by viewModel.connectionTimer.collectAsStateWithLifecycle()
    val trafficStats by viewModel.trafficStats.collectAsStateWithLifecycle()
    val latencyState by viewModel.latencyState.collectAsStateWithLifecycle()
    val activeSessionPort by viewModel.activeSessionPort.collectAsStateWithLifecycle()
    val portModeRevision by viewModel.portModeRevision.collectAsStateWithLifecycle()

    LaunchedEffect(servers) {
        if (selectedServer == null && servers.isNotEmpty()) {
            viewModel.selectServer(servers.first())
        } else if (servers.isEmpty()) {
            viewModel.selectServer(null)
        }
    }

    val displayPort = selectedServer?.let { server ->
        activeSessionPort
        viewModel.resolvePortForDisplay(server)
    } ?: 22

    val randomPortMode = remember(selectedServer?.id, portModeRevision) {
        selectedServer?.let { server -> viewModel.isRandomPortMode(server.id) } ?: false
    }

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedServer?.let { viewModel.toggleVpn(context, it) }
        } else {
            viewModel.reportVpnPermissionDenied()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.targetValue == DrawerValue.Open) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Spacer(Modifier.height(16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text(stringResource(R.string.server_management)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onServerManagementClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.advanced_settings)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAdvancedSettingsClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    label = { Text("Per-App Split Tun") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPerAppSplitTunClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = null) },
                    label = { Text(stringResource(R.string.udp_tunnel)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onUdpTunnelClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.logs)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onAboutClick()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.drawer_menu)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onAboutClick) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = stringResource(R.string.about)
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
                    .background(DashboardBackground)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ConnectionTimerSection(timerState = connectionTimer)

                Spacer(modifier = Modifier.height(8.dp))

                ConnectButtonSection(
                    vpnState = vpnState,
                    onClick = {
                        when {
                            selectedServer == null -> viewModel.reportNoServerSelected()
                            vpnState == VpnState.CONNECTED || vpnState == VpnState.CONNECTING -> {
                                viewModel.toggleVpn(context, selectedServer!!)
                            }
                            else -> {
                                val intent = VpnService.prepare(context)
                                if (intent != null) {
                                    vpnLauncher.launch(intent)
                                } else {
                                    viewModel.toggleVpn(context, selectedServer!!)
                                }
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ConnectedStatusCapsule(vpnState = vpnState)

                if (vpnState == VpnState.ERROR && !errorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                ServerDashboardCard(
                    server = selectedServer,
                    displayPort = displayPort,
                    latencyState = latencyState,
                    protocolLabel = stringResource(R.string.chip_4iproto),
                    randomPortMode = randomPortMode,
                    onTogglePortMode = {
                        selectedServer?.let { server ->
                            val nextIsRandom = !randomPortMode
                            viewModel.toggleRandomPortMode(server.id)
                            val message = if (nextIsRandom) {
                                context.getString(R.string.random_port_mode)
                            } else {
                                context.getString(R.string.fixed_port_mode)
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenServerManagement = onServerManagementClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                TrafficChartCard(trafficStats = trafficStats)

                Spacer(modifier = Modifier.height(16.dp))

                StatsSummaryCard(
                    latencyState = latencyState,
                    trafficStats = trafficStats
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
