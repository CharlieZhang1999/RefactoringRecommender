package cmu.csdetector.extractor;

import cmu.csdetector.metrics.calculators.type.BaseLCOM;
import cmu.csdetector.metrics.calculators.type.LCOM3Calculator;
import cmu.csdetector.resources.Type;
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

/**
 * It applies the extract method refactoring, a.k.a. moving the extracted method to the target class.
 * We do this by making a copy of the source project, and then applying the refactoring on the copy.
 * Finally, we compare the LCOM of the target class and the source class before and after the refactoring.
 */
public class RefactoringEvaluator {
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
     * The target class that the extracted method is moved to
     */
    private final TypeDeclaration targetClass;
    /**
     * The extracted method we are going to move, a.k.a, opportunity
     */
    private final MethodDeclaration extractedMethod;
    /**
     * Used to replace the file content of the source class with the opportunity extracted code
     */
    private final CompilationUnit refactoredCU;
    private Double originalSourceClassLCOM;
    private Double originalTargetClassLCOM;
    private Double refactoredSourceClassLCOM;
    private Double refactoredTargetClassLCOM;

    public RefactoringEvaluator(Path sourceProject, TypeDeclaration sourceClass, TypeDeclaration targetClass, MethodDeclaration extractedMethod, CompilationUnit refactoredCU) {
        this.sourceProject = sourceProject;
        this.targetProject = sourceProject.resolveSibling(sourceProject.getFileName() + "_refactored");
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.extractedMethod = extractedMethod;
        this.refactoredCU = refactoredCU;
    }

    private Double calcLCOM(ASTNode target) {
        return new LCOM3Calculator().getValue(target);
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
                    // if it is the source class, remove the extracted method from it before saving
                    String refactoredSrcCode = refactoredCU.toString();
                    Files.write(targetFilepath, refactoredSrcCode.getBytes());
                } else if (sourceFileName.startsWith(targetClass.getName().getFullyQualifiedName())) {
                    // if it is the target class, insert the extracted method to it before saving
                    insertExtractedMethodToTarget();
                    String targetSrcCode = targetClass.getRoot().toString();
                    Files.write(targetFilepath, targetSrcCode.getBytes());
                } else {
                    Files.copy(sourceFilepath, targetFilepath);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Insert the extracted method to the target class
     */
    private void insertExtractedMethodToTarget() {
        // get AST of the target class
        AST ast = targetClass.getAST();
        // copy the extracted method onto the target class
        MethodDeclaration newExtractedMethod = ast.newMethodDeclaration();
        newExtractedMethod.setName(ast.newSimpleName(extractedMethod.getName().getIdentifier()));
        newExtractedMethod.setReturnType2((org.eclipse.jdt.core.dom.Type) ASTNode.copySubtree(ast, extractedMethod.getReturnType2()));
        newExtractedMethod.setBody((Block) ASTNode.copySubtree(ast, extractedMethod.getBody()));
        newExtractedMethod.modifiers().addAll(ASTNode.copySubtrees(ast, extractedMethod.modifiers()));
        newExtractedMethod.parameters().addAll(ASTNode.copySubtrees(ast, extractedMethod.parameters()));
        // insert the copied extracted method to the target class
        targetClass.bodyDeclarations().add(newExtractedMethod);

    }

    /**
     * Calculate the LCOM of the source & target classes after the refactoring
     */
    private void calculateLCOMAfterRefactoring() throws IOException {
        JavaFilesFinder finder = new JavaFilesFinder(this.targetProject.toString());
        SourceFilesLoader loader = new SourceFilesLoader(finder);
        for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
            for (Type sourceType : sourceFile.getTypes()) {
                String sourceIdentifier = sourceType.getNodeAsTypeDeclaration().getName().getIdentifier();
                if (sourceIdentifier.startsWith(sourceClass.getName().getIdentifier())) { // refactored class was renamed as <original class name>Refactored
                    this.refactoredSourceClassLCOM = this.calcLCOM(sourceType.getNodeAsTypeDeclaration());
                } else if (sourceIdentifier.equals(targetClass.getName().getIdentifier())) {
                    this.refactoredTargetClassLCOM = this.calcLCOM(sourceType.getNodeAsTypeDeclaration());
                }
            }
        }
    }

    /**
     * Calculate the LCOM of the source & target classes before the refactoring
     */
    private void calculateLCOMBeforeRefactoring() throws IOException {
        JavaFilesFinder finder = new JavaFilesFinder(this.sourceProject.toString());
        SourceFilesLoader loader = new SourceFilesLoader(finder);
        for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
            for (Type sourceType : sourceFile.getTypes()) {
                String sourceIdentifier = sourceType.getNodeAsTypeDeclaration().getName().getIdentifier();
                if (sourceIdentifier.equals(sourceClass.getName().getIdentifier())) {
                    this.originalSourceClassLCOM = this.calcLCOM(sourceType.getNodeAsTypeDeclaration());
                } else if (sourceIdentifier.equals(targetClass.getName().getIdentifier())) {
                    this.originalTargetClassLCOM = this.calcLCOM(sourceType.getNodeAsTypeDeclaration());
                }
            }
        }
    }

    public void evaluate() throws IOException {
        try {
            calculateLCOMBeforeRefactoring();
            applyRefactoring();
            calculateLCOMAfterRefactoring();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanUp();
        }
    }

    public Double getOriginalSourceClassLCOM() {
        return originalSourceClassLCOM;
    }

    public Double getOriginalTargetClassLCOM() {
        return originalTargetClassLCOM;
    }

    public Double getRefactoredSourceClassLCOM() {
        return refactoredSourceClassLCOM;
    }

    public Double getRefactoredTargetClassLCOM() {
        return refactoredTargetClassLCOM;
    }

    public Double getLCOMImprovement() {
        return (originalSourceClassLCOM + originalTargetClassLCOM) - (refactoredSourceClassLCOM + refactoredTargetClassLCOM);
    }

    public void printEvaluation() {
        System.out.println("* Target class: " + targetClass.getName().getFullyQualifiedName());
        System.out.println("    Source class LCOM before refactoring: " + originalSourceClassLCOM);
        System.out.println("    Target class LCOM before refactoring: " + originalTargetClassLCOM);
        System.out.println("    Source class LCOM after refactoring: " + refactoredSourceClassLCOM);
        System.out.println("    Target class LCOM after refactoring: " + refactoredTargetClassLCOM);
        System.out.println("        LCOM improvement: " + getLCOMImprovement());
        System.out.println();
    }
}
