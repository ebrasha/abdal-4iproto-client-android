package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.UdpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UdpTunnelScreen(
    viewModel: UdpViewModel,
    onBackClick: () -> Unit
) {
    val udpTunnelEnabled by viewModel.udpTunnelEnabled.collectAsStateWithLifecycle()
    val keepaliveIntervalFlow by viewModel.keepaliveInterval.collectAsStateWithLifecycle()
    val socketBufferFlow by viewModel.socketBuffer.collectAsStateWithLifecycle()
    val readBufferSizeFlow by viewModel.readBufferSize.collectAsStateWithLifecycle()
    val bindAddressFlow by viewModel.bindAddress.collectAsStateWithLifecycle()
    val localUdpPortFlow by viewModel.localUdpPort.collectAsStateWithLifecycle()
    val targetHostFlow by viewModel.targetHost.collectAsStateWithLifecycle()
    val targetPortFlow by viewModel.targetPort.collectAsStateWithLifecycle()

    var keepalive by remember(keepaliveIntervalFlow) { mutableStateOf(keepaliveIntervalFlow) }
    var socketBuf by remember(socketBufferFlow) { mutableStateOf(socketBufferFlow) }
    var readBuf by remember(readBufferSizeFlow) { mutableStateOf(readBufferSizeFlow) }
    var bindAddr by remember(bindAddressFlow) { mutableStateOf(bindAddressFlow) }
    var localPort by remember(localUdpPortFlow) { mutableStateOf(localUdpPortFlow) }
    var targetHost by remember(targetHostFlow) { mutableStateOf(targetHostFlow) }
    var targetPort by remember(targetPortFlow) { mutableStateOf(targetPortFlow) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.udp_tunnel)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.udp_tunnel),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = stringResource(R.string.udp_tunnel_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = udpTunnelEnabled,
                            onCheckedChange = { viewModel.setUdpTunnelEnabled(it) }
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = keepalive,
                    onValueChange = { keepalive = it },
                    label = { Text(stringResource(R.string.keepalive_interval)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = socketBuf,
                    onValueChange = { socketBuf = it },
                    label = { Text(stringResource(R.string.socket_buffer)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = readBuf,
                    onValueChange = { readBuf = it },
                    label = { Text(stringResource(R.string.read_buffer_size)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bindAddr,
                    onValueChange = { bindAddr = it },
                    label = { Text(stringResource(R.string.bind_address)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = localPort,
                    onValueChange = { localPort = it },
                    label = { Text(stringResource(R.string.local_udp_port)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = targetHost,
                    onValueChange = { targetHost = it },
                    label = { Text(stringResource(R.string.target_host)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = targetPort,
                    onValueChange = { targetPort = it },
                    label = { Text(stringResource(R.string.target_port)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.saveSettings(
                            keepalive.trim(),
                            socketBuf.trim(),
                            readBuf.trim(),
                            bindAddr.trim(),
                            localPort.trim(),
                            targetHost.trim(),
                            targetPort.trim()
                        )
                        Toast.makeText(context, context.getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(stringResource(R.string.save_settings))
                }
            }
        }
    }
}
