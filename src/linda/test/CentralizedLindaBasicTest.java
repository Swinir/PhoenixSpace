package linda.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;
import java.util.Collection;

public class CentralizedLindaBasicTest {

    private Linda linda;

    @Before
    public void setUp() {
        linda = new CentralizedLinda();
    }

    @Test
    public void testWriteAndRead() {
        Tuple tuple = new Tuple(42, "hello");
        linda.write(tuple);

        Tuple template = new Tuple(Integer.class, String.class);
        Tuple result = linda.tryRead(template);

        assertNotNull("Should find matching tuple", result);
        assertEquals("Should match first element", 42, result.get(0));
        assertEquals("Should match second element", "hello", result.get(1));
    }

    @Test
    public void testWriteAndTake() {
        Tuple tuple = new Tuple(100, true);
        linda.write(tuple);

        Tuple template = new Tuple(Integer.class, Boolean.class);
        Tuple result = linda.tryTake(template);

        assertNotNull("Should find matching tuple", result);
        assertEquals("Should match first element", 100, result.get(0));
        assertEquals("Should match second element", true, result.get(1));

        // Verifier que le tuple a été retiré
        Tuple secondResult = linda.tryRead(template);
        assertNull("Tuple should be removed after take", secondResult);
    }

    @Test
    public void testTryReadNonExistent() {
        Tuple template = new Tuple(String.class, Integer.class);
        Tuple result = linda.tryRead(template);
        assertNull("Should return null for non-existent tuple", result);
    }

    @Test
    public void testTryTakeNonExistent() {
        Tuple template = new Tuple(Double.class);
        Tuple result = linda.tryTake(template);
        assertNull("Should return null for non-existent tuple", result);
    }

    @Test
    public void testMultipleTuples() {
        linda.write(new Tuple(1, "first"));
        linda.write(new Tuple(2, "second"));
        linda.write(new Tuple(3, "third"));

        Tuple template = new Tuple(Integer.class, String.class);

        // Essaye de prendre les tuples un par un
        Tuple result1 = linda.tryTake(template);
        assertNotNull("Should find first tuple", result1);

        Tuple result2 = linda.tryTake(template);
        assertNotNull("Should find second tuple", result2);

        Tuple result3 = linda.tryTake(template);
        assertNotNull("Should find third tuple", result3);

        // Vérifie que le quatrième essai ne trouve rien
        Tuple result4 = linda.tryTake(template);
        assertNull("Should not find fourth tuple", result4);
    }
}
