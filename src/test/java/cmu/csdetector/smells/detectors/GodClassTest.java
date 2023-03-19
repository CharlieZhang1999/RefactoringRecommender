package cmu.csdetector.smells.detectors;

import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellName;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GodClassTest {
    // Classes that have GodClass smell, other than these should not be detected to have GodClass smell
    Set<String> godClassNames = Set.of(
            "cmu.csdetector.dummy.smells.BlobClassSample",
            "cmu.csdetector.dummy.smells.BrainClassWithOneBrainMethod"
    );
    @Test
    void detectNonTargetClasses() throws IOException {
        File testPath = new File("src/test/java/cmu/csdetector/dummy/smells");
        var types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        GodClass godClass = new GodClass();
        types.forEach((t) -> {
            String className = t.getFullyQualifiedName();
            // non-target classes should not have GodClass smell
            if (!godClassNames.contains(className)) {
                List<Smell> smells = godClass.detect(t);
                assertEquals(0, smells.size());
            }
        });
    }

    @Test
    void detectTargetClasses() throws IOException {
        File testPath = new File("src/test/java/cmu/csdetector/dummy/smells");
        var types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        GodClass godClass = new GodClass();
        types.forEach((t) -> {
            String className = t.getFullyQualifiedName();
            // target classes should have GodClass smell
            if (godClassNames.contains(className)) {
                List<Smell> smells = godClass.detect(t);
                assertEquals(1, smells.size());
                System.out.println(className + ": " + smells.get(0).getReason());
            }
        });
    }

    @Test
    void getSmellName() {
        assertEquals(new GodClass().getSmellName(), SmellName.GodClass);
    }
}
