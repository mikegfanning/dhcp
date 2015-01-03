package org.code_revue.dhcp.device;

import org.code_revue.dhcp.util.AddressUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link org.code_revue.dhcp.device.DeviceRegistry} using in memory map of devices, indexed by
 * hardware address.
 * @author Mike Fanning
 */
public class SimpleDeviceRegistry implements DeviceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDeviceRegistry.class);

    private Map<String, NetworkDevice> devices = new ConcurrentHashMap<>();

    @Override
    public Collection<NetworkDevice> getAllDevices() {
        return devices.values();
    }

    @Override
    public NetworkDevice getDevice(byte[] hardwareAddress) {
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

    @Override
    public NetworkDevice resetDevice(byte[] hardwareAddress) {
        logger.debug("Resetting networked device status {}", hardwareAddress);
        NetworkDevice device = getDevice(hardwareAddress);
        device.setStatus(DeviceStatus.DISCOVERED);
        return device;
    }
}
