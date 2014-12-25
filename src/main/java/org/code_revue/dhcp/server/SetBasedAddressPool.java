package org.code_revue.dhcp.server;

import org.code_revue.dhcp.util.AddressUtils;

import java.util.*;

/**
 * @author Mike Fanning
 */
public class SetBasedAddressPool implements DhcpAddressPool {

    private int start, end;

    private NavigableSet<Integer> borrowed = new TreeSet<>();

    private Set<Integer> exclusions = new HashSet<>();

    public SetBasedAddressPool() { }

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

    public byte[] getStart() {
        return AddressUtils.convertToByteArray(start);
    }

    public void setStart(byte[] start) {
        this.start = AddressUtils.convertToInt(start);
    }

    public byte[] getEnd() {
        return AddressUtils.convertToByteArray(end);
    }

    public void setEnd(byte[] end) {
        this.end = AddressUtils.convertToInt(end);
    }

    public Iterable<byte[]> getExclusions() {
        return new Iterable<byte[]>() {
            @Override
            public Iterator<byte[]> iterator() {
                return new ConvertingIterator(exclusions.iterator());
            }
        };
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

    public boolean addExclusion(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        borrowAddress(address);
        return exclusions.add(addr);
    }

    public boolean removeExclusion(byte[] address) {
        int addr = AddressUtils.convertToInt(address);
        boolean removed = exclusions.remove(addr);
        returnAddress(address);
        return removed;
    }
}
