package cmu.csdetector.extractor;

import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Type;
import cmu.csdetector.util.GenericCollector;
import cmu.csdetector.util.TypeLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

class OpportunityExtractorTest {
    private Type getComplexClass() throws IOException {
        File testPath = new File("examples/ComplexClass/src/main/java/paper/example");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);
        return types.stream().filter(t -> t.getFullyQualifiedName().equals("paper.example.ComplexClass")).findFirst().get();
    }

    private Method getRefactoringExample() throws IOException {
        File testPath = new File("examples/RefactoringExample/src/main/java");
        List<Type> types = TypeLoader.loadAllFromDir(testPath);
        GenericCollector.collectAll(types);
        var refactoringExampleClass = types.stream().filter(t -> t.getFullyQualifiedName().equals("Customer")).findFirst().get();
        return refactoringExampleClass.findMethodByName("statement");
    }

    private void prettyPrintStatementsTable(SortedMap<Integer, Set<String>> statementsTable) {
        statementsTable.forEach((lineNum, statements) -> {
            System.out.println("Line " + lineNum + ": " + statements);
        });
    }

    private void prettyPrintOpportunities(Set<List<Integer>> opportunitySet) {
        opportunitySet.stream().sorted(
                // sort by the first line number, then by the last line number
                Comparator.comparingInt((List<Integer> o) -> o.get(0)).thenComparingInt(o -> o.get(o.size() - 1))
        ).forEach(opportunity -> {
            System.out.println("Opportunity: " + opportunity);
        });
    }

    @Test
    void extract() throws IOException {
        var smellyTarget = this.getRefactoringExample();
        // var smellyTarget = this.getComplexClass();
        StatementVisitor visitor = new StatementVisitor();
        smellyTarget.getNode().accept(visitor);
        SortedMap<Integer, Set<String>> statementsTable = visitor.getLineNumToStatementsTable(smellyTarget.getSourceFile().getCompilationUnit());

        // extract the opportunities
        Set<List<Integer>> opportunitySet = new HashSet<>();
        for (int step = 1; step < statementsTable.size(); step++) {
            OpportunityExtractor opportunityExtractor = new OpportunityExtractor(statementsTable, step);
            while (opportunityExtractor.hasNext()) {
                List<List<Integer>> opportunities = opportunityExtractor.next();
                // add the opportunities whose length is greater than 1 to the map
                opportunities.stream().filter(opportunity -> opportunity.size() > 1).forEach(opportunitySet::add);
            }

        }
        // pretty print the opportunities
        this.prettyPrintOpportunities(opportunitySet);
    }
}
