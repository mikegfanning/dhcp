package org.code_revue.dhcp.util;

import java.util.Comparator;
import java.util.StringTokenizer;

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

    public static byte[] convertToByteArray(String address) {
        StringTokenizer tokenizer = new StringTokenizer(address, ".");
        byte[] answer = new byte[tokenizer.countTokens()];
        int i = 0;
        while (tokenizer.hasMoreTokens()) {
            answer[i] = (byte) (Integer.parseInt(tokenizer.nextToken()) & 0xff);
            i++;
        }
        return answer;
    }

    public static String convertToString(byte[] address) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (byte b: address) {
            if (!first) {
                builder.append('.');
            }
            first = false;
            builder.append(b & 0xff);
        }
        return builder.toString();
    }

    public static String convertToString(int address) {
        return convertToString(convertToByteArray(address));
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

}
