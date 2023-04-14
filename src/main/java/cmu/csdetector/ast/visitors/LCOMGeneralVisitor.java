package cmu.csdetector.ast.visitors;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * LCOMGeneralVisitor essentially counts `m` and `a`, meanwhile collects method declarations.
 */
public class LCOMGeneralVisitor extends ClassFieldAccessCollector {
    private Integer m;

    private final ArrayList<MethodDeclaration> methodDeclarations;

    public LCOMGeneralVisitor(TypeDeclaration declaringType) {
        super(declaringType);
        this.methodDeclarations = new ArrayList<>();
        this.m = 0;
    }

    /**
     * visit method declaration to count the number of methods in a class `m`
     */
    @Override
    public boolean visit(MethodDeclaration node) {
        this.methodDeclarations.add(node);
        this.m++;
        return true;
    }

    public ArrayList<MethodDeclaration> getMethodDeclarations() {
        return methodDeclarations;
    }

    public Integer getM() {
        return m;
    }

    public Integer getA() {
        Set<IVariableBinding> fields = new HashSet<>();
        ITypeBinding type = this.declaringTypeBinding;
        boolean fromSuperClass = false;
        while (type != null) {
            IVariableBinding[] localFields = type.getDeclaredFields();
            if (fromSuperClass) {
                // when checking its superclass, only add the fields that are not private
                Stream.of(localFields).filter(field -> field.getModifiers() != Modifier.PRIVATE).forEach(fields::add);
            } else {
                fields.addAll(Arrays.asList(localFields));
            }
            // try to go to its superclass and fetch all its local fields
            type = type.getSuperclass();
            fromSuperClass = true;
        }

        return fields.size();
    }
}
