package cmu.csdetector.extractor;

import cmu.csdetector.ast.visitors.MethodCollector;
import org.eclipse.jdt.core.dom.*;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.resources.Type;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFilesLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
    private final MethodVariableCollector methodVariableCollector = new MethodVariableCollector();
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

    private Boolean isBetween(CompilationUnit cu, ASTNode node, int startLineNumber, int endLineNUmber) {
        int start = cu.getLineNumber(node.getStartPosition());
        int end = cu.getLineNumber(node.getStartPosition() + node.getLength());

        return startLineNumber <= start && end <= endLineNUmber;
    }

    public void extract() {
        // Step 1: Accept the visitor to build the statement table
        resource.getNode().accept(statementVisitor);
//        if(resource instanceof Method){
//            resource.getNode().accept(Met);
//        }
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

                        // calculate 3 types of LCOM for Step 4
                        em.calculateLCOM(this.belongingType.getNodeAsTypeDeclaration());

                        return em;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter((ExtractedMethod em) -> em.getExtractedMethodDeclaration() != null && em.getRefactoredTypeDeclaration() != null) // drop uncompilable opportunities
                .collect(Collectors.toList());

        // Step 4: Filter & ranking the opportunities
        OpportunityProcessor opportunityProcessor = new OpportunityProcessor(extractedMethods);
        extractedMethods = opportunityProcessor.process(); // processed opportunities

        // Step 5: Assign the method name, parameters, and return type to each method declaration
        // TODO: Implement this step, e.g., using GPT fine-tuning
        extractedMethods.forEach(em -> {
            MethodDeclaration method;
            if (this.resource instanceof Method) {
                method = (MethodDeclaration) this.resource.getNode();
            } else {
                CompilationUnit cu = this.resource.getSourceFile().getCompilationUnit();
                MethodCollector collector = new MethodCollector();
                this.resource.getNode().accept(collector);
                List<MethodDeclaration> methods = collector.getNodesCollected();
                method = null;
                for (MethodDeclaration md: methods) {

                    int start = cu.getLineNumber(md.getStartPosition()) - 1;
                    int end = cu.getLineNumber(md.getStartPosition() + md.getLength()) - 1;
                    if (em.getLineRange()[0] >= start && em.getLineRange()[1] <= end) {
                        method = md;
                        break;
                    }
                }
            }
            List<SimpleName> arr = new ArrayList<>();
            if (method == null) {
                System.out.println("Method not found!");
            } else {
                method.accept(methodVariableCollector);
                arr = methodVariableCollector.getNodesCollected();
            }
            List<String> declared = new ArrayList<>();
            List<String> used = new ArrayList<>();
            for (SimpleName sn: arr) {
                if (isBetween(cu, sn, em.getLineRange()[0], em.getLineRange()[1])) {
                    if (sn.isDeclaration()) {
                        declared.add(sn.getIdentifier());
                    } else {
                        used.add(sn.getIdentifier());
                    }
                }
            }
            List<String> params = new ArrayList<>();
            for (String id: used) {
                if ((!declared.contains(id)) && (!params.contains(id))) {
                    params.add(id);
                }
            }

            String methodBody = em.getExtractedMethodDeclaration().getBody().toString();
            SignatureRecommender recommender = new SignatureRecommender(em);
            em.setExtractedMethodName("extractedMethod");
            em.setExtractedMethodParameters(new ArrayList<>());
            em.setExtractedMethodReturnType(PrimitiveType.VOID);
        });

        // Step 6: Finding the Target class for each opportunity
        Map<MethodDeclaration, Map<Type, Double>> extractionImprovements = new HashMap<>(); // <extracted method declaration, <target class, improvement>>
        List<Type> candidateClasses = getCandidateClasses();
        for (ExtractedMethod em : extractedMethods) {
            MethodDeclaration extractedMethodDeclaration = em.getExtractedMethodDeclaration();
            System.out.println("===== Extracted Method =====");
            System.out.println(extractedMethodDeclaration);
            for (Type candidateTargetClass : candidateClasses) {
                RefactoringEvaluator evaluator = new RefactoringEvaluator(this.getSourceFileDirectory(), this.belongingType.getNodeAsTypeDeclaration(), candidateTargetClass.getNodeAsTypeDeclaration(), extractedMethodDeclaration, em.getRefactoredTypeCU());
                try {
                    evaluator.evaluate();
                    Double reduction = evaluator.getLCOMReduction(); // the larger the better
                    if (reduction <= 0) { // negative refactoring
                        System.out.println("Skip negative refactoring: " + candidateTargetClass.getNodeAsTypeDeclaration().getName().getIdentifier());
                        continue;
                    }
                    evaluator.printEvaluation();
                    // update results
                    extractionImprovements.putIfAbsent(extractedMethodDeclaration, new HashMap<>());
                    extractionImprovements.get(extractedMethodDeclaration).put(candidateTargetClass, reduction);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("*** Best Target Class: " + extractionImprovements.get(extractedMethodDeclaration).entrySet().stream().max(Comparator.comparingDouble(Map.Entry::getValue)).get().getKey().getNodeAsTypeDeclaration().getName().getIdentifier());
        }
        // TODO: add results to the output json file
    }

    /**
     * Get all candidate classes in the same package who may be the target class to move the extracted method to.
     *
     * @return All candidate classes in the same package
     */
    private List<Type> getCandidateClasses() {
        // find the target type declaration by identifier
        JavaFilesFinder finder = new JavaFilesFinder(this.getSourceFileDirectory().toString());
        try {
            SourceFilesLoader loader = new SourceFilesLoader(finder);
            return loader.getLoadedSourceFiles().stream()
                    .flatMap(sourceFile -> sourceFile.getTypes().stream())
                    .filter(type -> !type.getNodeAsTypeDeclaration().getName().getIdentifier().equals(this.belongingType.getNodeAsTypeDeclaration().getName().getIdentifier())) // exclude the belonging type
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private Path getSourceFileDirectory() {
        return sourceFile.toPath().getParent();
    }
}
