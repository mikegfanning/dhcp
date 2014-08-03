package org.code_revue.dhcp.message;

import org.junit.Assert;
import org.junit.Test;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * @author Mike Fanning
 */
public class TestDhcpMessageOverlay {

    @Test
    public void getters() throws Exception {

        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/dhcp0.dat", "r")) {
            FileChannel channel = file.getChannel();
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            DhcpMessageOverlay overlay = new DhcpMessageOverlay(buffer);

            Assert.assertEquals(DhcpOpCode.REQUEST, overlay.getOpCode());
            Assert.assertEquals(HardwareType.ETHERNET, overlay.getHardwareType());
            Assert.assertEquals(6, overlay.getHardwareAddressLength());
            Assert.assertEquals(0, overlay.getHops());
            Assert.assertEquals(0x13065f5f, overlay.getTransactionId());
            Assert.assertEquals(7, overlay.getSeconds());
            Assert.assertFalse(overlay.isBroadcast());

            final byte[] emptyAddress = new byte[] { 0, 0, 0, 0};
            Assert.assertArrayEquals(emptyAddress, overlay.getClientIpAddress());
            Assert.assertArrayEquals(emptyAddress, overlay.getYourIpAddress());
            Assert.assertArrayEquals(emptyAddress, overlay.getServerIpAddress());
            Assert.assertArrayEquals(emptyAddress, overlay.getGatewayIpAddress());

            DhcpOption[] options = new DhcpOption[4];
            options = overlay.getOptions().toArray(options);

            Assert.assertEquals(DhcpOptionType.MESSAGE_TYPE, options[0].getType());
            Assert.assertEquals(DhcpOptionType.REQUESTED_IP_ADDR, options[1].getType());
            Assert.assertEquals(DhcpOptionType.HOST_NAME, options[2].getType());
            Assert.assertEquals(DhcpOptionType.PARAMETER_REQUEST_LIST, options[3].getType());

            Assert.assertEquals(DhcpMessageType.DHCP_DISCOVER,
                    DhcpMessageType.getByNumericCode(options[0].getOptionData()[0]));
            Assert.assertArrayEquals(new byte[] { (byte) 192, (byte) 168, 1, 8 }, options[1].getOptionData());
            Assert.assertEquals("raspberrypi", new String(options[2].getOptionData()));

            byte[] paramList = options[3].getOptionData();
            DhcpOptionType[] parameterList = new DhcpOptionType[paramList.length];
            for (int i = 0; i < paramList.length; i++) {
                parameterList[i] = DhcpOptionType.getByNumericCode(paramList[i]);
            }

            Assert.assertEquals(DhcpOptionType.SUBNET_MASK, parameterList[0]);
            Assert.assertEquals(DhcpOptionType.BROADCAST_ADDR, parameterList[1]);
            Assert.assertEquals(DhcpOptionType.TIME_OFFSET, parameterList[2]);
            Assert.assertEquals(DhcpOptionType.ROUTER, parameterList[3]);
            Assert.assertEquals(DhcpOptionType.DOMAIN_NAME, parameterList[4]);
            Assert.assertEquals(DhcpOptionType.DNS_SERVER, parameterList[5]);
            Assert.assertEquals(DhcpOptionType.DNS_SEARCH_DOMAIN, parameterList[6]);
            Assert.assertEquals(DhcpOptionType.HOST_NAME, parameterList[7]);
            Assert.assertEquals(DhcpOptionType.NETBIOS_NAME_SERVER, parameterList[8]);
            Assert.assertEquals(DhcpOptionType.NETBIOS_SCOPE, parameterList[9]);
            Assert.assertEquals(DhcpOptionType.INTERFACE_MTU, parameterList[10]);
            Assert.assertEquals(DhcpOptionType.CLASSLESS_STATIC_ROUTES, parameterList[11]);
            Assert.assertEquals(DhcpOptionType.NTP_SERVER, parameterList[12]);

        }

    }

}
