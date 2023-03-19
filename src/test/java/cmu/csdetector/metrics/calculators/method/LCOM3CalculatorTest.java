package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LCOM3CalculatorTest {
    private final Map<String, Double> nameToLCOM3 = Map.of(
            "cmu.csdetector.dummy.lcom.DummyDad", 0.0,
            "cmu.csdetector.dummy.lcom.DummySon", 1.0,
            "cmu.csdetector.dummy.lcom.DummyGrandSon", 0.0,
            "cmu.csdetector.dummy.lcom.DummyLCOM", 1.0714285714285714,
            "cmu.csdetector.dummy.lcom.EmptyClass", 0.0
    );

    @Test
    void computeValue() throws IOException {
        LCOM3Calculator calculator = new LCOM3Calculator();
        File testPath = new File("src/test/java/cmu/csdetector/dummy/lcom");
        var types = TypeLoader.loadAllFromDir(testPath);
        types.forEach((t) -> {
            Double value = calculator.computeValue(t.getNode());
            assertEquals(nameToLCOM3.get(t.getFullyQualifiedName()), value, 1e-6);
        });
    }

    @Test
    void getMetricName() {
        LCOM3Calculator calculator = new LCOM3Calculator();
        assertEquals(MetricName.LCOM3, calculator.getMetricName());
    }

    @Test
    void shouldComputeAggregate() {
        LCOM3Calculator calculator = new LCOM3Calculator();
        assertTrue(calculator.shouldComputeAggregate());
    }
}
