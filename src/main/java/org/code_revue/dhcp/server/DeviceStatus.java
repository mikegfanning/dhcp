package org.code_revue.dhcp.server;

/**
 * Enumeration of the different types of status that a network device attached to the DCHP server can be in. A device
 * that has been offered a DHCP lease should be in the {@link #OFFERED} state, after the client has sent a request
 * message and the server has responded with an ACK, it should be in the {@link #ACKNOWLEDGED} state, and if a device
 * sends an inform message, which probably means that it is using a static IP and just wants other config parameters, it
 * should be in {@link #DISCOVERED}. If there is some kind of problem during the DHCP configuration process, such as the
 * server responding with a NAK or the client sending a decline message, the device should be in {@link #DISCOVERED}.
 *
 * @author Mike Fanning
 */
public enum DeviceStatus {
    DISCOVERED,
    OFFERED,
    ACKNOWLEDGED
}
