package org.code_revue.dhcp.message;

import java.util.Arrays;

/**
 * Simple implementation of the {@link org.code_revue.dhcp.message.DhcpOption} interface that has a byte array backing
 * it.
 * @author Mike Fanning
 */
public class ByteArrayOption implements DhcpOption {

    private final DhcpOptionType type;
    private final byte[] data;

    /**
     * Constructor that will set the type according to the first byte in the supplied array. The second byte, the
     * length, will be discarded, and the remainder of the data will be copied to the backing data array.
     * @param data
     * @throws java.lang.IllegalArgumentException If the data parameter is null or has length 0
     */
    public ByteArrayOption(byte[] data) {
        if (null == data || data.length < 2) {
            throw new IllegalArgumentException("Invalid data parameter.");
        }

        if (data[1] != (data.length - 2)) {
            throw new IllegalArgumentException("Illegal length parameter");
        }

        this.type = DhcpOptionType.getByNumericCode(data[0]);
        if (data.length > 1) {
            this.data = Arrays.copyOfRange(data, 2, data.length);
        } else {
            this.data = new byte[0];
        }
    }

    /**
     * Create a new object with supplied type and data. Not that unlike other constructor(s), the data should <b>not</b>
     * include the type byte at the beginning.
     * @param type
     * @param data
     * @throws java.lang.IllegalArgumentException If either parameter is null
     */
    public ByteArrayOption(DhcpOptionType type, byte[] data, int offset, int length) {
        if (null == type || null == data) {
            throw new IllegalArgumentException("Invalid null parameter");
        }

        this.type = type;
        this.data = Arrays.copyOfRange(data, offset, offset + length);
    }

    public ByteArrayOption(DhcpOptionType type, byte[] data) {
        this(type, data, 0, data.length);
    }

    @Override
    public DhcpOptionType getType() {
        return type;
    }

    @Override
    public byte[] getOptionData() {
        return data;
    }
}
