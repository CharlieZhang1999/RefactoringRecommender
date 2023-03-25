package cmu.csdetector.extractor;

import cmu.csdetector.metrics.calculators.type.LCOM2Calculator;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.resources.Type;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
import cmu.csdetector.resources.loader.SourceFilesLoader;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
     * If the resource is a method, the belonging class of the method
     */
    private final Type belongingType;
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
    private final StatementVisitor statementVisitor = new StatementVisitor();

    /**
     * Constructor for a class
     */
    public Extractor(Type resource) {
        this.resource = resource;
        this.belongingType = resource;
        this.cu = resource.getSourceFile().getCompilationUnit();
        this.sourceFile = resource.getSourceFile().getFile();
    }

    /**
     * Constructor for a method
     */
    public Extractor(Method resource, Type belongingType) {
        this.resource = resource;
        this.belongingType = belongingType;
        this.cu = resource.getSourceFile().getCompilationUnit();
        this.sourceFile = resource.getSourceFile().getFile();
    }

    public void extract() {
        // Step 1: Accept the visitor to build the statement table
        resource.getNode().accept(statementVisitor);
        SortedMap<Integer, Set<String>> statementsTable = statementVisitor.getLineNumToStatementsTable(cu);

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
                        // build the extracted method declaration
                        ExtractedMethod em = new ExtractedMethod(sourceFile, startLine, endLine);
                        em.create();

                        // calculate 3 types of LCOM
                        //var nodesToBeRemoved = this.statementVisitor.getNodesByLineNumbers(em.getLineNumbers(), statementsTable);
                        double originalLCOM2 = new LCOM2Calculator().getValue(this.belongingType.getNodeAsTypeDeclaration());
                        double refactoredLCOM2 = 0;
                        double opportunityLCOM2 = 0;
                        em.setThreeLCOM(originalLCOM2, refactoredLCOM2, opportunityLCOM2);

                        return em;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter((ExtractedMethod em) -> em.getExtractedMethodDeclaration() != null)
                .collect(Collectors.toList());

        // filter & ranking
        OpportunityProcessor opportunityProcessor = new OpportunityProcessor(extractedMethods);
        opportunityProcessor.process();

        // Step 4: Assign the method name, parameters, and return type to each method declaration
        // TODO: Implement this step, e.g., using GPT fine-tuning
        extractedMethods.forEach(method -> {
            method.setExtractedMethodName("extractedMethod");
            method.setExtractedMethodParameters(new ArrayList<>());
            method.setExtractedMethodReturnType(PrimitiveType.VOID);
//            System.out.println(method.getMethodDeclaration().toString());
        });

        // Step 5: Finding the Target class
        // TODO: Implement this step, e.g., LCOM
    }

    /**
     * Get a copy of the belonging type of the target resource,
     * which is able to resolve bindings.
     * @return The cloned belonging type
     */
    private Type getClonedBelongingType() {
        // get file path from source file
        String filePath = sourceFile.getAbsolutePath();
        // get its directory path
        String dirPath = filePath.substring(0, filePath.lastIndexOf(File.separator));
        // find the target type declaration by identifier
        JavaFilesFinder finder = new JavaFilesFinder(dirPath);
        try {
            SourceFilesLoader loader = new SourceFilesLoader(finder);
            for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
                for (Type sourceType : sourceFile.getTypes()) {
                    if (sourceType.getNodeAsTypeDeclaration().getName().getIdentifier().equals(this.belongingType.getNodeAsTypeDeclaration().getName().getIdentifier())) {
                        return sourceType;
                    }
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private TypeDeclaration getRefactoredBelongingType(TypeDeclaration refactoredTypeDeclaration) {
        return null;
    }

    private TypeDeclaration getOpportunityBelongingType() {
        return null;
    }
}
