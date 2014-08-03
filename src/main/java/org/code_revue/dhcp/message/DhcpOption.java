package org.code_revue.dhcp.message;

/**
 * @author Mike Fanning
 */
public interface DhcpOption {

    public DhcpOptionType getType();

    public byte[] getOptionData();

}
