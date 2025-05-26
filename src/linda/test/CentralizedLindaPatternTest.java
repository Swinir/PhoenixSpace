package linda.test;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import linda.Linda;
import linda.Tuple;
import linda.shm.CentralizedLinda;

public class CentralizedLindaPatternTest {

    private Linda linda;

    @Before
    public void setUp() {
        linda = new CentralizedLinda();
    }

    @Test
    public void testExactMatch() {
        linda.write(new Tuple(42, "exact", true));

        Tuple exactTemplate = new Tuple(42, "exact", true);
        Tuple result = linda.tryRead(exactTemplate);

        assertNotNull("Should match exact tuple", result);
    }

    @Test
    public void testTypeMatch() {
        linda.write(new Tuple(100, "type", false));

        Tuple typeTemplate = new Tuple(Integer.class, String.class, Boolean.class);
        Tuple result = linda.tryRead(typeTemplate);

        assertNotNull("Should match by type", result);
        assertEquals("Should have correct values", 100, result.get(0));
        assertEquals("Should have correct values", "type", result.get(1));
        assertEquals("Should have correct values", false, result.get(2));
    }

    @Test
    public void testMixedMatch() {
        linda.write(new Tuple("fixed", 200, "another"));

        Tuple mixedTemplate = new Tuple("fixed", Integer.class, String.class);
        Tuple result = linda.tryRead(mixedTemplate);

        assertNotNull("Should match mixed template", result);
        assertEquals("Should match fixed value", "fixed", result.get(0));
        assertEquals("Should match type", 200, result.get(1));
        assertEquals("Should match type", "another", result.get(2));
    }

    @Test
    public void testNoMatch() {
        linda.write(new Tuple(1, 2, 3));

        Tuple wrongTemplate = new Tuple(String.class, Integer.class, Integer.class);
        Tuple result = linda.tryRead(wrongTemplate);

        assertNull("Should not match wrong template", result);
    }

    @Test
    public void testNestedTuples() {
        Tuple innerTuple = new Tuple("inner", 42);
        linda.write(new Tuple("outer", innerTuple, true));

        Tuple template = new Tuple(String.class, Tuple.class, Boolean.class);
        Tuple result = linda.tryRead(template);

        assertNotNull("Should match nested tuple", result);
        assertEquals("Should match outer string", "outer", result.get(0));
        assertTrue("Should match inner tuple", result.get(1) instanceof Tuple);
        assertEquals("Should match boolean", true, result.get(2));
    }
}
