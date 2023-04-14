package cmu.csdetector.metrics;

import cmu.csdetector.util.TypeLoader;
import cmu.csdetector.ast.visitors.TypeDeclarationCollector;
import cmu.csdetector.metrics.calculators.type.LCOM2Calculator;
import cmu.csdetector.resources.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;
import java.io.IOException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LCOM2Test {
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
     * Calculates and returns the lcom2 value for a particular class
     */
    private Double getLCOM2Value(String className) throws NullPointerException{
        Type typeClass = findTypeByName(className);

        TypeDeclarationCollector visitor = new TypeDeclarationCollector();
        assert typeClass != null;

        typeClass.getNode().accept(visitor);

        TypeDeclaration type = visitor.getNodesCollected().get(0);
        LCOM2Calculator calculator = new LCOM2Calculator();

        return calculator.getValue(type);
    }

    @BeforeAll
    public void setUp() throws IOException {
        File dir = new File("src/test/java/cmu/csdetector/dummy/lcom");
        this.types = TypeLoader.loadAllFromDir(dir);

    }

    /*
     * Test Empty class, the LCOM2 must be 0
     */
    @Test
    public void testLCOM2EmptyClass() {
        Double obtained = getLCOM2Value("EmptyClass");

        Assertions.assertEquals(0, obtained, DELTA);

    }

    /*
     * The lcom.DummyLCOM class has LCOM2 = 0.714285
     */
    @Test
    public void testLCOM2NotEmptyClass() {
        Double obtained = getLCOM2Value("DummyLCOM");

        Assertions.assertEquals(0.714285, obtained, DELTA);

    }

    /*
     * Test class with LCOM2 must be 0.5
     */
    @Test
    public void testLCOM2DummyDad() {
        Double obtained = getLCOM2Value("DummyDad");

        Assertions.assertEquals(0.5, obtained, DELTA);

    }

    /*
     * The LCOM2 must be 0.5
     */
    @Test
    public void testLCOM2DummySon() {
        Double obtained = getLCOM2Value("DummySon");

        Assertions.assertEquals(0.5, obtained, DELTA);

    }

    /*
     * The LCOM2 must be 0
     */
    @Test
    public void testLCOM2DummyGrandSon() {
        Double obtained = getLCOM2Value("DummyGrandSon");

        Assertions.assertEquals(0.0, obtained, DELTA);

    }

}
