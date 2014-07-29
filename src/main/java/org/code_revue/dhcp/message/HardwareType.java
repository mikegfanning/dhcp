package org.code_revue.dhcp.message;

/**
 * Enumeration of different hardware types accepted for DHCP.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm">
 *     http://www.tcpipguide.com/free/t_DHCPMessageFormat.htm</a>
 */
public enum HardwareType {

    ETHERNET(1),
    IEEE802(6),
    ARCNET(7),
    LOCALTALK(11),
    LOCALNET(12),
    SMDS(14),
    FRAME_RELAY(15),
    ATM(16),
    HDLC(17),
    FIBRE(18),
    ATM2(19),
    SERIAL(20);

    private final int numericCode;

    private HardwareType(int numericCode) {
        this.numericCode = numericCode;
    }

    public int getNumericCode() {
        return numericCode;
    }

    public static HardwareType getByNumericCode(int code) {
        for (HardwareType type: HardwareType.values()) {
            if (code == type.getNumericCode()) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid Hardware Type code.");
    }

}
