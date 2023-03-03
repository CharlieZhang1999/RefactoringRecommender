package cmu.csdetector.ast.visitors;

import cmu.csdetector.ast.CollectorVisitor;
import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Visit a method body to find all accessed fields from the method in the class
 * During simple name visit, this current visitor will use binding to determine if the simple name
 * correspond to a field or not. If the simple name refers to a variable, the visitor will check if the
 * variable is local or class field
 */

public class ClassFieldAccessCollector extends CollectorVisitor<IVariableBinding> {
    /**
     * Represent the Type that declares the method being visited
     */
    private ITypeBinding declareTypeBinding;
    private Set<IVariableBinding> allVariables;

    public ClassFieldAccessCollector(TypeDeclaration typeDeclaration){
        this.declareTypeBinding = typeDeclaration.resolveBinding();
        this.allVariables = this.getVariablesHierarchy();
    }

    @Override
    public boolean visit(SimpleName node) {
        // logic to check if the node correspond to a variable, if so, check if the node is a field in class

        if (this.declareTypeBinding == null){
            return false; // means don't compute anything
        }
        IBinding binding = node.resolveBinding();
        if(binding == null){
            return false;
        }

        /**
         * Check if the binding refers to a variable. If so, check whether the variable us a field in class
         */
        if (binding.getKind() == IBinding.VARIABLE) {
            IVariableBinding variableBinding = (IVariableBinding) binding;
            // Check if it is collected yet
            if(!wasAlreadyCollected(variableBinding) && this.allVariables.contains(variableBinding)){
                this.addCollectedNode(variableBinding);
            }
        }
        return true;
    }

    private Set<IVariableBinding> getVariablesHierarchy(){
        Set<IVariableBinding> fields = new HashSet<>();
        ITypeBinding type = this.declareTypeBinding;

        while (type != null){
            IVariableBinding[] localFields = type.getDeclaredFields();
            fields.addAll(Arrays.asList(localFields));
            type = type.getSuperclass();
        }
        return fields;
    }


}
