package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.code_revue.dhcp.util.AddressUtils;
import org.code_revue.dhcp.util.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.*;
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

    private DhcpAddressPool pool;

    // Should probably move this into some separate component altogether, guarantee thread safety therein, etc.
    private Map<String, NetworkDevice> devices = new HashMap<>();

    // Like the devices, should probably move this into some separate component with interface.
    private Map<DhcpOptionType, DhcpOption> configuration = new HashMap<>();

    public StandardEngine() throws UnknownHostException {
        this(Inet4Address.getLocalHost().getAddress());
    }

    public StandardEngine(byte[] serverIpAddress) {
        this(serverIpAddress, DEFAULT_TTL);
    }

    public StandardEngine(byte[] serverIpAddress, int ipAddressLeaseTime) {
        // TODO: This isn't an address, shouldn't be using "AddressUtils". Get it together nube.
        DhcpOption leaseTimeOption = new ByteArrayOption(DhcpOptionType.IP_ADDR_LEASE_TIME,
                AddressUtils.convertToByteArray(ipAddressLeaseTime));
        configuration.put(leaseTimeOption.getType(), leaseTimeOption);
        setServerIpAddress(serverIpAddress);
    }

    @Override
    protected DhcpPayload handleDhcpDiscover(DhcpMessageOverlay message, Map<DhcpOptionType, DhcpOption> options) {

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
        DhcpOption reqAddr = options.get(DhcpOptionType.REQUESTED_IP_ADDR);

        if (null != reqAddr) {
            borrowedAddress = pool.borrowAddress(reqAddr.getOptionData());
        }

        if (null == borrowedAddress) {
            borrowedAddress = pool.borrowAddress();
        }

        DhcpPayload response = null;

        if (null != borrowedAddress) {
            DhcpMessageBuilder builder = new DhcpMessageBuilder();
            builder.setOpCode(DhcpOpCode.REPLY)
                    .setHardwareType(HardwareType.ETHERNET)
                    .setTransactionId(message.getTransactionId())
                    .setYourIpAddress(borrowedAddress)
                    .setServerIpAddress(getServerIpAddress())
                    .setBroadcast(message.isBroadcast())
                    .setGatewayIpAddress(message.getGatewayIpAddress())
                    .setHardwareAddress(message.getClientHardwareAddress())
                    .addOption(DhcpMessageType.OFFER.getOption())
                    .addOption(configuration.get(DhcpOptionType.SERVER_ID))
                    .addOption(configuration.get(DhcpOptionType.IP_ADDR_LEASE_TIME));

            DhcpOption paramList = options.get(DhcpOptionType.PARAMETER_REQUEST_LIST);
            if (null != paramList) {
                byte[] parameterList = paramList.getOptionData();
                for (byte param: parameterList) {
                    DhcpOptionType offeredOptionType = DhcpOptionType.getByNumericCode(param);
                    DhcpOption offeredOption = getConfiguration(offeredOptionType);
                    if (null != offeredOption) {
                        builder.addOption(offeredOption);
                    }
                }
            }

            Map<DhcpOptionType, DhcpOption> offeredOptions = new HashMap<>();
            for (DhcpOption option: builder.getOptions()) {
                offeredOptions.put(option.getType(), option);
            }

            response = new DhcpPayload(BROADCAST_ADDRESS, true, builder.build());

            // Update device state.
            if (logger.isDebugEnabled()) {
                logger.debug("Updating device settings for client {}",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
            }

            device.setStatus(DeviceStatus.OFFERED);
            device.setIpAddress(borrowedAddress);
            Calendar expiration = Calendar.getInstance();
            expiration.add(Calendar.SECOND, getIpAddressLeaseTime());
            device.setLeaseExpiration(expiration.getTime());
            device.setOptions(offeredOptions);
        }

        return response;
    }

    @Override
    protected DhcpPayload handleDhcpRequest(DhcpMessageOverlay message, Map<DhcpOptionType, DhcpOption> options) {

        // Validate message, update device status, if the requested address is valid, return DHCP Acknowledgement,
        // otherwise, DHCP NAK
        NetworkDevice device = getDevice(message.getClientHardwareAddress());

        if (!Arrays.equals(EMPTY_ADDRESS, message.getYourIpAddress())) {
            logger.warn("Client {} submitted REQUEST with invalid address(es)",
                    AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
            return null;
        }

        DhcpOption serverId = options.get(DhcpOptionType.SERVER_ID);
        DhcpOption requestedIpAddress = options.get(DhcpOptionType.REQUESTED_IP_ADDR);
        if (null == serverId) {
            byte[] requestedAddress;
            if (null == requestedIpAddress) {
                // Client is attempting to renew or rebind
                requestedAddress = message.getClientIpAddress();
            } else {
                // Client is attempting to init-reboot
                if (!Arrays.equals(EMPTY_ADDRESS, message.getClientIpAddress())) {
                    logger.warn("Client {} sent a message indicating it is in the INIT-REBOOT state, but sent non-zero " +
                            "ciddr", AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
                    return null;
                }
                requestedAddress = requestedIpAddress.getOptionData();
            }

            if ((new Date()).compareTo(device.getLeaseExpiration()) <= 0) {
                Calendar now = Calendar.getInstance();
                now.add(Calendar.SECOND, getIpAddressLeaseTime());
                device.setLeaseExpiration(now.getTime());
                device.getOptions().put(DhcpOptionType.IP_ADDR_LEASE_TIME,
                        configuration.get(DhcpOptionType.IP_ADDR_LEASE_TIME));
            } else {
                byte[] offeredIpAddress =  pool.borrowAddress(requestedAddress);

                if (null == offeredIpAddress) {
                    // TODO: Return NAK
                } else {
                    device.setIpAddress(offeredIpAddress);
                    device.getOptions().put(DhcpOptionType.REQUESTED_IP_ADDR,
                            new ByteArrayOption(DhcpOptionType.REQUESTED_IP_ADDR, offeredIpAddress));
                }
            }
        } else if (!Arrays.equals(serverId.getOptionData(), getServerIpAddress())) {
            // Client is going to use another DHCP server. We can return the address we assigned to it to the pool.
            if (logger.isInfoEnabled()) {
                logger.info("Client {} has elected to use another DHCP server {}",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()),
                        LoggerUtils.ipAddressToString(serverId.getOptionData()));
            }
            resetDevice(device);
            return null;
        } else {
            // Client selected this server during initial select
            if (!DeviceStatus.OFFERED.equals(device.getStatus())) {
                logger.warn("Client {} sent a message indicating it is in the SELECTING state, doesn't match server",
                        AddressUtils.hardwareAddressToString(device.getHardwareAddress()));
                return null;
            }
        }

        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REPLY)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(message.getTransactionId())
                .setClientIpAddress(message.getClientIpAddress())
                .setYourIpAddress(device.getIpAddress())
                .setServerIpAddress(getServerIpAddress())
                .setBroadcast(message.isBroadcast())
                .setGatewayIpAddress(message.getGatewayIpAddress())
                .setHardwareAddress(message.getClientHardwareAddress())
                .addOption(DhcpMessageType.ACK.getOption());

        Map<DhcpOptionType, DhcpOption> devOptions = device.getOptions();
        for (DhcpOption option: devOptions.values()) {
            if (!DhcpOptionType.MESSAGE_TYPE.equals(option.getType())) {
                builder.addOption(option);
            }
        }

        device.setStatus(DeviceStatus.ACKNOWLEDGED);

        return new DhcpPayload(BROADCAST_ADDRESS, true, builder.build());
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
    protected DhcpPayload handleDhcpInform(DhcpMessageOverlay message, Map<DhcpOptionType, DhcpOption> options) {

        // Validate message, update device status and send back DHCP Acknowledgement with requested configuration info
        NetworkDevice device = getDevice(message.getClientHardwareAddress());
        DhcpMessageBuilder builder = new DhcpMessageBuilder();
        builder.setOpCode(DhcpOpCode.REPLY)
                .setHardwareType(HardwareType.ETHERNET)
                .setTransactionId(message.getTransactionId())
                .setBroadcast(false)
                .setYourIpAddress(message.getClientIpAddress())
                .setServerIpAddress(getServerIpAddress())
                .setGatewayIpAddress(message.getGatewayIpAddress())
                .setHardwareAddress(message.getClientHardwareAddress())
                .addOption(DhcpMessageType.ACK.getOption());

        DhcpOption paramList = options.get(DhcpOptionType.PARAMETER_REQUEST_LIST);
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

    public byte[] getServerIpAddress() {
        DhcpOption option = configuration.get(DhcpOptionType.SERVER_ID);
        if (null == option) {
            return null;
        }
        return option.getOptionData();
    }

    public void setServerIpAddress(byte[] address) {
        DhcpOption option = new ByteArrayOption(DhcpOptionType.SERVER_ID, address);
        configuration.put(option.getType(), option);
    }

    public int getIpAddressLeaseTime() {
        DhcpOption option = configuration.get(DhcpOptionType.IP_ADDR_LEASE_TIME);
        if (null == option) {
            return DEFAULT_TTL;
        }
        return (new BigInteger(option.getOptionData())).intValue();
    }

    public DhcpAddressPool getAddressPool() {
        return pool;
    }

    public void setAddressPool(DhcpAddressPool pool) {
        this.pool = pool;
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
     * This method sets multiple DHCP configuration options via the provided list. When a client sends a DHCP Discover
     * or Inform message seeking configuration options, this mapping will determine the reponse. Note that the response
     * type of the supplied {@link org.code_revue.dhcp.message.DhcpOption} need not match the request type of the
     * {@link org.code_revue.dhcp.message.DhcpOptionType}.
     * @param options List of configured response values
     */
    public void setConfigurations(List<DhcpOption> options) {
        for (DhcpOption option: options) {
            setConfiguration(option);
        }
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
     * Returns an unmodifiable view of the configurations for this engine.
     * @return Unmodifiable map of configured options
     */
    public Map<DhcpOptionType, DhcpOption> getConfiguration() {
        return Collections.unmodifiableMap(configuration);
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
            pool.returnAddress(offeredAddress);
        }
        device.setStatus(DeviceStatus.DISCOVERED);
    }
}
