package org.code_revue.dhcp.server;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Mike Fanning
 */
public class StandardIp4AddressPool {

    private int start, end;
    // TODO: Replace this BitSet with something thread safe
    private BitSet flags;

    // Comparator will keep the integer representation of the IP addresses in the appropriate order.
    Set<Integer> exclusions = new ConcurrentSkipListSet<>(new Comparator<Integer>() {
        @Override
        public int compare(Integer i1, Integer i2) {
            if (i1 >> 31 == i2 >> 31) {
                return i1 - i2;
            } else {
                return i2;
            }
        }
    });

    /**
     * Creates a new IPv4 address pool with the supplied start and end addresses.
     * @param start Starting IP address, inclusive
     * @param end Ending IP address, inclusive
     * @throws java.lang.IllegalArgumentException If the addresses are malformed or the start is after the end
     */
    public StandardIp4AddressPool(byte[] start, byte[] end) {
        if (start.length != 4 || end.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 Address");
        }

        this.start = convertToInt(start);
        this.end = convertToInt(end);

        if (this.start > this.end && !(this.start >= 0 && this.end < 0)) {
            throw new IllegalArgumentException("Start Address is after End Address");
        }

        int range = this.end - this.start + 1;
        if (range <= 0) {
            throw new IllegalArgumentException("Address range is too large");
        }

        this.flags = new BitSet(range);
    }

    /**
     * Adds an address exclusion to the pool. This will prevent the pool from lending this address to callers.
     * @param address IPv4 address to exclude from the pool
     * @return If the address was not already excluded
     */
    public boolean addExclusion(byte[] address) {
        int addr = convertToInt(address);

        synchronized (this) {
            if (addr >= start && addr <= end) {
                flags.set(addr - start);
            }
        }

        return exclusions.add(addr);
    }

    /**
     * Removes an address exclusion from the pool. This will allow the address to be borrowed by callers, provided it is
     * within the pool's range.
     * @param address IPv4 address to remove from the exclusion list
     * @return If the address was previously excluded
     */
    public boolean removeExclusion(byte[] address) {
        int addr = convertToInt(address);
        boolean removed = exclusions.remove(addr);

        synchronized (this) {
            if (removed && addr >= start && addr <= end) {
                flags.clear(addr - start);
            }
        }

        return removed;
    }

    /**
     * Get an {@link java.lang.Iterable} of the addresses that have been excluded from this pool. The return value will
     * be sorted from least (0.0.0.0) to greatest (255.255.255.255).
     * @return Iterable of IPv4 addresses
     */
    public Iterable<byte[]> getExclusions() {
        List<byte[]> result = new ArrayList<>();
        for (Integer i: exclusions) {
            result.add(convertToByteArray(i));
        }
        return result;
    }

    /**
     * Borrow an address from the pool. This will prevent the pool from lending out the address again until it has been
     * returned via the {@link #returnAddress(byte[])} method.
     * @return IPv4 address
     * @throws java.lang.IndexOutOfBoundsException If there are no available addresses
     */
    public byte[] borrowAddress() {

        int address;

        synchronized (this) {
            if (flags.cardinality() == (end - start + 1)) {
                throw new IndexOutOfBoundsException("No addresses are available");
            }
            int offset = flags.nextClearBit(0);
            address = start + offset;
            flags.set(offset);
        }

        return convertToByteArray(address);
    }

    /**
     * Attempts to borrow a specific address from the pool. If possible, the address is returned, otherwise null is
     * returned.
     * @param address IPv4 address to borrow
     * @return Address or null if not available
     * @throws java.lang.IllegalArgumentException If address is malformed
     */
    public byte[] borrowAddress(byte[] address) {
        if (address.length != 4) {
            throw new IllegalArgumentException("Invalid Address");
        }

        int addr = convertToInt(address);

        synchronized (this) {
            if (addr < start || addr > end || flags.get(addr - start)) {
                return null;
            }
            flags.set(addr - start);
        }
        return address;
    }

    /**
     * Returns an address that has already been borrowed to the pool. If the address is within range, this will release
     * it and allow others to borrow it via the {@link #borrowAddress()} method. Note that returning an address that is
     * out of the range of the pool will not throw any exceptions. This should allow the start and end of the range to
     * be modified without creating a bunch of exceptions when addresses are returned.
     * @param address
     * @throws java.lang.IllegalArgumentException If the address is malformed
     */
    public void returnAddress(byte[] address) {
        if (address.length != 4) {
            throw new IllegalArgumentException("Invalid Address");
        }
        int offset = convertToInt(address) - start;
        synchronized (this) {
            if (offset > 0 && offset < flags.size()) {
                flags.clear(offset);
            }
        }
    }

    private int convertToInt(byte[] address) {
        return ByteBuffer.wrap(address).getInt();
    }

    private byte[] convertToByteArray(int address) {
        return ByteBuffer.wrap(new byte[4]).putInt(address).array();
    }

}
