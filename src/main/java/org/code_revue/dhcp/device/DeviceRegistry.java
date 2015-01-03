package org.code_revue.dhcp.device;

import java.util.Collection;

/**
 * Specifies the interface for classes that manage network devices that communicate with the DHCP server.
 * @author Mike Fanning
 */
public interface DeviceRegistry {

    /**
     * Returns all of the devices in the registry.
     * @return Devices in this registry
     */
    public Collection<NetworkDevice> getAllDevices();

    /**
     * Get the device with the supplied hardware address, creating a new one if necessary.
     * @param hardwareAddress Hardware address (MAC) of device
     * @return Information about this device
     */
    public NetworkDevice getDevice(byte[] hardwareAddress);

    /**
     * Resets device to initial "Discovered" status.
     * @param hardwareAddress Hardware address (MAC) of device to reset
     * @return Updated information about device
     */
    public NetworkDevice resetDevice(byte[] hardwareAddress);

    /**
     * Updates a device in the registry.
     * @param device Device to be updated
     * @return Updated information about device
     */
    public NetworkDevice updateDevice(NetworkDevice device);

}
