package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.DhcpMessageOverlay;
import org.code_revue.dhcp.message.DhcpOpCode;
import org.code_revue.dhcp.util.AddressUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

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
    private static byte[] hardwareAddress;

    private static byte[] addressPoolStart;
    private static byte[] addressPoolEnd;

    private StandardEngine engine;

    @BeforeClass
    public static void setupClass() throws UnknownHostException, SocketException {
        emptyAddress = new byte[] { 0, 0, 0, 0 };
        broadcastAddress = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };

        clientWireAddress = new InetSocketAddress(Inet4Address.getByAddress(emptyAddress), 68);
        clientBroadcastAddress = new InetSocketAddress(Inet4Address.getByAddress(broadcastAddress), 67);
        serverBroadcastAddress = new InetSocketAddress(Inet4Address.getByAddress(broadcastAddress), 68);

        serverIpAddress = Inet4Address.getLocalHost().getAddress();
        hardwareAddress = NetworkInterface.getByInetAddress(InetAddress.getLocalHost()).getHardwareAddress();

        addressPoolStart = new byte[] { (byte) 192, (byte) 168, 1, 2 };
        addressPoolEnd = new byte[] { (byte) 192, (byte) 168, 1, 10 };
    }

    @Before
    public void setup() throws UnknownHostException, SocketException {
        engine = new StandardEngine();
        engine.setServerIpAddress(serverIpAddress);
        engine.setHardwareAddress(hardwareAddress);
        engine.addAddressPool(new StandardIp4AddressPool(addressPoolStart, addressPoolEnd));
    }

    @Test
    public void discover() throws Exception {

        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/dhcp0.dat", "r")) {
            FileChannel channel = file.getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            DhcpPayload incoming = new DhcpPayload(clientWireAddress, buffer);
            DhcpPayload outgoing = engine.processDhcpPayload(incoming);

            Assert.assertEquals(serverBroadcastAddress, outgoing.getAddress());

            DhcpMessageOverlay response = new DhcpMessageOverlay(outgoing.getData());
            Assert.assertEquals(DhcpOpCode.REPLY, response.getOpCode());
            Assert.assertEquals(hardwareAddress.length, response.getHardwareAddressLength());
            Assert.assertEquals(0x13065f5f, response.getTransactionId());
            Assert.assertArrayEquals(emptyAddress, response.getClientIpAddress());
            Assert.assertArrayEquals(serverIpAddress, response.getServerIpAddress());

            int yourAddress = AddressUtils.convertToInt(response.getYourIpAddress());
            int startAsInt = AddressUtils.convertToInt(addressPoolStart);
            int endAsInt = AddressUtils.convertToInt(addressPoolEnd);
            Assert.assertTrue(AddressUtils.ADDRESS_COMPARATOR.compare(yourAddress, startAsInt) >= 0);
            Assert.assertTrue(AddressUtils.ADDRESS_COMPARATOR.compare(yourAddress, endAsInt) <= 0);

            Assert.assertArrayEquals(emptyAddress, response.getGatewayIpAddress());
            Assert.assertArrayEquals(hardwareAddress, response.getClientHardwareAddress());
            Assert.assertEquals(DhcpMessageOverlay.MAGIC_COOKIE, response.getMagicCookie());

        }

    }
}
