package org.code_revue.dhcp.server;

/**
 * Can be used by other DHCP server components to manage IP addresses, allowing callers to borrow addresses and then
 * return them when they are released.
 *
 * @author Mike Fanning
 */
public interface DhcpAddressPool {

    public byte[] borrowAddress();

    public byte[] borrowAddress(byte[] address);

    public void returnAddress(byte[] address);
    
}
