package cmu.csdetector.extractor;

import cmu.csdetector.metrics.calculators.type.LCOM2Calculator;
import cmu.csdetector.metrics.calculators.type.LCOM3Calculator;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;

public class ExtractedMethod {
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
    private MethodDeclaration methodDeclaration;

    public ExtractedMethod(String sourceFilePath, int startLine, int endLine) throws IOException {
        this.sourceFilePath = sourceFilePath;
        this.maxLineNum = Files.lines(Paths.get(sourceFilePath)).toArray().length;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public ExtractedMethod(File sourceFile, int startLine, int endLine) throws IOException {
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
        // create an AST
        AST ast = AST.newAST(AST.JLS17);

        // new a method declaration
        methodDeclaration = ast.newMethodDeclaration();

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
                methodDeclaration = null;
                return;
            }
            case 1 -> {
                // if the method body only contains one statement, then directly use the statement as the method body
                methodDeclaration.setBody(this.cloneBlock(ast, (Block) methodBody.statements().get(0)));
            }
            default -> {
                methodDeclaration.setBody(methodBody);
            }
        }

        // update the end line number
        endLine = newEndLine;
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

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public Integer[] getLineRange() {
        return new Integer[]{startLine, endLine};
    }

    /**
     * TODO: use this to infer the method name, return type, and parameters
     *
     * @return the method body
     */
    public Block getMethodBody() {
        return methodDeclaration.getBody();
    }

    public void setMethodName(String methodName) {
        this.methodDeclaration.setName(this.methodDeclaration.getAST().newSimpleName(methodName));
    }

    public void setMethodReturnType(PrimitiveType.Code returnType) {
        this.methodDeclaration.setReturnType2(this.methodDeclaration.getAST().newPrimitiveType(returnType));
    }

    public void setMethodParameters(List<SingleVariableDeclaration> parameters) {
        this.methodDeclaration.parameters().addAll(parameters);
    }
}
