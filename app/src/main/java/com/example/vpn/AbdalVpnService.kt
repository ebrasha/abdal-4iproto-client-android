/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : SSH Tunnel
 * File Name : AbdalVpnService.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-03 15:59:00
 * Description : VPN service that opens an SSH session, exposes a local SOCKS5 proxy backed by SSH
 *               direct-tcpip channels, and routes all TUN traffic through it via hev-socks5-tunnel.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.IpPrefix
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.SoundManager
import com.example.util.TunnelLogger
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class AbdalVpnService : VpnService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var session: Session? = null
    private var socksProxy: SshSocksProxy? = null
    private var connectJob: Job? = null
    private var reconnectJob: Job? = null
    private var monitorJob: Job? = null
    private var activeConfig: ConnectionConfig? = null
    private var reconnectAttempts = 0
    private val reconnectScheduled = AtomicBoolean(false)
    private val userRequestedDisconnect = AtomicBoolean(false)

    // True once the tunnel has reached the CONNECTED state at least once for the active session.
    // The kill switch only engages after a successful connection, never during the first connect attempt.
    private var tunnelEverEstablished = false

    // Single IPs / CIDR blocks (from Advanced Settings) that must bypass the tunnel.
    private var whitelistRanges: List<Cidr> = emptyList()

    private data class ConnectionConfig(
        val ip: String,
        val port: Int,
        val username: String,
        val password: String,
        val fakeIp: Boolean,
        val killSwitch: Boolean
    )

    /** A single IPv4 CIDR block used to describe VPN routes. */
    private data class Cidr(val address: String, val prefixLength: Int)

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.DISCONNECT"
        const val EXTRA_IP = "EXTRA_IP"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_USERNAME = "EXTRA_USERNAME"
        const val EXTRA_PASSWORD = "EXTRA_PASSWORD"
        const val EXTRA_FAKE_IP = "EXTRA_FAKE_IP"
        const val EXTRA_KILL_SWITCH = "EXTRA_KILL_SWITCH"
        const val EXTRA_WHITELIST = "EXTRA_WHITELIST"

        private const val TAG = "AbdalVpnService"
        private const val NOTIFICATION_CHANNEL_ID = "AbdalVpnChannel"
        private const val NOTIFICATION_ID = 1
        private const val TUN_ADDRESS = "10.8.0.2"
        private const val TUN_PREFIX = 30
        private const val TUN_MTU = 1500
        private const val PRIMARY_DNS = "8.8.8.8"
        private const val SECONDARY_DNS = "8.8.4.4"
        private const val SSH_CONNECT_TIMEOUT_MS = 20_000
        private const val SSH_CLIENT_VERSION = "SSH-2.0-Abdal-4iProto-Android"
        private const val SSH_MONITOR_INTERVAL_MS = 5_000L
        private val RECONNECT_DELAYS_MS = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L, 30_000L, 60_000L)

        // RFC 1918 private ranges plus other special-use ranges that must bypass the tunnel so that
        // LAN devices (local FTP server, router, printers, casting) and loopback keep working while connected.
        private val PRIVATE_RANGES = listOf(
            Cidr("10.0.0.0", 8),
            Cidr("100.64.0.0", 10),
            Cidr("127.0.0.0", 8),
            Cidr("169.254.0.0", 16),
            Cidr("172.16.0.0", 12),
            Cidr("192.168.0.0", 16),
            Cidr("224.0.0.0", 3)
        )

        // Pre-computed complement of PRIVATE_RANGES within 0.0.0.0/0. Used on Android versions below 13,
        // where VpnService.Builder.excludeRoute is unavailable, so that only public traffic enters the tunnel.
        private val PUBLIC_RANGES = listOf(
            Cidr("0.0.0.0", 5), Cidr("8.0.0.0", 7),
            Cidr("11.0.0.0", 8), Cidr("12.0.0.0", 6), Cidr("16.0.0.0", 4),
            Cidr("32.0.0.0", 3), Cidr("64.0.0.0", 3), Cidr("96.0.0.0", 6), Cidr("100.0.0.0", 10),
            Cidr("100.128.0.0", 9), Cidr("101.0.0.0", 8), Cidr("102.0.0.0", 7), Cidr("104.0.0.0", 5),
            Cidr("112.0.0.0", 5), Cidr("120.0.0.0", 6), Cidr("124.0.0.0", 7), Cidr("126.0.0.0", 8),
            Cidr("128.0.0.0", 3), Cidr("160.0.0.0", 5), Cidr("168.0.0.0", 8), Cidr("169.0.0.0", 9),
            Cidr("169.128.0.0", 10), Cidr("169.192.0.0", 11), Cidr("169.224.0.0", 12),
            Cidr("169.240.0.0", 13), Cidr("169.248.0.0", 14), Cidr("169.252.0.0", 15),
            Cidr("169.255.0.0", 16), Cidr("170.0.0.0", 7), Cidr("172.0.0.0", 12),
            Cidr("172.32.0.0", 11), Cidr("172.64.0.0", 10), Cidr("172.128.0.0", 9),
            Cidr("173.0.0.0", 8), Cidr("174.0.0.0", 7), Cidr("176.0.0.0", 4),
            Cidr("192.0.0.0", 9), Cidr("192.128.0.0", 11), Cidr("192.160.0.0", 13),
            Cidr("192.169.0.0", 16), Cidr("192.170.0.0", 15), Cidr("192.172.0.0", 14),
            Cidr("192.176.0.0", 12), Cidr("192.192.0.0", 10), Cidr("193.0.0.0", 8),
            Cidr("194.0.0.0", 7), Cidr("196.0.0.0", 6), Cidr("200.0.0.0", 5), Cidr("208.0.0.0", 4)
        )
    }

    override fun onCreate() {
        super.onCreate()
        SoundManager.init(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                TunnelLogger.info(TAG, "Disconnect requested by user")
                userRequestedDisconnect.set(true)
                activeConfig = null
                tunnelEverEstablished = false
                reconnectScheduled.set(false)
                reconnectJob?.cancel()
                connectJob?.cancel()
                monitorJob?.cancel()
                teardown(resetState = true, stopService = true)
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_IP)
                val username = intent.getStringExtra(EXTRA_USERNAME)
                if (ip.isNullOrBlank() || username.isNullOrBlank()) {
                    fail("Missing server host or username")
                    return START_NOT_STICKY
                }
                val port = intent.getIntExtra(EXTRA_PORT, 22)
                val password = intent.getStringExtra(EXTRA_PASSWORD).orEmpty()
                val fakeIp = intent.getBooleanExtra(EXTRA_FAKE_IP, false)
                val killSwitch = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                whitelistRanges = parseWhitelist(intent.getStringExtra(EXTRA_WHITELIST).orEmpty())
                val config = ConnectionConfig(ip, port, username, password, fakeIp, killSwitch)

                activeConfig = config
                reconnectAttempts = 0
                tunnelEverEstablished = false
                userRequestedDisconnect.set(false)
                reconnectScheduled.set(false)
                reconnectJob?.cancel()
                startForegroundNotification()
                VpnStateNotifier.updateState(VpnState.CONNECTING)
                TunnelLogger.info(TAG, "Connect requested -> $username@$ip:$port")
                connect(config, isReconnect = false)
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        TunnelLogger.warn(TAG, "VPN permission revoked by system")
        userRequestedDisconnect.set(true)
        activeConfig = null
        reconnectScheduled.set(false)
        reconnectJob?.cancel()
        connectJob?.cancel()
        monitorJob?.cancel()
        teardown(resetState = true, stopService = true)
        super.onRevoke()
    }

    override fun onDestroy() {
        userRequestedDisconnect.set(true)
        activeConfig = null
        reconnectScheduled.set(false)
        reconnectJob?.cancel()
        connectJob?.cancel()
        monitorJob?.cancel()
        teardown(resetState = true, stopService = true)
        job.cancel()
        super.onDestroy()
    }

    private fun connect(config: ConnectionConfig, isReconnect: Boolean) {
        connectJob?.cancel()
        connectJob = scope.launch {
            try {
                reconnectScheduled.set(false)
                stopTunnelEngineAndSession()
                // Keep the (blocking) interface up while the kill switch is active so traffic stays blocked
                // until a new tunnel interface atomically replaces it; otherwise drop it as before.
                val keepInterfaceForKillSwitch =
                    config.killSwitch && tunnelEverEstablished && !userRequestedDisconnect.get()
                if (!keepInterfaceForKillSwitch) {
                    closeVpnInterface()
                }
                if (userRequestedDisconnect.get()) {
                    return@launch
                }

                val sshSession = openSshSession(config.ip, config.port, config.username, config.password)
                if (userRequestedDisconnect.get()) {
                    sshSession.disconnect()
                    return@launch
                }
                session = sshSession
                TunnelLogger.info(TAG, "SSH session established")

                val fakeDns = if (config.fakeIp) {
                    TunnelLogger.info(TAG, "Fake-IP/FakeDNS enabled: domains are resolved on the SSH server")
                    FakeDnsResolver()
                } else {
                    null
                }
                val proxy = SshSocksProxy(sshSession, fakeDns) { failure ->
                    handleUnexpectedSessionFailure(failure)
                }
                val socksPort = proxy.start()
                socksProxy = proxy

                val tunFd = establishVpn()
                    ?: throw IllegalStateException("VPN interface could not be established")
                TunnelLogger.info(TAG, "TUN interface established (fd=${tunFd.fd})")

                startTunnelEngine(tunFd, socksPort)

                VpnStateNotifier.updateState(VpnState.CONNECTED)
                SoundManager.playConnect()
                reconnectAttempts = 0
                tunnelEverEstablished = true
                startSessionMonitor(sshSession)
                TunnelLogger.info(TAG, "Tunnel is up. All traffic now routes through SSH.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (userRequestedDisconnect.get()) {
                    return@launch
                }
                // Plays once per failure cycle (de-duplicated inside SoundManager) and re-arms on reconnect.
                SoundManager.playError()
                val message = if (isReconnect) "Reconnect failed" else "Connection failed"
                TunnelLogger.error(TAG, message, e)
                stopTunnelEngineAndSession()
                if (isRecoverableConnectionFailure(e)) {
                    // scheduleReconnect manages the interface (kill switch or release).
                    scheduleReconnect(config, e)
                } else {
                    closeVpnInterface()
                    VpnStateNotifier.updateState(
                        VpnState.ERROR,
                        e.message ?: "SSH authentication or network error"
                    )
                    teardown(resetState = false, stopService = true)
                }
            }
        }
    }

    private fun startSessionMonitor(sshSession: Session) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (!userRequestedDisconnect.get() && activeConfig != null && session === sshSession) {
                delay(SSH_MONITOR_INTERVAL_MS)
                if (!sshSession.isConnected) {
                    handleUnexpectedSessionFailure(IllegalStateException("SSH session is no longer connected"))
                    return@launch
                }
            }
        }
    }

    private fun handleUnexpectedSessionFailure(failure: Throwable) {
        if (userRequestedDisconnect.get()) {
            return
        }
        val config = activeConfig ?: return
        // An established tunnel dropped unexpectedly; signal it once (re-armed by the next successful connect).
        SoundManager.playError()
        scheduleReconnect(config, failure)
    }

    private fun scheduleReconnect(config: ConnectionConfig, failure: Throwable) {
        if (!reconnectScheduled.compareAndSet(false, true)) {
            return
        }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts += 1
            val delayMs = reconnectDelayForAttempt(reconnectAttempts)
            VpnStateNotifier.updateState(VpnState.CONNECTING)
            TunnelLogger.warn(
                TAG,
                "SSH tunnel interrupted; reconnect attempt $reconnectAttempts in ${delayMs / 1_000}s",
                failure
            )
            stopTunnelEngineAndSession()
            val useKillSwitch =
                config.killSwitch && tunnelEverEstablished && !userRequestedDisconnect.get()
            if (useKillSwitch) {
                engageKillSwitch()
            } else {
                closeVpnInterface()
            }
            delay(delayMs)
            if (!userRequestedDisconnect.get() && activeConfig == config) {
                connect(config, isReconnect = true)
            }
        }
    }

    /**
     * Engages the kill switch by replacing the active interface with a blocking VPN interface that captures all
     * traffic but is never serviced, so packets are dropped. The app itself is excluded so reconnect attempts can
     * still reach the SSH server. The interface stays up until the tunnel is restored or the user disconnects.
     */
    private fun engageKillSwitch() {
        try {
            val builder = Builder()
                .setBlocking(false)
                .addAddress(TUN_ADDRESS, TUN_PREFIX)
                .setMtu(TUN_MTU)
                .setSession(getString(R.string.app_name))

            // Capture the same address space the real tunnel would (internet traffic) so that, with no engine
            // servicing the interface, those packets are dropped. Private/whitelist ranges stay off the tunnel.
            configureRoutes(builder)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                TunnelLogger.warn(TAG, "Could not exclude self from kill switch routes", e)
            }
            val blockingFd = builder.establish()
            if (blockingFd == null) {
                TunnelLogger.error(TAG, "Kill switch interface could not be established")
                return
            }
            val old = vpnInterface
            vpnInterface = blockingFd
            if (old != null && old !== blockingFd) {
                try {
                    old.close()
                } catch (_: Exception) {
                }
            }
            TunnelLogger.warn(TAG, "Kill switch engaged: internet traffic is blocked until the tunnel is restored")
        } catch (e: Exception) {
            TunnelLogger.error(TAG, "Failed to engage kill switch", e)
            closeVpnInterface()
        }
    }

    private fun reconnectDelayForAttempt(attempt: Int): Long {
        val index = (attempt - 1).coerceIn(0, RECONNECT_DELAYS_MS.lastIndex)
        return RECONNECT_DELAYS_MS[index]
    }

    private fun isRecoverableConnectionFailure(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        return !message.contains("auth fail") &&
            !message.contains("authentication failed") &&
            !message.contains("invalid privatekey") &&
            !message.contains("userauth fail")
    }

    private fun openSshSession(ip: String, port: Int, username: String, password: String): Session {
        TunnelLogger.info(TAG, "Opening SSH connection to $ip:$port ...")
        configureSshAlgorithms()
        val jsch = JSch()
        val sshSession = jsch.getSession(username, ip, port).apply {
            setPassword(password)
            // Identify this client to the server (SSH equivalent of an HTTP User-Agent); must include the SSH-2.0- prefix.
            setClientVersion(SSH_CLIENT_VERSION)
            setConfig("StrictHostKeyChecking", "no")
            setConfig("PreferredAuthentications", "password,keyboard-interactive,publickey")
            setConfig("ServerAliveInterval", "30")
            setConfig("ServerAliveCountMax", "2")
            // Offer a broad set of key exchange and host key algorithms so that older or minimally
            // configured servers (which may only advertise curve25519 or SHA1 based algorithms) can negotiate.
            setConfig(
                "kex",
                "curve25519-sha256,curve25519-sha256@libssh.org," +
                    "ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521," +
                    "diffie-hellman-group-exchange-sha256,diffie-hellman-group16-sha512," +
                    "diffie-hellman-group18-sha512,diffie-hellman-group14-sha256," +
                    "diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1," +
                    "diffie-hellman-group1-sha1"
            )
            setConfig(
                "server_host_key",
                "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521," +
                    "rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss"
            )
            setConfig(
                "PubkeyAcceptedAlgorithms",
                "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521," +
                    "rsa-sha2-512,rsa-sha2-256,ssh-rsa"
            )
            // The control connection must bypass the VPN to avoid a routing loop.
            setSocketFactory(ProtectedSocketFactory(this@AbdalVpnService))
        }
        sshSession.connect(SSH_CONNECT_TIMEOUT_MS)
        return sshSession
    }

    private fun configureSshAlgorithms() {
        JSch.setConfig("ssh-ed25519", "com.jcraft.jsch.bc.SignatureEd25519")
        JSch.setConfig("ssh-ed448", "com.jcraft.jsch.bc.SignatureEd448")
        JSch.setConfig("keypairgen.eddsa", "com.jcraft.jsch.bc.KeyPairGenEdDSA")
        JSch.setConfig(
            "kex",
            "diffie-hellman-group14-sha1,curve25519-sha256,curve25519-sha256@libssh.org," +
                "ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521," +
                "diffie-hellman-group-exchange-sha256,diffie-hellman-group16-sha512," +
                "diffie-hellman-group18-sha512,diffie-hellman-group14-sha256," +
                "diffie-hellman-group-exchange-sha1,diffie-hellman-group1-sha1"
        )
        JSch.setConfig(
            "server_host_key",
            "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521," +
                "rsa-sha2-512,rsa-sha2-256,ssh-rsa,ssh-dss"
        )
        JSch.setConfig(
            "cipher.s2c",
            "aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc"
        )
        JSch.setConfig(
            "cipher.c2s",
            "aes256-ctr,aes192-ctr,aes128-ctr,aes256-cbc,aes192-cbc,aes128-cbc"
        )
        JSch.setConfig(
            "mac.s2c",
            "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
        )
        JSch.setConfig(
            "mac.c2s",
            "hmac-sha2-256,hmac-sha2-512,hmac-sha1"
        )
        JSch.setConfig(
            "PubkeyAcceptedAlgorithms",
            "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521," +
                "rsa-sha2-512,rsa-sha2-256,ssh-rsa"
        )
    }

    private fun startTunnelEngine(tunFd: ParcelFileDescriptor, socksPort: Int) {
        if (!HevSocksTunnel.isLibraryAvailable()) {
            throw IllegalStateException("Native tunnel engine is not available in this build")
        }
        TunnelLogger.info(TAG, "Starting tunnel engine -> SOCKS5 127.0.0.1:$socksPort")
        HevSocksTunnel.start(this, tunFd, socksPort)
    }

    private fun establishVpn(): ParcelFileDescriptor? {
        return try {
            val builder = Builder()
                .setBlocking(false)
                .addAddress(TUN_ADDRESS, TUN_PREFIX)
                .addDnsServer(PRIMARY_DNS)
                .addDnsServer(SECONDARY_DNS)
                .setMtu(TUN_MTU)
                .setSession(getString(R.string.app_name))

            configureRoutes(builder)

            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: Exception) {
                TunnelLogger.warn(TAG, "Could not exclude self from VPN routes", e)
            }

            val fd = builder.establish()
            if (fd == null) {
                TunnelLogger.error(TAG, "Builder.establish() returned null (permission missing?)")
                return null
            }
            // Atomically replace any previous interface (e.g. a kill switch blocking interface) without a leak gap.
            val old = vpnInterface
            vpnInterface = fd
            if (old != null && old !== fd) {
                try {
                    old.close()
                } catch (_: Exception) {
                }
            }
            fd
        } catch (e: Exception) {
            TunnelLogger.error(TAG, "Failed to establish VPN interface", e)
            null
        }
    }

    /**
     * Configures split tunneling so that private/LAN ranges bypass the tunnel and stay on the local network.
     * On Android 13+ the whole address space is routed and private ranges are carved out via excludeRoute.
     * On older versions only the pre-computed public ranges are routed, achieving the same effect.
     */
    private fun configureRoutes(builder: Builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            builder.addRoute("0.0.0.0", 0)
            (PRIVATE_RANGES + whitelistRanges).forEach { range ->
                try {
                    builder.excludeRoute(IpPrefix(InetAddress.getByName(range.address), range.prefixLength))
                } catch (e: Exception) {
                    TunnelLogger.warn(TAG, "Could not exclude route ${range.address}/${range.prefixLength}", e)
                }
            }
            if (whitelistRanges.isNotEmpty()) {
                TunnelLogger.info(TAG, "Whitelist: ${whitelistRanges.size} range(s) bypass the tunnel")
            }
            TunnelLogger.info(TAG, "Split tunneling enabled: private LAN ranges bypass the tunnel")
        } else {
            PUBLIC_RANGES.forEach { range ->
                try {
                    builder.addRoute(range.address, range.prefixLength)
                } catch (e: Exception) {
                    TunnelLogger.warn(TAG, "Could not add route ${range.address}/${range.prefixLength}", e)
                }
            }
            if (whitelistRanges.isNotEmpty()) {
                TunnelLogger.warn(TAG, "Whitelist bypass requires Android 13+; ignoring on this device")
            }
            TunnelLogger.info(TAG, "Split tunneling enabled (legacy): only public ranges route through the tunnel")
        }
    }

    /**
     * Parses a comma separated list of single IPv4 addresses and CIDR blocks (e.g. "10.0.0.1, 192.168.1.0/24").
     * Invalid entries are skipped. Single addresses are treated as /32.
     */
    private fun parseWhitelist(raw: String): List<Cidr> {
        return raw.split(',').mapNotNull { token ->
            val entry = token.trim()
            if (entry.isEmpty()) return@mapNotNull null
            if (entry.contains('/')) {
                val parts = entry.split('/')
                val address = parts.getOrNull(0)?.trim().orEmpty()
                val prefix = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (parts.size != 2 || prefix == null || prefix !in 0..32 || !isValidIpv4(address)) {
                    TunnelLogger.warn(TAG, "Ignoring invalid whitelist entry: $entry")
                    return@mapNotNull null
                }
                Cidr(address, prefix)
            } else {
                if (!isValidIpv4(entry)) {
                    TunnelLogger.warn(TAG, "Ignoring invalid whitelist entry: $entry")
                    return@mapNotNull null
                }
                Cidr(entry, 32)
            }
        }
    }

    private fun isValidIpv4(address: String): Boolean {
        val octets = address.split('.')
        if (octets.size != 4) return false
        return octets.all { octet ->
            val value = octet.toIntOrNull()
            value != null && value in 0..255
        }
    }

    private fun startForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.vpn_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setContentText(getString(R.string.vpn_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun fail(message: String) {
        TunnelLogger.error(TAG, message)
        VpnStateNotifier.updateState(VpnState.ERROR, message)
        stopSelf()
    }

    /**
     * Stops the tunnel engine, local SOCKS proxy and SSH session, but leaves the VPN interface untouched.
     * This lets the kill switch keep blocking traffic across reconnects.
     */
    private fun stopTunnelEngineAndSession() {
        monitorJob?.cancel()
        monitorJob = null
        try {
            HevSocksTunnel.stop()
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Error stopping tunnel engine", e)
        }
        try {
            socksProxy?.stop()
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Error stopping SOCKS proxy", e)
        }
        try {
            session?.disconnect()
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Error disconnecting SSH", e)
        }

        socksProxy = null
        session = null
    }

    /**
     * Closes and clears the VPN interface, removing the system VPN (and any kill switch blocking).
     */
    private fun closeVpnInterface() {
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            TunnelLogger.warn(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null
    }

    private fun cleanupTunnelResources() {
        stopTunnelEngineAndSession()
        closeVpnInterface()
    }

    private fun teardown(resetState: Boolean, stopService: Boolean) {
        cleanupTunnelResources()
        if (resetState) {
            VpnStateNotifier.updateState(VpnState.DISCONNECTED)
            TunnelLogger.info(TAG, "Disconnected")
        }
        if (stopService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
