package org.code_revue.dhcp.util;

import java.util.Comparator;

/**
 * @author Mike Fanning
 */
public class AddressUtils {

    public static final Comparator<Integer> addressComparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer i1, Integer i2) {
            if (i1 >> 31 == i2 >> 31) {
                return i1 - i2;
            } else {
                return i2;
            }        }
    };

    public static int convertToInt(byte[] address) {
        return   address[3] & 0xff |
                (address[2] & 0xff) << 8 |
                (address[1] & 0xff) << 16 |
                (address[0] & 0xff) << 24;
    }

    public static byte[] convertToByteArray(int address) {
        return new byte[] {
                (byte) ((address >> 24) & 0xff),
                (byte) ((address >> 16) & 0xff),
                (byte) ((address >> 8) & 0xff),
                (byte) (address & 0xff)
        };
    }

}
