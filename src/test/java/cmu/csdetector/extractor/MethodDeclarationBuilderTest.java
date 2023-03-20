package cmu.csdetector.extractor;

import cmu.csdetector.util.GetTargetType;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

class MethodDeclarationBuilderTest {

    @Test
    void createMethodFromLineNumbers() throws IOException {
        // var smellyTarget = GetTargetType.getRefactoringExample();
        var smellyTarget = GetTargetType.getComplexClass();
        var sourceFile = smellyTarget.getSourceFile().getFile();

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
                opportunitySet.addAll(opportunities);
            }
        }
        opportunitySet.forEach((List<Integer> opportunity) -> {
            int startLine = opportunity.get(0);
            int endLine = opportunity.get(opportunity.size() - 1);
            try {
                MethodDeclarationBuilder methodDeclarationBuilder = new MethodDeclarationBuilder(sourceFile);
                MethodDeclaration method = methodDeclarationBuilder.createMethodFromLineNumbers("test", startLine, endLine);
                System.out.println("Opportunity: [" + startLine + " - " + endLine + "]");
                System.out.println(method);
                System.out.println("--------------------");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
