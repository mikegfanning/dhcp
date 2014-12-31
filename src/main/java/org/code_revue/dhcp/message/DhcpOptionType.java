package org.code_revue.dhcp.message;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Enumeration of DHCP options.
 *
 * @author Mike Fanning
 * @see <a href="http://www.tcpipguide.com/free/t_SummaryOfDHCPOptionsBOOTPVendorInformationFields-2.htm">
 *     http://www.tcpipguide.com/free/t_SummaryOfDHCPOptionsBOOTPVendorInformationFields-2.htm</a>
 */
public enum DhcpOptionType {

    PAD(0),
    SUBNET_MASK(1),
    TIME_OFFSET(2),
    ROUTER(3),
    TIME_SERVER(4),
    IEN_116_NAME_SERVER(5),
    DNS_SERVER(6),
    LOG_SERVER(7),
    COOKIE_SERVER(8),
    LPR_SERVER(9),
    IMPRESS_SERVER(10),
    RESOURCE_LOCATION_SERVER(11),
    HOST_NAME(12),
    BOOT_FILE_SIZE(13),
    MERIT_DUMP_FILE(14),
    DOMAIN_NAME(15),
    SWAP_SERVER(16),
    ROOT_PATH(17),
    EXTENSIONS_PATH(18),
    IP_FORWARDING(19),
    NON_LOCAL_SOURCE_ROUTING(20),
    POLICY_FILTER(21),
    MAX_DGRAM_REASSEMBLY_SIZE(22),
    DEFAULT_IP_TTL(23),
    PATH_MTU_AGING_TIMEOUT(24),
    PATH_MTU_PLATEAU_TABLE(25),
    INTERFACE_MTU(26),
    SUBNETS_LOCAL(27),
    BROADCAST_ADDR(28),
    MASK_DISCOVERY(29),
    MASK_SUPPLIER(30),
    ROUTER_DISCOVERY(31),
    ROUTER_SOLICITATION_ADDR(32),
    STATIC_ROUTE(33),
    TRAILER_ENCAPSULATION(34),
    ARP_CACHE_TIMEOUT(35),
    ETHERNET_ENCAPSULATION(36),
    DEFAULT_TTL(37),
    TCP_KEEPALIVE_INTERVAL(38),
    TCP_KEEPALIVE_GARBAGE(39),
    NIS_DOMAIN(40),
    NIS_SERVER(41),
    NTP_SERVER(42),
    VENDOR_SPECIFIC(43),
    NETBIOS_NAME_SERVER(44),
    NETBIOS_DIST_DGRAM_SERVER(45),
    NETBIOS_NODE_TYPE(46),
    NETBIOS_SCOPE(47),
    X_WINDOWS_FONT_SERVER(48),
    X_WINDOWS_DISP_MANAGER(49),
    REQUESTED_IP_ADDR(50),
    IP_ADDR_LEASE_TIME(51),
    OVERLOAD(52),
    MESSAGE_TYPE(53),
    SERVER_ID(54),
    PARAMETER_REQUEST_LIST(55),
    MESSAGE(56),
    MAX_DHCP_MESSAGE_SIZE(57),
    RENEWAL_TIME_VALUE(58),
    REBINDING_TIME_VALUE(59),
    VENDOR_CLASS_ID(60),
    CLIENT_ID(61),
    TFTP_SERVER_NAME(66),
    BOOTFILE_NAME(67),
    CLIENT_FQDN(81),
    LDAP(95),
    DNS_SEARCH_DOMAIN(119),
    CLASSLESS_STATIC_ROUTES(121),
    RESERVED(253),
    END(255);

    private final int numericCode;

    private static final Comparator<Object> comp = new Comparator<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return ((DhcpOptionType) o1).getNumericCode() - ((int) o2);
        }
    };

    private DhcpOptionType(int numericCode) {
        this.numericCode = numericCode;
    }

    /**
     * Gets the numeric code for this option.
     * @return
     */
    public int getNumericCode() {
        return numericCode;
    }

    /**
     * Gets an option by its numeric value.
     * @param code
     * @return
     */
    public static DhcpOptionType getByNumericCode(int code) {
        int index = Arrays.binarySearch(DhcpOptionType.values(), code, comp);
        if (code >= 224 && code <= 254) {
            return DhcpOptionType.RESERVED;
        } else if (index < 0) {
            throw new IllegalArgumentException("Invalid numeric code: " + code);
        }
        return DhcpOptionType.values()[index];
    }

    /**
     * Convenience method when working with byte-oriented data ({@link java.nio.ByteBuffer}s, byte arrays, things like
     * that). This will automatically converted the signed parameter into an integer, so the byte value -1 will become
     * 255 and such.
     * @param code
     * @return
     */
    public static DhcpOptionType getByNumericCode(byte code) {
        return getByNumericCode(code & 0xff);
    }

}
