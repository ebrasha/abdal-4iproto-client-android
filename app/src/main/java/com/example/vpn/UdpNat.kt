/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : UdpNat.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-07 08:14:14
 * Description : Transparent UDP NAT that carries arbitrary UDP flows over the Abdal 4iProto direct-udpip
 *               protocol. Scoped to a single SOCKS5 UDP-ASSOCIATE relay socket, it maps each
 *               (client, destination) pair to a dedicated direct-udpip SSH channel, frames datagrams in
 *               both directions, and reclaims idle flows. This is what makes all Android UDP traffic
 *               (QUIC/HTTP3, games, VoIP, etc.) traverse the tunnel, not just DNS.
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
import java.net.SocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages transparent UDP forwarding for one SOCKS5 UDP association.
 *
 * @param session       the established SSH session used to open direct-udpip channels.
 * @param relay         the loopback relay socket SOCKS responses are sent back through.
 * @param onSessionDown invoked once if the SSH session is detected as down.
 */
class UdpNat(
    private val session: Session,
    private val relay: DatagramSocket,
    private val onSessionDown: (Throwable) -> Unit = {}
) {

    /** Identifies a UDP flow by its local source endpoint and original destination. */
    private data class FlowKey(val client: SocketAddress, val host: String, val port: Int)

    /**
     * A single tunneled UDP flow: one direct-udpip channel dedicated to a (client, destination) pair.
     * [replyHeader] is the exact SOCKS5 UDP request header echoed in front of every response so the
     * SOCKS client (hev-socks5-tunnel) can match the datagram back to the right socket.
     */
    private inner class Flow(
        val channel: Channel,
        val channelOut: OutputStream,
        val client: SocketAddress,
        val replyHeader: ByteArray
    ) {
        val writeLock = Any()
        @Volatile
        var lastSeen: Long = System.currentTimeMillis()
        lateinit var reader: Thread
    }

    private val flows = ConcurrentHashMap<FlowKey, Flow>()
    private val closed = AtomicBoolean(false)

    private val sweeper = Thread({ sweepLoop() }, "abdal-udp-nat-sweeper").apply {
        isDaemon = true
        start()
    }

    /**
     * Forwards one client datagram to its destination, opening a direct-udpip channel on first sight.
     *
     * @param client       the local UDP source (where responses must be returned).
     * @param replyHeader  the SOCKS5 UDP header bytes to prepend on responses (encodes original dest).
     * @param originalHost the destination address as seen by the client (may be a fake IP).
     * @param resolvedHost the address announced to the server (real domain when fake-IP is active).
     * @param port         the destination UDP port.
     * @param payload      the UDP payload bytes.
     */
    fun forward(
        client: SocketAddress,
        replyHeader: ByteArray,
        originalHost: String,
        resolvedHost: String,
        port: Int,
        payload: ByteArray
    ) {
        if (closed.get() || payload.isEmpty()) {
            return
        }
        val key = FlowKey(client, originalHost, port)
        val existing = flows[key]
        val flow = existing ?: openFlow(key, client, replyHeader, resolvedHost, port) ?: return
        flow.lastSeen = System.currentTimeMillis()
        try {
            UdpFrame.write(flow.channelOut, flow.writeLock, payload, 0, payload.size)
        } catch (e: IOException) {
            reportFailureIfSessionDown(e)
            closeFlow(key, flow)
        }
    }

    /** Opens, registers and starts a new flow, or returns null when the limit is hit or open fails. */
    @Synchronized
    private fun openFlow(
        key: FlowKey,
        client: SocketAddress,
        replyHeader: ByteArray,
        resolvedHost: String,
        port: Int
    ): Flow? {
        // Re-check under the lock in case a concurrent caller created the flow first.
        flows[key]?.let { return it }
        if (closed.get()) {
            return null
        }
        if (flows.size >= MAX_FLOWS) {
            // Protect against runaway channel/thread growth; drop until idle flows are reclaimed.
            TunnelLogger.warn(TAG, "UDP NAT flow limit reached ($MAX_FLOWS); dropping datagram to $resolvedHost:$port")
            return null
        }
        if (!session.isConnected) {
            reportFailureIfSessionDown(IOException("SSH session is down"))
            return null
        }
        return try {
            val channel = AbdalUdpChannelFactory.openDirectUdpIp(session, resolvedHost, port)
            channel.connect(CHANNEL_CONNECT_TIMEOUT_MS)
            val flow = Flow(channel, channel.outputStream, client, replyHeader)
            flow.reader = Thread({ pumpChannelToClient(key, flow) }, "abdal-udp-nat-rx").apply {
                isDaemon = true
            }
            flows[key] = flow
            flow.reader.start()
            flow
        } catch (e: Exception) {
            reportFailureIfSessionDown(e)
            TunnelLogger.warn(TAG, "Failed to open UDP flow to $resolvedHost:$port", e)
            null
        }
    }

    /** Reads framed datagrams from the channel and returns them to the client with the SOCKS header. */
    private fun pumpChannelToClient(key: FlowKey, flow: Flow) {
        val input = try {
            DataInputStream(flow.channel.inputStream)
        } catch (e: IOException) {
            closeFlow(key, flow)
            return
        }
        while (!closed.get()) {
            try {
                val payload = UdpFrame.read(input)
                if (payload.isEmpty()) {
                    // Keepalive frame from the server; nothing to deliver.
                    continue
                }
                flow.lastSeen = System.currentTimeMillis()
                if (relay.isClosed) {
                    break
                }
                val reply = ByteArray(flow.replyHeader.size + payload.size)
                System.arraycopy(flow.replyHeader, 0, reply, 0, flow.replyHeader.size)
                System.arraycopy(payload, 0, reply, flow.replyHeader.size, payload.size)
                relay.send(DatagramPacket(reply, reply.size, flow.client))
            } catch (e: IOException) {
                if (!closed.get()) {
                    reportFailureIfSessionDown(e)
                }
                break
            }
        }
        closeFlow(key, flow)
    }

    /** Periodically reclaims flows that have seen no traffic within [IDLE_TIMEOUT_MS]. */
    private fun sweepLoop() {
        while (!closed.get()) {
            try {
                Thread.sleep(SWEEP_INTERVAL_MS)
            } catch (_: InterruptedException) {
                return
            }
            val now = System.currentTimeMillis()
            for ((key, flow) in flows) {
                if (now - flow.lastSeen >= IDLE_TIMEOUT_MS) {
                    closeFlow(key, flow)
                }
            }
        }
    }

    private fun closeFlow(key: FlowKey, flow: Flow) {
        // Only the owner of the map entry tears it down, so concurrent paths don't double-close.
        if (!flows.remove(key, flow)) {
            return
        }
        try {
            flow.channel.disconnect()
        } catch (_: Exception) {
        }
    }

    /** Stops the NAT, closing the sweeper and every active flow/channel. */
    fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        sweeper.interrupt()
        val snapshot = flows.entries.toList()
        flows.clear()
        for ((_, flow) in snapshot) {
            try {
                flow.channel.disconnect()
            } catch (_: Exception) {
            }
            flow.reader.interrupt()
        }
    }

    private fun reportFailureIfSessionDown(failure: Throwable) {
        if (!session.isConnected) {
            onSessionDown(failure)
        }
    }

    companion object {
        private const val TAG = "UdpNat"
        private const val CHANNEL_CONNECT_TIMEOUT_MS = 15_000

        // UDP NAT session tuning. Flows idle longer than the timeout are reclaimed.
        private const val IDLE_TIMEOUT_MS = 60_000L
        private const val SWEEP_INTERVAL_MS = 15_000L
        private const val MAX_FLOWS = 512
    }
}
