package org.code_revue.dhcp.server;

import org.code_revue.dhcp.util.AddressUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Mike Fanning
 */
public class TestSetBasedAddressPool {

    private final byte[] address1 = new byte[] { 100, 0, 0, 1 };
    private final byte[] address2 = new byte[] { 101, 0, 0, 1 };
    private final byte[] address3 = new byte[] { (byte) 130, 0, 0, 1};
    private final byte[] address4 = new byte[] { (byte) 131, 0, 0, 1};
    private final byte[] address5 = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 254};
    private final byte[] address6 = new byte[] { (byte) 192, (byte) 168, 1, 10};
    private final byte[] address7 = new byte[] { (byte) 192, (byte) 168, 1, 19};
    private final byte[] address8 = new byte[] { (byte) 192, (byte) 168, 1, 12};
    private final byte[] address9 = new byte[] { (byte) 192, (byte) 168, 1, 5};

    @Test
    public void getters() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address1, address2);
        assertEquals("100.0.0.1", AddressUtils.convertToString(pool.getStart()));
        assertEquals("101.0.0.1", AddressUtils.convertToString(pool.getEnd()));
    }

    @Test
    public void borrowAndReturn() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address1, address2);
        byte[] address = pool.borrowAddress();
        pool.returnAddress(address);
    }

    @Test
    public void borrowAllAddresses() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);
        Set<Integer> borrowed = new HashSet<>();
        for (int c = 0; c < 10; c++) {
            if (!borrowed.add(AddressUtils.convertToInt(pool.borrowAddress()))) {
                throw new IllegalStateException("Pool gave back the same address twice!");
            }
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void borrowTooManyAddresses() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);
        for (int c = 0; c < 11; c++) {
            pool.borrowAddress();
        }
    }

    @Test
    public void addSameExclusion() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);
        for (int c = 0; c < 10; c++) {
            pool.addExclusion(address6);
        }

        Iterable<byte[]> exclusions = pool.getExclusions();
        int count = 0;
        for (byte[] address: exclusions) {
            assertArrayEquals(address6, address);
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void changeStartAddress() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);

        byte[] address = Arrays.copyOf(address8, 4);
        byte[] borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);
        address[3] = 14;
        borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);
        address[3] = 17;
        borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);

        pool.setStart(address9);

        address[3] = 12;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 14;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 17;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);

        pool.setStart(address6);

        address[3] = 12;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 14;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 17;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);

    }

    @Test
    public void changeEndAddress() {
        // 192.168.1.5 to 192.168.1.12
        SetBasedAddressPool pool = new SetBasedAddressPool(address9, address8);

        byte[] address = Arrays.copyOf(address9, 4);
        byte[] borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);
        address[3] = 7;
        borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);
        address[3] = 9;
        borrowedAddress = pool.borrowAddress(address);
        assertArrayEquals(address, borrowedAddress);

        // 192.168.1.19
        pool.setEnd(address7);
        address[3] = 5;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 7;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 9;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);

        // 192.168.1.12
        pool.setEnd(address8);
        address[3] = 5;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 7;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
        address[3] = 9;
        borrowedAddress = pool.borrowAddress(address);
        assertNull(borrowedAddress);
    }

    @Test
    public void addRemoveExclusion() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);
        assertTrue(pool.addExclusion(address6));
        assertFalse(pool.addExclusion(address6));
        assertTrue(pool.removeExclusion(address6));
        assertFalse(pool.removeExclusion(address1));

        Iterable<byte[]> exclusions = pool.getExclusions();
        int count = 0;
        for (byte[] address: exclusions) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void borrowSpecificAddress() {
        SetBasedAddressPool pool = new SetBasedAddressPool(address6, address7);
        assertNotNull(pool.borrowAddress(address7));
        assertNull(pool.borrowAddress(address7));
    }

}
