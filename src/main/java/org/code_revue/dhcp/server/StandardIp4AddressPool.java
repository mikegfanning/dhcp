package org.code_revue.dhcp.server;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * @author Mike Fanning
 */
public class StandardIp4AddressPool {

    private int start, end;
    // TODO: Replace this BitSet with something thread safe
    private BitSet flags;

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

        this.start = ByteBuffer.wrap(start).getInt();
        this.end = ByteBuffer.wrap(end).getInt();

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
     * Borrow an address from the pool. This will prevent the pool from lending out the address again until it has been
     * returned via the {@link #returnAddress(byte[])} method.
     * @return IPv4 address
     * @throws java.lang.IndexOutOfBoundsException If there are no available addresses
     */
    public byte[] borrowAddress() {
        if (flags.cardinality() == (end - start + 1)) {
            throw new IndexOutOfBoundsException("No addresses are available");
        }
        int offset = flags.nextClearBit(0);
        flags.set(offset);
        return ByteBuffer.wrap(new byte[4]).putInt(start + offset).array();
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
        int offset = ByteBuffer.wrap(address).getInt() - start;
        if (offset > 0 && offset < flags.size()) {
            flags.clear(offset);
        }
    }

}
