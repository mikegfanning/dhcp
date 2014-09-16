package org.code_revue.dhcp.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestAddressUtils {

    @Test
    public void convertToByteArray() {
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 0 }, AddressUtils.convertToByteArray("0.0.0.0"));
        Assert.assertArrayEquals(new byte[] { 127, 0, 0, 1 }, AddressUtils.convertToByteArray("127.0.0.1"));
        Assert.assertArrayEquals(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 },
                AddressUtils.convertToByteArray("255.255.255.255"));
    }

    @Test
    public void hardwareAddressToString() {
        Assert.assertEquals("00:00:00:00:00:00", AddressUtils.hardwareAddressToString(new byte[] { 0, 0, 0, 0, 0, 0 }));
        Assert.assertEquals("ff:ff:ff:ff:ff:ff", AddressUtils.hardwareAddressToString(new byte[]
                {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255}));
        Assert.assertEquals("80:08", AddressUtils.hardwareAddressToString(new byte[] { (byte) 128, 8 }));
    }

}
