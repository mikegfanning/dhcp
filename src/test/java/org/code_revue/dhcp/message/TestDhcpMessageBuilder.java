package org.code_revue.dhcp.message;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * @author Mike Fanning
 */
public class TestDhcpMessageBuilder {

    @Test
    public void buildMessage() throws IOException {
        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        ByteBuffer message = builder.setOpCode(DhcpOpCode.REQUEST)
                .setHardwareType(HardwareType.ETHERNET)
                .setHardwareAddress(new byte[]{(byte) 0xb8, 0x27, (byte) 0xeb, 0x65, (byte) 0xef, 0x58})
                .setTransactionId(0x13065f5f)
                .setSeconds((short) 7)
                .addOption(new ByteArrayOption(DhcpOptionType.MESSAGE_TYPE, new byte[]{3}))
                .addOption(new ByteArrayOption(DhcpOptionType.SERVER_ID, new byte[]{(byte) 192, (byte) 168, 1, 1}))
                .addOption(new ByteArrayOption(DhcpOptionType.REQUESTED_IP_ADDR,
                        new byte[] { (byte) 192, (byte) 168, 1, 8}))
                .addOption(new ByteArrayOption(DhcpOptionType.HOST_NAME, "raspberrypi".getBytes()))
                .addOption(new ByteArrayOption(DhcpOptionType.PARAMETER_REQUEST_LIST,
                        new byte[] { 0x01, 0x1c, 0x02, 0x03, 0x0f, 0x06, 0x77, 0x0c, 0x2c, 0x2f, 0x1a, 0x79, 0x2a}))
                .build();

        try (RandomAccessFile file = new RandomAccessFile("src/test/resources/dhcp1.dat", "r")) {
            byte[] fileData = new byte[284];
            file.read(fileData, 0, fileData.length);
            byte[] messageData = new byte[284];
            message.get(messageData, 0, messageData.length);

            Assert.assertArrayEquals(fileData, messageData);
        }
    }

}
