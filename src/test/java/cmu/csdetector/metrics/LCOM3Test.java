package cmu.csdetector.metrics;

import cmu.csdetector.ast.visitors.TypeDeclarationCollector;
import cmu.csdetector.metrics.calculators.type.LCOM3Calculator;
import cmu.csdetector.resources.Type;
import cmu.csdetector.util.TypeLoader;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LCOM3Test {
    private final double DELTA = 0.000001d;
    private List<Type> types;

    private Type findTypeByName(String name) {
        for (Type type : types) {
            TypeDeclaration td = (TypeDeclaration)type.getNode();
            String typeName = td.getName().toString();

            if (typeName.equals(name)) {
                return type;
            }
        }
        return null;
    }

    /*
     * Calculates and returns the lcom3 value for a particular class
     */
    private Double getLCOM3Value(String className) throws NullPointerException{
        Type typeClass = findTypeByName(className);

        TypeDeclarationCollector visitor = new TypeDeclarationCollector();
        assert typeClass != null;

        typeClass.getNode().accept(visitor);

        TypeDeclaration type = visitor.getNodesCollected().get(0);
        LCOM3Calculator calculator = new LCOM3Calculator();

        return calculator.getValue(type);
    }

    @BeforeAll
    public void setUp() throws IOException {
        File dir = new File("src/test/java/cmu/csdetector/dummy/lcom");
        this.types = TypeLoader.loadAllFromDir(dir);

    }

    /*
     * Test Empty class, the LCOM3 must be 0
     */
    @Test
    public void testLCOM3EmptyClass() {
        Double obtained = getLCOM3Value("EmptyClass");

        Assertions.assertEquals(0, obtained, DELTA);

    }

    /*
     * The lcom.DummyLCOM class has LCOM3 = 1.071428571
     */
    @Test
    public void testLCOM2NotEmptyClass() {
        Double obtained = getLCOM3Value("DummyLCOM");

        Assertions.assertEquals(1.071428571, obtained, DELTA);

    }

    /*
     * Test class with LCOM3 must be 0.5
     */
    @Test
    public void testLCOM3DummyDad() {
        Double obtained = getLCOM3Value("DummyDad");

        Assertions.assertEquals(0.0, obtained, DELTA);

    }

    /*
     * Dummy class with 1 attribute (from DummyDad) and 2 method, the LCOM3 must be 1
     */
    @Test
    public void testLCOM3DummySon() {
        Double obtained = getLCOM3Value("DummySon");

        Assertions.assertEquals(1.0, obtained, DELTA);

    }

    /*
     * The LCOM2 must be 0
     */
    @Test
    public void testLCOM3DummyGrandSon() {
        Double obtained = getLCOM3Value("DummyGrandSon");

        Assertions.assertEquals(0.0, obtained, DELTA);

    }

}
