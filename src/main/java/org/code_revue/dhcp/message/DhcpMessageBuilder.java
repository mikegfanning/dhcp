package org.code_revue.dhcp.message;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builder class to aid in the construction of DHCP messages. The class uses {@link java.nio.ByteBuffer}s extensively,
 * and changes to the buffer returned by the {@link #build()} method will change the underlying state of the builder
 * object. Therefore, repeated calls to the {@link #build()} method will result in references to
 * {@link java.nio.ByteBuffer}s that share the same memory.
 * <p>
 * This class will fill in the transaction id and client hardware address if none is provided. Also, setter method
 * invocation can be stacked to make code more compact:
 * <pre>
 * {@code
 * DhcpMessageBuilder builder = new DhcpMessageBuilder();
 * builder.setHops(5).setSeconds(7).setBroadcast(true);
 * }
 * </pre>
 * </p>
 *
 * @author Mike Fanning
 */
public class DhcpMessageBuilder {

    public static final int MAX_UDP_SIZE = 512;

    /**
     * @see <a href="http://en.wikipedia.org/wiki/Dhcp#DHCP_discovery">
     *     http://en.wikipedia.org/wiki/Dhcp#DHCP_discovery</a>
     */
    public static final int MAGIC_COOKIE = 0x63825363;

    private static Random rand;

    static {
        try {
            rand = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            rand = new Random(System.nanoTime());
        }
    }

    private final ByteBuffer buffer;
    private final List<DhcpOption> options = new ArrayList<>();
    private final DhcpMessageOverlay overlay;

    private boolean txIdSet = false;
    private boolean hardwareAddressSet = false;

    /**
     * Creates a builder object with an empty buffer.
     */
    public DhcpMessageBuilder() {
        buffer = ByteBuffer.allocateDirect(MAX_UDP_SIZE);
        overlay = new DhcpMessageOverlay(buffer);
    }

    /**
     * Creates a new builder object using the supplied buffer. You better make sure there's enough room in it or you're
     * going to be in a heap of trouble.
     * @param buffer
     * @throws java.lang.IllegalArgumentException If the buffer is less than the length of a DHCP header. Just because
     * this exception isn't thrown doesn't mean you're in the clear, buffer-wise. You still may need more room for
     * options.
     */
    public DhcpMessageBuilder(ByteBuffer buffer) {
        if (buffer.capacity() < DhcpMessageOverlay.HEADER_LENGTH) {
            throw new IllegalArgumentException("Buffer is too small.");
        }
        this.buffer = buffer;
        overlay = new DhcpMessageOverlay(buffer);
    }

    public DhcpMessageBuilder setOpCode(DhcpOpCode code) {
        overlay.setOpCode(code);
        return this;
    }

    public DhcpMessageBuilder setHardwareType(HardwareType type) {
        overlay.setHardwareType(type);
        return this;
    }

    public DhcpMessageBuilder setHardwareAddress(byte[] address) {
        overlay.setHardwareAddressLength((byte) address.length);
        overlay.setClientHardwareAddress(address);
        hardwareAddressSet = true;
        return this;
    }

    public DhcpMessageBuilder setHops(byte hops) {
        overlay.setHops(hops);
        return this;
    }

    public DhcpMessageBuilder setTransactionId(int id) {
        overlay.setTransactionId(id);
        txIdSet = true;
        return this;
    }

    public DhcpMessageBuilder setSeconds(short seconds) {
        overlay.setSeconds(seconds);
        return this;
    }

    public DhcpMessageBuilder setBroadcast(boolean broadcast) {
        overlay.setBroadcast(broadcast);
        return this;
    }

    public DhcpMessageBuilder setClientIpAddress(byte[] ipAddress) {
        overlay.setClientIpAddress(ipAddress);
        return this;
    }

    public DhcpMessageBuilder setYourIpAddress(byte[] ipAddress) {
        overlay.setYourIpAddress(ipAddress);
        return this;
    }

    public DhcpMessageBuilder setServerIpAddress(byte[] ipAddress) {
        overlay.setServerIpAddress(ipAddress);
        return this;
    }

    public DhcpMessageBuilder setGatewayIpAddress(byte[] ipAddress) {
        overlay.setGatewayIpAddress(ipAddress);
        return this;
    }

    public DhcpMessageBuilder setServerName(byte[] serverName) {
        overlay.setServerName(serverName);
        return this;
    }

    public DhcpMessageBuilder setBootFilename(byte[] filename) {
        overlay.setBootFileName(filename);
        return this;
    }

    public DhcpMessageBuilder addOption(DhcpOption option) {
        options.add(option);
        return this;
    }

    /**
     * Returns a {@link java.nio.ByteBuffer} with the binary data for a DHCP message, as configured via this builder
     * object.
     * @return Binary DHCP message
     */
    public ByteBuffer build() {
        if (!txIdSet) {
            overlay.setTransactionId(rand.nextInt());
        }

        if (!hardwareAddressSet) {
            try {
                InetAddress ip = InetAddress.getLocalHost();
                NetworkInterface net = NetworkInterface.getByInetAddress(ip);
                byte[] mac = net.getHardwareAddress();
                setHardwareAddress(mac);
            } catch (Exception e) {
                // Making a good faith effort to set hardware address, swallow exception if something goes wrong.
            }
        }

        buffer.position(DhcpMessageOverlay.HEADER_LENGTH);
        buffer.putInt(MAGIC_COOKIE);

        for (DhcpOption option: options) {
            buffer.put((byte) option.getType().getNumericCode());
            byte[] data = option.getOptionData();
            buffer.put((byte) data.length);
            buffer.put(data);
        }

        buffer.put((byte) DhcpOptionType.END.getNumericCode());

        buffer.limit(buffer.position());
        buffer.position(0);
        return buffer.slice();
    }

}
