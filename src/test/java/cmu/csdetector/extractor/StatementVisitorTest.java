package cmu.csdetector.extractor;

import cmu.csdetector.resources.Type;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementVisitorTest {
    @Test
    void visit() throws IOException {
        File testPath = new File("examples/ComplexClass/src/main/java/paper/example");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        Type complexClass = types.stream().filter(t -> t.getFullyQualifiedName().equals("paper.example.ComplexClass")).findFirst().get();
        var visitor = new StatementVisitor();
        complexClass.getNode().accept(visitor);
        var sortedMap = visitor.getLineNumToStatementsTable(complexClass.getSourceFile().getCompilationUnit());

        assertEquals(23, sortedMap.size());
        assertArrayEquals(new String[]{"rcs", "length", "manifests", "rcs.length"}, sortedMap.get(6).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "length", "i", "rcs.length"}, sortedMap.get(8).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec"}, sortedMap.get(9).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, sortedMap.get(10).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "rcs", "i", "grabRes"}, sortedMap.get(11).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, sortedMap.get(12).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "rcs", "i", "grabNonFileSetRes"}, sortedMap.get(13).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec.length", "rec", "length", "j"}, sortedMap.get(15).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "getName", "name", "replace", "j"}, sortedMap.get(16).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, sortedMap.get(17).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i", "afs"}, sortedMap.get(19).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "equals", "afs.getFullPath", "afs"}, sortedMap.get(20).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "name", "afs.getFullPath", "afs"}, sortedMap.get(21).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "equals", "afs.getPref", "getPref", "afs.getFullPath", "afs"}, sortedMap.get(22).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "getProj", "afs.getPref", "getPref", "afs"}, sortedMap.get(23).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "endsWith", "pr.endsWith"}, sortedMap.get(24).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr"}, sortedMap.get(25).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "name"}, sortedMap.get(27).toArray(String[]::new));
        assertArrayEquals(new String[]{"name", "equalsIgnoreCase", "MANIFEST_NAME", "name.equalsIgnoreCase"}, sortedMap.get(30).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "manifests", "i", "j"}, sortedMap.get(31).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests", "i"}, sortedMap.get(35).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests", "i"}, sortedMap.get(36).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests"}, sortedMap.get(39).toArray(String[]::new));
    }
}
