/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : UdpFrame.kt
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-07 08:14:14
 * Description : Single source of truth for the Abdal 4iProto UDP-over-SSH datagram framing. Every
 *               datagram on a direct-udpip channel is carried as [2-byte big-endian length][N-byte
 *               payload]; a zero-length frame (0x00 0x00) is the keepalive. These wire constants are
 *               fixed by the server protocol and are never user-configurable.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.example.vpn

import java.io.DataInputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Stateless codec for the proprietary 2-byte length-prefixed datagram framing shared by the fixed UDP
 * tunnel ([SshUdpTunnel]) and the transparent UDP NAT ([UdpNat]). Keeping this in one place guarantees
 * both paths speak the exact same wire format the Abdal 4iProto server expects.
 */
object UdpFrame {

    /** Maximum datagram payload, bounded by the 2-byte length field. Fixed by the protocol. */
    const val MAX_DATAGRAM_SIZE = 65535

    // Zero-length frame: the protocol keepalive marker (no payload).
    private val KEEPALIVE_FRAME = byteArrayOf(0x00, 0x00)

    /**
     * Writes a single framed datagram. The 2-byte big-endian header and the payload are combined into
     * one buffer so the whole frame leaves with a single write, as the protocol recommends. Writes are
     * serialised on [writeLock] so frames from different threads never interleave on the channel.
     *
     * Empty or oversized datagrams (which cannot be represented by the 2-byte length field) are dropped.
     */
    fun write(out: OutputStream, writeLock: Any, data: ByteArray, offset: Int, length: Int) {
        if (length <= 0 || length > MAX_DATAGRAM_SIZE) {
            return
        }
        val frame = ByteArray(2 + length)
        frame[0] = ((length ushr 8) and 0xFF).toByte()
        frame[1] = (length and 0xFF).toByte()
        System.arraycopy(data, offset, frame, 2, length)
        synchronized(writeLock) {
            out.write(frame)
            out.flush()
        }
    }

    /** Writes a zero-length keepalive frame (0x00 0x00). */
    fun writeKeepalive(out: OutputStream, writeLock: Any) {
        synchronized(writeLock) {
            out.write(KEEPALIVE_FRAME)
            out.flush()
        }
    }

    /**
     * Reads exactly one frame using read-full semantics: precisely 2 length bytes, then precisely N
     * payload bytes (a frame may be split across several TCP segments). Returns an empty array for a
     * keepalive frame (length 0). Throws on EOF or I/O error, signalling the channel is gone.
     */
    @Throws(IOException::class)
    fun read(input: DataInputStream): ByteArray {
        val length = input.readUnsignedShort()
        if (length == 0) {
            return EMPTY
        }
        val payload = ByteArray(length)
        input.readFully(payload)
        return payload
    }

    private val EMPTY = ByteArray(0)
}
