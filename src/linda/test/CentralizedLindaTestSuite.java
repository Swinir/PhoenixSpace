package linda.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        CentralizedLindaBasicTest.class,
        CentralizedLindaBlockingTest.class,
        CentralizedLindaCollectionTest.class,
        CentralizedLindaPatternTest.class,
        CentralizedLindaConcurrencyTest.class
})
public class CentralizedLindaTestSuite {
    // Cette classe est vide. Elle sert uniquement à lancer les tests que nous avons écrits plus facieement
}
