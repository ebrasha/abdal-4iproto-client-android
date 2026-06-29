/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : EditServerScreen.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-29 03:29:39
 * Description : Form screen to edit saved SSH server credentials with country flag and multi-port support.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.AppViewModel
import com.example.ui.components.CountryCodeField
import com.example.ui.util.resolveServerFormError
import com.example.util.CountryCatalog
import com.example.util.ServerFormErrors
import com.example.util.ServerFormValidator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditServerScreen(
    serverId: Int,
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val servers by viewModel.allServers.collectAsStateWithLifecycle()
    val server = servers.find { it.id == serverId }

    if (server == null) {
        return
    }

    var countryCode by remember(server) { mutableStateOf(server.countryCode) }
    var name by remember(server) { mutableStateOf(server.name) }
    var ip by remember(server) { mutableStateOf(server.ip) }
    var ports by remember(server) { mutableStateOf(server.ports) }
    var username by remember(server) { mutableStateOf(server.username) }
    var password by remember(server) { mutableStateOf(server.password) }
    var fieldErrors by remember(server) { mutableStateOf(ServerFormErrors()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_server)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CountryCodeField(
                countryCode = countryCode,
                onCountryCodeChange = { countryCode = it },
                isError = fieldErrors.countryCode != null,
                errorText = resolveServerFormError(fieldErrors.countryCode)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.server_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fieldErrors.name != null,
                supportingText = {
                    resolveServerFormError(fieldErrors.name)?.let { Text(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text(stringResource(R.string.server_ip)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fieldErrors.ip != null,
                supportingText = {
                    resolveServerFormError(fieldErrors.ip)?.let { Text(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ports,
                onValueChange = { ports = it },
                label = { Text(stringResource(R.string.ports_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = fieldErrors.ports != null,
                supportingText = {
                    resolveServerFormError(fieldErrors.ports)?.let { Text(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fieldErrors.username != null,
                supportingText = {
                    resolveServerFormError(fieldErrors.username)?.let { Text(it) }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = fieldErrors.password != null,
                supportingText = {
                    resolveServerFormError(fieldErrors.password)?.let { Text(it) }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val errors = ServerFormValidator.validate(
                        name = name,
                        countryCode = countryCode,
                        ip = ip,
                        ports = ports,
                        username = username,
                        password = password
                    )
                    fieldErrors = errors
                    if (!errors.hasErrors) {
                        viewModel.updateServer(
                            server.copy(
                                name = name.trim(),
                                ip = ip.trim(),
                                ports = ports.trim(),
                                countryCode = CountryCatalog.normalizeInput(countryCode),
                                username = username.trim(),
                                password = password
                            )
                        )
                        onBackClick()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.update_server))
            }
        }
    }
}
