package org.code_revue.dhcp.server;

import static org.junit.Assert.*;

import org.code_revue.dhcp.util.AddressUtils;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Mike Fanning
 */
public class TestStandardIp4AddressPool {

    private final byte[] address1 = new byte[] { 100, 0, 0, 1 };
    private final byte[] address2 = new byte[] { 101, 0, 0, 1 };
    private final byte[] address3 = new byte[] { (byte) 130, 0, 0, 1};
    private final byte[] address4 = new byte[] { (byte) 131, 0, 0, 1};
    private final byte[] address5 = new byte[] { (byte) 255, (byte) 255, (byte) 255, (byte) 254};
    private final byte[] address6 = new byte[] { (byte) 192, (byte) 168, 1, 10};
    private final byte[] address7 = new byte[] { (byte) 192, (byte) 168, 1, 19};

    @Test
    public void validConstructors() {
        new StandardIp4AddressPool(address1, address1);
        new StandardIp4AddressPool(address1, address2);
        new StandardIp4AddressPool(address1, address3);
        new StandardIp4AddressPool(address1, address4);
        new StandardIp4AddressPool(address2, address2);
        new StandardIp4AddressPool(address2, address3);
        new StandardIp4AddressPool(address2, address4);
        new StandardIp4AddressPool(address3, address3);
        new StandardIp4AddressPool(address3, address4);
        new StandardIp4AddressPool(address4, address4);
        new StandardIp4AddressPool("100.0.0.1", "101.0.0.1");
    }

    @Test
    public void getters() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address1, address2);
        assertEquals("100.0.0.1", AddressUtils.convertToString(pool.getStart()));
        assertEquals("101.0.0.1", AddressUtils.convertToString(pool.getEnd()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor1() {
        new StandardIp4AddressPool(address2, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor2() {
        new StandardIp4AddressPool(address3, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor3() {
        new StandardIp4AddressPool(address4, address1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor4() {
        new StandardIp4AddressPool(address3, address2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor5() {
        new StandardIp4AddressPool(address4, address2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor6() {
        new StandardIp4AddressPool(address4, address3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidConstructor7() {
        new StandardIp4AddressPool("101.0.0.1", "100.0.0.1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void outOfRange() {
        new StandardIp4AddressPool(address1, address5);
    }

    @Test
    public void borrowAndReturn() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address1, address2);
        byte[] address = pool.borrowAddress();
        pool.returnAddress(address);
    }

    @Test
    public void borrowAllAddresses() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address6, address7);
        for (int c = 0; c < 10; c++) {
            pool.borrowAddress();
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void borrowTooManyAddresses() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address6, address7);
        for (int c = 0; c < 11; c++) {
            pool.borrowAddress();
        }
    }

    @Test
    public void addSameExclusion() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address6, address7);
        for (int c = 0; c < 10; c++) {
            pool.addExclusion(address6);
        }

        Iterable<byte[]> exclusions = pool.getExclusions();
        int count = 0;
        for (byte[] address: exclusions) {
            assertArrayEquals(address6, address);
            count++;
        }
        assertEquals(1, count);
    }

    @Test
    public void addRemoveExclusion() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address6, address7);
        assertTrue(pool.addExclusion(address6));
        assertFalse(pool.addExclusion(address6));
        assertTrue(pool.removeExclusion(address6));
        assertFalse(pool.removeExclusion(address1));

        Iterable<byte[]> exclusions = pool.getExclusions();
        int count = 0;
        for (byte[] address: exclusions) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void borrowSpecificAddress() {
        StandardIp4AddressPool pool = new StandardIp4AddressPool(address6, address7);
        assertNotNull(pool.borrowAddress(address7));
        assertNull(pool.borrowAddress(address7));
    }

    @Test
    public void concurrentBorrow() throws InterruptedException {

        final StandardIp4AddressPool pool = new StandardIp4AddressPool(address1, address2);
        final int numThreads = 50;
        final int numAddresses = 1000;
        final CyclicBarrier barrier = new CyclicBarrier(numThreads);
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final BlockingQueue<Integer> addresses = new ArrayBlockingQueue<Integer>(numThreads * numAddresses);

        Runnable borrower = new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await(5, TimeUnit.SECONDS);

                    for (int c = 0; c < numAddresses; c++) {
                        byte[] addr = pool.borrowAddress();
                        addresses.add(ByteBuffer.wrap(addr).getInt());
                    }

                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int c = 0; c < numThreads; c++) {
            executor.submit(borrower);
        }

        latch.await(5, TimeUnit.SECONDS);

        Set<Integer> checker = new ConcurrentSkipListSet<>();
        for (Integer i: addresses) {
            assertTrue(checker.add(i));
        }

        executor.shutdown();

    }

}
