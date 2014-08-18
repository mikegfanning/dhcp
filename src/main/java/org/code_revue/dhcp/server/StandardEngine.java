package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @author Mike Fanning
 */
public class StandardEngine extends AbstractEngine {

    private static final Logger logger = LoggerFactory.getLogger(StandardEngine.class);
    private static final byte[] EMPTY_ADDRESS = new byte[] { 0, 0, 0, 0 };
    private static final SocketAddress BROADCAST_ADDRESS;

    static {
        SocketAddress address = null;
        try {
            address = new InetSocketAddress(InetAddress.getByAddress(
                    new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 255 }), 68);
        } catch (UnknownHostException e) {
            logger.error("Could not resolve broadcast address", e);
            address = InetSocketAddress.createUnresolved("255.255.255.255", 68);
        } finally {
            BROADCAST_ADDRESS = address;
        }
    }

    private List<DhcpAddressPool> pools = new ArrayList<>();

    private Map<DhcpOptionType, DhcpOption> configuration = new HashMap<>();

    @Override
    protected DhcpPayload handleDhcpDiscover(DhcpMessageOverlay message, DhcpOption reqAddr, DhcpOption paramList) {

        // Validate message, register device, borrow address from pool, return DHCP Offer
        if (!Arrays.equals(EMPTY_ADDRESS, message.getClientIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getYourIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getServerIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getGatewayIpAddress())) {
            return null;
        }

        // TODO: Register device and state

        byte[] borrowedAddress = null;

        if (null != reqAddr) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress(reqAddr.getOptionData());
                if (null != borrowedAddress) {
                    break;
                }
            }
        }

        if (null == borrowedAddress) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress();
                if (null != borrowedAddress) {
                    break;
                }
            }
        }

        DhcpPayload response = null;

        if (null != borrowedAddress) {
            DhcpMessageBuilder builder = new DhcpMessageBuilder();
            builder.setOpCode(DhcpOpCode.REPLY)
                    .setHardwareType(HardwareType.ETHERNET)
                    .setTransactionId(message.getTransactionId())
                    .setYourIpAddress(borrowedAddress)
                    .setServerIpAddress(serverIpAddress)
                    .setHardwareAddress(hardwareAddress);

            if (null != paramList) {
                byte[] parameterList = paramList.getOptionData();
                for (byte param: parameterList) {
                    DhcpOption responseOption = getConfiguration(DhcpOptionType.getByNumericCode(param));
                    if (null != responseOption) {
                        builder.addOption(responseOption);
                    }
                }
            }

            response = new DhcpPayload(BROADCAST_ADDRESS, builder.build());
        }

        return response;
    }

    @Override
    protected DhcpPayload handleDhcpRequest(DhcpMessageOverlay message) {

        // Validate message, update device status, if the requested address is valid, return DHCP Acknowledgement,
        // otherwise, DHCP NAK
        if (!Arrays.equals(EMPTY_ADDRESS, message.getClientIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getYourIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getGatewayIpAddress())) {
            return null;
        }

        if (!Arrays.equals(serverIpAddress, message.getServerIpAddress())) {
            return null;
        }

        // TODO: Lookup device status and compare transaction id, etc.

        // TODO: Lookup previously assigned IP address and configuration information

        return null;
    }

    @Override
    protected void handleDhcpDecline(DhcpMessageOverlay message) {

        // Validate message, update device status to reflect that it will not use the supplied IP address and return it
        // to the pool

    }

    @Override
    protected void handleDhcpRelease(DhcpMessageOverlay message) {

        // Validate message, update device, and return address to pool

    }

    @Override
    protected DhcpPayload handleDhcpInform(DhcpMessageOverlay message) {

        // Validate message, update device status and send back DHCP Acknowledgement with requested configuration info

        return null;
    }

    public void addAddressPool(DhcpAddressPool pool) {
        pools.add(pool);
    }

    public boolean removeAddressPool(DhcpAddressPool pool) {
        return pools.remove(pool);
    }

    /**
     * This method sets a DHCP configuration option. When a client sends a DHCP Discover or Inform message seeking
     * configuration options, this mapping will determine the response. Note that the response type of the supplied
     * {@link org.code_revue.dhcp.message.DhcpOption} need not match the request type of the
     * {@link org.code_revue.dhcp.message.DhcpOptionType}.
     * @param optionType Requested configuration option type, usually via the parameter list in DHCP Discover or Inform
     *                   message types
     * @param option Response value
     */
    public void setConfiguration(DhcpOptionType optionType, DhcpOption option) {
        configuration.put(optionType, option);
    }

    /**
     * Returns the DHCP configuration parameter that this engine will return for the given option type.
     * @param optionType Requested configuration option type
     * @return Server configuration option for the supplied type
     */
    public DhcpOption getConfiguration(DhcpOptionType optionType) {
        return configuration.get(optionType);
    }

    /**
     * Removes a configuration option from the server's set of parameters.
     * @param optionType Requested configuration option type
     * @return Option previously associated with this type
     */
    public DhcpOption removeConfiguration(DhcpOptionType optionType) {
        return configuration.remove(optionType);
    }
}
