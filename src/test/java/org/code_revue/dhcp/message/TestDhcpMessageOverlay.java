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

        }

    }

}
