package org.code_revue.dhcp.util;

import org.code_revue.dhcp.message.DhcpMessageOverlay;
import org.code_revue.dhcp.message.DhcpOption;

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

    public static String prettyPrintDhcpMessage(ByteBuffer buffer) {
        return prettyPrintDhcpMessage(new DhcpMessageOverlay(buffer));
    }

    public static String prettyPrintDhcpMessage(DhcpMessageOverlay overlay) {
        StringBuilder builder = new StringBuilder();
        builder.append("OP Code: ").append(overlay.getOpCode()).append('\n')
                .append("Hardware Type: ").append(overlay.getHardwareType()).append('\n')
                .append("Hops: ").append(overlay.getHops()).append('\n')
                .append("Transaction ID: ").append(toHexString(overlay.getTransactionId(), ' ')).append('\n')
                .append("Seconds: ").append(overlay.getSeconds()).append('\n')
                .append("Broadcast: ").append(overlay.isBroadcast()).append('\n')
                .append("Client IP Address: ").append(ipAddressToString(overlay.getClientIpAddress())).append('\n')
                .append("Your IP Address: ").append(ipAddressToString(overlay.getYourIpAddress())).append('\n')
                .append("Server IP Address: ").append(ipAddressToString(overlay.getServerIpAddress())).append('\n')
                .append("Gateway IP Address: ").append(ipAddressToString(overlay.getGatewayIpAddress())).append('\n')
                .append("Client Hardware Address: ").append(toHexString(overlay.getClientHardwareAddress(), ':'))
                .append('\n')
                .append("Magic Cookie: ").append(toHexString(overlay.getMagicCookie()))
                .append("\n\nOptions\n-------\n");

        for (DhcpOption option: overlay.getOptions().values()) {
            builder.append(option.getType()).append(": ").append(toHexString(option.getOptionData(), ' ')).append('\n');
        }
        return builder.toString();
    }

    /**
     * Shamelessly ripped off from
     * <a href="http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java">here</a>.
     * @param data
     * @param separator
     * @return
     */
    public static String toHexString(byte[] data, char separator) {
        char[] hexChars;
        if (data.length > 0) {
            hexChars = new char[(data.length * 3) - 1];
        } else {
            return "";
        }
        for (int j = 0; j < data.length; j++) {
            int v = data[j] & 0xff;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            if (j != data.length - 1) {
                hexChars[j * 3 + 2] = separator;
            }
        }
        return new String(hexChars);
    }

    /**
     * Shamelessly ripped off from
     * <a href="http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java">here</a>.
     * @param data
     * @return
     */
    public static String toHexString(byte[] data) {
        char[] hexChars = new char[data.length * 2];
        for (int j = 0; j < data.length; j++) {
            int v = data[j] & 0xff;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String toHexString(int i, char separator) {
        return toHexString(convertToByteArray(i), separator);
    }

    public static String toHexString(int i) {
        return toHexString(convertToByteArray(i));
    }

    public static byte[] convertToByteArray(int address) {
        return new byte[] {
                (byte) ((address >> 24) & 0xff),
                (byte) ((address >> 16) & 0xff),
                (byte) ((address >> 8) & 0xff),
                (byte) (address & 0xff)
        };
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
