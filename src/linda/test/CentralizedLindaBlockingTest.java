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

        // On demande un thread de lecture
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

        // On attend un peu pour s'assurer que le lecteur est bloqué
        Thread.sleep(100);

        linda.write(new Tuple("test", 42));

        // On attend que le lecteur se termine
        assertTrue("Reader should complete", latch.await(2, TimeUnit.SECONDS));

        Tuple found = result.get();
        assertNotNull("Should find tuple", found);
        assertEquals("Should match string", "test", found.get(0));
        assertEquals("Should match integer", 42, found.get(1));

        // Vérifier que le tuple est toujours là (read ne fois pas le supprimer)
        Tuple stillThere = linda.tryRead(new Tuple(String.class, Integer.class));
        assertNotNull("Tuple should still be in space after read", stillThere);
    }

    @Test(timeout = 5000)
    public void testBlockingTake() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Tuple> result = new AtomicReference<>();

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

        Thread.sleep(100);

        linda.write(new Tuple(true, "blocking"));

        assertTrue("Taker should complete", latch.await(2, TimeUnit.SECONDS));

        Tuple found = result.get();
        assertNotNull("Should find tuple", found);
        assertEquals("Should match boolean", true, found.get(0));
        assertEquals("Should match string", "blocking", found.get(1));

        // Vérifier que le tuple a été retiré (take doit le supprimer)
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

        Thread.sleep(100);

        linda.write(new Tuple(999));

        assertTrue("All readers should complete", latch.await(2, TimeUnit.SECONDS));

        // Vérifier que tous les lecteurs ont trouvé le tuple
        for (int i = 0; i < 3; i++) {
            assertNotNull("Reader " + i + " should find tuple", results[i].get());
            assertEquals("Reader " + i + " should find correct value", 999, results[i].get().get(0));
        }
    }
}
