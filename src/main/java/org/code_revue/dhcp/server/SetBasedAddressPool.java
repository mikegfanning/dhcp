package org.code_revue.dhcp.server;

import org.code_revue.dhcp.util.AddressUtils;

import java.util.*;

/**
 * Implementation of {@link org.code_revue.dhcp.server.DhcpAddressPool} that hands out IPv4 addresses within the
 * specified range, excluding addresses that have specifically been set aside via the "exclusions" mechanism. This
 * implementation is not thread safe, and will not throw any exceptions related to address ranges (e.g. when the start
 * address is after the end address), making it more suitable for ORM frameworks.
 * <p>
 * The {@link org.code_revue.dhcp.server.BitSetAddressPool} uses less memory in worst-case scenarios, but this shouldn't
 * be an issue unless there are severe memory constraints or a lot of hosts.
 * </p>
 *
 * @author Mike Fanning
 * @see {@link org.code_revue.dhcp.server.BitSetAddressPool}
 */
public class SetBasedAddressPool implements DhcpAddressPool {

    private int start, end;

    private NavigableSet<Integer> borrowed = new TreeSet<>();

    private Set<Integer> exclusions = new HashSet<>();

    /**
     * Creates an empty pool with no start/end addresses or exclusions.
     */
    public SetBasedAddressPool() { }

    /**
     * Creates an empty pool from the supplied start address to the end addresses, with no exclusions.
     * @param start
     * @param end
     */
    public SetBasedAddressPool(byte[] start, byte[] end) {
        setStart(start);
        setEnd(end);
    }

    @Override
    public byte[] borrowAddress() {
        int addr = start;
        Iterator<Integer> it = borrowed.iterator();
        while (it.hasNext()) {
            int next = it.next();
            if (next == addr) {
                addr++;
            } else {
                break;
            }
        }

        if (addr > end) {
            throw new IndexOutOfBoundsException("No addresses are available");
        }
        borrowed.add(addr);
        return AddressUtils.convertToByteArray(addr);
    }

    @Override
    public byte[] borrowAddress(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        if (borrowed.contains(addr)) {
            return null;
        }

        borrowed.add(addr);
        return address;
    }

    @Override
    public void returnAddress(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        if (!exclusions.contains(addr)) {
            borrowed.remove(addr);
        }
    }

    /**
     * Get the beginning address of this DHCP pool, inclusive.
     * @return Start address
     */
    public byte[] getStart() {
        return AddressUtils.convertToByteArray(start);
    }

    /**
     * Set the first address of this DHCP pool, inclusive.
     * @param start Start address
     */
    public void setStart(byte[] start) {
        this.start = AddressUtils.convertToInt(start);
    }

    /**
     * Get the end address of this DHCP pool, inclusive.
     * @return End Address
     */
    public byte[] getEnd() {
        return AddressUtils.convertToByteArray(end);
    }

    /**
     * Set the end address of this DHCP pool, inclusive.
     * @param end End Address
     */
    public void setEnd(byte[] end) {
        this.end = AddressUtils.convertToInt(end);
    }

    /**
     * Gets the excluded addresses that will not be handed out by this pool when the {@link #borrowAddress()} or
     * {@link #borrowAddress(byte[])} methods are invoked.
     * @return Addresses that are excluded
     */
    public Iterable<byte[]> getExclusions() {
        // My special Iterator adapter that converted the integers to byte[] doesn't play nice with jsp. Argh.
        List<byte[]> answer = new ArrayList<>();
        for (Integer e: exclusions) {
            answer.add(AddressUtils.convertToByteArray(e));
        }
        return answer;
    }

    private class ConvertingIterator implements Iterator<byte[]> {
        private final Iterator<Integer> it;

        public ConvertingIterator(Iterator<Integer> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public byte[] next() {
            return AddressUtils.convertToByteArray(it.next());
        }
    }

    /**
     * Add an exclusion to the list of addresses that will not be returned when the {@link #borrowAddress()} or
     * {@link #borrowAddress(byte[])} methods are invoked.
     * @param address Excluded address
     * @return Indicates whether the exclusions was already present
     */
    public boolean addExclusion(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        borrowAddress(address);
        return exclusions.add(addr);
    }

    /**
     * Removes an exclusion from the list of addresses that will not be returned via the {@link #borrowAddress()} or
     * {@link #borrowAddress(byte[])} methods.
     * @param address Address to remove from exclusion list
     * @return True if the address was in the exclusion list, false otherwise
     */
    public boolean removeExclusion(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        boolean removed = exclusions.remove(addr);
        returnAddress(address);
        return removed;
    }
}
