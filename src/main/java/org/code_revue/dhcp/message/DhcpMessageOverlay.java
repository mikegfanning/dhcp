package org.code_revue.dhcp.message;

import java.nio.ByteBuffer;

/**
 * Wrapper class around a {@link java.nio.ByteBuffer} to make it easier to work with DHCP messages.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm">
 *     http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm</a>
 */
public class DhcpMessageOverlay {

    private ByteBuffer messageData;
    private ByteBuffer headerData;
    private ByteBuffer optionsData;

    public static final int HEADER_LENGTH = 236;

    /**
     * Creates a new message overlay for DHCP data.
     * @param data DHCP message
     */
    public DhcpMessageOverlay(ByteBuffer data) {
        this.messageData = data;
        this.messageData.position(0);
        this.messageData.limit(HEADER_LENGTH);
        this.headerData = this.messageData.slice();
        this.messageData.position(HEADER_LENGTH);
        this.messageData.limit(this.messageData.capacity());
        this.optionsData = this.messageData.slice();
    }

    public DhcpOpCode getOpCode() {
        return DhcpOpCode.getByNumericCode(headerData.get(0));
    }

    public HardwareType getHardwareType() {
        return HardwareType.getByNumericCode(headerData.get(1));
    }

    public byte getHardwareAddressLength() {
        return headerData.get(2);
    }

    public byte getHops() {
        return headerData.get(3);
    }

    public int getTransactionId() {
        return headerData.getInt(4);
    }

    public short getSeconds() {
        return headerData.getShort(8);
    }

    public boolean isBroadcast() {
        return (headerData.get(10) & 0b10000000) != 0;
    }

    public byte[] getClientIpAddress() {
        return getByteArray(12, 4);
    }

    public byte[] getYourIpAddress() {
        return getByteArray(16, 4);
    }

    public byte[] getServerIpAddress() {
        return getByteArray(20, 4);
    }

    public byte[] getGatewayIpAddress() {
        return getByteArray(24, 4);
    }

    public byte[] getClientHardwareAddress() {
        return getByteArray(28, 16);
    }

    public String getServerName() {
        return new String(getByteArray(44, 64));
    }

    public String getBootFilename() {
        return new String(getByteArray(108, 128));
    }

    private byte[] getByteArray(int offset, int length) {
        messageData.position(offset).limit(offset + length);
        return messageData.slice().array();
    }
}
