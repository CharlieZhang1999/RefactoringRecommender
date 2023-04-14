package cmu.csdetector.smells.detectors;

import cmu.csdetector.resources.Type;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellName;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LongParameterListTest {
    // Classes that have LongParameterList smell, other than these should not be detected to have LongParameterList smell
    private final String targetClass = "cmu.csdetector.dummy.smells.LongParameterListSample";

    // Has smell
    @Test
    void detect_LongParameterListSampleMethod() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        LongParameterList longParameterList = new LongParameterList();
        List<Smell> smells = longParameterList.detect(targetClassType.findMethodByName("LongParameterListSampleMethod"));
        assertEquals(1, smells.size());
        System.out.println(smells.get(0).getReason());
    }

    // Has smell because the average number of parameters is about 1.1
    @Test
    void detect_FourParameterListSampleMethod() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        LongParameterList longParameterList = new LongParameterList();
        List<Smell> smells = longParameterList.detect(targetClassType.findMethodByName("FourParameterListSampleMethod"));
        assertEquals(1, smells.size());
        System.out.println(smells.get(0).getReason());
    }

    // No smell because 2 parameters is not greater than 3
    @Test
    void detect_TwoParameterListSampleMethod() throws IOException {
        Type targetClassType = TypeLoader.getTargetClassType(targetClass);

        LongParameterList longParameterList = new LongParameterList();
        List<Smell> smells = longParameterList.detect(targetClassType.findMethodByName("TwoParameterListSampleMethod"));
        assertEquals(0, smells.size());
    }

    @Test
    void detectNonTargetClasses() throws IOException {
        File testPath = new File("src/test/java/cmu/csdetector/dummy/smells");
        var types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        LongParameterList longParameterList = new LongParameterList();
        types.forEach((t) -> {
            // skip the target class
            if (t.getFullyQualifiedName().equals(targetClass)) {
                return;
            }
            // non-target class methods should not have LongParameterList smell
            t.getMethods().forEach((m) -> {
                List<Smell> smells = longParameterList.detect(m);
                assertEquals(0, smells.size());
            });
        });
    }

    @Test
    void getSmellName() {
        assertEquals(SmellName.LongParameterList, new LongParameterList().getSmellName());
    }
}
