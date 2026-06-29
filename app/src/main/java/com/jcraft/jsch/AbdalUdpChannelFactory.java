/*
 **********************************************************************
 * -------------------------------------------------------------------
 * Project Name : Abdal 4iProto Android
 * File Name : AbdalUdpChannelFactory.java
 * Author : Ebrahim Shafiei (EbraSha)
 * Email : Prof.Shafiei@Gmail.com
 * Created On : 2026-06-07 07:21:20
 * Description : Opens an Abdal 4iProto proprietary "direct-udpip" SSH channel. JSch hardcodes the
 *               supported channel types, so this helper lives in the com.jcraft.jsch package to reuse
 *               the proven direct-tcpip stream forwarder while overriding only the wire channel-type
 *               string. The Extra Data record (HostToConnect, PortToConnect, OriginatorAddress,
 *               OriginatorPort) is produced identically to RFC 4254 direct-tcpip, as the server expects.
 * -------------------------------------------------------------------
 *
 * "Coding is an engaging and beloved hobby for me. I passionately and insatiably pursue knowledge in cybersecurity and programming."
 * – Ebrahim Shafiei
 *
 **********************************************************************
 */

package com.jcraft.jsch;

/**
 * Factory that produces a channel whose CHANNEL_OPEN advertises the proprietary {@code direct-udpip}
 * type required by the Abdal 4iProto server, instead of the standard {@code direct-tcpip}.
 *
 * <p>The returned {@link Channel} is fully initialised and registered with the session; the caller is
 * responsible for invoking {@link Channel#connect(int)} and then reading/writing the framed datagrams
 * via {@link Channel#getInputStream()} / {@link Channel#getOutputStream()}.</p>
 */
public final class AbdalUdpChannelFactory {

    /** Exact wire string for the proprietary channel type. Must never change. */
    private static final byte[] DIRECT_UDPIP_TYPE = Util.str2byte("direct-udpip");

    private AbdalUdpChannelFactory() {
        // Utility class; not instantiable.
    }

    /**
     * Opens a {@code direct-udpip} channel to the given final UDP destination.
     *
     * @param session   an established SSH session.
     * @param targetHost final UDP destination host announced in the channel Extra Data (PortToConnect).
     * @param targetPort final UDP destination port announced in the channel Extra Data (PortToConnect).
     * @return a ready-to-connect channel whose type string is {@code direct-udpip}.
     * @throws JSchException if the channel cannot be created (e.g. the session is disconnecting).
     */
    public static Channel openDirectUdpIp(Session session, String targetHost, int targetPort)
            throws JSchException {
        // Reuse JSch's stream forwarder so window sizes, IO pipes and channel registration are set up
        // exactly like a working direct-tcpip channel; only the advertised type differs.
        Channel channel = session.getStreamForwarder(targetHost, targetPort);
        // genChannelOpenPacket() serialises this.type into the CHANNEL_OPEN message, so overriding the
        // package-private type field is sufficient to switch the wire protocol to direct-udpip.
        channel.type = DIRECT_UDPIP_TYPE;
        return channel;
    }
}
