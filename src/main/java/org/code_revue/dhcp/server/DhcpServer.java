package org.code_revue.dhcp.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class implements a simple DHCP server that will read from a UDP socket, process the request, and respond
 * appropriately. All requests are processed via an implementation of {@link org.code_revue.dhcp.server.DhcpEngine}.
 * Note that this class implements the {@link java.lang.Runnable} interface, making it easy to start the server in a
 * separate thread.
 * <p>
 * The normal lifecycle for this class is as follows:
 * <ol>
 *     <li>Construct new server</li>
 *     <li>Use setters to configure instance</li>
 *     <li>Call {@link #start()} method to initialize channel/socket</li>
 *     <li>Call {@link #run()} method to begin processing DHCP messages</li>
 *     <li>Call {@link #stop()} method to shut down server</li>
 * </ol>
 * </p>
 *
 * @author Mike Fanning
 */
public class DhcpServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DhcpServer.class);

    public static final int DEFAULT_DHCP_SERVER_PORT = 67;
    public static final int MAX_UDP_PACKET_SIZE = 512;

    private volatile boolean running = false;

    private DatagramChannel channel;
    private DhcpEngine engine;
    private int port = DEFAULT_DHCP_SERVER_PORT;

    private AtomicLong receiveCount = new AtomicLong(0);
    private AtomicLong sendCount = new AtomicLong(0);
    private AtomicLong errorCount = new AtomicLong(0);

    /**
     * Starts the server, which consists of opening a datagram channel or socket, binding it to a port (default 67) and
     * configuring it for broadcast. Most DHCP responses will be broadcasts to 255.255.255.255.
     *
     * @throws IOException If there is an error while opening and binding the socket.
     * @see <a href="http://en.wikipedia.org/wiki/Dhcp">http://en.wikipedia.org/wiki/Dhcp</a>
     */
    private void start() throws IOException {

        logger.info("Starting DHCP Server");

        if (running) {
            throw new IllegalStateException("DHCP Server is already running");
        }

        channel = DatagramChannel.open();
        logger.debug("Binding DatagramChannel to port {}", port);
        channel.bind(new InetSocketAddress(port));
        logger.debug("Setting DatagramChannel to broadcast");
        channel.socket().setBroadcast(true);
        running  = true;

    }

    /**
     * Receives DHCP packets, passes them to the {@link org.code_revue.dhcp.server.DhcpEngine}, and sends responses (if
     * necessary). This continues until the server is stopped by calling the {@link #stop()} method.
     */
    public void run() {

        logger.info("DHCP Server is running");

        while (running) {
            try {

                ByteBuffer messageBuffer = ByteBuffer.allocateDirect(MAX_UDP_PACKET_SIZE);
                SocketAddress address = channel.receive(messageBuffer);
                receiveCount.incrementAndGet();
                logger.debug("Message received from {}", address);

                DhcpPayload message = new DhcpPayload(address, messageBuffer);
                DhcpPayload response = engine.processDhcpPayload(message);

                if (null != response) {
                    channel.send(response.getData(), response.getAddress());
                    sendCount.incrementAndGet();
                    logger.debug("Message sent to {}", response.getAddress());
                }

            }  catch (AsynchronousCloseException e) {
                // This probably indicates that another thread has stopped the server
                if (running) {
                    logger.error("Socket receive was interrupted", e);
                    errorCount.incrementAndGet();
                }
            } catch (IOException e) {
                logger.error("Error sending or receiving message", e);
                errorCount.incrementAndGet();
            } catch (Exception e) {
                logger.error("Miscellaneous error caught", e);
                errorCount.incrementAndGet();
            }
        }

    }

    /**
     * Stops the server. This will close and release any underlying resources.
     * @throws IOException
     */
    private void stop() throws IOException {

        logger.info("Stopping DHCP Server");

        if (!running) {
            logger.warn("DHCP Server is already stopped");
        } else {
            running = false;
            channel.close();
        }
    }

    /**
     * Check to see if the server is currently running.
     * @return
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the {@link org.code_revue.dhcp.server.DhcpEngine} used by this server to process requests.
     * @return
     */
    public DhcpEngine getEngine() {
        return engine;
    }

    /**
     * Set the {@link org.code_revue.dhcp.server.DhcpEngine} used by this server to process requests.
     * @param engine
     */
    public void setEngine(DhcpEngine engine) {
        this.engine = engine;
    }

    /**
     * Get the port this server will be bound to. Default is {@link #DEFAULT_DHCP_SERVER_PORT}.
     * @return
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port this server will be bound to.
     * @param port
     */
    public void setPort(int port) {
        if (running) {
            throw new IllegalStateException("DHCP Server is already running");
        }
        this.port = port;
    }

    /**
     * Get the number of UDP messages that have been received by this server. Note that not all messages will
     * necessarily be valid, so this may not equal the value returned by {@link #getSendCount()}.
     * @return Number of UDP messages received
     */
    public long getReceiveCount() {
        return receiveCount.get();
    }

    /**
     * Get the number of DHCP messages sent to clients by this server. Not that not all requests will result in a
     * response, so this may not equals the value returned by {@link #getReceiveCount()}.
     * @return Number of DHCP responses sent
     */
    public long getSendCount() {
        return sendCount.get();
    }

    /**
     * Get the number of errors that the server caught while running. Note that the
     * {@link org.code_revue.dhcp.server.DhcpEngine} does not throw any checked exceptions, so this count will only
     * include runtime exceptions that are thrown by it.
     * @return
     */
    public long getErrorCount() {
        return errorCount.get();
    }
}