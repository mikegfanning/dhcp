package org.code_revue.dhcp.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestAddressUtils {

    @Test
    public void convertToByteArray() {
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, AddressUtils.convertToByteArray("0.0.0.0"));
        assertArrayEquals(new byte[] { 127, 0, 0, 1 }, AddressUtils.convertToByteArray("127.0.0.1"));
        assertArrayEquals(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 },
                AddressUtils.convertToByteArray("255.255.255.255"));
    }

    @Test
    public void convertToString() {
        assertEquals("0.0.0.0", AddressUtils.convertToString(new byte[] { 0, 0, 0, 0 }));
        assertEquals("255.255.255.255",
                AddressUtils.convertToString(new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 }));
        assertEquals("192.168.100.1", AddressUtils.convertToString(new byte[] { (byte) 192, (byte) 168, 100, 1 }));
    }

    @Test
    public void hardwareAddressToString() {
        assertEquals("00:00:00:00:00:00", AddressUtils.hardwareAddressToString(new byte[] { 0, 0, 0, 0, 0, 0 }));
        assertEquals("ff:ff:ff:ff:ff:ff", AddressUtils.hardwareAddressToString(new byte[]
                {(byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255, (byte) 255}));
        assertEquals("80:08", AddressUtils.hardwareAddressToString(new byte[] { (byte) 128, 8 }));
    }

}
