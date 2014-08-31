package org.code_revue.dhcp;

import org.code_revue.dhcp.message.ByteArrayOption;
import org.code_revue.dhcp.message.DhcpOptionType;
import org.code_revue.dhcp.server.DhcpServer;
import org.code_revue.dhcp.server.StandardEngine;
import org.code_revue.dhcp.server.StandardIp4AddressPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple server implementation for testing.
 * @author Mike Fanning
 */
public class TestApp {

    private static final Logger logger = LoggerFactory.getLogger(TestApp.class);

    public static void main(String... args) throws IOException {

        byte[] start = new byte[] { (byte) 192, (byte) 168, 1, 120 };
        byte[] end = new byte[] { (byte) 192, (byte) 168, 1, (byte) 160 };
        StandardIp4AddressPool pool = new StandardIp4AddressPool(start, end);

        byte[] subnetMask = new byte[] { (byte) 255, (byte) 255, (byte) 255, 0 };
        byte[] router = new byte[] { (byte) 192, (byte) 168, 1, 1 };
        byte[] dns = new byte[] { (byte) 208, 67, (byte) 222, (byte) 222, (byte) 208, 67, (byte) 220, (byte) 220 };

        StandardEngine engine = new StandardEngine(Inet4Address.getLocalHost().getAddress());
        engine.setAddressPool(pool);
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.SUBNET_MASK, subnetMask));
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.ROUTER, router));
        engine.setConfiguration(new ByteArrayOption(DhcpOptionType.DNS_SERVER, dns));

        final DhcpServer server = new DhcpServer();
        server.setPort(1067);
        server.setEngine(engine);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    server.start();
                    server.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        System.out.print("Command: ");
        Scanner scanner = new Scanner(System.in);
        String command = scanner.nextLine().toLowerCase().trim();
        while (!"exit".equals(command)) {
            // Should probably add some commands for dumping config and such
            System.out.print("Command: ");
            command = scanner.nextLine().toLowerCase().trim();
        }

        server.stop();
        executorService.shutdown();
    }

}
