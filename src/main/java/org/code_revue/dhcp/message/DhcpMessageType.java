package org.code_revue.dhcp.message;

/**
 * Enumeration of different DHCP message types.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_SummaryOfDHCPOptionsBOOTPVendorInformationFields-6.htm">
 *     http://www.tcpipguide.com/free/t_SummaryOfDHCPOptionsBOOTPVendorInformationFields-6.htm</a>
 */
public enum DhcpMessageType {

    DISCOVER(1),
    OFFER(2),
    REQUEST(3),
    DECLINE(4),
    ACK(5),
    NAK(6),
    RELEASE(7),
    INFORM(8);

    private final int code;

    private final DhcpOption option;

    private DhcpMessageType(int codeNum) {
        this.code = codeNum;
        this.option = new DhcpOption() {
            @Override
            public DhcpOptionType getType() {
                return DhcpOptionType.MESSAGE_TYPE;
            }

            @Override
            public byte[] getOptionData() {
                return new byte[] { (byte) code };
            }
        };
    }

    /**
     * Get the numeric code for this message type.
     * @return
     */
    public int getNumericCode() {
        return code;
    }

    /**
     * Get the DHCP message type by its numeric code.
     * @param code
     * @return
     * @throws java.lang.IllegalArgumentException If the code is not valid
     */
    public static DhcpMessageType getByNumericCode(int code) {
        if (code < 1 || code > 8) {
            throw new IllegalArgumentException("Invalid numeric code.");
        }
        return DhcpMessageType.values()[code - 1];
    }

    /**
     * Utility method for getting a {@link org.code_revue.dhcp.message.DhcpOption} without having to go through the
     * trouble of constructing one yourself.
     * @return Option for the appropriate message type
     */
    public DhcpOption getOption() {
        return option;
    }

}
