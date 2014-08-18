package org.code_revue.dhcp.server;

/**
 * @author Mike Fanning
 */
public interface DhcpEngine {

    /**
     * Hardware address used by this engine when constructing DHCP Offer, Acknowledgement, etc. packets.
     * @param hardwareAddress
     */
    public void setHardwareAddress(byte[] hardwareAddress);

    /**
     * Set the server IP address used by this engine when constructing DHCP Offer, Acknowledgement, etc. packets.
     * @param serverIpAddress
     */
    public void setServerIpAddress(byte[] serverIpAddress);

    /**
     * Processes a {@link org.code_revue.dhcp.server.DhcpPayload}, which contains an address and the data from a UDP
     * packet, and returns a response payload to send back to the client.
     * @param payload
     * @return Response payload, or null if there was a problem with the request payload
     */
    public DhcpPayload processDhcpPayload(DhcpPayload payload);

}
