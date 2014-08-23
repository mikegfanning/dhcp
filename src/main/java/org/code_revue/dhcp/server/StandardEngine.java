package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.code_revue.dhcp.util.AddressUtils;
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

    public static final int DEFAULT_TTL = 86400;

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

    private Map<String, NetworkDevice> devices = new HashMap<>();

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

        NetworkDevice device = devices.get(AddressUtils.hardwareAddressToString(message.getClientHardwareAddress()));
        if (null == device) {
            device = new NetworkDevice();
            device.setHardwareAddress(message.getClientHardwareAddress());
            device.setStatus(DeviceStatus.DISCOVERED);
            devices.put(AddressUtils.hardwareAddressToString(device.getHardwareAddress()), device);
        } else if (DeviceStatus.OFFERED.equals(device.getStatus()) ||
                DeviceStatus.ACKNOWLEDGED.equals(device.getStatus()) ){
            // If the device has already been offered a lease or has acknowledged it we'll ignore subsequent discover
            // messages until the lease expires.
            return null;
        }

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
                    .setHardwareAddress(message.getClientHardwareAddress());

            Map<DhcpOptionType, DhcpOption> offeredOptions = new HashMap<>();
            if (null != paramList) {
                byte[] parameterList = paramList.getOptionData();
                for (byte param: parameterList) {
                    DhcpOptionType offeredOptionType = DhcpOptionType.getByNumericCode(param);
                    DhcpOption offeredOption = getConfiguration(offeredOptionType);
                    if (null != offeredOption) {
                        offeredOptions.put(offeredOptionType, offeredOption);
                        builder.addOption(offeredOption);
                    }
                }
                offeredOptions.put(DhcpOptionType.MESSAGE_TYPE, DhcpMessageType.DHCP_OFFER.getOption());
                builder.addOption(DhcpMessageType.DHCP_OFFER.getOption());
            }

            response = new DhcpPayload(BROADCAST_ADDRESS, builder.build());

            // Update device state.
            device.setStatus(DeviceStatus.OFFERED);
            device.setIpAddress(borrowedAddress);
            Calendar expiration = Calendar.getInstance();
            expiration.add(Calendar.SECOND, DEFAULT_TTL);
            device.setLeaseExpiration(expiration.getTime());
            device.setTransactionId(message.getTransactionId());
            device.setOptions(offeredOptions);
        }

        return response;
    }

    @Override
    protected DhcpPayload handleDhcpRequest(DhcpMessageOverlay message, DhcpOption serverId) {

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

        NetworkDevice device = devices.get(AddressUtils.hardwareAddressToString(message.getClientHardwareAddress()));
        if (null == device) {
            return null;
        }

        if (null == serverId) {
            return null;
        } else if (!Arrays.equals(serverId.getOptionData(), serverIpAddress)) {
            // Client is going to use another DHCP server. We can return the address we assigned to it to the pool.
            device.setStatus(DeviceStatus.DISCOVERED);
            byte[] offeredAddress = device.getIpAddress();
            for (DhcpAddressPool pool: pools) {
                pool.returnAddress(offeredAddress);
            }
            return null;
        }

        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REPLY)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(message.getTransactionId())
                .setYourIpAddress(device.getIpAddress())
                .setServerIpAddress(serverIpAddress)
                .setHardwareAddress(message.getClientHardwareAddress());

        Map<DhcpOptionType, DhcpOption> options = device.getOptions();
        options.put(DhcpOptionType.MESSAGE_TYPE, DhcpMessageType.DHCP_ACK.getOption());
        for (DhcpOption option: options.values()) {
            builder.addOption(option);
        }

        return new DhcpPayload(BROADCAST_ADDRESS, builder.build());
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
     * @param option Response value
     */
    public void setConfiguration(DhcpOption option) {
        configuration.put(option.getType(), option);
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