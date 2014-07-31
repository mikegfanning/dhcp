package org.code_revue.dhcp.message;

import junit.framework.Assert;
import org.junit.Test;

/**
 * @author Mike Fanning
 */
public class TestDhcpOption {

    @Test
    public void getByNumericCode() {
        Assert.assertEquals(DhcpOption.PAD, DhcpOption.getByNumericCode(0));
        Assert.assertEquals(DhcpOption.END, DhcpOption.getByNumericCode(255));

        Assert.assertEquals(DhcpOption.TCP_KEEPALIVE_GARBAGE, DhcpOption.getByNumericCode(39));
        Assert.assertEquals(DhcpOption.MESSAGE_TYPE, DhcpOption.getByNumericCode(53));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidNumericCode() {
        DhcpOption.getByNumericCode(256);
    }

}
