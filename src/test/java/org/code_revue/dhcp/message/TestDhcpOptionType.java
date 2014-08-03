package org.code_revue.dhcp.message;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestDhcpOptionType {

    @Test
    public void getByNumericCode() {
        Assert.assertEquals(DhcpOptionType.PAD, DhcpOptionType.getByNumericCode(0));
        Assert.assertEquals(DhcpOptionType.END, DhcpOptionType.getByNumericCode(255));

        Assert.assertEquals(DhcpOptionType.TCP_KEEPALIVE_GARBAGE, DhcpOptionType.getByNumericCode(39));
        Assert.assertEquals(DhcpOptionType.MESSAGE_TYPE, DhcpOptionType.getByNumericCode(53));
    }

    @Test
    public void lookupAllDefinedOptions() {
        for (DhcpOptionType type: DhcpOptionType.values()) {
            Assert.assertEquals(type, DhcpOptionType.getByNumericCode(type.getNumericCode()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidNumericCode() {
        DhcpOptionType.getByNumericCode(256);
    }

}
