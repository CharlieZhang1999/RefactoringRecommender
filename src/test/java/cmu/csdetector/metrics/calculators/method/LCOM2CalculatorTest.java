package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LCOM2CalculatorTest {
    private final Map<String, Double> nameToLCOM2 = Map.of(
            "cmu.csdetector.dummy.lcom.DummyDad", 0.5,
            "cmu.csdetector.dummy.lcom.DummySon", 0.5,
            "cmu.csdetector.dummy.lcom.DummyGrandSon", 0.0,
            "cmu.csdetector.dummy.lcom.DummyLCOM", 0.7142857142857143,
            "cmu.csdetector.dummy.lcom.EmptyClass", 0.0
    );

    @Test
    void computeValue() throws IOException {
        File testPath = new File("src/test/java/cmu/csdetector/dummy/lcom");
        var types = TypeLoader.loadAllFromDir(testPath);
        LCOM2Calculator calculator = new LCOM2Calculator();
        types.forEach((t) -> {
            Double value = calculator.computeValue(t.getNode());
            assertEquals(nameToLCOM2.get(t.getFullyQualifiedName()), value, 1e-6, t.getFullyQualifiedName());
        });
    }

    @Test
    void getMetricName() {
        LCOM2Calculator calculator = new LCOM2Calculator();
        assertEquals(MetricName.LCOM2, calculator.getMetricName());
    }

    @Test
    void shouldComputeAggregate() {
        LCOM2Calculator calculator = new LCOM2Calculator();
        assertTrue(calculator.shouldComputeAggregate());
    }
}
