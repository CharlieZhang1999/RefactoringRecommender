package cmu.csdetector.extractor;

import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * StatementVisitor helps build StatementsTable
 */
public class StatementVisitor extends ASTVisitor {

    // maps variable name to the node
    private final Map<String, Set<ASTNode>> variableToNodes = new HashMap<>();
    // maps method name to the node
    private final Map<String, Set<ASTNode>> methodToNodes = new HashMap<>();

    // maps else / else if to the if node
    private final Map<Statement, IfStatement> elseToIf = new HashMap<>();

    private void addVariableToNodes(String variableName, ASTNode node) {
        variableToNodes.computeIfAbsent(variableName, k -> new HashSet<>()).add(node);
    }

    private void addMethodToNodes(String methodName, ASTNode node) {
        methodToNodes.computeIfAbsent(methodName, k -> new HashSet<>()).add(node);
    }

    /**
     * Visit a method invocation node
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(MethodInvocation node) {
        // add the node to the methodToNodes map
        this.addMethodToNodes(node.getName().toString(), node);
        // add arguments to the variableToNodes map
        for (Object obj : node.arguments()) {
            if (obj instanceof SimpleName) {
                this.addVariableToNodes(((SimpleName) obj).getIdentifier(), node);
            }
        }
        // add optional expression and whole expression to the variableToNodes map
        // e.g. `a.b.c()` -> `a`, `a.b`, `a.b.c`
        Expression expression = node.getExpression();
        if (expression instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) expression).getIdentifier(), node);
            this.addMethodToNodes(((SimpleName) expression).getIdentifier() + "." + node.getName().toString(), node);
        }
        return super.visit(node);
    }

    /**
     * Visit an assignment node. E.g. `a = b + c` -> `a`, `b`, `c`
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(Assignment node) {
        // add the left hand side to the variableToNodes map
        if (node.getLeftHandSide() instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) node.getLeftHandSide()).getIdentifier(), node);
        }
        // add the right hand side to the variableToNodes map
        if (node.getRightHandSide() instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) node.getRightHandSide()).getIdentifier(), node);
        } else if (node.getRightHandSide() instanceof InfixExpression) {
            // add the left and right operands to the variableToNodes map
            this.visit((InfixExpression) node.getRightHandSide());
        }
        return super.visit(node);
    }

    /**
     * Visit a variable declaration statement node
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(VariableDeclarationStatement node) {
        for (Object obj : node.fragments()) {
            // add the node to the variableToNodes map
            this.addVariableToNodes(((VariableDeclarationFragment) obj).getName().getIdentifier(), node);
        }
        return super.visit(node);
    }

    /**
     * Visit a qualified name node to extract a series of variables. E.g. `a.b.c` -> `a.b.c`, `a.b`, `a`, `b`, `c`
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(QualifiedName node) {
        // get the identifiers of the qualified name
        var identifiers = node.toString().split("\\.");
        String nextIdentifier = "";
        // add all the identifiers to the variableToNodes map
        for (var identifier : identifiers) {
            this.addVariableToNodes(identifier, node);
            nextIdentifier = nextIdentifier.isEmpty() ? identifier : nextIdentifier + "." + identifier;
            this.addVariableToNodes(nextIdentifier, node);
        }
        return super.visit(node);
    }

    /**
     * Visit an array access node to extract the variable in the array access
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(ArrayAccess node) {

        // add the arrayExpression to the variableToNodes map
        // omit the case when the node is another array access
        if (!(node.getArray() instanceof ArrayAccess)) {
            this.addVariableToNodes(node.getArray().toString(), node);
        }
        // add the IndexExpression to the variableToNodes map
        // omit the case when the index is a number
        if (node.getIndex() instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) node.getIndex()).getIdentifier(), node);
        }
        return super.visit(node);
    }

    /**
     * Visit an infix expression node to extract the left and right operands. E.g. `a + b` -> `a`, `b`
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(InfixExpression node) {
        // add left and right operands to the variableToNodes / methodToNodes map
        for (var operand : List.of(node.getLeftOperand(), node.getRightOperand())) {
            if (operand instanceof SimpleName) {
                this.addVariableToNodes(((SimpleName) operand).getIdentifier(), node);
            } else if (operand instanceof MethodInvocation) {
                // process it as MethodInvocation
                this.visit((MethodInvocation) operand);
            } else if (operand instanceof InfixExpression) {
                // process it as InfixExpression
                this.visit((InfixExpression) operand);
            }
        }
        return super.visit(node);
    }

    /**
     * Visit a method invocation node to extract the method name and the expression. E.g. `a.b()` -> `a.b`, `a`
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(FieldAccess node) {
        // get the field name
        var fieldName = node.getName().toString();
        // add the field name to the variableToNodes map
        this.addVariableToNodes(fieldName, node);
        // get the expression
        Expression expression = node.getExpression();
        if (expression instanceof ArrayAccess) { // if the expression is an array access instead of SimpleName, get the array
            expression = ((ArrayAccess) expression).getArray();
        }
        // add the field name to the variableToNodes map
        this.addVariableToNodes(expression.toString(), node);
        // add expression.fieldName to the variableToNodes map
        this.addVariableToNodes(expression + "." + fieldName, node);
        return super.visit(node);
    }

    /**
     * Visit an instanceof expression node to extract the left variable in the expression. E.g. `a instanceof T` -> `a`
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(InstanceofExpression node) {
        // add the left hand side to the variableToNodes map
        if (node.getLeftOperand() instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) node.getLeftOperand()).getIdentifier(), node);
        } else if (node.getLeftOperand() instanceof ArrayAccess) {
            // process it as ArrayAccess
            this.visit((ArrayAccess) node.getLeftOperand());
        }
        return super.visit(node);
    }

    /**
     * Capture else / else if statements and add them along with the if statement to the elseToIf map
     *
     * @param node the node to visit
     */
    @Override
    public void endVisit(IfStatement node) {
        Statement elseStatement = node.getElseStatement();
        if (elseStatement != null) {
            // add the else statement to the elseToIf map
            this.elseToIf.put(elseStatement, node);
        }
    }

