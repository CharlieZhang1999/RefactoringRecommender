package cmu.csdetector.extractor;

import cmu.csdetector.resources.Type;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementVisitorTest {
    @Test
    void visit() throws IOException {
        File testPath = new File("examples/ComplexClass/src/main/java/paper/example");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        Type complexClass = types.stream().filter(t -> t.getFullyQualifiedName().equals("paper.example.ComplexClass")).findFirst().get();
        StatementVisitor visitor = new StatementVisitor();
        complexClass.getNode().accept(visitor);
        SortedMap<Integer, Set<String>> statementsTable = visitor.getLineNumToStatementsTable(complexClass.getSourceFile().getCompilationUnit());

        assertEquals(23, statementsTable.size());
        assertArrayEquals(new String[]{"rcs", "length", "manifests", "rcs.length"}, statementsTable.get(6).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "length", "i", "rcs.length"}, statementsTable.get(8).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec"}, statementsTable.get(9).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, statementsTable.get(10).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "rcs", "i", "grabRes"}, statementsTable.get(11).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, statementsTable.get(12).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "rcs", "i", "grabNonFileSetRes"}, statementsTable.get(13).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec.length", "rec", "length", "j"}, statementsTable.get(15).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "getName", "name", "replace", "j"}, statementsTable.get(16).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i"}, statementsTable.get(17).toArray(String[]::new));
        assertArrayEquals(new String[]{"rcs", "i", "afs"}, statementsTable.get(19).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "equals", "afs.getFullPath", "afs"}, statementsTable.get(20).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "name", "afs.getFullPath", "afs"}, statementsTable.get(21).toArray(String[]::new));
        assertArrayEquals(new String[]{"getFullPath", "getProj", "equals", "afs.getPref", "getPref", "afs.getFullPath", "afs"}, statementsTable.get(22).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "getProj", "afs.getPref", "getPref", "afs"}, statementsTable.get(23).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "endsWith", "pr.endsWith"}, statementsTable.get(24).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr"}, statementsTable.get(25).toArray(String[]::new));
        assertArrayEquals(new String[]{"pr", "name"}, statementsTable.get(27).toArray(String[]::new));
        assertArrayEquals(new String[]{"name", "equalsIgnoreCase", "MANIFEST_NAME", "name.equalsIgnoreCase"}, statementsTable.get(30).toArray(String[]::new));
        assertArrayEquals(new String[]{"rec", "manifests", "i", "j"}, statementsTable.get(31).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests", "i"}, statementsTable.get(35).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests", "i"}, statementsTable.get(36).toArray(String[]::new));
        assertArrayEquals(new String[]{"manifests"}, statementsTable.get(39).toArray(String[]::new));
    }
}
