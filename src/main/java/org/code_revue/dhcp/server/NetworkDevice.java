package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.DhcpOption;
import org.code_revue.dhcp.message.DhcpOptionType;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of this class represent a network device that has communicated with the DHCP server in some way. Right now
 * it has a bunch of basic information about the status of the device (i.e. where it is in the DHCP process), what
 * hardware address it has, when its lease is going to expire, etc. For now it is just a simple bean.
 *
 * @author Mike Fanning
 */
public class NetworkDevice {

    private DeviceStatus status;

    private byte[] hardwareAddress;

    private byte[] ipAddress;

    private Date leaseExpiration;

    private Map<DhcpOptionType, DhcpOption> options = new HashMap<>();

    /**
     * Tracks the state of the device through the DHCP configuration process. See
     * {@link org.code_revue.dhcp.server.DeviceStatus} for more information about the meaning of the different states.
     * @return
     */
    public DeviceStatus getStatus() {
        return status;
    }

    /**
     * Tracks the state of the device through the DHCP configuration process. See
     * {@link org.code_revue.dhcp.server.DeviceStatus} for more information about the meaning of the different states.
     * @return
     */
    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public byte[] getHardwareAddress() {
        return hardwareAddress;
    }

    public void setHardwareAddress(byte[] hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    public byte[] getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(byte[] ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getLeaseExpiration() {
        return leaseExpiration;
    }

    public void setLeaseExpiration(Date leaseExpiration) {
        this.leaseExpiration = leaseExpiration;
    }

    /**
     * The last set of DHCP options that were sent to a client as part of an Offer, Acknowledgement or Inform message.
     * @return
     */
    public Map<DhcpOptionType, DhcpOption> getOptions() {
        return options;
    }

    /**
     * The last set of DHCP options that were sent to a client as part of an Offer, Acknowledgement or Inform message.
     * @param options
     */
    public void setOptions(Map<DhcpOptionType, DhcpOption> options) {
        this.options = options;
    }
}