    /**
     * Visit a for statement node to extract all the variables in the loop statement
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(ForStatement node) {
        // extract all the variables in the for statement
        Set<String> variableNames = new HashSet<>();
        // initializer
        for (var initializer : node.initializers()) {
            if (initializer instanceof VariableDeclarationExpression) {
                ((VariableDeclarationExpression) initializer).fragments().forEach(fragment -> {
                    variableNames.add(((VariableDeclarationFragment) fragment).getName().getIdentifier());
                });
            }
        }
        // expression
        if (node.getExpression() instanceof InfixExpression) {
            this.visit((InfixExpression) node.getExpression());
        }
        // add the node to the variableToNodes map
        variableNames.forEach(variableName -> this.addVariableToNodes(variableName, node));
        return super.visit(node);
    }

    /**
     * Visit a while statement node to extract all the variables in the loop statement
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(WhileStatement node) {
        // expression
        if (node.getExpression() instanceof InfixExpression) {
            this.visit((InfixExpression) node.getExpression());
        }
        return super.visit(node);
    }

    /**
     * Visit a return statement node to extract the returned variable
     *
     * @param node the node to visit
     */
    @Override
    public boolean visit(ReturnStatement node) {
        // add the node to the variableToNodes map
        if (node.getExpression() instanceof SimpleName) {
            this.addVariableToNodes(((SimpleName) node.getExpression()).getIdentifier(), node);
        } else if (node.getExpression() instanceof MethodInvocation) {
            // process it as MethodInvocation
            this.visit((MethodInvocation) node.getExpression());
        } else if (node.getExpression() instanceof InfixExpression) {
            // process it as InfixExpression
            this.visit((InfixExpression) node.getExpression());
        }
        return super.visit(node);
    }

    /**
     * get the combined map of variable name and called methods to nodes
     * @return the combined map
     */
    public Map<String, Set<ASTNode>> getMergedNameToNodesMap() {
        Map<String, Set<ASTNode>> mergedMap = new HashMap<>();
        mergedMap.putAll(this.variableToNodes);
        mergedMap.putAll(this.methodToNodes);
        return mergedMap;
    }

    /**
     * Get the combined sorted map of line number to accessed variables and called methods
     */
    public SortedMap<Integer, Set<String>> getLineNumToStatementsTable(CompilationUnit cu) {
        SortedMap<Integer, Set<String>> mergedTable = new TreeMap<>();

        // convert ASTNode to line number
        Function<ASTNode, Integer> getLineNumFromNode = node -> cu.getLineNumber(node.getStartPosition());

        // map nodes in the variableToNodes map to line numbers
        for (var maps : List.of(this.variableToNodes, this.methodToNodes)) {
            for (var entry : maps.entrySet()) {
                for (ASTNode node : entry.getValue()) {
                    int lineNum = getLineNumFromNode.apply(node);
                    mergedTable.computeIfAbsent(lineNum, k -> new HashSet<>()).add(entry.getKey());
                }
            }
        }

        // process elseToIf map: add the line number of the else statement with names of if statement variables to the map
        this.elseToIf.forEach((key, value) -> {
            int elseLineNum = getLineNumFromNode.apply(key);
            int ifLineNum = getLineNumFromNode.apply(value);
            mergedTable.computeIfAbsent(elseLineNum, k -> new HashSet<>()).addAll(mergedTable.get(ifLineNum));
        });

        return mergedTable;
    }
}
