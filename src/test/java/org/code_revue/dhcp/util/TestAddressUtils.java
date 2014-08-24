package org.code_revue.dhcp.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestAddressUtils {

    @Test
    public void hardwareAddressToString() {
        Assert.assertEquals("00:00:00:00:00:00", AddressUtils.hardwareAddressToString(new byte[] { 0, 0, 0, 0, 0, 0 }));
        Assert.assertEquals("ff:ff:ff:ff:ff:ff", AddressUtils.hardwareAddressToString(new byte[]
                {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255}));
        Assert.assertEquals("80:08", AddressUtils.hardwareAddressToString(new byte[] { (byte) 128, 8 }));
    }

    @Test
    public void ipAddressToString() {
        Assert.assertEquals("0.0.0.0", AddressUtils.ipAddressToString(new byte[] { 0, 0, 0, 0 }));
        Assert.assertEquals("255.255.255.255", AddressUtils.ipAddressToString(new byte[]
                { (byte) 255, (byte) 255, (byte) 255, (byte) 255 }));
        Assert.assertEquals("192.168.1.8", AddressUtils.ipAddressToString(new byte[] { (byte) 192, (byte) 168, 1, 8 }));
    }

}
