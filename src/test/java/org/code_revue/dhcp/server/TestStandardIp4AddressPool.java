package org.code_revue.dhcp.server;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author Mike Fanning
 */
public class TestStandardIp4AddressPool {

    private final byte[] address1 = new byte[] { 100, 0, 0, 1 };
    private final byte[] address2 = new byte[] { 101, 0, 0, 1 };
    private final byte[] address3 = new byte[] { (byte) 130, 0, 0, 1};
    private final byte[] address4 = new byte[] { (byte) 131, 0, 0, 1};
    private final byte[] address5 = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 254};

    @Test
    public void validConstructors() {
        new StandardIp4AddressPool(address1, address1);
        new StandardIp4AddressPool(address1, address2);
        new StandardIp4AddressPool(address1, address3);
        new StandardIp4AddressPool(address1, address4);
        new StandardIp4AddressPool(address2, address2);
        new StandardIp4AddressPool(address2, address3);
        new StandardIp4AddressPool(address2, address4);
        new StandardIp4AddressPool(address3, address3);
        new StandardIp4AddressPool(address3, address4);
        new StandardIp4AddressPool(address4, address4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor1() {
        new StandardIp4AddressPool(address2, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor2() {
        new StandardIp4AddressPool(address3, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor3() {
        new StandardIp4AddressPool(address4, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor4() {
        new StandardIp4AddressPool(address3, address2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor5() {
        new StandardIp4AddressPool(address4, address2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor6() {
        new StandardIp4AddressPool(address4, address3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void outOfRange() {
        new StandardIp4AddressPool(address1, address5);
    }

    @Test
    public void testBorrowAndReturn() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address1, address2);
        byte[] address = pool.borrowAddress();
        pool.returnAddress(address);
    }

}
