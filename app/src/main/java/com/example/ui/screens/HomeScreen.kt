/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : HomeScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:02:25
 * Description : Main screen with connect control and server selection list.
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ServerEntity
import com.example.ui.AppViewModel
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

    LaunchedEffect(servers) {
        if (selectedServer == null && servers.isNotEmpty()) {
            viewModel.selectServer(servers.first())
        } else if (servers.isEmpty()) {
            viewModel.selectServer(null)
        }
    }

    val buttonBgColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.DISCONNECTED -> Color.DarkGray
            VpnState.CONNECTING -> Color(0xFFD4A373)
            VpnState.CONNECTED -> Color(0xFF4CAF50)
            VpnState.ERROR -> Color(0xFFF44336)
        },
        animationSpec = tween(500),
        label = "connectButtonColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "haloTransition")
    val haloAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloAnim"
    )

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
    val killSwitchEnabled by viewModel.killSwitchEnabled.collectAsStateWithLifecycle()
    val fakeIpEnabled by viewModel.fakeIpEnabled.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
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
                    icon = { Icon(Icons.Default.Security, contentDescription = null) },
                    label = {
                        Column {
                            Text(stringResource(R.string.kill_switch))
                            Text(stringResource(R.string.kill_switch_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    badge = {
                        Switch(checked = killSwitchEnabled, onCheckedChange = null)
                    },
                    selected = false,
                    onClick = { viewModel.setKillSwitch(!killSwitchEnabled) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dns, contentDescription = null) },
                    label = {
                        Column {
                            Text(stringResource(R.string.fake_ip_dns))
                            Text(stringResource(R.string.fake_ip_dns_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    badge = {
                        Switch(checked = fakeIpEnabled, onCheckedChange = null)
                    },
                    selected = false,
                    onClick = { viewModel.setFakeIp(!fakeIpEnabled) },
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
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.drawer_menu))
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
            },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SmallFloatingActionButton(
                    onClick = onLogClick,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.view_logs)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FloatingActionButton(onClick = onServerManagementClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_server)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        if (vpnState == VpnState.CONNECTED) {
                            val currentRadius = (size.width / 2f) + (60.dp.toPx() * haloAnim)
                            val alpha = (1f - haloAnim) * 0.4f
                            drawCircle(
                                color = Color(0xFF4CAF50).copy(alpha = alpha),
                                radius = currentRadius
                            )
                        }
                    }
                    .clip(CircleShape)
                    .background(buttonBgColor)
                    .clickable {
                        when {
                            selectedServer == null -> {
                                viewModel.reportNoServerSelected()
                            }
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
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = stringResource(R.string.connect),
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = when (vpnState) {
                    VpnState.DISCONNECTED -> stringResource(R.string.status_disconnected)
                    VpnState.CONNECTING -> stringResource(R.string.status_connecting)
                    VpnState.CONNECTED -> stringResource(R.string.status_connected)
                    VpnState.ERROR -> stringResource(R.string.status_error)
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = buttonBgColor
            )

            if (vpnState == VpnState.ERROR && !errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.select_server),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (servers.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = stringResource(R.string.no_servers),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(servers) { server ->
                        ServerItem(
                            server = server,
                            isSelected = server.id == selectedServer?.id,
                            onClick = { viewModel.selectServer(server) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun ServerItem(server: ServerEntity, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = server.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${server.ip}:${server.port}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
