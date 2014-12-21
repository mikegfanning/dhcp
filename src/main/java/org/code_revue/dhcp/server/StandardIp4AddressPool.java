package org.code_revue.dhcp.server;

import org.code_revue.dhcp.util.AddressUtils;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Implementation of {@link org.code_revue.dhcp.server.DhcpAddressPool} for IPv4 addresses. The pool hands out addresses
 * from a range, as defined by the start and end parameters of the constructor. It also has a set of excluded addresses
 * that the pool will not issue.
 *
 * @author Mike Fanning
 */
public class StandardIp4AddressPool implements DhcpAddressPool {

    private int start, end;
    private BitSet flags;

    // Comparator will keep the integer representation of the IP addresses in the appropriate order.
    Set<Integer> exclusions = new ConcurrentSkipListSet<>(AddressUtils.ADDRESS_COMPARATOR);

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

        this.start = AddressUtils.convertToInt(start);
        this.end = AddressUtils.convertToInt(end);

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
     * Creates a new IPv4 address pool with the supplied start and end addresses.
     * @param start Starting IP address, inclusive
     * @param end Ending IP address, inclusive
     * @throws java.lang.IllegalArgumentException If the addresses are malformed or the start is after the end
     */
    public StandardIp4AddressPool(String start, String end) {
        this(AddressUtils.convertToByteArray(start), AddressUtils.convertToByteArray(end));
    }

    /**
     * Returns the starting address of the address pool represented as an integer.
     * @return
     */
    public byte[] getStart() {
        return AddressUtils.convertToByteArray(start);
    }

    /**
     * Change the beginning address of the pool. This will preserve the state of borrowed addresses if the new starting
     * address is less than (in IPv4 terms) the previous start address. Otherwise, the state of borrowed addresses will
     * only be preserved for borrows that are greater than or equal to the new starting address.
     * @param address Starting IP address of the pool, inclusive
     */
    public void setStart(byte[] address) {
        int newStart = AddressUtils.convertToInt(address);

        synchronized (this) {
            if (newStart == this.start) {
                return;
            }

            if (newStart > this.end && !(newStart >= 0 && this.end < 0)) {
                throw new IllegalArgumentException("Start Address is after End Address");
            }

            int range = this.end - newStart + 1;
            if (range <= 0) {
                throw new IllegalArgumentException("Address range is too large");
            }

            this.start = newStart;
            BitSet newFlags = new BitSet(range);
            // Copy flags, starting from the end and working towards the beginning.
            for (int i = 0; i < Math.min(this.flags.length(), range); i++) {
                newFlags.set(range - i - 1, this.flags.get(this.flags.length() - 1));
            }
            this.flags = newFlags;
        }
    }

    /**
     * Returns the ending address of the address pool represented as an integer.
     * @return
     */
    public byte[] getEnd() {
        return AddressUtils.convertToByteArray(end);
    }

    /**
     * Change the ending address of the pool. This will preserve the state of borrowed addresses if the new end address
     * is greater than (in IPv4 terms) the previous end address. Otherwise, the state of borrowed addesses will only be
     * preserved if the borrowed address is less than or equal to the new end address.
     * @param address Ending IP address of the pool, inclusive
     */
    public synchronized void setEnd(byte[] address) {
        int newEnd = AddressUtils.convertToInt(address);
        synchronized (this) {
            if (newEnd == this.end) {
                return;
            }

            if (this.start > newEnd && !(this.start >= 0 && newEnd < 0)) {
                throw new IllegalArgumentException("Start Address is after End Address");
            }

            int range = newEnd - this.start + 1;
            if (range <= 0) {
                throw new IllegalArgumentException("Address range is too large");
            }

            this.end = newEnd;
            BitSet newFlags = new BitSet(range);
            newFlags.or(this.flags);
            this.flags = newFlags;
        }
    }

    /**
     * Adds an address exclusion to the pool. This will prevent the pool from lending this address to callers.
     * @param address IPv4 address to exclude from the pool
     * @return If the address was not already excluded
     */
    public boolean addExclusion(byte[] address) {
        int addr = AddressUtils.convertToInt(address);

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
        int addr = AddressUtils.convertToInt(address);
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
            result.add(AddressUtils.convertToByteArray(i));
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

        return AddressUtils.convertToByteArray(address);
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

        int addr = AddressUtils.convertToInt(address);

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
        int offset = AddressUtils.convertToInt(address) - start;
        synchronized (this) {
            if (offset > 0 && offset < flags.size()) {
                flags.clear(offset);
            }
        }
    }

}
