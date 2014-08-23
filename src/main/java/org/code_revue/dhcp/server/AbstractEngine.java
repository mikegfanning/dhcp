package org.code_revue.dhcp.server;

import org.code_revue.dhcp.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

/**
 * This class partially implements a {@link org.code_revue.dhcp.server.DhcpEngine}, processing messages, handing out
 * address leases, tracking device status, etc. It uses the Template design pattern to define basic validation and has
 * several abstract methods that implementations can use to define the server's behavior for different message types.
 * Implementations should override the {@link #isValidPayload(DhcpPayload)} method and implement their own address
 * leasing strategy and device tracking.
 *
 * @see <a href="https://www.ietf.org/rfc/rfc2131.txt"></a>
 * @see <a href="http://en.wikipedia.org/wiki/Template_method_pattern"></a>
 * @author Mike Fanning
 */
public abstract class AbstractEngine implements DhcpEngine {

    private static final Logger logger = LoggerFactory.getLogger(AbstractEngine.class);

    protected byte[] hardwareAddress;

    protected byte[] serverIpAddress;

    public void setHardwareAddress(byte[] hardwareAddress) {
        this.hardwareAddress = Arrays.copyOf(hardwareAddress, hardwareAddress.length);
    }

    public void setServerIpAddress(byte[] serverIpAddress) {
        this.serverIpAddress = Arrays.copyOf(serverIpAddress, serverIpAddress.length);
    }

    /**
     * Processes a {@link org.code_revue.dhcp.server.DhcpPayload} and returns one in response. This includes validation,
     * device status management, DHCP option configuration, etc.
     * <p>
     * Currently, this interface and method will return null if there is an error processing the request (e.g. invalid
     * message type, garbled payload), which means the client will get no response for some classes of error. Might want
     * to make this throw exceptions if there are issues with the payload, so that callers are aware of bogus messages.
     * Still waffling on this.
     * </p>
     * @param payload Client address information and message data sent to the server
     * @return Response message, or null if there was an error
     */
    @Override
    public final DhcpPayload processDhcpPayload(DhcpPayload payload) {

        logger.debug("DHCP payload received");

        if (DhcpMessageOverlay.HEADER_LENGTH > payload.getData().capacity()) {
            logger.error("DHCP message is too short");
            return null;
        }

        DhcpMessageOverlay message = new DhcpMessageOverlay(payload.getData());

        // Perform validation on the payload, regardless of current device status
        if (!DhcpOpCode.REQUEST.equals(message.getOpCode())) {
            logger.error("Invalid DHCP message op code");
            return null;
        }

        if (DhcpMessageOverlay.MAGIC_COOKIE != message.getMagicCookie()) {
            logger.error("Invalid magic cookie in DHCP message");
            return null;
        }

        if (!isValidPayload(payload)) {
            return null;
        }

        // Message should have a DHCP message type
        Map<DhcpOptionType, DhcpOption> options = message.getOptions();
        DhcpMessageType messageType = null;
        if (!options.containsKey(DhcpOptionType.MESSAGE_TYPE)) {
            logger.error("DHCP message does not contain a message type");
            return null;
        } else {
            byte[] typeData = options.get(DhcpOptionType.MESSAGE_TYPE).getOptionData();
            if (1 != typeData.length) {
                logger.error("DHCP message type field is incorrect length");
                return null;
            } else {
                messageType = DhcpMessageType.getByNumericCode(typeData[0]);
            }
        }

        // Handle DHCP message by type - ignore any other message types.
        DhcpPayload response = null;
        switch (messageType) {
            case DISCOVER:
                logger.trace("Handling DHCP Discover message");
                response = handleDhcpDiscover(message, options.get(DhcpOptionType.REQUESTED_IP_ADDR),
                        options.get(DhcpOptionType.PARAMETER_REQUEST_LIST));
                break;
            case REQUEST:
                logger.trace("Handling DHCP Request message");
                response = handleDhcpRequest(message, options.get(DhcpOptionType.SERVER_ID));
                break;
            case DECLINE:
                logger.trace("Handling DHCP Decline message");
                handleDhcpDecline(message);
                break;
            case RELEASE:
                logger.trace("Handling DHCP Release message");
                handleDhcpRelease(message);
                break;
            case INFORM:
                logger.trace("Handling DHCP Inform message");
                response = handleDhcpInform(message);
                break;
            default:
                logger.error("Invalid DHCP message type");
        }

        return response;

    }

    /**
     * This method is called by {@link #processDhcpPayload(DhcpPayload)} to validate the DHCP payload sent for
     * processing. If it returns true, processing will continue; otherwise nothing will be returned to the client. The
     * default implementation always returns true, but this method is intended to be overridden by subclasses.
     * @return True if the payload is valid and processing should continue, false otherwise
     */
    protected boolean isValidPayload(DhcpPayload payload) {
        return true;
    }

    /**
     * Handles the DHCP Discover message type. Provided the message data is valid, this should return a payload that
     * will broadcast a DHCP Offer message.
     * @param message DHCP Disover message data
     * @param reqAddr Requested IP Address option
     * @param paramList Parameter Request List option
     * @return If message is valid, a payload containing a DHCP Offer message, otherwise, null
     */
    protected abstract DhcpPayload handleDhcpDiscover(DhcpMessageOverlay message, DhcpOption reqAddr,
                                                      DhcpOption paramList);

    /**
     * Handles the DHCP Request message type. If the message data is valid, this should return DHCP Acknowledgement. If
     * the message is invalid, it should return a DHCP NAK message, or possibly null if something is really messed up.
     * @param message DHCP Request message data
     * @param serverId Server IP Address option
     * @return If server accepts the request, a DHCP Acknowledgment payload, otherwise a DHCP NAK or null
     */
    protected abstract DhcpPayload handleDhcpRequest(DhcpMessageOverlay message, DhcpOption serverId);

    /**
     * Handles the DHCP Decline message type. If the message is valid, this will signal to the server that the client
     * does not want the supplied IP address, because it thinks (correctly or incorrectly) that it is already in use.
     * The server shouldn't return a response to this message (as far as I can tell).
     * @param message DHCP Decline message data
     */
    protected abstract void handleDhcpDecline(DhcpMessageOverlay message);

    /**
     * Handles the DHCP Release message type. If the message is valid, this will return the supplied IP address to the
     * pool. The RFC says the server SHOULD (their caps, not mine) retain client information for future DHCP
     * transactions (i.e. so it can hand out the same address, if possible).
     * @param message DHCP Release message data
     */
    protected abstract void handleDhcpRelease(DhcpMessageOverlay message);

    /**
     * Handles the DHCP Inform message type. If the message is valid, the server should respond with a DHCP
     * Acknowledgement containing additional configuration parameters. The server should check for IP address
     * consistency, but MUST NOT (again, RFC caps, not mine) check for a lease - the client could have a self assigned
     * IP outside the server's scope and simply be requesting local configuration information.
     * @param message DHCP Inform message data
     * @return If the message is valid, a DHCP Acknowledgement containing local configuration parameters
     */
    protected abstract DhcpPayload handleDhcpInform(DhcpMessageOverlay message);

}
