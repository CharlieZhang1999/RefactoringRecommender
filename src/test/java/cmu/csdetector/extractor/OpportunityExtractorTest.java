package cmu.csdetector.extractor;

import cmu.csdetector.util.GetTargetType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OpportunityExtractorTest {
    private void prettyPrintOpportunities(Set<List<Integer>> opportunitySet) {
        opportunitySet.stream()
            // sort by the first line number, then by the last line number
            .sorted(Comparator.comparingInt((List<Integer> o) -> o.get(0)).thenComparingInt(o -> o.get(o.size() - 1)))
            .forEach(opportunity -> {
                System.out.println("Opportunity: " + opportunity);
            });
    }

    @Test
    void extract() throws IOException {
        // var smellyTarget = GetTargetType.getRefactoringExample();
        var smellyTarget = GetTargetType.getComplexClass();

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

        // check the opportunities
        assertTrue(opportunitySet.size() > 0);
        assertTrue(opportunitySet.stream().allMatch(opportunity -> opportunity.size() > 1));

        // pretty print the opportunities
        this.prettyPrintOpportunities(opportunitySet);
    }
}
