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
        final int numThreads = 5;
        final int tuplesPerThread = 50;
        final CountDownLatch writeLatch = new CountDownLatch(numThreads);
        final CountDownLatch readLatch = new CountDownLatch(numThreads);
        final AtomicInteger totalTuplesWritten = new AtomicInteger(0);
        final AtomicInteger totalTuplesRead = new AtomicInteger(0);

        // Demandes d'écriture de tuples
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
                    // On attend que tous les écrivains aient terminé
                    writeLatch.await(5, TimeUnit.SECONDS);

                    // On laisse un peu de temps pour s'assurer que les écritures sont visibles
                    Thread.sleep(100);

                    Tuple template = new Tuple(Integer.class, Integer.class, String.class);
                    int readCount = 0;

                    // On lit les tuples écrits
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

        assertEquals("Should write expected number of tuples",
                numThreads * tuplesPerThread, totalTuplesWritten.get());

        assertTrue("Should read at least as many tuples as written",
                totalTuplesRead.get() >= numThreads * tuplesPerThread);
    }

    @Test(timeout = 10000)
    public void testConcurrentTake() throws InterruptedException {
        final int numTuples = 1000;
        final int numTakers = 10;

        // On ecrit les tuples
        for (int i = 0; i < numTuples; i++) {
            linda.write(new Tuple(i, "item"));
        }

        final CountDownLatch latch = new CountDownLatch(numTakers);
        final List<Integer> takenValues = Collections.synchronizedList(new ArrayList<>());

        // On crée les threads pour effectuer les takes
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

        // On vérifie que tous les tuples ont été pris
        Tuple remaining = linda.tryRead(new Tuple(Integer.class, String.class));
        assertNull("No tuples should remain", remaining);
    }

    @Test(timeout = 5000)
    public void testProducerConsumer() throws InterruptedException {
        final int numItems = 100;
        final CountDownLatch producerLatch = new CountDownLatch(1);
        final CountDownLatch consumerLatch = new CountDownLatch(1);
        final List<Integer> consumed = Collections.synchronizedList(new ArrayList<>());

        Thread producer = new Thread(() -> {
            for (int i = 0; i < numItems; i++) {
                linda.write(new Tuple("item", i));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            producerLatch.countDown();
        });

        Thread consumer = new Thread(() -> {
            try {
                Tuple template = new Tuple("item", Integer.class);
                for (int i = 0; i < numItems; i++) {
                    Tuple item = linda.take(template); // Take Bloquant
                    consumed.add((Integer) item.get(1));
                }
                consumerLatch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        consumer.start();
        Thread.sleep(50); // On laisse le temps au consommateur de consommer les premiers éléments
        producer.start();

        assertTrue("Producer should complete", producerLatch.await(3, TimeUnit.SECONDS));
        assertTrue("Consumer should complete", consumerLatch.await(3, TimeUnit.SECONDS));

        assertEquals("Should consume all items", numItems, consumed.size());

        // On vérifie que l'espace est vide
        Tuple remaining = linda.tryRead(new Tuple("item", Integer.class));
        assertNull("Space should be empty", remaining);
    }
}
