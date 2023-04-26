package cmu.csdetector.extractor;

import cmu.csdetector.ast.visitors.MethodCollector;
import cmu.csdetector.extractor.evaluator.RefactoringCCEvaluator;
import cmu.csdetector.extractor.evaluator.RefactoringLCOMEvaluator;
import cmu.csdetector.predictor.Predictor;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.resources.Type;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFilesLoader;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The main pipeline for extracting opportunities for a given target resource.
 */
public class Extractor {
    private final boolean DEBUG = true;
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

    private final MethodAssignmentCollector methodAssignmentCollector = new MethodAssignmentCollector();
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

    private Boolean isAfter(CompilationUnit cu, ASTNode node, int LineNUmber) {
        int start = cu.getLineNumber(node.getStartPosition());

        return start > LineNUmber;
    }

    public void extract() {
        // Step 1: Accept the visitor to build the statement table
        resource.getNode().accept(statementVisitor);
        SortedMap<Integer, Set<String>> statementsTable = statementVisitor.getLineNumToStatementsTable(cu);
        if (this.DEBUG) {
            // pretty print the statement table in ascii table
            System.out.println("===== Statement Table =====");
            System.out.println("Line Number | Statements");
            statementsTable.forEach((lineNum, statements) -> {
                System.out.println(String.format("%11d | %s", lineNum, statements.stream().collect(Collectors.joining(", "))));
            });
        }

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
        if (this.DEBUG) {
            // pretty print the opportunities in ascii table
            System.out.println("===== Opportunities =====");
            System.out.println("Start Line | End Line | Variables");
            opportunitySet.forEach(opportunity -> {
                String statements = opportunity.stream()
                        .map(statementsTable::get)
                        .map(statementsSet -> statementsSet.stream().collect(Collectors.joining(", ")))
                        .collect(Collectors.joining(", "));
                System.out.println(String.format("%10d | %8d | %s", opportunity.get(0), opportunity.get(opportunity.size() - 1), statements));
            });
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
        int beforeExtractedMethodCount = extractedMethods.size();
        OpportunityProcessor opportunityProcessor = new OpportunityProcessor(extractedMethods);
        extractedMethods = opportunityProcessor.process(); // processed opportunities
        int afterExtractedMethodCount = extractedMethods.size();
        if (this.DEBUG) {
            System.out.println(String.format("===== Opportunities Filtered ====="));
            System.out.println(String.format("Before: %d, After: %d", beforeExtractedMethodCount, afterExtractedMethodCount));
        }

        // Step 5: Assign the method name, parameters, and return type to each method declaration
        extractedMethods.forEach(em -> {

            MethodDeclaration method = null;
            if (this.resource instanceof Method) {
                method = (MethodDeclaration) this.resource.getNode();
            } else {
                CompilationUnit cu = this.resource.getSourceFile().getCompilationUnit();
                MethodCollector collector = new MethodCollector();
                this.resource.getNode().accept(collector);
                List<MethodDeclaration> methods = collector.getNodesCollected();
                for (MethodDeclaration md: methods) {

                    int start = cu.getLineNumber(md.getStartPosition());
                    int end = cu.getLineNumber(md.getStartPosition() + md.getLength());
                    if (em.getLineRange()[0] >= start && em.getLineRange()[1] <= end) {
                        method = md;
                        break;
                    }
                }
            }
            List<SimpleName> variables = new ArrayList<>();
            List<SimpleName> assignments = new ArrayList<>();
            if (method == null) {
                System.out.println("Method not found!");
            } else {
                method.accept(methodVariableCollector);
                variables = methodVariableCollector.getNodesCollected();
                method.accept(methodAssignmentCollector);
                assignments = methodAssignmentCollector.getNodesCollected();
            }
            List<SimpleName> declared = new ArrayList<>();
            List<SimpleName> used = new ArrayList<>();
            List<SimpleName> usedAfter = new ArrayList<>();
            for (SimpleName sn: variables) {
                if (isBetween(cu, sn, em.getLineRange()[0], em.getLineRange()[1])) {
                    if (sn.isDeclaration()) {
                        declared.add(sn);
                    } else {
                        used.add(sn);
                    }
                } else if (isAfter(cu, sn, em.getLineRange()[1])) {
                    usedAfter.add(sn);
                }
            }

            List<SimpleName> assigned = new ArrayList<>();
            for (SimpleName sn: assignments) {
                if (isBetween(cu, sn, em.getLineRange()[0], em.getLineRange()[1])) {
                    assigned.add(sn);
                }
            }

            List<SimpleName> params = new ArrayList<>();
            for (SimpleName sn: used) {
                if (((!containsSimpleName(declared, sn)) && (!containsSimpleName(params, sn))) && (em.getExtractedMethodDeclaration().toString().contains(sn.getIdentifier()))){
                    params.add(sn);
                }
            }

            AST ast = em.getExtractedMethodDeclaration().getAST();

            List<SingleVariableDeclaration> paramsWithType = params.stream().map( p -> {
                ITypeBinding typeBinding = p.resolveTypeBinding();
                SingleVariableDeclaration vd = ast.newSingleVariableDeclaration();
                vd.setType(constructTypeFromString(typeBinding.getName(), ast));
                vd.setName(ast.newSimpleName(p.getIdentifier()));
                return vd;
            }).collect(Collectors.toList());

            List<SimpleName> returns = new ArrayList<>();
            for (SimpleName id: assigned) {
                if ((containsSimpleName(usedAfter, id) && (!containsSimpleName(returns, id))) && (em.getExtractedMethodDeclaration().toString().contains(id.getIdentifier()))){
                    returns.add(id);
                }
            }

            org.eclipse.jdt.core.dom.Type returnType = ast.newPrimitiveType(PrimitiveType.VOID);
            if (returns.size() == 1) {
                returnType = constructTypeFromString(returns.get(0).resolveTypeBinding().getName(), ast);
            } else if(returns.size() > 1) {
                System.out.println("You can return one of the following: [");
                for (SimpleName sn: returns) {
                    System.out.print(sn.getIdentifier() + " ");
                }
                System.out.print("]");
            }

            SignatureRecommender recommender = new SignatureRecommender(em, this.resource, this.cu);
            String methodBody = em.getExtractedMethodDeclaration().getBody().toString();

            String API_KEY = "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";
            Predictor predictor = new Predictor("text-davinci-003", 0, API_KEY, 7);

            String methodName;
            try {
                methodName = predictor.predictMethodName(methodBody);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                methodName = "extractedMethod";
            }

            em.setExtractedMethodName(methodName);
            em.setExtractedMethodParameters(paramsWithType);
            em.setExtractedMethodReturnType(returnType);
        });

        // Step 6: Finding the Target class for each opportunity
        Map<MethodDeclaration, Map<Type, Double>> extractionImprovements = new HashMap<>(); // <extracted method declaration, <target class, improvement>>

        List<Type> candidateClasses = null;

        // Feature Envy
        if(this.resource instanceof Method){
            candidateClasses = getCandidateClasses();
            for (ExtractedMethod em : extractedMethods) {
                MethodDeclaration extractedMethodDeclaration = em.getExtractedMethodDeclaration();
                if (this.DEBUG) {
                    System.out.println("===== Extracted Method =====");
                    System.out.println(extractedMethodDeclaration);
                }
                for (Type candidateTargetClass : candidateClasses) {
                    RefactoringLCOMEvaluator evaluator = new RefactoringLCOMEvaluator(this.getSourceFileDirectory(), this.belongingType.getNodeAsTypeDeclaration(), candidateTargetClass.getNodeAsTypeDeclaration(), extractedMethodDeclaration, em.getRefactoredTypeCU());

                    try {
                        evaluator.evaluate();
                        Double reduction = evaluator.getReduction(); // the larger, the better
                        if (reduction > 0) { // negative refactoring
                            System.out.println("Skip negative refactoring: " + candidateTargetClass.getNodeAsTypeDeclaration().getName().getIdentifier());
                            continue;
                        }
                        if (this.DEBUG) {
                            evaluator.printEvaluation();
                        }
                        em.setBeforeRefactorMetrics(evaluator.getBeforeRefactorMetrics());
                        em.setAfterRefactorMetrics(evaluator.getAfterRefactorMetrics());
                        // update results
                        extractionImprovements.putIfAbsent(extractedMethodDeclaration, new HashMap<>());
                        extractionImprovements.get(extractedMethodDeclaration).put(candidateTargetClass, reduction);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (extractionImprovements.get(extractedMethodDeclaration) == null || extractionImprovements.get(extractedMethodDeclaration).isEmpty()) {
                    System.out.println("No positive refactoring found for this extracted method. Try extracting method in the local class");
                    RefactoringCCEvaluator evaluator = new RefactoringCCEvaluator(this.getSourceFileDirectory(), this.belongingType.getNodeAsTypeDeclaration(),  em);
                    try{
                        evaluator.evaluate();
                        String targetClass = this.belongingType.getNodeAsTypeDeclaration().getName().getIdentifier();
                        em.setTargetClass(targetClass);
                        if (this.DEBUG) {
                            evaluator.printEvaluation();
                        }
                        em.setBeforeRefactorMetrics(evaluator.getBeforeRefactorMetrics());
                        em.setAfterRefactorMetrics(evaluator.getAfterRefactorMetrics());
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
                else{
                    String targetClass = extractionImprovements.get(extractedMethodDeclaration).entrySet().stream().min(Comparator.comparingDouble(Map.Entry::getValue)).get().getKey().getNodeAsTypeDeclaration().getName().getIdentifier();
                    em.setTargetClass(targetClass);
                    System.out.println("*** Best Target Class: " + targetClass);
                }
                this.resource.addExtractedMethod(em);
            }
        }

        // ComplexClass
        else{
            for (ExtractedMethod em : extractedMethods) {
                MethodDeclaration extractedMethodDeclaration = em.getExtractedMethodDeclaration();
                if (this.DEBUG) {
                    System.out.println("===== Extracted Method =====");
                    System.out.println(extractedMethodDeclaration);
                }
                RefactoringCCEvaluator evaluator = new RefactoringCCEvaluator(this.getSourceFileDirectory(), this.belongingType.getNodeAsTypeDeclaration(), em);
                try{
                    evaluator.evaluate();
                    String targetClass = this.belongingType.getNodeAsTypeDeclaration().getName().getIdentifier();
                    em.setTargetClass(targetClass);
                    if (this.DEBUG) {
                        evaluator.printEvaluation();
                    }
                    em.setBeforeRefactorMetrics(evaluator.getBeforeRefactorMetrics());
                    em.setAfterRefactorMetrics(evaluator.getAfterRefactorMetrics());
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                this.resource.addExtractedMethod(em);
            }
        }
    }


    private org.eclipse.jdt.core.dom.Type constructTypeFromString(String typeString, AST ast){
        int arrayDimension = 0;
        while(typeString.endsWith("[]")){
            arrayDimension++;
            typeString = typeString.substring(0, typeString.length() - 2);
        }
        if (arrayDimension > 0){
            return ast.newArrayType(constructTypeFromObjectString(typeString, ast), arrayDimension);
        }

        return constructTypeFromObjectString(typeString, ast);
    }


    private org.eclipse.jdt.core.dom.Type constructTypeFromObjectString(String typeString, AST ast){
        HashSet<String> set = new HashSet<>(Arrays.asList("byte", "short", "char", "int", "long", "float", "double", "boolean", "void"));

        if(set.contains(typeString)){
            PrimitiveType type = ast.newPrimitiveType(PrimitiveType.toCode(typeString));
            return type;
        }

        return ast.newSimpleType(ast.newName(typeString));
    }

    private boolean containsSimpleName(List<SimpleName> sns, SimpleName simpleName) {
        for (SimpleName sn : sns) {
            if(Objects.equals(sn.getIdentifier(), simpleName.getIdentifier()))
                return true;
        }
        return false;
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
