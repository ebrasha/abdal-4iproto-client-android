package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UdpViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("abdal_vpn_prefs", Context.MODE_PRIVATE)

    private val _udpTunnelEnabled = MutableStateFlow(prefs.getBoolean("udp_tunnel_enabled", true))
    val udpTunnelEnabled: StateFlow<Boolean> = _udpTunnelEnabled

    private val _keepaliveInterval = MutableStateFlow(prefs.getString("udp_keepalive_interval", "30") ?: "30")
    val keepaliveInterval: StateFlow<String> = _keepaliveInterval

    private val _socketBuffer = MutableStateFlow(prefs.getString("udp_socket_buffer", "4096") ?: "4096")
    val socketBuffer: StateFlow<String> = _socketBuffer

    private val _readBufferSize = MutableStateFlow(prefs.getString("udp_read_buffer_size", "65535") ?: "65535")
    val readBufferSize: StateFlow<String> = _readBufferSize

    private val _bindAddress = MutableStateFlow(prefs.getString("udp_bind_address", "127.0.0.1") ?: "127.0.0.1")
    val bindAddress: StateFlow<String> = _bindAddress

    private val _localUdpPort = MutableStateFlow(prefs.getString("udp_local_port", "0") ?: "0")
    val localUdpPort: StateFlow<String> = _localUdpPort

    private val _targetHost = MutableStateFlow(prefs.getString("udp_target_host", "8.8.8.8") ?: "8.8.8.8")
    val targetHost: StateFlow<String> = _targetHost

    private val _targetPort = MutableStateFlow(prefs.getString("udp_target_port", "53") ?: "53")
    val targetPort: StateFlow<String> = _targetPort

    fun setUdpTunnelEnabled(enabled: Boolean) {
        _udpTunnelEnabled.value = enabled
        prefs.edit().putBoolean("udp_tunnel_enabled", enabled).apply()
    }

    fun saveSettings(
        keepalive: String,
        socketBuf: String,
        readBuf: String,
        bindAddr: String,
        localPort: String,
        targetHostInput: String,
        targetPortInput: String
    ) {
        _keepaliveInterval.value = keepalive
        _socketBuffer.value = socketBuf
        _readBufferSize.value = readBuf
        _bindAddress.value = bindAddr
        _localUdpPort.value = localPort
        _targetHost.value = targetHostInput
        _targetPort.value = targetPortInput

        prefs.edit()
            .putString("udp_keepalive_interval", keepalive)
            .putString("udp_socket_buffer", socketBuf)
            .putString("udp_read_buffer_size", readBuf)
            .putString("udp_bind_address", bindAddr)
            .putString("udp_local_port", localPort)
            .putString("udp_target_host", targetHostInput)
            .putString("udp_target_port", targetPortInput)
            .apply()
    }
}
