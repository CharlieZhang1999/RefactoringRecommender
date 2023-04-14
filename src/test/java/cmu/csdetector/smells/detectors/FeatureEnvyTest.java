package cmu.csdetector.smells.detectors;

import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Type;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellName;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeatureEnvyTest {
    private final String targetClass = "cmu.csdetector.dummy.smells.FeatureEnvyMethod";

    private void printSmells(List<Smell> smells, String identifier) {
        for (Smell smell : smells) {
            System.out.println(identifier + ": " + smell.getReason());
        }
    }

    @Test
    void detectNonTargetClasses() throws IOException {
        File testPath = new File("src/test/java/cmu/csdetector/dummy/smells");
        var types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        types.forEach((t) -> {
            // skip the target class
            if (t.getFullyQualifiedName().equals(targetClass)) {
                return;
            }
            t.getMethods().forEach((m) -> {
                List<Smell> smells = featureEnvy.detect(m);
                // non-target class methods should not have FeatureEnvy smell
                assertEquals(0, smells.size());
            });
        });
    }

    @Test
    void detect_localA() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("localA");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_localB() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("localB");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_localC() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("localC");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_localD() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("localD");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_superLocal() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("superLocal");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_superForeign() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("superForeign");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(2, smells.size());
        this.printSmells(smells, "superForeign");
    }

    @Test
    void detect_mostLocal() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("mostLocal");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(0, smells.size());
    }

    @Test
    void detect_mostForeign() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        FeatureEnvy featureEnvy = new FeatureEnvy();
        Method m = targetClassType.findMethodByName("mostForeign");
        List<Smell> smells = featureEnvy.detect(m);
        assertEquals(1, smells.size());
        this.printSmells(smells, "mostForeign");
    }

    @Test
    void getSmellName() {
        assertEquals(new FeatureEnvy().getSmellName(), SmellName.FeatureEnvy);
    }
}
