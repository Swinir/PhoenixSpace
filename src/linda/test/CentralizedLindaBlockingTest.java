package linda.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CentralizedLindaBlockingTest {

    private Linda linda;

    @Before
    public void setUp() {
        linda = new CentralizedLinda();
    }

    @Test(timeout = 5000)
    public void testBlockingRead() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Tuple> result = new AtomicReference<>();

        // Start reader thread
        Thread reader = new Thread(() -> {
            try {
                Tuple template = new Tuple(String.class, Integer.class);
                Tuple found = linda.read(template);
                result.set(found);
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        reader.start();

        // Wait a bit to ensure reader is blocked
        Thread.sleep(100);

        // Write matching tuple
        linda.write(new Tuple("test", 42));

        // Wait for reader to complete
        assertTrue("Reader should complete", latch.await(2, TimeUnit.SECONDS));

        Tuple found = result.get();
        assertNotNull("Should find tuple", found);
        assertEquals("Should match string", "test", found.get(0));
        assertEquals("Should match integer", 42, found.get(1));

        // Verify tuple is still there (read doesn't remove)
        Tuple stillThere = linda.tryRead(new Tuple(String.class, Integer.class));
        assertNotNull("Tuple should still be in space after read", stillThere);
    }

    @Test(timeout = 5000)
    public void testBlockingTake() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Tuple> result = new AtomicReference<>();

        // Start taker thread
        Thread taker = new Thread(() -> {
            try {
                Tuple template = new Tuple(Boolean.class, String.class);
                Tuple found = linda.take(template);
                result.set(found);
                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        taker.start();

        // Wait a bit to ensure taker is blocked
        Thread.sleep(100);

        // Write matching tuple
        linda.write(new Tuple(true, "blocking"));

        // Wait for taker to complete
        assertTrue("Taker should complete", latch.await(2, TimeUnit.SECONDS));

        Tuple found = result.get();
        assertNotNull("Should find tuple", found);
        assertEquals("Should match boolean", true, found.get(0));
        assertEquals("Should match string", "blocking", found.get(1));

        // Verify tuple is removed (take removes it)
        Tuple shouldBeGone = linda.tryRead(new Tuple(Boolean.class, String.class));
        assertNull("Tuple should be removed after take", shouldBeGone);
    }

    @Test(timeout = 5000)
    public void testMultipleBlockingReaders() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(3);
        final AtomicReference<Tuple>[] results = new AtomicReference[3];

        for (int i = 0; i < 3; i++) {
            results[i] = new AtomicReference<>();
            final int index = i;

            Thread reader = new Thread(() -> {
                try {
                    Tuple template = new Tuple(Integer.class);
                    Tuple found = linda.read(template);
                    results[index].set(found);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            reader.start();
        }

        // Wait a bit to ensure all readers are blocked
        Thread.sleep(100);

        // Write one matching tuple
        linda.write(new Tuple(999));

        // All readers should be unblocked
        assertTrue("All readers should complete", latch.await(2, TimeUnit.SECONDS));

        // All should have found the same tuple
        for (int i = 0; i < 3; i++) {
            assertNotNull("Reader " + i + " should find tuple", results[i].get());
            assertEquals("Reader " + i + " should find correct value", 999, results[i].get().get(0));
        }
    }
}
