package org.code_revue.dhcp.server;

/**
 * Can be used by other DHCP server components to manage IP addresses, allowing callers to borrow addresses and then
 * return them when they are released.
 *
 * @author Mike Fanning
 */
public interface DhcpAddressPool {

    /**
     * Borrows an address from the pool. This should make the address ineligible for subsequent borrowing until the
     * address has been returned via the {@link #returnAddress(byte[])} method. If all the addresses in the pool have
     * been borrowed, this method will return null.
     * @return An IP address or null if the pool is empty
     */
    public byte[] borrowAddress();

    /**
     * Borrow a specific address from the pool. This makes the address ineligible for subsequent borrowing until it has
     * been returned to the pool via the {@link #returnAddress(byte[])} method. If the requested IP address is not
     * managed by the pool or it has already been borrowed, this method will return null.
     * @param address IP address to borrow from the pool
     * @return The IP address in the parameter or null if it is not available
     */
    public byte[] borrowAddress(byte[] address);

    /**
     * Return an address to the pool, making it available for subsequent borrowing, provided it was originally part of
     * the pool.
     * @param address IP address to return to the pool
     */
    public void returnAddress(byte[] address);
    
}
