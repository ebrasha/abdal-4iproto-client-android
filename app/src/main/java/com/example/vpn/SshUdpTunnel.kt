/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : SshUdpTunnel.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-07 07:21:20
 * Description : Full UDP-over-SSH tunnel for the Abdal 4iProto proprietary protocol. Forwards datagrams
 *               between a local UDP socket and a single "direct-udpip" SSH channel. Each datagram is
 *               carried inside the channel stream framed as [2-byte big-endian length][payload]; a frame
 *               of length 0 is a keepalive. The wire constants are fixed by the server protocol and are
 *               never user-configurable.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import com.example.util.TunnelLogger
import com.jcraft.jsch.AbdalUdpChannelFactory
import com.jcraft.jsch.Channel
import com.jcraft.jsch.Session
import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bridges a local UDP endpoint to a remote UDP destination through a proprietary Abdal 4iProto
 * {@code direct-udpip} SSH channel. Datagrams sent by local applications to the bound address are
 * framed and pushed through the SSH channel; framed datagrams arriving from the channel are delivered
 * back to the most recent local sender.
 *
 * @param session       an established SSH session used to open the channel.
 * @param config        validated local tunnel settings.
 * @param onSessionDown invoked once if the underlying SSH session is detected as down.
 */
class SshUdpTunnel(
    private val session: Session,
    private val config: UdpTunnelConfig,
    private val onSessionDown: (Throwable) -> Unit = {}
) {

    private val running = AtomicBoolean(false)
    private val sessionFailureReported = AtomicBoolean(false)

    // Serialises writes to the channel so datagram frames and keepalive frames never interleave.
    private val writeLock = Any()

    @Volatile
    private var channel: Channel? = null

    @Volatile
    private var channelOut: OutputStream? = null

    @Volatile
    private var localSocket: DatagramSocket? = null

    // Most recent local sender; channel responses are returned to this endpoint (typical single-flow UDP).
    @Volatile
    private var lastClient: SocketAddress? = null

    private val workers = mutableListOf<Thread>()

    /**
     * Opens the channel, binds the local UDP socket and starts the forwarding/keepalive threads.
     * @throws Exception if the channel or local socket cannot be established.
     */
    fun start() {
        if (!running.compareAndSet(false, true)) {
            return
        }
        try {
            openChannel()
            bindLocalSocket()
            startWorkers()
            TunnelLogger.info(
                TAG,
                "UDP tunnel up: local ${config.bindAddress}:${localSocket?.localPort} " +
                    "-> ${config.targetHost}:${config.targetPort} (keepalive ${config.keepaliveIntervalSeconds}s)"
            )
        } catch (e: Exception) {
            TunnelLogger.error(TAG, "Failed to start UDP tunnel", e)
            stop()
            throw e
        }
    }

    /** Stops forwarding and releases the channel, local socket and worker threads. */
    fun stop() {
        if (!running.getAndSet(false)) {
            // Still release resources in case start() failed midway.
        }
        try {
            localSocket?.close()
        } catch (_: Exception) {
        }
        try {
            channel?.disconnect()
        } catch (_: Exception) {
        }
        workers.forEach { it.interrupt() }
        workers.clear()
        localSocket = null
        channel = null
        channelOut = null
        lastClient = null
    }

    private fun openChannel() {
        if (!session.isConnected) {
            throw IOException("SSH session is down")
        }
        val opened = AbdalUdpChannelFactory.openDirectUdpIp(session, config.targetHost, config.targetPort)
        opened.connect(CHANNEL_CONNECT_TIMEOUT_MS)
        channel = opened
        channelOut = opened.outputStream
    }

    private fun bindLocalSocket() {
        val socket = DatagramSocket(null)
        socket.reuseAddress = true
        // Best-effort buffer sizing; the OS may clamp these to its own limits.
        try {
            socket.receiveBufferSize = config.socketBufferBytes
            socket.sendBufferSize = config.socketBufferBytes
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Unable to apply UDP socket buffer size", e)
        }
        socket.bind(InetSocketAddress(config.bindAddress, config.localUdpPort))
        localSocket = socket
    }

    private fun startWorkers() {
        val socket = localSocket ?: throw IOException("Local UDP socket is not bound")
        val channelIn = DataInputStream(channel?.inputStream ?: throw IOException("Channel input unavailable"))

        val localToChannel = Thread({ pumpLocalToChannel(socket) }, "abdal-udp-uplink").apply {
            isDaemon = true
        }
        val channelToLocal = Thread({ pumpChannelToLocal(channelIn, socket) }, "abdal-udp-downlink").apply {
            isDaemon = true
        }
        val keepalive = Thread({ keepaliveLoop() }, "abdal-udp-keepalive").apply {
            isDaemon = true
        }

        workers.addAll(listOf(localToChannel, channelToLocal, keepalive))
        workers.forEach { it.start() }
    }

    /** Reads datagrams from local apps and writes them, framed, into the SSH channel. */
    private fun pumpLocalToChannel(socket: DatagramSocket) {
        val buffer = ByteArray(config.readBufferSize)
        while (running.get() && !socket.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                lastClient = packet.socketAddress
                writeFrame(buffer, packet.offset, packet.length)
            } catch (e: IOException) {
                if (running.get()) {
                    reportFailureIfSessionDown(e)
                    TunnelLogger.warn(TAG, "UDP uplink stopped", e)
                }
                return
            }
        }
    }

    /** Reads framed datagrams from the SSH channel and delivers them to the last local sender. */
    private fun pumpChannelToLocal(channelIn: DataInputStream, socket: DatagramSocket) {
        while (running.get()) {
            try {
                val payload = UdpFrame.read(channelIn)
                if (payload.isEmpty()) {
                    // Keepalive frame from the server; nothing to deliver.
                    continue
                }
                val client = lastClient ?: continue
                if (!socket.isClosed) {
                    socket.send(DatagramPacket(payload, payload.size, client))
                }
            } catch (e: IOException) {
                if (running.get()) {
                    reportFailureIfSessionDown(e)
                    TunnelLogger.warn(TAG, "UDP downlink stopped", e)
                }
                return
            }
        }
    }

    /** Periodically writes a zero-length keepalive frame to keep the server-side tunnel fresh. */
    private fun keepaliveLoop() {
        val intervalMs = config.keepaliveIntervalSeconds.toLong() * 1000L
        while (running.get()) {
            try {
                Thread.sleep(intervalMs)
            } catch (_: InterruptedException) {
                return
            }
            if (!running.get()) {
                return
            }
            try {
                writeKeepalive()
            } catch (e: IOException) {
                if (running.get()) {
                    reportFailureIfSessionDown(e)
                    TunnelLogger.warn(TAG, "UDP keepalive failed", e)
                }
                return
            }
        }
    }

    /** Writes a single framed datagram through the shared codec. */
    private fun writeFrame(data: ByteArray, offset: Int, length: Int) {
        val out = channelOut ?: throw IOException("Channel output unavailable")
        UdpFrame.write(out, writeLock, data, offset, length)
    }

    /** Writes a zero-length keepalive frame through the shared codec. */
    private fun writeKeepalive() {
        val out = channelOut ?: throw IOException("Channel output unavailable")
        UdpFrame.writeKeepalive(out, writeLock)
    }

    private fun reportFailureIfSessionDown(failure: Throwable) {
        if (!session.isConnected && sessionFailureReported.compareAndSet(false, true)) {
            onSessionDown(failure)
        }
    }

    companion object {
        private const val TAG = "SshUdpTunnel"
        private const val CHANNEL_CONNECT_TIMEOUT_MS = 15_000
    }
}
