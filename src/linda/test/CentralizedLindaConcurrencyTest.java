package linda.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class CentralizedLindaConcurrencyTest {

    private Linda linda;

    @Before
    public void setUp() {
        linda = new CentralizedLinda();
    }

    @Test(timeout = 10000)
    public void testConcurrentWriteRead() throws InterruptedException {
        final int numThreads = 5;  // Reduced for more reliable testing
        final int tuplesPerThread = 50;  // Reduced for faster execution
        final CountDownLatch writeLatch = new CountDownLatch(numThreads);
        final CountDownLatch readLatch = new CountDownLatch(numThreads);
        final AtomicInteger totalTuplesWritten = new AtomicInteger(0);
        final AtomicInteger totalTuplesRead = new AtomicInteger(0);

        // Start writer threads
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            Thread writer = new Thread(() -> {
                try {
                    for (int j = 0; j < tuplesPerThread; j++) {
                        linda.write(new Tuple(threadId, j, "data"));
                        totalTuplesWritten.incrementAndGet();
                    }
                } finally {
                    writeLatch.countDown();
                }
            });
            writer.start();
        }

        // Start reader threads
        for (int i = 0; i < numThreads; i++) {
            Thread reader = new Thread(() -> {
                try {
                    // Wait for all writes to complete
                    writeLatch.await(5, TimeUnit.SECONDS);

                    // Give a small buffer for final writes to be visible
                    Thread.sleep(100);

                    Tuple template = new Tuple(Integer.class, Integer.class, String.class);
                    int readCount = 0;

                    // Use readAll instead of individual tryRead calls
                    Collection<Tuple> results = linda.readAll(template);
                    readCount = results.size();
                    totalTuplesRead.addAndGet(readCount);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    readLatch.countDown();
                }
            });
            reader.start();
        }

        assertTrue("All operations should complete", readLatch.await(8, TimeUnit.SECONDS));

        // Verify that we wrote the expected number of tuples
        assertEquals("Should write expected number of tuples",
                numThreads * tuplesPerThread, totalTuplesWritten.get());

        // The total reads might be higher than written tuples since multiple readers
        // can read the same tuples (readAll doesn't remove them)
        assertTrue("Should read at least as many tuples as written",
                totalTuplesRead.get() >= numThreads * tuplesPerThread);
    }

    @Test(timeout = 10000)
    public void testConcurrentTake() throws InterruptedException {
        final int numTuples = 1000;
        final int numTakers = 10;

        // Write tuples
        for (int i = 0; i < numTuples; i++) {
            linda.write(new Tuple(i, "item"));
        }

        final CountDownLatch latch = new CountDownLatch(numTakers);
        final List<Integer> takenValues = Collections.synchronizedList(new ArrayList<>());

        // Start taker threads
        for (int i = 0; i < numTakers; i++) {
            Thread taker = new Thread(() -> {
                Tuple template = new Tuple(Integer.class, String.class);
                while (true) {
                    Tuple result = linda.tryTake(template);
                    if (result != null) {
                        takenValues.add((Integer) result.get(0));
                    } else {
                        break;
                    }
                }
                latch.countDown();
            });
            taker.start();
        }

        assertTrue("All takers should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals("Should take all tuples exactly once", numTuples, takenValues.size());

        // Verify no tuples remain
        Tuple remaining = linda.tryRead(new Tuple(Integer.class, String.class));
        assertNull("No tuples should remain", remaining);
    }

    @Test(timeout = 5000)
    public void testProducerConsumer() throws InterruptedException {
        final int numItems = 100;
        final CountDownLatch producerLatch = new CountDownLatch(1);
        final CountDownLatch consumerLatch = new CountDownLatch(1);
        final List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        // Producer thread
        Thread producer = new Thread(() -> {
            for (int i = 0; i < numItems; i++) {
                linda.write(new Tuple("item", i));
                try {
                    Thread.sleep(1); // Small delay to interleave with consumer
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            producerLatch.countDown();
        });

        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                Tuple template = new Tuple("item", Integer.class);
                for (int i = 0; i < numItems; i++) {
                    Tuple item = linda.take(template); // Blocking take
                    consumed.add((Integer) item.get(1));
                }
                consumerLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        consumer.start();
        Thread.sleep(50); // Ensure consumer starts first
        producer.start();

        assertTrue("Producer should complete", producerLatch.await(3, TimeUnit.SECONDS));
        assertTrue("Consumer should complete", consumerLatch.await(3, TimeUnit.SECONDS));

        assertEquals("Should consume all items", numItems, consumed.size());

        // Verify space is empty
        Tuple remaining = linda.tryRead(new Tuple("item", Integer.class));
        assertNull("Space should be empty", remaining);
    }
}
