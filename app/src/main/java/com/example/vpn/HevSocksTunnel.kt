/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : HevSocksTunnel.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:59:00
 * Description : Writes the hev-socks5-tunnel YAML config and routes TUN traffic through the local SSH SOCKS proxy.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import android.content.Context
import android.os.ParcelFileDescriptor
import com.example.util.TunnelLogger
import hev.sockstun.TProxyService
import java.io.File
import java.io.FileOutputStream

/**
 * Bridges the VPN TUN interface to the local SOCKS5 proxy on 127.0.0.1 using the native hev-socks5-tunnel engine.
 */
object HevSocksTunnel {

    private const val TAG = "HevSocksTunnel"
    private const val TUN_MTU = 1500

    /**
     * Indicates whether the native library was loaded successfully.
     */
    fun isLibraryAvailable(): Boolean = TProxyService.libraryLoaded()

    /**
     * Writes the engine config and launches the native tunnel on a dedicated thread.
     * The native call blocks for the lifetime of the tunnel, so any failure is reported via the logger.
     */
    fun start(context: Context, tunInterface: ParcelFileDescriptor, socksPort: Int) {
        val configFile = File(context.cacheDir, "hev-tunnel.yml")
        val yaml = buildString {
            appendLine("misc:")
            appendLine("  task-stack-size: 81920")
            appendLine("tunnel:")
            appendLine("  mtu: $TUN_MTU")
            appendLine("socks5:")
            appendLine("  port: $socksPort")
            appendLine("  address: '127.0.0.1'")
            appendLine("  udp: 'udp'")
        }
        FileOutputStream(configFile, false).use { it.write(yaml.toByteArray()) }
        TunnelLogger.info(TAG, "Engine config written to ${configFile.absolutePath}")

        Thread(
            {
                try {
                    TProxyService.TProxyStartService(configFile.absolutePath, tunInterface.fd)
                    TunnelLogger.info(TAG, "Tunnel engine thread exited")
                } catch (t: Throwable) {
                    TunnelLogger.error(TAG, "Tunnel engine crashed", t)
                    VpnStateNotifier.updateState(VpnState.ERROR, "Tunnel engine failed to start")
                }
            },
            "hev-socks5-tunnel"
        ).apply {
            isDaemon = true
            start()
        }
    }

    /**
     * Stops the native tunnel engine if it is running.
     */
    fun stop() {
        if (!TProxyService.libraryLoaded()) {
            return
        }
        try {
            TProxyService.TProxyStopService()
        } catch (t: Throwable) {
            TunnelLogger.warn(TAG, "Error while stopping tunnel engine", t)
        }
    }
}
