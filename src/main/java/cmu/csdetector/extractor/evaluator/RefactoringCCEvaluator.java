package cmu.csdetector.extractor.evaluator;

import cmu.csdetector.extractor.ExtractedMethod;
import cmu.csdetector.metrics.calculators.method.CyclomaticComplexityCalculator;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
import cmu.csdetector.resources.loader.SourceFilesLoader;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;


/**
 * It applies the extract method refactoring, a.k.a. moving the extracted method to the target class.
 * We do this by making a copy of the source project, and then applying the refactoring on the copy.
 * Finally, we compare the LCOM of the target class and the source class before and after the refactoring.
 */
public class RefactoringCCEvaluator implements IRefactoringEvaluator {
    /**
     * The source project, read-only
     * e.g. SmellDetector/examples/ComplexClass or SmellDetector/examples/RefactoringExample
     */
    private final Path sourceProject;

    /**
     * The target project. It will be a copy of the source project, and the refactoring is applied on this project.
     * It should not exist before the refactoring.
     * In the end of the lifetime of this class, the target project is deleted.
     */
    private final Path targetProject;

    /**
     * The source class that the extracted method is moved from
     */
    private final TypeDeclaration sourceClass;

    /**
     * The extracted method we are going to move, a.k.a, opportunity
     */
    private final ExtractedMethod extractedMethod;
    /**
     * Used to replace the file content of the source class with the opportunity extracted code
     */

    private Map<String, Double> beforeRefactorMetrics;
    private Map<String, Double> afterRefactorMetrics;

    public RefactoringCCEvaluator(Path sourceProject, TypeDeclaration sourceClass, ExtractedMethod extractedMethod) {
        this.sourceProject = sourceProject;
        this.targetProject = sourceProject.resolveSibling(sourceProject.getFileName() + "_refactored");
        this.sourceClass = sourceClass;
        this.extractedMethod = extractedMethod;
    }

    private Map<String, Double> calcCC(cmu.csdetector.resources.Type type) {
        Map<String, Double> map = new HashMap<>();
        for (Method method : type.getMethods()) {
            map.put(method.getFullyQualifiedName(), new CyclomaticComplexityCalculator().getValue(method.getNode()));
        }
        return map;
    }


    /**
     * Clean up the target project directory
     */
    private void cleanUp() throws IOException {
        Files.walkFileTree(targetProject, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copy the source project to the target project while applying the extract method refactoring
     */
    private void applyRefactoring() throws IOException {

        Files.walkFileTree(sourceProject, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetPath = targetProject.resolve(sourceProject.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path sourceFilepath, BasicFileAttributes attrs) throws IOException {
                Path targetFilepath = targetProject.resolve(sourceProject.relativize(sourceFilepath));
                String sourceFileName = sourceFilepath.getFileName().toString();

                if (sourceFileName.startsWith(sourceClass.getName().getFullyQualifiedName())) {
                    insertExtractedMethodToTarget();
                    String targetSrcCode = extractedMethod.getRefactoredTypeDeclaration().getRoot().toString();
                    Files.write(targetFilepath, targetSrcCode.getBytes());
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Insert the extracted method to the target class
     */
    private void insertExtractedMethodToTarget() {
        TypeDeclaration td = extractedMethod.getRefactoredTypeDeclaration();
        // get AST of the target class
        AST ast = td.getAST();
        MethodDeclaration md = extractedMethod.getExtractedMethodDeclaration();
        // copy the extracted method onto the target class
        MethodDeclaration newExtractedMethod = ast.newMethodDeclaration();
        newExtractedMethod.setName(ast.newSimpleName(md.getName().getIdentifier()));
        newExtractedMethod.setReturnType2((org.eclipse.jdt.core.dom.Type) ASTNode.copySubtree(ast, md.getReturnType2()));
        newExtractedMethod.setBody((Block) ASTNode.copySubtree(ast, md.getBody()));
        newExtractedMethod.modifiers().addAll(ASTNode.copySubtrees(ast, md.modifiers()));
        newExtractedMethod.parameters().addAll(ASTNode.copySubtrees(ast, md.parameters()));
        // insert the copied extracted method to the target class
        td.bodyDeclarations().add(newExtractedMethod);

    }

    /**
     * Calculate the LCOM of the source & target classes after the refactoring
     */
    private void calculateCCAfterRefactoring() throws IOException {
        JavaFilesFinder finder = new JavaFilesFinder(this.targetProject.toString());
        SourceFilesLoader loader = new SourceFilesLoader(finder);
        for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
            for (cmu.csdetector.resources.Type sourceType : sourceFile.getTypes()) {
                String sourceIdentifier = sourceType.getNodeAsTypeDeclaration().getName().getIdentifier();
                if (sourceIdentifier.startsWith(sourceClass.getName().getIdentifier())) { // refactored class was renamed as <original class name>Refactored
                    this.afterRefactorMetrics = this.calcCC(sourceType);
                }
            }
        }
    }

    /**
     * Calculate the LCOM of the source & target classes before the refactoring
     */
    private void calculateCCBeforeRefactoring() throws IOException {
        JavaFilesFinder finder = new JavaFilesFinder(this.sourceProject.toString());
        SourceFilesLoader loader = new SourceFilesLoader(finder);
        for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
            for (cmu.csdetector.resources.Type sourceType : sourceFile.getTypes()) {
                String sourceIdentifier = sourceType.getNodeAsTypeDeclaration().getName().getIdentifier();
                if (sourceIdentifier.equals(sourceClass.getName().getIdentifier())) {
                    this.beforeRefactorMetrics = this.calcCC(sourceType);
                }
            }
        }
    }

    public void evaluate() throws IOException {
        try {
            calculateCCBeforeRefactoring();
            applyRefactoring();
            calculateCCAfterRefactoring();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp();
        }
    }

    public Map<String, Double> getBeforeRefactorMetrics() {
        return beforeRefactorMetrics;
    }


    public Map<String, Double> getAfterRefactorMetrics() {
        return afterRefactorMetrics;
    }


    /**
     * Calculate the benefit of the refactoring in terms of LCOM reduction (lower is better)
     * @return LCOM reduction
     */
    public Double getReduction() {
        return 0.0;
    }

    public void printEvaluation() {
        System.out.println("* Source class: " + sourceClass.getName().getFullyQualifiedName());
        System.out.println("    Source class CC before refactoring: " + beforeRefactorMetrics);
        System.out.println("    Source class CC after refactoring: " + afterRefactorMetrics);
        System.out.println();
    }
}
