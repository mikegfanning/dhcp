package org.code_revue.dhcp.server;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 * Simple bean for all of the information related to a DHCP packet. This is kind of a DTO between the network layer and
 * the engine that will handle address pools and leases and what-not.
 * @author Mike Fanning
 */
public class DhcpPayload {

    private SocketAddress address;

    private ByteBuffer data;

    public DhcpPayload(SocketAddress address, ByteBuffer data) {
        this.address = address;
        this.data = data;
    }

    /**
     * Get the socket address for this payload. This can either be the address the message originated from (if we're
     * receiving) or the destination (if we're sending).
     * @return
     */
    public SocketAddress getAddress() {
        return address;
    }

    /**
     * Set the socket address for this payload.
     * @param address
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }

    /**
     * Get the data from this payload. Normally this will be the contents of a DHCP UDP packet.
     * @return
     */
    public ByteBuffer getData() {
        return data;
    }

    /**
     * Set the data for this payload.
     * @param data
     */
    public void setData(ByteBuffer data) {
        this.data = data;
    }
}
