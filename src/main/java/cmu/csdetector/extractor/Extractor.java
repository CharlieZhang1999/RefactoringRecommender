package cmu.csdetector.extractor;

import cmu.csdetector.resources.Resource;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main pipeline for extracting opportunities for a given target resource.
 */
public class Extractor {
    /**
     * The target resource to extract opportunities from
     */
    private final Resource resource;
    /**
     * The compilation unit of the target resource
     */
    private final CompilationUnit cu;
    /**
     * The source file of the target resource
     */
    private final File sourceFile;
    /**
     * The visitor to build statement table
     */
    private final StatementVisitor visitor = new StatementVisitor();

    public Extractor(Resource resource) {
        this.resource = resource;
        this.cu = resource.getSourceFile().getCompilationUnit();
        this.sourceFile = resource.getSourceFile().getFile();
    }

    public void extract() {
        // Step 1: Accept the visitor to build the statement table
        resource.getNode().accept(visitor);
        SortedMap<Integer, Set<String>> statementsTable = visitor.getLineNumToStatementsTable(cu);

        // Step 2: Extract the opportunities for each step
        Set<List<Integer>> opportunitySet = new HashSet<>();
        for (int step = 1; step < statementsTable.size(); step++) {
            StepIterator stepIterator = new StepIterator(statementsTable, step);
            while (stepIterator.hasNext()) {
                List<List<Integer>> opportunities = stepIterator.next();
                // add the opportunities whose length is greater than 1 to the map
                opportunitySet.addAll(opportunities);
            }
        }

        // Step 3: Create method declarations for all opportunities
        List<ExtractedMethod> extractedMethods = opportunitySet.stream()
                .map(opportunity -> {
                    int startLine = opportunity.get(0);
                    int endLine = opportunity.get(opportunity.size() - 1);
                    try {
                        ExtractedMethod em = new ExtractedMethod(sourceFile, startLine, endLine);
                        em.create();
                        return em;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter((ExtractedMethod em) -> em.getMethodDeclaration() != null)
                .collect(Collectors.toList());

        // Step 4: Assign the method name, parameters, and return type to each method declaration
        // TODO: Implement this step, e.g., using GPT fine-tuning
        extractedMethods.forEach(method -> {
            method.setMethodName("extractedMethod");
            method.setMethodParameters(new ArrayList<>());
            method.setMethodReturnType(PrimitiveType.VOID);
            System.out.println(method.getMethodDeclaration().toString());
        });

        // Step 5: Finding the Target class
        // TODO: Implement this step, e.g., LCOM
        extractedMethods.forEach(em -> {
            System.out.println("em.getLCOM2()");
        });
    }
}
