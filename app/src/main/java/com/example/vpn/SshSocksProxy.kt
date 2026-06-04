/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : SshSocksProxy.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:59:00
 * Description : Local SOCKS5 server that bridges incoming connections to SSH direct-tcpip channels (JSch),
 *               providing the dynamic forwarding that JSch itself does not implement. TCP is fully
 *               proxied; UDP ASSOCIATE is supported only for DNS (port 53) via DNS-over-TCP.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import com.example.util.TunnelLogger
import com.jcraft.jsch.Channel
import com.jcraft.jsch.Session
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * A minimal SOCKS5 proxy bound to the loopback interface. Every accepted connection is forwarded to the
 * remote destination through an SSH "direct-tcpip" channel, so all traffic leaves the device via the SSH server.
 */
class SshSocksProxy(
    private val session: Session,
    private val fakeDns: FakeDnsResolver? = null,
    private val onSessionFailure: (Throwable) -> Unit = {}
) {

    @Volatile
    private var serverSocket: ServerSocket? = null
    private val running = AtomicBoolean(false)
    private val sessionFailureReported = AtomicBoolean(false)
    private val lastFailureLogAt = AtomicLong(0)
    private val suppressedFailureLogs = AtomicInteger(0)

    /** The actual port the proxy is listening on (assigned by the OS). */
    var listenPort: Int = -1
        private set

    /**
     * Binds the proxy to 127.0.0.1 on an ephemeral port and starts accepting connections.
     * @return the bound port number.
     */
    fun start(): Int {
        val socket = ServerSocket()
        socket.reuseAddress = true
        socket.bind(InetSocketAddress(LOOPBACK, 0))
        serverSocket = socket
        listenPort = socket.localPort
        running.set(true)

        Thread({ acceptLoop(socket) }, "ssh-socks-accept").apply {
            isDaemon = true
            start()
        }
        TunnelLogger.info(TAG, "SOCKS5 proxy listening on $LOOPBACK:$listenPort")
        return listenPort
    }

    /**
     * Stops the proxy and releases the listening socket.
     */
    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            TunnelLogger.warn(TAG, "Error closing SOCKS server socket", e)
        }
        serverSocket = null
    }

    private fun acceptLoop(socket: ServerSocket) {
        while (running.get() && !socket.isClosed) {
            try {
                val client = socket.accept()
                Thread({ handleClient(client) }, "ssh-socks-conn").apply {
                    isDaemon = true
                    start()
                }
            } catch (e: IOException) {
                if (running.get()) {
                    TunnelLogger.warn(TAG, "Accept loop error", e)
                }
            }
        }
    }

    private fun handleClient(client: Socket) {
        try {
            client.tcpNoDelay = true
            client.soTimeout = HANDSHAKE_TIMEOUT_MS
            val input = DataInputStream(client.getInputStream())
            val output = client.getOutputStream()

            if (!performGreeting(input, output)) {
                client.close()
                return
            }

            // Request: VER, CMD, RSV, ATYP, DST.ADDR, DST.PORT
            val ver = input.read()
            val cmd = input.read()
            input.read() // RSV
            if (ver != SOCKS_VERSION) {
                replyFailure(output, REP_GENERAL_FAILURE)
                client.close()
                return
            }

            val atyp = input.read()
            val address = readAddress(input, atyp)
            val port = (input.read() shl 8) or input.read()

            when (cmd) {
                CMD_CONNECT -> handleConnect(client, input, output, address, port)
                CMD_UDP_ASSOCIATE -> handleUdpAssociate(client, output)
                else -> {
                    replyFailure(output, REP_COMMAND_NOT_SUPPORTED)
                    client.close()
                }
            }
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Client handling failed", e)
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun performGreeting(input: DataInputStream, output: OutputStream): Boolean {
        val version = input.read()
        if (version != SOCKS_VERSION) {
            return false
        }
        val methodCount = input.read()
        if (methodCount <= 0) {
            return false
        }
        val methods = ByteArray(methodCount)
        input.readFully(methods)
        // Always answer "no authentication required".
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), 0x00))
        output.flush()
        return true
    }

    private fun handleConnect(
        client: Socket,
        clientIn: InputStream,
        clientOut: OutputStream,
        host: String,
        port: Int
    ) {
        // In fake-IP mode the destination is a synthetic IP; resolve it back to the original domain so the SSH
        // server performs the real DNS resolution (remote DNS). Falls back to the literal address otherwise.
        val targetHost = fakeDns?.takeIf { it.isFakeIp(host) }?.lookup(host) ?: host

        val channel: Channel
        try {
            if (!session.isConnected) {
                val failure = IOException("SSH session is down")
                reportSessionFailure(failure)
                logRateLimitedFailure("SSH session unavailable; rejecting SOCKS request to $targetHost:$port", failure)
                replyFailure(clientOut, REP_HOST_UNREACHABLE)
                client.close()
                return
            }
            channel = session.getStreamForwarder(targetHost, port)
            channel.connect(CHANNEL_CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            if (!session.isConnected) {
                reportSessionFailure(e)
            }
            logRateLimitedFailure("Failed to open SSH channel to $targetHost:$port", e)
            replyFailure(clientOut, REP_HOST_UNREACHABLE)
            try {
                client.close()
            } catch (_: IOException) {
            }
            return
        }

        replySuccess(clientOut)
        // Once tunneling starts the connection may stay open for a long time.
        client.soTimeout = 0

        val channelIn = channel.inputStream
        val channelOut = channel.outputStream

        val upstream = Thread({ pump(clientIn, channelOut) }, "ssh-socks-up")
        upstream.isDaemon = true
        upstream.start()

        // Downstream pump runs on this thread.
        pump(channelIn, clientOut)

        try {
            upstream.join(500)
        } catch (_: InterruptedException) {
        }
        closeQuietly(channel)
        try {
            client.close()
        } catch (_: IOException) {
        }
    }

    /**
     * Minimal UDP ASSOCIATE support, used only to resolve DNS by tunneling queries over TCP (RFC 7766).
     * Non-DNS UDP datagrams are dropped, which is an accepted limitation of SSH based tunnels.
     */
    private fun handleUdpAssociate(client: Socket, clientOut: OutputStream) {
        val relay = DatagramSocket(InetSocketAddress(LOOPBACK, 0))
        val relayPort = relay.localPort

        // Reply with the relay endpoint the client should send datagrams to.
        val bnd = ByteArray(10)
        bnd[0] = SOCKS_VERSION.toByte()
        bnd[1] = REP_SUCCESS.toByte()
        bnd[2] = 0x00
        bnd[3] = ATYP_IPV4.toByte()
        bnd[4] = 127; bnd[5] = 0; bnd[6] = 0; bnd[7] = 1
        bnd[8] = ((relayPort shr 8) and 0xFF).toByte()
        bnd[9] = (relayPort and 0xFF).toByte()
        clientOut.write(bnd)
        clientOut.flush()

        val relayThread = Thread({ udpRelayLoop(relay) }, "ssh-socks-udp")
        relayThread.isDaemon = true
        relayThread.start()

        // Keep the control connection open; close the relay when the client disconnects.
        try {
            val controlIn = client.getInputStream()
            val scratch = ByteArray(256)
            while (controlIn.read(scratch) >= 0) {
                // Drain; presence of the TCP connection keeps the association alive.
            }
        } catch (_: IOException) {
        } finally {
            relay.close()
            try {
                client.close()
            } catch (_: IOException) {
            }
        }
    }

    private fun udpRelayLoop(relay: DatagramSocket) {
        val buffer = ByteArray(UDP_BUFFER_SIZE)
        while (!relay.isClosed) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                relay.receive(packet)
                handleUdpDatagram(relay, packet)
            } catch (e: IOException) {
                if (!relay.isClosed) {
                    TunnelLogger.warn(TAG, "UDP relay error", e)
                }
                return
            }
        }
    }

    private fun handleUdpDatagram(relay: DatagramSocket, packet: DatagramPacket) {
        val data = packet.data
        val length = packet.length
        if (length < 10) {
            return
        }
        // SOCKS5 UDP header: RSV(2) FRAG(1) ATYP(1) DST.ADDR DST.PORT(2)
        val atyp = data[3].toInt() and 0xFF
        var index = 4
        val destHost: String
        when (atyp) {
            ATYP_IPV4 -> {
                destHost = "${data[index].toInt() and 0xFF}.${data[index + 1].toInt() and 0xFF}." +
                    "${data[index + 2].toInt() and 0xFF}.${data[index + 3].toInt() and 0xFF}"
                index += 4
            }
            ATYP_DOMAIN -> {
                val len = data[index].toInt() and 0xFF
                index += 1
                destHost = String(data, index, len, Charsets.US_ASCII)
                index += len
            }
            ATYP_IPV6 -> {
                val raw = ByteArray(16)
                System.arraycopy(data, index, raw, 0, 16)
                destHost = InetAddress.getByAddress(raw).hostAddress ?: return
                index += 16
            }
            else -> return
        }
        val destPort = ((data[index].toInt() and 0xFF) shl 8) or (data[index + 1].toInt() and 0xFF)
        index += 2

        // Header bytes that must be echoed back to the client in front of the response payload.
        val header = data.copyOfRange(0, index)
        val payload = data.copyOfRange(index, length)

        if (destPort != DNS_PORT) {
            // Only DNS is tunneled over SSH; other UDP traffic is unsupported.
            return
        }

        // With fake-IP enabled, answer DNS locally (no real query leaves the device); fall back to DNS-over-TCP
        // when the query cannot be synthesized (e.g. unsupported record types).
        val response = if (fakeDns != null) {
            fakeDns.synthesize(payload) ?: resolveDnsOverTcp(destHost, payload)
        } else {
            resolveDnsOverTcp(destHost, payload)
        } ?: return
        val reply = ByteArray(header.size + response.size)
        System.arraycopy(header, 0, reply, 0, header.size)
        System.arraycopy(response, 0, reply, header.size, response.size)
        try {
            relay.send(DatagramPacket(reply, reply.size, packet.address, packet.port))
        } catch (e: IOException) {
            TunnelLogger.warn(TAG, "Failed to return DNS reply", e)
        }
    }

    private fun resolveDnsOverTcp(dnsHost: String, query: ByteArray): ByteArray? {
        var channel: Channel? = null
        return try {
            if (!session.isConnected) {
                val failure = IOException("SSH session is down")
                reportSessionFailure(failure)
                logRateLimitedFailure("SSH session unavailable; rejecting DNS query to $dnsHost", failure)
                return null
            }
            channel = session.getStreamForwarder(dnsHost, DNS_PORT)
            channel.connect(CHANNEL_CONNECT_TIMEOUT_MS)
            val out = channel.outputStream
            val input = DataInputStream(channel.inputStream)

            // DNS over TCP frames the message with a 2 byte big-endian length prefix.
            out.write((query.size shr 8) and 0xFF)
            out.write(query.size and 0xFF)
            out.write(query)
            out.flush()

            val high = input.read()
            val low = input.read()
            if (high < 0 || low < 0) {
                return null
            }
            val responseLength = (high shl 8) or low
            if (responseLength <= 0 || responseLength > UDP_BUFFER_SIZE) {
                return null
            }
            val response = ByteArray(responseLength)
            input.readFully(response)
            response
        } catch (e: Exception) {
            if (!session.isConnected) {
                reportSessionFailure(e)
            }
            logRateLimitedFailure("DNS-over-TCP query to $dnsHost failed", e)
            null
        } finally {
            closeQuietly(channel)
        }
    }

    private fun readAddress(input: DataInputStream, atyp: Int): String {
        return when (atyp) {
            ATYP_IPV4 -> {
                val raw = ByteArray(4)
                input.readFully(raw)
                "${raw[0].toInt() and 0xFF}.${raw[1].toInt() and 0xFF}." +
                    "${raw[2].toInt() and 0xFF}.${raw[3].toInt() and 0xFF}"
            }
            ATYP_DOMAIN -> {
                val len = input.read()
                val raw = ByteArray(len)
                input.readFully(raw)
                String(raw, Charsets.US_ASCII)
            }
            ATYP_IPV6 -> {
                val raw = ByteArray(16)
                input.readFully(raw)
                InetAddress.getByAddress(raw).hostAddress ?: throw IOException("Invalid IPv6 address")
            }
            else -> throw IOException("Unsupported SOCKS address type: $atyp")
        }
    }

    private fun replySuccess(output: OutputStream) {
        output.write(byteArrayOf(SOCKS_VERSION.toByte(), REP_SUCCESS.toByte(), 0x00, ATYP_IPV4.toByte(), 0, 0, 0, 0, 0, 0))
        output.flush()
    }

    private fun replyFailure(output: OutputStream, code: Int) {
        try {
            output.write(byteArrayOf(SOCKS_VERSION.toByte(), code.toByte(), 0x00, ATYP_IPV4.toByte(), 0, 0, 0, 0, 0, 0))
            output.flush()
        } catch (_: IOException) {
        }
    }

    private fun pump(source: InputStream, destination: OutputStream) {
        val buffer = ByteArray(TCP_BUFFER_SIZE)
        try {
            while (true) {
                val read = source.read(buffer)
                if (read < 0) {
                    break
                }
                destination.write(buffer, 0, read)
                destination.flush()
            }
        } catch (_: IOException) {
            // Either side closed; normal during teardown.
        }
    }

    private fun closeQuietly(channel: Channel?) {
        try {
            channel?.disconnect()
        } catch (_: Exception) {
        }
    }

    private fun reportSessionFailure(failure: Throwable) {
        if (sessionFailureReported.compareAndSet(false, true)) {
            onSessionFailure(failure)
        }
    }

    private fun logRateLimitedFailure(message: String, failure: Throwable) {
        val now = System.currentTimeMillis()
        val last = lastFailureLogAt.get()
        if (now - last >= FAILURE_LOG_INTERVAL_MS && lastFailureLogAt.compareAndSet(last, now)) {
            val suppressed = suppressedFailureLogs.getAndSet(0)
            val suffix = if (suppressed > 0) " (suppressed $suppressed similar failures)" else ""
            TunnelLogger.warn(TAG, "$message$suffix", failure)
        } else {
            suppressedFailureLogs.incrementAndGet()
        }
    }

    companion object {
        private const val TAG = "SshSocksProxy"
        private const val LOOPBACK = "127.0.0.1"

        private const val SOCKS_VERSION = 0x05
        private const val CMD_CONNECT = 0x01
        private const val CMD_UDP_ASSOCIATE = 0x03

        private const val ATYP_IPV4 = 0x01
        private const val ATYP_DOMAIN = 0x03
        private const val ATYP_IPV6 = 0x04

        private const val REP_SUCCESS = 0x00
        private const val REP_GENERAL_FAILURE = 0x01
        private const val REP_HOST_UNREACHABLE = 0x04
        private const val REP_COMMAND_NOT_SUPPORTED = 0x07

        private const val DNS_PORT = 53
        private const val HANDSHAKE_TIMEOUT_MS = 30_000
        private const val CHANNEL_CONNECT_TIMEOUT_MS = 15_000
        private const val FAILURE_LOG_INTERVAL_MS = 5_000L
        private const val TCP_BUFFER_SIZE = 32 * 1024
        private const val UDP_BUFFER_SIZE = 64 * 1024
    }
}
