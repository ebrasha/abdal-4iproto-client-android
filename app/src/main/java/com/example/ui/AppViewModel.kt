/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : AppViewModel.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:02:25
 * Description : ViewModel for server list and VPN connect/disconnect actions.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ServerEntity
import com.example.data.ServerRepository
import com.example.util.SoundManager
import com.example.vpn.AbdalVpnService
import com.example.vpn.VpnState
import com.example.vpn.VpnStateNotifier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServerRepository
    val allServers: StateFlow<List<ServerEntity>>

    private val _selectedServer = kotlinx.coroutines.flow.MutableStateFlow<ServerEntity?>(null)
    val selectedServer: StateFlow<ServerEntity?> = _selectedServer
    fun selectServer(server: ServerEntity?) { _selectedServer.value = server }

    val vpnState: StateFlow<VpnState> = VpnStateNotifier.vpnState
    val errorMessage: StateFlow<String?> = VpnStateNotifier.errorMessage

    private val prefs = application.getSharedPreferences("abdal_vpn_prefs", Context.MODE_PRIVATE)

    private val _killSwitchEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("kill_switch", false))
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled
    fun setKillSwitch(enabled: Boolean) { 
        _killSwitchEnabled.value = enabled
        prefs.edit().putBoolean("kill_switch", enabled).apply()
        SoundManager.playSwitch()
    }

    private val _fakeIpEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("fake_ip", false))
    val fakeIpEnabled: StateFlow<Boolean> = _fakeIpEnabled
    fun setFakeIp(enabled: Boolean) { 
        _fakeIpEnabled.value = enabled
        prefs.edit().putBoolean("fake_ip", enabled).apply()
        SoundManager.playSwitch()
    }

    private val _whitelistIps = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("whitelist_ips", "") ?: "")
    val whitelistIps: StateFlow<String> = _whitelistIps
    fun setWhitelistIps(ips: String) { 
        _whitelistIps.value = ips
        prefs.edit().putString("whitelist_ips", ips).apply()
    }

    init {
        SoundManager.init(application)
        val serverDao = AppDatabase.getDatabase(application).serverDao()
        repository = ServerRepository(serverDao)
        allServers = repository.allServers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addServer(name: String, ip: String, port: Int, user: String, pass: String) {
        viewModelScope.launch {
            repository.insert(ServerEntity(name = name, ip = ip, port = port, username = user, password = pass))
        }
    }

    fun updateServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.update(server)
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.delete(server)
        }
    }

    fun reportVpnPermissionDenied() {
        VpnStateNotifier.updateState(VpnState.ERROR, "VPN permission was denied")
    }

    fun reportNoServerSelected() {
        VpnStateNotifier.updateState(VpnState.ERROR, "Select a server before connecting")
    }

    fun toggleVpn(context: Context, server: ServerEntity) {
        if (vpnState.value == VpnState.CONNECTED || vpnState.value == VpnState.CONNECTING) {
            SoundManager.playDisconnect()
            val intent = Intent(context, AbdalVpnService::class.java).apply {
                action = AbdalVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
        } else {
            SoundManager.playStart()
            val intent = Intent(context, AbdalVpnService::class.java).apply {
                action = AbdalVpnService.ACTION_CONNECT
                putExtra(AbdalVpnService.EXTRA_IP, server.ip)
                putExtra(AbdalVpnService.EXTRA_PORT, server.port)
                putExtra(AbdalVpnService.EXTRA_USERNAME, server.username)
                putExtra(AbdalVpnService.EXTRA_PASSWORD, server.password)
                putExtra(AbdalVpnService.EXTRA_FAKE_IP, _fakeIpEnabled.value)
                putExtra(AbdalVpnService.EXTRA_KILL_SWITCH, _killSwitchEnabled.value)
                putExtra(AbdalVpnService.EXTRA_WHITELIST, _whitelistIps.value)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
