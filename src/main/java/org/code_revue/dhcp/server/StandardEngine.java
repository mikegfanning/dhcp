package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.code_revue.dhcp.util.AddressUtils;
import org.code_revue.dhcp.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.nio.ByteBuffer;
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

    // Should probably move this into some separate component altogether, guarantee thread safety therein, etc.
    private Map<String, NetworkDevice> devices = new HashMap<>();

    // Like the devices, should probably move this into some separate component with interface.
    private Map<DhcpOptionType, DhcpOption> configuration = new HashMap<>();

    @Override
    protected DhcpPayload handleDhcpDiscover(DhcpMessageOverlay message, DhcpOption reqAddr, DhcpOption paramList) {

        // Validate message, register device, borrow address from pool, return DHCP Offer
        NetworkDevice device = getDevice(message.getClientHardwareAddress());

        if (!Arrays.equals(EMPTY_ADDRESS, message.getClientIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getYourIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getServerIpAddress())) {
            logger.warn("Client {} submitted REQUEST with invalid address(es)",
                    AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
            return null;
        }

        if (!DeviceStatus.DISCOVERED.equals(device.getStatus())){
            // If the device has already been offered a lease or has acknowledged it we'll ignore subsequent discover
            // messages until the lease expires.
            if (logger.isWarnEnabled()) {
                logger.warn("Client {} is in {} state, should be in OFFERED",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()), device.getStatus());
            }
            return null;
        }

        byte[] borrowedAddress = null;

        if (null != reqAddr) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress(reqAddr.getOptionData());
                if (null != borrowedAddress) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Borrowed address {} from pool", AddressUtils.ipAddressToString(borrowedAddress));
                    }
                    break;
                }
            }
        }

        if (null == borrowedAddress) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress();
                if (null != borrowedAddress) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Borrowed address {} from pool", AddressUtils.ipAddressToString(borrowedAddress));
                    }
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
                    .setBroadcast(message.isBroadcast())
                    .setGatewayIpAddress(message.getGatewayIpAddress())
                    .setHardwareAddress(message.getClientHardwareAddress())
                    .addOption(DhcpMessageType.OFFER.getOption());

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
            }

            response = new DhcpPayload(BROADCAST_ADDRESS, message.isBroadcast(), builder.build());

            // Update device state.
            if (logger.isDebugEnabled()) {
                logger.debug("Updating device settings for client {}",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
            }
            device.setStatus(DeviceStatus.OFFERED);
            device.setIpAddress(borrowedAddress);
            Calendar expiration = Calendar.getInstance();
            expiration.add(Calendar.SECOND, DEFAULT_TTL);
            device.setLeaseExpiration(expiration.getTime());
            device.setOptions(offeredOptions);
        }

        return response;
    }

    @Override
    protected DhcpPayload handleDhcpRequest(DhcpMessageOverlay message, DhcpOption serverId) {

        // Validate message, update device status, if the requested address is valid, return DHCP Acknowledgement,
        // otherwise, DHCP NAK
        NetworkDevice device = getDevice(message.getClientHardwareAddress());

        if (!Arrays.equals(EMPTY_ADDRESS, message.getClientIpAddress()) ||
                !Arrays.equals(EMPTY_ADDRESS, message.getYourIpAddress())) {
            logger.warn("Client {} submitted REQUEST with invalid address(es)",
                    AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
            return null;
        }

        if (!DeviceStatus.OFFERED.equals(device.getStatus())) {
            if (logger.isWarnEnabled()) {
                logger.warn("Client {} is in {} state, should be in OFFERED",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()), device.getStatus());
            }
            return null;
        }

        if (null == serverId) {
            return null;
        } else if (!Arrays.equals(serverId.getOptionData(), serverIpAddress)) {
            // Client is going to use another DHCP server. We can return the address we assigned to it to the pool.
            if (logger.isInfoEnabled()) {
                logger.info("Client {} has elected to use another DHCP server {}",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()),
                        AddressUtils.ipAddressToString(serverId.getOptionData()));
            }
            resetDevice(device);
            return null;
        }

        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REPLY)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(message.getTransactionId())
                .setClientIpAddress(message.getClientIpAddress())
                .setYourIpAddress(device.getIpAddress())
                .setServerIpAddress(serverIpAddress)
                .setBroadcast(message.isBroadcast())
                .setGatewayIpAddress(message.getGatewayIpAddress())
                .setHardwareAddress(message.getClientHardwareAddress())
                .addOption(DhcpMessageType.ACK.getOption());

        Map<DhcpOptionType, DhcpOption> options = device.getOptions();
        for (DhcpOption option: options.values()) {
            builder.addOption(option);
        }

        device.setStatus(DeviceStatus.ACKNOWLEDGED);

        return new DhcpPayload(BROADCAST_ADDRESS, message.isBroadcast(), builder.build());
    }

    @Override
    protected void handleDhcpDecline(DhcpMessageOverlay message) {

        // Validate message, update device status to reflect that it will not use the supplied IP address and return it
        // to the pool
        NetworkDevice device = getDevice(message.getClientHardwareAddress());
        if (DeviceStatus.OFFERED.equals(device.getStatus())) {
            resetDevice(device);
        }

    }

    @Override
    protected void handleDhcpRelease(DhcpMessageOverlay message) {

        // Validate message, update device, and return address to pool
        NetworkDevice device = getDevice(message.getClientHardwareAddress());
        if (DeviceStatus.ACKNOWLEDGED.equals(device.getStatus())) {
            resetDevice(device);
        }

    }

    @Override
    protected DhcpPayload handleDhcpInform(DhcpMessageOverlay message, DhcpOption paramList) {

        // Validate message, update device status and send back DHCP Acknowledgement with requested configuration info
        NetworkDevice device = getDevice(message.getClientHardwareAddress());
        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REPLY)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(message.getTransactionId())
                .setBroadcast(false)
                .setYourIpAddress(message.getClientIpAddress())
                .setServerIpAddress(serverIpAddress)
                .setGatewayIpAddress(message.getGatewayIpAddress())
                .setHardwareAddress(message.getClientHardwareAddress())
                .addOption(DhcpMessageType.ACK.getOption());

        Map<DhcpOptionType, DhcpOption> informOptions = new HashMap<>();
        if (null != paramList) {
            byte[] parameterList = paramList.getOptionData();
            for (byte param: parameterList) {
                DhcpOptionType optionType = DhcpOptionType.getByNumericCode(param);
                DhcpOption option = getConfiguration(optionType);
                if (null != option) {
                    informOptions.put(optionType, option);
                    builder.addOption(option);
                }
            }
        }
        device.setOptions(informOptions);

        SocketAddress clientAddress = null;
        try {
            clientAddress = new InetSocketAddress(InetAddress.getByAddress(message.getClientIpAddress()), 68);
        } catch (UnknownHostException e) {
            logger.error("Could not resolve client IP address", e);
        }

        return new DhcpPayload(clientAddress, message.isBroadcast(), builder.build());
    }

    public void addAddressPool(DhcpAddressPool pool) {
        logger.debug("Adding address pool {}", pool);
        pools.add(pool);
    }

    public boolean removeAddressPool(DhcpAddressPool pool) {
        logger.debug("Removing address pool {}", pool);
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
        logger.debug("Setting configuration option {}", option);
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

    private NetworkDevice getDevice(byte[] hardwareAddress) {
        String address = AddressUtils.hardwareAddressToString(hardwareAddress);
        logger.debug("Retrieving network device with hardware address {}", address);
        NetworkDevice device = devices.get(address);
        if (null == device) {
            logger.debug("Device not found, creating new record for {}", address);
            device = new NetworkDevice();
            device.setStatus(DeviceStatus.DISCOVERED);
            device.setHardwareAddress(hardwareAddress);
            devices.put(address, device);
        }
        return device;
    }

    private void resetDevice(NetworkDevice device) {
        logger.debug("Resetting networked device status");
        DeviceStatus status = device.getStatus();
        if (DeviceStatus.OFFERED.equals(status) || DeviceStatus.ACKNOWLEDGED.equals(status)) {
            logger.debug("Returning address to pool");
            byte[] offeredAddress = device.getIpAddress();
            for (DhcpAddressPool pool : pools) {
                pool.returnAddress(offeredAddress);
            }
        }
        device.setStatus(DeviceStatus.DISCOVERED);
    }
}
