package cmu.csdetector.extractor;

import cmu.csdetector.ast.ASTBuilder;
import cmu.csdetector.console.ConsoleProgressMonitor;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
import cmu.csdetector.resources.loader.SourceFileASTRequestor;
import cmu.csdetector.resources.loader.SourceFilesLoader;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class ExtractedMethod {
    private final File sourceFile;
    /**
     * The Path of the source code file
     */
    private final String sourceFilePath;
    /**
     * The maximum line number of the source file
     */
    private final int maxLineNum;
    private final int startLine;
    /**
     * The end line number of the opportunity.
     * It is possible to get updated when the opportunity represents a partial block.
     */
    private int endLine;

    /**
     * Extracted method declaration
     */
    private MethodDeclaration extractedMethodDeclaration;
    private TypeDeclaration refactoredTypeDeclaration;
    private double originalLCOM = 0;
    private double opportunityLCOM = 0;
    private double refactoredLCOM = 0;

    public ExtractedMethod(File sourceFile, int startLine, int endLine) throws IOException {
        this.sourceFile = sourceFile;
        this.sourceFilePath = sourceFile.getAbsolutePath();
        this.maxLineNum = Files.lines(Paths.get(sourceFilePath)).toArray().length;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    /**
     * Read the line of code from the source file by the given line number
     *
     * @param lineNumber The line number
     * @return The line of code
     * @throws IOException Files related exceptions
     */
    private String readLineByNumber(int lineNumber) throws IOException {
        try {
            return Files.lines(Paths.get(sourceFilePath)).skip(lineNumber - 1).findFirst().get();
        } catch (NoSuchElementException e) {
            throw new IOException("Error reading line " + lineNumber + " from file " + sourceFilePath);
        }
    }

    /**
     * Create a method declaration from the given line numbers which come from an opportunity.
     * Sometimes the opportunity leads to a partial block, so we need to read the next line to complete the block.
     *
     * @return the method declaration
     * @throws IOException Files related exceptions
     */
    public void create() throws IOException {
        createExtractedMethod();
        createRefactoredType();
    }

    private void createExtractedMethod() throws IOException {
        // create an AST
        AST ast = AST.newAST(AST.JLS17);

        // new a method declaration
        extractedMethodDeclaration = ast.newMethodDeclaration();
        extractedMethodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        // create the method body
        Block methodBody = ast.newBlock();

        // fill the method body
        int newEndLine = startLine;
        for (; newEndLine <= this.maxLineNum && newEndLine <= endLine; newEndLine++) {
            String statementStr = this.readLineByNumber(newEndLine);
            Block stmt = cloneBlock(ast, (Block) this.buildStatementFromString(statementStr));

            // if the statementStr represents a partial multi-line block (e.g. IfStatement)
            // then we need to read the next line to complete the block
            int i = newEndLine + 1;
            for (; i < this.maxLineNum && stmt.statements().isEmpty(); i++) {
                statementStr += this.readLineByNumber(i);
                stmt = this.cloneBlock(ast, (Block) this.buildStatementFromString(statementStr));
            }
            newEndLine = i - 1;

            // avoid adding empty blocks
            if (!stmt.statements().isEmpty()) {
                methodBody.statements().add(stmt);
            }
        }

        int methodBodySize = methodBody.statements().size();
        switch (methodBodySize) {
            case 0 -> {
                // if the method body is empty, then explicitly set to null, which should be dropped later
                extractedMethodDeclaration = null;
                return;
            }
            case 1 -> {
                // if the method body only contains one statement, then directly use the statement as the method body
                extractedMethodDeclaration.setBody(this.cloneBlock(ast, (Block) methodBody.statements().get(0)));
            }
            default -> {
                extractedMethodDeclaration.setBody(methodBody);
            }
        }

        // update the end line number
        endLine = newEndLine;

        // build a new file from extracted method
        CompilationUnit cu = ast.newCompilationUnit();
        TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
        typeDeclaration.bodyDeclarations().add(extractedMethodDeclaration);
        typeDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        cu.types().add(typeDeclaration);
        List<String> packageDeclarations = Files.lines(Paths.get(sourceFilePath)).filter(line -> line.startsWith("package")).collect(Collectors.toList());

        // save the new class to a new java file
        String newFilePath = sourceFilePath.replace(".java", "_opportunity.java");
        this.saveJavaFile(newFilePath, String.join("\n", packageDeclarations) + "\n" + cu);
    }

    private void createRefactoredType() throws IOException {
        // remove [startLine, endLine] from the original method
        List<String> allLines = Files.lines(Paths.get(sourceFilePath)).collect(Collectors.toList());
        List<String> newLines = new ArrayList<>();
        for (int i = 0; i < allLines.size(); i++) {
            if (i < startLine - 1 || i > endLine - 1) {
                newLines.add(allLines.get(i));
            }
        }

        // create a new method declaration
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(String.join("\n", newLines).toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.recordModifications();

        refactoredTypeDeclaration = (TypeDeclaration) cu.types().get(0);

        // save refactoredTypeDeclaration to a new file
        String newFilePath = sourceFilePath.replace(".java", "_refactored.java");
        this.saveJavaFile(newFilePath, cu.toString());
    }

    /**
     * Convert a code snippet to a statement
     *
     * @param statement The code snippet string
     * @return Statement
     */
    private Statement buildStatementFromString(String statement) {
        ASTParser parser = ASTParser.newParser(AST.JLS17);
        parser.setKind(ASTParser.K_STATEMENTS);
        parser.setSource(statement.toCharArray());
        return (Statement) parser.createAST(null);
    }

    /**
     * Clone a block
     *
     * @return the cloned block
     * @see <a href="https://www.eclipse.org/forums/index.php/t/1087653/">Insert Block from a different AST inside MethodDeclaration</a>
     */
    private Block cloneBlock(AST ast, Block block) {
        return (Block) ASTNode.copySubtree(ast, block);
    }

    public MethodDeclaration getExtractedMethodDeclaration() {
        return extractedMethodDeclaration;
    }

    public TypeDeclaration getRefactoredTypeDeclaration() {
        return refactoredTypeDeclaration;
    }

    public Integer[] getLineRange() {
        return new Integer[]{startLine, endLine};
    }

    public List<Integer> getLineNumbers() {
        List<Integer> lines = new ArrayList<>();
        var range = getLineRange();
        for (int i = range[0]; i <= range[1]; i++) {
            lines.add(i);
        }
        return lines;
    }

    /**
     * TODO: use this to infer the method name, return type, and parameters
     *
     * @return the method body
     */
    public Block getExtractedMethodBody() {
        return extractedMethodDeclaration.getBody();
    }

    public void setExtractedMethodName(String methodName) {
        this.extractedMethodDeclaration.setName(this.extractedMethodDeclaration.getAST().newSimpleName(methodName));
    }

    public void setExtractedMethodReturnType(PrimitiveType.Code returnType) {
        this.extractedMethodDeclaration.setReturnType2(this.extractedMethodDeclaration.getAST().newPrimitiveType(returnType));
    }

    public void setExtractedMethodParameters(List<SingleVariableDeclaration> parameters) {
        this.extractedMethodDeclaration.parameters().addAll(parameters);
    }

    public void setThreeLCOM(double originalLCOM, double refactoredLCOM, double opportunityLCOM) {
        this.originalLCOM = originalLCOM;
        this.refactoredLCOM = refactoredLCOM;
        this.opportunityLCOM = opportunityLCOM;
    }

    public double getOriginalLCOM() {
        return originalLCOM;
    }

    public double getRefactoredLCOM() {
        return refactoredLCOM;
    }

    public double getOpportunityLCOM() {
        return opportunityLCOM;
    }

    private void saveJavaFile(String filePath, String content) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        writer.write(content);
        writer.close();
    }
}
