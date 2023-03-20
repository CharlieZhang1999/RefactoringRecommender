package cmu.csdetector.extractor;

import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

public class MethodDeclarationBuilder {
    private final String sourceFilePath;
    private final int maxLineNum;

    public MethodDeclarationBuilder(String sourceFilePath) throws IOException {
        this.sourceFilePath = sourceFilePath;
        this.maxLineNum = Files.lines(Paths.get(sourceFilePath)).toArray().length;
    }

    public MethodDeclarationBuilder(File sourceFile) throws IOException {
        this.sourceFilePath = sourceFile.getAbsolutePath();
        this.maxLineNum = Files.lines(Paths.get(sourceFilePath)).toArray().length;
    }

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
     * @param methodName the name of the method
     * @param startLine the opportunity's first element
     * @param endLine the opportunity's last element
     * @return the method declaration
     * @throws IOException Files related exceptions
     */
    public MethodDeclaration createMethodFromLineNumbers(String methodName, int startLine, int endLine) throws IOException {
        // create a compilation unit
        AST ast = AST.newAST(AST.JLS17);
        CompilationUnit cu = ast.newCompilationUnit();

        // create the method declaration
        MethodDeclaration methodDeclaration = ast.newMethodDeclaration();
        methodDeclaration.setName(ast.newSimpleName(methodName));
        //methodDeclaration.setReturnType2(ast.newSimpleType(ast.newName(returnType)));

        // create the method body
        Block methodBody = ast.newBlock();
        for (int i = startLine; i <= this.maxLineNum && i <= endLine; i++) {
            String statementStr = this.readLineByNumber(i);
            Block stmt = cloneBlock(ast, (Block) buildStatementFromString(statementStr));

            // if the statementStr represents a line that is a part of a multi-line block like IfStatement,
            // then we need to read the next line to complete the block
            int j = i + 1;
            for (; j < this.maxLineNum && stmt.statements().isEmpty(); j++) {
                statementStr += this.readLineByNumber(j);
                stmt = (Block) ASTNode.copySubtree(ast, buildStatementFromString(statementStr));
            }
            i = j - 1;

            // avoid adding empty blocks
            if (!stmt.statements().isEmpty()) {
                methodBody.statements().add(stmt);
            }
        }

        // if the method body only contains one statement, then directly use the statement as the method body
        int methodBodySize = methodBody.statements().size();
        if (methodBodySize == 0) {
            return null;
        }
        if (methodBodySize == 1) {
            methodDeclaration.setBody(cloneBlock(ast, (Block) methodBody.statements().get(0)));
        } else {
            methodDeclaration.setBody(methodBody);
        }

        // add the method to the compilation unit
        TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
        typeDeclaration.bodyDeclarations().add(methodDeclaration);
        cu.types().add(typeDeclaration);

        return methodDeclaration;
    }

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
}
