package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.code_revue.dhcp.util.AddressUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

/**
 * @author Mike Fanning
 */
public class TestStandardEngine {

    private static byte[] emptyAddress;
    private static byte[] broadcastAddress;

    private static SocketAddress clientWireAddress;
    private static SocketAddress clientBroadcastAddress;
    private static SocketAddress serverBroadcastAddress;

    private static byte[] serverIpAddress;

    private static byte[] addressPoolStart;
    private static byte[] addressPoolEnd;

    private static ByteBuffer readOnlyDiscoverMessage;

    private StandardEngine engine;
    private ByteBuffer discoverMessage;

    @BeforeClass
    public static void setupClass() throws IOException {
        emptyAddress = new byte[] { 0, 0, 0, 0 };
        broadcastAddress = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };

        clientWireAddress = new InetSocketAddress(Inet4Address.getByAddress(emptyAddress), 68);
        clientBroadcastAddress = new InetSocketAddress(Inet4Address.getByAddress(broadcastAddress), 67);
        serverBroadcastAddress = new InetSocketAddress(Inet4Address.getByAddress(broadcastAddress), 68);

        serverIpAddress = Inet4Address.getLocalHost().getAddress();

        addressPoolStart = new byte[] { (byte) 192, (byte) 168, 1, 2 };
        addressPoolEnd = new byte[] { (byte) 192, (byte) 168, 1, 10 };

        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/dhcp0.dat", "r")) {
            FileChannel channel = file.getChannel();
            readOnlyDiscoverMessage = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        }
    }

    @Before
    public void setup() throws Exception {
        engine = new StandardEngine(serverIpAddress);
        engine.setAddressPool(new StandardIp4AddressPool(addressPoolStart, addressPoolEnd));
        discoverMessage = ByteBuffer.allocateDirect(readOnlyDiscoverMessage.capacity()).put(readOnlyDiscoverMessage);
        readOnlyDiscoverMessage.position(0);
    }

    @Test
    public void discover() {
        // Set some configured options that the server will return.
        byte[] subnetMask = new byte[] { (byte) 255, (byte) 255, (byte) 255, 0 };
        byte[] leaseTimeout = new byte[] { 0, 0, 0, 60 };
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.IP_ADDR_LEASE_TIME, leaseTimeout));
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.SUBNET_MASK, subnetMask));
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.COOKIE_SERVER, subnetMask));

        DhcpMessageOverlay requestOverlay = new DhcpMessageOverlay(discoverMessage);
        DhcpPayload incoming = new DhcpPayload(clientWireAddress, discoverMessage);
        DhcpPayload outgoing = engine.processDhcpPayload(incoming);

        Assert.assertEquals(serverBroadcastAddress, outgoing.getAddress());

        DhcpMessageOverlay response = new DhcpMessageOverlay(outgoing.getData());
        Assert.assertEquals(DhcpOpCode.REPLY, response.getOpCode());
        Assert.assertEquals(6, response.getHardwareAddressLength());
        Assert.assertEquals(0x13065f5f, response.getTransactionId());
        Assert.assertArrayEquals(emptyAddress, response.getClientIpAddress());
        Assert.assertArrayEquals(serverIpAddress, response.getServerIpAddress());

        int yourAddress = AddressUtils.convertToInt(response.getYourIpAddress());
        int startAsInt = AddressUtils.convertToInt(addressPoolStart);
        int endAsInt = AddressUtils.convertToInt(addressPoolEnd);
        Assert.assertTrue(AddressUtils.ADDRESS_COMPARATOR.compare(yourAddress, startAsInt) >= 0);
        Assert.assertTrue(AddressUtils.ADDRESS_COMPARATOR.compare(yourAddress, endAsInt) <= 0);

        Assert.assertArrayEquals(emptyAddress, response.getGatewayIpAddress());
        Assert.assertArrayEquals(requestOverlay.getClientHardwareAddress(), response.getClientHardwareAddress());
        Assert.assertEquals(DhcpMessageOverlay.MAGIC_COOKIE, response.getMagicCookie());

        Map<DhcpOptionType, DhcpOption> options = response.getOptions();
        DhcpOption messageType = options.get(DhcpOptionType.MESSAGE_TYPE);
        Assert.assertEquals(DhcpMessageType.OFFER,
                DhcpMessageType.getByNumericCode(messageType.getOptionData()[0]));
        Assert.assertArrayEquals(subnetMask, options.get(DhcpOptionType.SUBNET_MASK).getOptionData());

        DhcpOption serverIdOption = options.get(DhcpOptionType.SERVER_ID);
        Assert.assertArrayEquals(serverIpAddress, serverIdOption.getOptionData());

        Assert.assertNotNull(options.get(DhcpOptionType.IP_ADDR_LEASE_TIME));

        // Client didn't request this parameter
        Assert.assertNull(options.get(DhcpOptionType.COOKIE_SERVER));

        // Server doesn't have config information for this parameter
        Assert.assertNull(options.get(DhcpOptionType.ARP_CACHE_TIMEOUT));

    }

    @Test
    public void badOpCode() {
        DhcpMessageOverlay overlay = new DhcpMessageOverlay(discoverMessage);
        overlay.setOpCode(DhcpOpCode.REPLY);
        DhcpPayload response = engine.processDhcpPayload(new DhcpPayload(clientWireAddress, discoverMessage));
        Assert.assertNull(response);
    }

    @Test
    public void badMagicCookie() {
        DhcpMessageOverlay overlay = new DhcpMessageOverlay(discoverMessage);
        overlay.setMagicCookie(~DhcpMessageOverlay.MAGIC_COOKIE);
        DhcpPayload response = engine.processDhcpPayload(new DhcpPayload(clientWireAddress, discoverMessage));
        Assert.assertNull(response);
    }

    @Test
    public void badMessage() {
        ByteBuffer message = ByteBuffer.allocateDirect(0);
        DhcpPayload response = engine.processDhcpPayload(new DhcpPayload(clientWireAddress, message));
    }

    @Test
    public void consecutiveDiscovers() {
        DhcpPayload response = engine.processDhcpPayload(new DhcpPayload(clientWireAddress, discoverMessage));
        Assert.assertNotNull(response);
        response = engine.processDhcpPayload(new DhcpPayload(clientWireAddress, discoverMessage));
        Assert.assertNull(response);
    }

    @Test
    public void request() {
        DhcpMessageOverlay discover = new DhcpMessageOverlay(discoverMessage);
        DhcpPayload discPayload = new DhcpPayload(clientWireAddress, discoverMessage);
        DhcpMessageOverlay offer = new DhcpMessageOverlay(engine.processDhcpPayload(discPayload).getData());

        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REQUEST)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(discover.getTransactionId())
                .setServerIpAddress(offer.getServerIpAddress())
                .setHardwareAddress(discover.getClientHardwareAddress())
                .addOption(DhcpMessageType.REQUEST.getOption())
                .addOption(new ByteArrayOption(DhcpOptionType.REQUESTED_IP_ADDR, offer.getYourIpAddress()))
                .addOption(new ByteArrayOption(DhcpOptionType.SERVER_ID, offer.getServerIpAddress()));
        DhcpPayload reqPayload = new DhcpPayload(clientWireAddress, builder.build());
        DhcpMessageOverlay request = new DhcpMessageOverlay(reqPayload.getData());
        DhcpMessageOverlay ack = new DhcpMessageOverlay(engine.processDhcpPayload(reqPayload).getData());

        Assert.assertEquals(DhcpOpCode.REPLY, ack.getOpCode());
        Assert.assertEquals(HardwareType.ETHERNET, ack.getHardwareType());
        Assert.assertEquals(request.getTransactionId(), ack.getTransactionId());
        Assert.assertArrayEquals(emptyAddress, ack.getClientIpAddress());
        Assert.assertArrayEquals(serverIpAddress, ack.getServerIpAddress());
        Assert.assertArrayEquals(emptyAddress, ack.getGatewayIpAddress());
        Assert.assertArrayEquals(request.getClientHardwareAddress(), ack.getClientHardwareAddress());
        Assert.assertEquals(DhcpMessageOverlay.MAGIC_COOKIE, ack.getMagicCookie());

        Map<DhcpOptionType, DhcpOption> ackOptions = ack.getOptions();
        Map<DhcpOptionType, DhcpOption> offerOptions = offer.getOptions();
        Assert.assertEquals(ackOptions.size(), offerOptions.size());
        for (DhcpOption ackOption: ackOptions.values()) {
            if (DhcpOptionType.MESSAGE_TYPE.equals(ackOption.getType())) {
                Assert.assertArrayEquals(DhcpMessageType.ACK.getOption().getOptionData(),
                        ackOption.getOptionData());
            } else {
                DhcpOption offerOption = offerOptions.get(ackOption.getType());
                Assert.assertNotNull(offerOption);
                Assert.assertArrayEquals(offerOption.getOptionData(), ackOption.getOptionData());
            }
        }
    }

}
