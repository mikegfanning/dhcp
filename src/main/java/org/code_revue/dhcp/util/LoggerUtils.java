package org.code_revue.dhcp.util;

import org.slf4j.Logger;

import java.nio.ByteBuffer;

/**
 * Some utility methods to simplify logging.
 *
 * @author Mike Fanning
 */
public class LoggerUtils {

    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String bufferToHexString(ByteBuffer buffer) {
        int initialPosition = buffer.position();
        buffer.position(0);

        int numLines = (buffer.capacity() / 16) + Math.min(1, buffer.capacity() % 16);
        StringBuilder builder = new StringBuilder((numLines * 10) + (buffer.capacity() * 3));
        for (int p = 0; p < buffer.capacity(); p++) {
            if (p % 16 == 0) {
                if (p != 0) {
                    builder.append('\n');
                }
                builder.append(String.format("%08x", p));
            }
            int v = buffer.get(p) & 0xff;
            if (p % 8 == 0 && p % 16 != 0) {
                builder.append(' ');
            }
            builder.append(' ').append(hexArray[v >>> 4]).append(hexArray[v & 0x0f]);
        }

        buffer.position(initialPosition);
        return builder.toString();
    }

}
