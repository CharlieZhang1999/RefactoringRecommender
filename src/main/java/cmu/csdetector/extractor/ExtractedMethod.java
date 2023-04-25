package cmu.csdetector.extractor;

import cmu.csdetector.metrics.calculators.type.BaseLCOM;
import cmu.csdetector.metrics.calculators.type.LCOM3Calculator;
import cmu.csdetector.resources.Type;
import cmu.csdetector.resources.loader.JavaFilesFinder;
import cmu.csdetector.resources.loader.SourceFile;
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
     * Extracted method declaration (opportunity)
     */
    private MethodDeclaration extractedMethodDeclaration;
    /**
     * The class declaration after refactoring (opportunity extracted)
     */
    private TypeDeclaration refactoredTypeDeclaration;

    /**
     * The compilation unit of the refactoredTypeDeclaration
     */
    private CompilationUnit refactoredTypeCU;
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
        AST ast = AST.newAST(AST.JLS11);

        // new a method declaration
        extractedMethodDeclaration = ast.newMethodDeclaration();
        extractedMethodDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));

        // create the method body
        Block methodBody = ast.newBlock();

        // fill the method body
        int newEndLine = startLine;
        for (; newEndLine <= this.maxLineNum && newEndLine <= endLine; newEndLine++) {
            String statementStr = this.readLineByNumber(newEndLine);
            ArrayList<Statement> stmtList = new ArrayList<>();
            Statement newStatement = this.buildStatementFromString(statementStr);
            if (newStatement instanceof Block) {
                ArrayList<Statement> blockStatements = new ArrayList<>();
                List<Statement> l = cloneBlock(ast, ((Block) newStatement)).statements();
                for (Statement s: l) {
                    stmtList.add(cloneStatement(ast, s));
                }
            } else {
                stmtList.add(cloneStatement(ast, newStatement));
            }
//            Block stmt = cloneBlock(ast, (Block) this.buildStatementFromString(statementStr));

            // if the statementStr represents a partial multi-line block (e.g. IfStatement)
            // then we need to read the next line to complete the block
            int i = newEndLine + 1;
            for (; i < this.endLine && stmtList.isEmpty(); i++) {
                statementStr = statementStr + "\n" + this.readLineByNumber(i);
                newStatement = this.buildStatementFromString(statementStr);
                if (newStatement instanceof Block) {
                    ArrayList<Statement> blockStatements = new ArrayList<>();
                    List<Statement> l = cloneBlock(ast, ((Block) newStatement)).statements();
                    for (Statement s: l) {
                        stmtList.add(cloneStatement(ast, s));
                    }

//                    stmtList.addAll(cloneBlock(ast, ((Block) newStatement)).statements());
                } else {
                    stmtList.add(cloneStatement(ast, newStatement));
                }
            }

            // avoid adding empty blocks
            if (!stmtList.isEmpty()) {
                for (Statement s: stmtList) {

                    methodBody.statements().add(s);
                }
//                methodBody.statements().addAll(stmtList);
                newEndLine = i - 1;
            }
        }

        int methodBodySize = methodBody.statements().size();
        switch (methodBodySize) {
            case 0:
                // if the method body is empty, then explicitly set to null, which should be dropped later
                extractedMethodDeclaration = null;
                return;

            case 1:
                // if the method body only contains one statement, then directly use the statement as the method body
//                extractedMethodDeclaration.setBody(this.cloneBlock(ast, (Block) methodBody.statements().get(0)));
                extractedMethodDeclaration.setBody(methodBody);
                break;

            default:
                extractedMethodDeclaration.setBody(methodBody);
                break;

        }

        // update the end line number
        endLine = newEndLine;

        // build a new file from extracted method
        CompilationUnit cu = ast.newCompilationUnit();
        TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
        typeDeclaration.bodyDeclarations().add(extractedMethodDeclaration);
        typeDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
        typeDeclaration.setName(ast.newSimpleName("Opportunity"));
        cu.types().add(typeDeclaration);
        List<String> packageDeclarations = Files.lines(Paths.get(sourceFilePath)).filter(line -> line.startsWith("package")).collect(Collectors.toList());

        // save the new class to a new java file
        this.saveJavaFile("Opportunity.java", String.join("\n", packageDeclarations) + "\n" + cu);
    }

    private void createRefactoredType() throws IOException {
        // build two stacks to track the block level (the number of "{" and "}")
        int frontBlockStack = 0;
        int rearBlockStack = 0;
        // remove [startLine, endLine] from the original method
        List<String> allLines = Files.lines(Paths.get(sourceFilePath)).collect(Collectors.toList());
        List<String> newLines = new ArrayList<>();
        int insertIdx = -1;
        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            // drop the empty lines and comments
            if (line.isBlank()) {
                continue;
            }
            if (line.trim().startsWith("//")) {
                continue;
            }
            if (i+1 < startLine) {
                if (line.contains("{")) {
                    frontBlockStack++;
                } else if (line.contains("}")) {
                    frontBlockStack--;
                }
            } else if (i+1 > endLine) {
                if (line.contains("}")) {
                    rearBlockStack++;
                } else if (line.contains("{")) {
                    rearBlockStack--;
                }
            } else {
                // update insertIdx
                if (insertIdx == -1) {
                    insertIdx = newLines.size();
                }
                continue;
            }
            newLines.add(line);
        }

        // record the size of both stacks to remedy the cuLines
        int stackSize = frontBlockStack + rearBlockStack;
        // insert the corresponding "{" and "}" into the insertIdx
        for (int i = 0; i < rearBlockStack; i++) {
            // insert "{" to the insertIdx
            newLines.add(insertIdx, "{");
        }
        for (int i = 0; i < frontBlockStack; i++) {
            // insert "}" to the insertIdx
            newLines.add(insertIdx, "}");
        }

        // create a new method declaration
        this.refactoredTypeCU = this.getCuFromLines(newLines);

        // set to null if cu lines < newLines, which means some code fragments are dropped by cu
        int cuLines = this.refactoredTypeCU.toString().split("\n").length;
        if (cuLines + stackSize < newLines.size() - 3) { // allow deviation of 3 lines
            this.refactoredTypeDeclaration = null;
            return;
        }

        // we got a valid extraction
        this.refactoredTypeDeclaration = (TypeDeclaration) this.refactoredTypeCU.types().get(0);
        String newClassName = refactoredTypeDeclaration.getName().getIdentifier() + "Refactored";
        this.refactoredTypeDeclaration.setName(this.refactoredTypeCU.getAST().newSimpleName(newClassName));

        // save refactoredTypeDeclaration to a new file
        this.saveJavaFile(newClassName + ".java", this.refactoredTypeCU.toString());
    }

    private CompilationUnit getCuFromLines(List<String> lines) {
        ASTParser parser = ASTParser.newParser(AST.JLS11);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(String.join("\n", lines).toCharArray());
        CompilationUnit cu =  (CompilationUnit) parser.createAST(null);
        cu.recordModifications();
        return cu;
    }

    /**
     * Convert a code snippet to a statement
     *
     * @param statement The code snippet string
     * @return Statement
     */
    private Statement buildStatementFromString(String statement) {
        ASTParser parser = ASTParser.newParser(AST.JLS11);
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

    private Statement cloneStatement(AST ast, Statement stmt) {
        return (Statement) ASTNode.copySubtree(ast, stmt);
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

    public void setExtractedMethodReturnType(org.eclipse.jdt.core.dom.Type type) {
        this.extractedMethodDeclaration.setReturnType2(type);
    }

    public void setExtractedMethodParameters(List<SingleVariableDeclaration> parameters) {
        this.extractedMethodDeclaration.parameters().addAll(parameters);
    }

    public double getOriginalLCOM() {
        return originalLCOM;
    }

    public void setOriginalLCOM(double originalLCOM) {
        this.originalLCOM = originalLCOM;
    }

    public double getRefactoredLCOM() {
        return refactoredLCOM;
    }

    public double getOpportunityLCOM() {
        return opportunityLCOM;
    }
    public CompilationUnit getRefactoredTypeCU() {
        return refactoredTypeCU;
    }
    /**
     * temporarily save the refactored type declaration to a new file for LCOM calculation
     * @param newFileName
     * @param content
     * @throws IOException
     */
    private void saveJavaFile(String newFileName, String content) throws IOException {
        // replace the original file name with the new file name
        String newFilePath = sourceFile.getParentFile().getAbsolutePath() + File.separator + newFileName;
        FileWriter writer = new FileWriter(newFileName.endsWith(".java") ? newFilePath : newFilePath + ".java");
        writer.write(content);
        writer.close();
    }

    /**
     * clean the temporary file
     * @param fileName
     */
    private void deleteJavaFile(String fileName) {
        String filePath = sourceFile.getParentFile().getAbsolutePath() + File.separator + fileName;
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * calculate LCOM for the original type declaration, the refactored type declaration, and the opportunity type declaration
     * @param belongingTypeDeclaration the class that contains the extracted method
     * @throws IOException
     */
    public void calculateLCOM(TypeDeclaration belongingTypeDeclaration) throws IOException {
        String originalClassName = belongingTypeDeclaration.getName().getIdentifier();
        String refactoredClassName = originalClassName + "Refactored";
        String opportunityClassName = "Opportunity";
        // get file path from source file
        String filePath = sourceFile.getAbsolutePath();
        // get its directory path
        String dirPath = filePath.substring(0, filePath.lastIndexOf(File.separator));
        // find the target type declaration by identifier
        JavaFilesFinder finder = new JavaFilesFinder(dirPath);
        SourceFilesLoader loader = new SourceFilesLoader(finder);
        for (SourceFile sourceFile : loader.getLoadedSourceFiles()) {
            for (Type sourceType : sourceFile.getTypes()) {
                String sourceIdentifier = sourceType.getNodeAsTypeDeclaration().getName().getIdentifier();
                if (!sourceIdentifier.equals(originalClassName) && !sourceIdentifier.equals(refactoredClassName) && !sourceIdentifier.equals(opportunityClassName)) {
                    continue;
                }
                Double lcom = new LCOM3Calculator().getValue(sourceType.getNodeAsTypeDeclaration());
                if (sourceIdentifier.equals(originalClassName)) {
                    this.originalLCOM = lcom;
                } else if (sourceIdentifier.equals(refactoredClassName)) {
                    this.refactoredLCOM = lcom;
                } else if (sourceIdentifier.equals(opportunityClassName)) {
                    this.opportunityLCOM = lcom;
                }
            }
        }

        // delete the refactored type declaration file
        this.deleteJavaFile(refactoredClassName + ".java");
        this.deleteJavaFile(opportunityClassName + ".java");
    }

}
