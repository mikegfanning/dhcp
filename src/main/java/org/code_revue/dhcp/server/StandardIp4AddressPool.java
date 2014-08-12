package org.code_revue.dhcp.server;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author Mike Fanning
 */
public class StandardIp4AddressPool {

    private int start, end;
    // TODO: Replace this BitSet with something thread safe
    private BitSet flags;

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

    public byte[] borrowAddress() {
        if (flags.cardinality() == flags.size()) {
            throw new IndexOutOfBoundsException("No addresses are available");
        }
        int offset = flags.nextClearBit(0);
        flags.set(offset);
        return ByteBuffer.wrap(new byte[4]).putInt(start + offset).array();
    }

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
