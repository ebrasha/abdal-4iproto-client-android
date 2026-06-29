/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : AppViewModel.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:02:25
 * Description : ViewModel for server list, VPN connect/disconnect, and dashboard runtime state.
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
import com.example.util.ConnectionTimerController
import com.example.util.ConnectionTimerState
import com.example.util.LatencyMeasurer
import com.example.util.LatencyState
import com.example.util.PortParser
import com.example.util.PingIntervalConfig
import com.example.util.SoundManager
import com.example.util.TrafficStatsState
import com.example.util.TunnelLogger
import com.example.util.TunnelStatsMonitor
import com.example.vpn.AbdalVpnService
import com.example.vpn.VpnState
import com.example.vpn.VpnStateNotifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ServerRepository
    val allServers: StateFlow<List<ServerEntity>>

    private val _selectedServer = MutableStateFlow<ServerEntity?>(null)
    val selectedServer: StateFlow<ServerEntity?> = _selectedServer.asStateFlow()

    fun selectServer(server: ServerEntity?) {
        _selectedServer.value = server
        if (server == null) {
            _activeSessionPort.value = null
            latencyMeasurer.updateTarget(null, 22)
        } else {
            refreshDisplayPortIfIdle(server)
        }
    }

    val vpnState: StateFlow<VpnState> = VpnStateNotifier.vpnState
    val errorMessage: StateFlow<String?> = VpnStateNotifier.errorMessage

    private val prefs = application.getSharedPreferences("abdal_vpn_prefs", Context.MODE_PRIVATE)

    private val _killSwitchEnabled = MutableStateFlow(prefs.getBoolean("kill_switch", true))
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()
    fun setKillSwitch(enabled: Boolean) {
        _killSwitchEnabled.value = enabled
        prefs.edit().putBoolean("kill_switch", enabled).apply()
        SoundManager.playSwitch()
    }

    private val _fakeIpEnabled = MutableStateFlow(prefs.getBoolean("fake_ip", true))
    val fakeIpEnabled: StateFlow<Boolean> = _fakeIpEnabled.asStateFlow()
    fun setFakeIp(enabled: Boolean) {
        _fakeIpEnabled.value = enabled
        prefs.edit().putBoolean("fake_ip", enabled).apply()
        SoundManager.playSwitch()
    }

    private val _whitelistIps = MutableStateFlow(prefs.getString("whitelist_ips", "") ?: "")
    val whitelistIps: StateFlow<String> = _whitelistIps.asStateFlow()
    fun setWhitelistIps(ips: String) {
        _whitelistIps.value = ips
        prefs.edit().putString("whitelist_ips", ips).apply()
    }

    private val _pingIntervalSeconds = MutableStateFlow(
        PingIntervalConfig.coerce(
            prefs.getInt(PingIntervalConfig.PREFS_KEY, PingIntervalConfig.DEFAULT_SECONDS)
        )
    )
    val pingIntervalSeconds: StateFlow<Int> = _pingIntervalSeconds.asStateFlow()

    fun setPingIntervalSeconds(seconds: Int) {
        val clamped = PingIntervalConfig.coerce(seconds)
        _pingIntervalSeconds.value = clamped
        prefs.edit().putInt(PingIntervalConfig.PREFS_KEY, clamped).apply()
        latencyMeasurer.setMeasureIntervalSeconds(clamped)
    }

    private val _pingMeasurementEnabled = MutableStateFlow(
        prefs.getBoolean(PingIntervalConfig.PREFS_KEY_ENABLED, true)
    )
    val pingMeasurementEnabled: StateFlow<Boolean> = _pingMeasurementEnabled.asStateFlow()

    fun setPingMeasurementEnabled(enabled: Boolean) {
        _pingMeasurementEnabled.value = enabled
        prefs.edit().putBoolean(PingIntervalConfig.PREFS_KEY_ENABLED, enabled).apply()
        latencyMeasurer.setEnabled(enabled)
        if (enabled) {
            refreshLatencyTarget(_selectedServer.value)
        }
        SoundManager.playSwitch()
    }

    private val _loggingEnabled = MutableStateFlow(
        prefs.getBoolean(TunnelLogger.PREFS_KEY_LOGGING_ENABLED, false)
    )
    val loggingEnabled: StateFlow<Boolean> = _loggingEnabled.asStateFlow()

    fun setLoggingEnabled(enabled: Boolean) {
        _loggingEnabled.value = enabled
        prefs.edit().putBoolean(TunnelLogger.PREFS_KEY_LOGGING_ENABLED, enabled).apply()
        TunnelLogger.setEnabled(enabled)
        SoundManager.playSwitch()
    }

    private val connectionTimerController = ConnectionTimerController()
    val connectionTimer: StateFlow<ConnectionTimerState> = connectionTimerController.state

    private val tunnelStatsMonitor = TunnelStatsMonitor(viewModelScope, application)
    val trafficStats: StateFlow<TrafficStatsState> = tunnelStatsMonitor.state

    private val latencyMeasurer = LatencyMeasurer(viewModelScope)
    val latencyState: StateFlow<LatencyState> = latencyMeasurer.state

    private val _activeSessionPort = MutableStateFlow<Int?>(null)
    val activeSessionPort: StateFlow<Int?> = _activeSessionPort.asStateFlow()

    private val _portModeRevision = MutableStateFlow(0)
    val portModeRevision: StateFlow<Int> = _portModeRevision.asStateFlow()

    init {
        SoundManager.init(application)
        TunnelLogger.setEnabled(_loggingEnabled.value)
        latencyMeasurer.setMeasureIntervalSeconds(_pingIntervalSeconds.value)
        latencyMeasurer.setEnabled(_pingMeasurementEnabled.value)
        val serverDao = AppDatabase.getDatabase(application).serverDao()
        repository = ServerRepository(serverDao)
        allServers = repository.allServers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            vpnState.collect { state ->
                connectionTimerController.onVpnStateChanged(state)
                tunnelStatsMonitor.onVpnStateChanged(state)
                if (state == VpnState.DISCONNECTED) {
                    selectedServer.value?.let { refreshLatencyTarget(it) }
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(1_000L)
                connectionTimerController.tickSecond()
            }
        }

        viewModelScope.launch {
            allServers.collect { servers ->
                val current = _selectedServer.value
                if (current == null) {
                    return@collect
                }
                val updated = servers.find { it.id == current.id }
                if (updated == null) {
                    _selectedServer.value = null
                    _activeSessionPort.value = null
                    latencyMeasurer.updateTarget(null, 22)
                } else if (updated != current) {
                    _selectedServer.value = updated
                    refreshDisplayPortIfIdle(updated)
                    refreshLatencyTarget(updated)
                }
            }
        }

        viewModelScope.launch {
            selectedServer.collect { server ->
                refreshLatencyTarget(server)
            }
        }
    }

    private fun refreshLatencyTarget(server: ServerEntity?) {
        val port = server?.let { resolvePortForDisplay(it) } ?: 22
        latencyMeasurer.updateTarget(server?.ip, port)
    }

    fun addServer(
        name: String,
        ip: String,
        ports: String,
        countryCode: String,
        user: String,
        pass: String
    ) {
        viewModelScope.launch {
            repository.insert(
                ServerEntity(
                    name = name,
                    ip = ip,
                    ports = ports,
                    countryCode = countryCode,
                    username = user,
                    password = pass
                )
            )
        }
    }

    fun updateServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.update(server)
            if (_selectedServer.value?.id == server.id) {
                _selectedServer.value = server
                refreshDisplayPortIfIdle(server)
                refreshLatencyTarget(server)
            }
        }
    }

    fun deleteServer(server: ServerEntity) {
        viewModelScope.launch {
            repository.delete(server)
        }
    }

    fun isRandomPortMode(serverId: Int): Boolean =
        prefs.getBoolean(portModeKey(serverId), false)

    fun setRandomPortMode(serverId: Int, random: Boolean) {
        prefs.edit().putBoolean(portModeKey(serverId), random).apply()
        _portModeRevision.value += 1
        SoundManager.playSwitch()
    }

    fun toggleRandomPortMode(serverId: Int) {
        if (isRandomPortMode(serverId)) {
            setRandomPortMode(serverId, false)
        } else {
            setRandomPortMode(serverId, true)
        }
    }

    /** Clears the live session port when idle; random pick happens only on connect. */
    private fun refreshDisplayPortIfIdle(server: ServerEntity) {
        if (vpnState.value == VpnState.CONNECTED || vpnState.value == VpnState.CONNECTING) {
            return
        }
        _activeSessionPort.value = null
        refreshLatencyTarget(server)
    }

    fun resolvePortForConnect(server: ServerEntity): Int {
        val port = if (isRandomPortMode(server.id)) {
            PortParser.pickRandom(server.ports)
        } else {
            PortParser.pickFixed(server.ports)
        }
        _activeSessionPort.value = port
        return port
    }

    fun resolvePortForDisplay(server: ServerEntity): Int =
        _activeSessionPort.value ?: PortParser.pickFixed(server.ports)

    fun reportVpnPermissionDenied() {
        VpnStateNotifier.updateState(VpnState.ERROR, "VPN permission was denied")
    }

    fun reportNoServerSelected() {
        VpnStateNotifier.updateState(VpnState.ERROR, "Select a server before connecting")
    }

    fun toggleVpn(context: Context, server: ServerEntity) {
        if (vpnState.value == VpnState.CONNECTED || vpnState.value == VpnState.CONNECTING) {
            SoundManager.playDisconnect()
            connectionTimerController.onUserDisconnectRequested()
            val intent = Intent(context, AbdalVpnService::class.java).apply {
                action = AbdalVpnService.ACTION_DISCONNECT
            }
            context.startService(intent)
        } else {
            SoundManager.playStart()
            connectionTimerController.onUserConnectRequested()
            val port = resolvePortForConnect(server)
            val intent = Intent(context, AbdalVpnService::class.java).apply {
                action = AbdalVpnService.ACTION_CONNECT
                putExtra(AbdalVpnService.EXTRA_IP, server.ip)
                putExtra(AbdalVpnService.EXTRA_PORT, port)
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

    private fun portModeKey(serverId: Int): String = "port_mode_random_$serverId"
}
