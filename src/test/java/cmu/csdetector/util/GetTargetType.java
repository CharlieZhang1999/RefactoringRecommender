package cmu.csdetector.util;

import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.resources.Type;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GetTargetType {
    public static Type getComplexClass() throws IOException {
        File testPath = new File("examples/ComplexClass/src/main/java/paper/example");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        return types.stream().filter(t -> t.getFullyQualifiedName().equals("paper.example.ComplexClass")).findFirst().get();
    }

    public static Method getRefactoringExample() throws IOException {
        File testPath = new File("examples/RefactoringExample/src/main/java");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);

        var refactoringExampleClass = types.stream().filter(t -> t.getFullyQualifiedName().equals("Customer")).findFirst().get();
        return refactoringExampleClass.findMethodByName("statement");
    }

    public static File getSourceFileByResource(Resource resource) {
        return resource.getSourceFile().getFile();
    }
}
