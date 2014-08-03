package org.code_revue.dhcp.message;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class around a {@link java.nio.ByteBuffer} to make it easier to work with DHCP messages. Kind of follows the
 * bean pattern, although it isn't backed by the same type of data the getters and setters use.
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

    public void setOpCode(DhcpOpCode code) {
        headerData.put(0, (byte) code.getNumericCode());
    }

    public HardwareType getHardwareType() {
        return HardwareType.getByNumericCode(headerData.get(1));
    }

    public void setHardwareType(HardwareType type) {
        headerData.put(1, (byte) type.getNumericCode());
    }

    public byte getHardwareAddressLength() {
        return headerData.get(2);
    }

    public void setHardwareAddressLength(byte length) {
        headerData.put(2, length);
    }

    public byte getHops() {
        return headerData.get(3);
    }

    public void setHops(byte hops) {
        headerData.put(3, hops);
    }

    public int getTransactionId() {
        return headerData.getInt(4);
    }

    public void setTransactionId(int id) {
        headerData.putInt(4, id);
    }

    public short getSeconds() {
        return headerData.getShort(8);
    }

    public void setSeconds(short seconds) {
        headerData.putShort(8, seconds);
    }

    public boolean isBroadcast() {
        return (headerData.get(10) & 0b10000000) != 0;
    }

    public void setBroadcast(boolean broadcast) {
        byte broadcastByte = headerData.get(10);
        if (broadcast) {
            broadcastByte = (byte) (broadcastByte | 0b10000000);
        } else {
            broadcastByte = (byte) (broadcastByte & 0b01111111);
        }
        headerData.put(10, broadcastByte);
    }

    public byte[] getClientIpAddress() {
        return getByteArray(12, 4);
    }

    public void setClientIpAddress(byte[] ipAddress) {
        headerData.position(12);
        headerData.put(ipAddress, 0, Math.min(ipAddress.length, 4));
    }

    public byte[] getYourIpAddress() {
        return getByteArray(16, 4);
    }

    public void setYourIpAddress(byte[] ipAddress) {
        headerData.position(16);
        headerData.put(ipAddress, 0, Math.min(ipAddress.length, 4));
    }

    public byte[] getServerIpAddress() {
        return getByteArray(20, 4);
    }

    public void setServerIpAddress(byte[] ipAddress) {
        headerData.position(20);
        headerData.put(ipAddress, 0, Math.min(ipAddress.length, 4));
    }

    public byte[] getGatewayIpAddress() {
        return getByteArray(24, 4);
    }

    public void setGatewayIpAddress(byte[] ipAddress) {
        headerData.position(24);
        headerData.put(ipAddress, 0, Math.min(ipAddress.length, 4));
    }

    public byte[] getClientHardwareAddress() {
        return getByteArray(28, 16);
    }

    public void setClientHardwareAddress(byte[] hardwareAddress) {
        headerData.position(28);
        headerData.put(hardwareAddress, 0, Math.min(hardwareAddress.length, 16));
    }

    public String getServerName() {
        return new String(getByteArray(44, 64));
    }

    public void setServerName(byte[] serverName) {
        headerData.position(44);
        headerData.put(serverName, 0, Math.min(serverName.length, 64));
    }

    public String getBootFilename() {
        return new String(getByteArray(108, 128));
    }

    public void setBootFileName(byte[] fileName) {
        headerData.position(108);
        headerData.put(fileName, 0, Math.min(fileName.length, 128));
    }

    public List<DhcpOption> getOptions() {

        // Skip over magic cookie
        optionsData.position(0);
        optionsData.getInt();

        List<DhcpOption> answer = new ArrayList<>();
        byte[] data = new byte[255];

        DhcpOptionType optionType = DhcpOptionType.getByNumericCode(optionsData.get());
        while (DhcpOptionType.END != optionType) {
            byte length = optionsData.get();
            optionsData.get(data, 0, length);
            answer.add(new ByteArrayOption(optionType, data, 0, length));
            optionType = DhcpOptionType.getByNumericCode(optionsData.get());
        }

        return answer;

    }

    private byte[] getByteArray(int offset, int length) {
        byte[] answer = new byte[length];
        for (int i = 0; i < length; i++) {
            answer[i] = messageData.get(offset + i);
        }
        return answer;
    }

}
