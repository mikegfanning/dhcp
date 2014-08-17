package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;

import java.util.*;

/**
 * @author Mike Fanning
 */
public class StandardEngine extends AbstractEngine {

    private List<DhcpAddressPool> pools = new ArrayList<>();

    private Map<DhcpOptionType, DhcpOption> configuration = new HashMap<>();

    @Override
    protected DhcpPayload handleDhcpDiscover(DhcpMessageOverlay message) {

        // Validate message, register device, borrow address from pool, return DHCP Offer
        byte[] emptyAddress = new byte[] { 0, 0, 0, 0 };

        if (!Arrays.equals(emptyAddress, message.getClientIpAddress()) ||
                !Arrays.equals(emptyAddress, message.getYourIpAddress()) ||
                !Arrays.equals(emptyAddress, message.getServerIpAddress()) ||
                !Arrays.equals(emptyAddress, message.getGatewayIpAddress())) {
            return null;
        }

        Map<DhcpOptionType, DhcpOption> options = message.getOptions();
        byte[] borrowedAddress = null;

        if (options.containsKey(DhcpOptionType.REQUESTED_IP_ADDR)) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress();
                if (null != borrowedAddress) {
                    break;
                }
            }
        }

        if (null == borrowedAddress) {
            for (DhcpAddressPool pool: pools) {
                borrowedAddress = pool.borrowAddress();
                if (null != borrowedAddress) {
                    break;
                }
            }
        }

        DhcpPayload response = null;

        if (null != borrowedAddress) {
            DhcpMessageBuilder builder = new DhcpMessageBuilder();
            builder.setOpCode(DhcpOpCode.REPLY)
                    .setHardwareType(HardwareType.ETHERNET)
                    .setTransactionId(message.getTransactionId())
                    .setYourIpAddress(borrowedAddress);
            // TODO: setHardwareAddress - maybe this should happen higher up the stack?
            // TODO: setServerIpAddress - maybe this should happen higher up the stack?

            if (options.containsKey(DhcpOptionType.PARAMETER_REQUEST_LIST)) {
                byte[] parameterList = options.get(DhcpOptionType.PARAMETER_REQUEST_LIST).getOptionData();
                for (byte param: parameterList) {
                    DhcpOption responseOption = configuration.get(DhcpOptionType.getByNumericCode(param));
                    if (null != responseOption) {
                        builder.addOption(responseOption);
                    }
                }
            }

            response = new DhcpPayload(null, builder.build());
        }

        return response;
    }

    @Override
    protected DhcpPayload handleDhcpRequest(DhcpMessageOverlay message) {

        // Validate message, update device status, if the requested address is valid, return DHCP Acknowledgement,
        // otherwise, DHCP NAK

        return null;
    }

    @Override
    protected void handleDhcpDecline(DhcpMessageOverlay message) {

        // Validate message, update device status to reflect that it will not use the supplied IP address and return it
        // to the pool

    }

    @Override
    protected void handleDhcpRelease(DhcpMessageOverlay message) {

        // Validate message, update device, and return address to pool

    }

    @Override
    protected DhcpPayload handleDhcpInform(DhcpMessageOverlay message) {

        // Validate message, update device status and send back DHCP Acknowledgement with requested configuration info

        return null;
    }
}
