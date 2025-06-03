package linda.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import java.util.Collection;

public class CentralizedLindaCollectionTest {

    private Linda linda;

    @Before
    public void setUp() {
        linda = new CentralizedLinda();
    }

    @Test
    public void testReadAll() {
        linda.write(new Tuple(1, "a"));
        linda.write(new Tuple(2, "b"));
        linda.write(new Tuple(3, "c"));
        linda.write(new Tuple("different", 4));

        Tuple template = new Tuple(Integer.class, String.class);
        Collection<Tuple> results = linda.readAll(template);

        assertEquals("Should find 3 matching tuples", 3, results.size());

        // Verifier les valeurs des tuples
        Collection<Tuple> stillThere = linda.readAll(template);
        assertEquals("Tuples should still be there after readAll", 3, stillThere.size());
    }

    @Test
    public void testTakeAll() {
        linda.write(new Tuple(10, true));
        linda.write(new Tuple(20, false));
        linda.write(new Tuple(30, true));
        linda.write(new Tuple("string", 40));

        Tuple template = new Tuple(Integer.class, Boolean.class);
        Collection<Tuple> results = linda.takeAll(template);

        assertEquals("Should take 3 matching tuples", 3, results.size());

        // On vérifie que les tuples correspondants ont été retirés
        Collection<Tuple> shouldBeEmpty = linda.readAll(template);
        assertEquals("No matching tuples should remain", 0, shouldBeEmpty.size());

        // On vérifie que les tuples non correspondants sont toujours là
        Tuple remaining = linda.tryRead(new Tuple(String.class, Integer.class));
        assertNotNull("Non-matching tuple should remain", remaining);
    }

    @Test
    public void testReadAllEmpty() {
        Tuple template = new Tuple(Double.class);
        Collection<Tuple> results = linda.readAll(template);

        assertNotNull("Should return collection", results);
        assertEquals("Should return empty collection", 0, results.size());
    }

    @Test
    public void testTakeAllEmpty() {
        Tuple template = new Tuple(Character.class);
        Collection<Tuple> results = linda.takeAll(template);

        assertNotNull("Should return collection", results);
        assertEquals("Should return empty collection", 0, results.size());
    }
}
