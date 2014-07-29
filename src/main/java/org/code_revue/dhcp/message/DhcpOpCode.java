package org.code_revue.dhcp.message;

/**
 * Enumeration of operation codes for DHCP messages.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm">
 *     http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm</a>
 */
public enum DhcpOpCode {

    REQUEST(1),
    REPLY(2);

    private final int numericCode;

    private DhcpOpCode(int numericCode) {
        this.numericCode = numericCode;
    }

    public int getNumericCode() {
        return numericCode;
    }

    public static DhcpOpCode getByNumericCode(int numericCode) {
        if (1 == numericCode) {
            return REQUEST;
        } else if (2 == numericCode) {
            return REPLY;
        } else {
            throw new IllegalArgumentException("Invalid DHCP Op Code.");
        }
    }

}
