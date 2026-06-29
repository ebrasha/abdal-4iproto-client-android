/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : ProtectedSocketFactory.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:02:25
 * Description : JSch socket factory that bypasses the VPN tunnel for the SSH control connection.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import android.net.VpnService
import com.example.util.TunnelLogger
import com.jcraft.jsch.SocketFactory
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Creates TCP sockets excluded from the VPN interface so the SSH session stays reachable.
 */
class ProtectedSocketFactory(
    private val vpnService: VpnService,
    private val connectTimeoutMs: Int = 20_000
) : SocketFactory {

    override fun createSocket(host: String, port: Int): Socket {
        val socket = Socket()
        socket.tcpNoDelay = true
        socket.reuseAddress = true

        // Bind first so Android has a concrete file descriptor to protect.
        // On some devices protect(Socket) returns false for a brand-new unbound socket.
        socket.bind(null)
        if (vpnService.protect(socket)) {
            TunnelLogger.info(TAG, "SSH control socket protected from VPN routing")
        } else {
            TunnelLogger.warn(
                TAG,
                "Could not protect SSH control socket; continuing because VPN is not established yet"
            )
        }
        socket.connect(InetSocketAddress(host, port), connectTimeoutMs)
        return socket
    }

    override fun getInputStream(socket: Socket): InputStream = socket.getInputStream()

    override fun getOutputStream(socket: Socket): OutputStream = socket.getOutputStream()

    companion object {
        private const val TAG = "ProtectedSocketFactory"
    }
}
