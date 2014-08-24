package org.code_revue.dhcp.util;

import java.util.Comparator;

/**
 * Basically a bunch of junk for dealing with the weirdness of signed vs. unsigned bytes in Java.
 * @author Mike Fanning
 */
public class AddressUtils {

    public static final Comparator<Integer> ADDRESS_COMPARATOR = new Comparator<Integer>() {
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

    public static String hardwareAddressToString(byte[] address) {
        StringBuilder builder = new StringBuilder((address.length * 3) - 1);
        boolean cherry = false;
        for (byte segment: address) {
            if (cherry) {
                builder.append(':');
            } else {
                cherry = true;
            }
            builder.append(String.format("%02x", segment & 0xff));
        }
        return builder.toString();
    }

    public static String ipAddressToString(byte[] address) {
        StringBuilder builder = new StringBuilder(address.length * 4);
        boolean cherry = false;
        for (byte segment: address) {
            if (cherry) {
                builder.append('.');
            } else {
                cherry = true;
            }
            builder.append(segment & 0xff);
        }
        return builder.toString();
    }

}
