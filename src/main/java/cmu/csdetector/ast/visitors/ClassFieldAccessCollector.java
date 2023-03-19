package cmu.csdetector.ast.visitors;

import cmu.csdetector.ast.CollectorVisitor;
import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Visits a method body in order to find all accesses class fields. During
 * SimpleName visits, this visitor uses binding to determine if the simple name refers
 * to a field or not. If the simple name refers to a field, the visitor checks if the field
 * belongs to the class
 *
 * @author leonardo
 */
public class ClassFieldAccessCollector extends CollectorVisitor<IVariableBinding> {

	/**
	 * Type that declares the method being visited
	 */
	protected final ITypeBinding declaringTypeBinding;

	private Set<IVariableBinding> allVariables;

	public ClassFieldAccessCollector(TypeDeclaration declaringType) {
		this.declaringTypeBinding = declaringType.resolveBinding();
		this.allVariables = this.getVariablesInHierarchy();
	}

	public boolean visit(SimpleName node) {
		/*
		fragment used to demonstrate how to collect info on the ast

		if (this.declaringTypeBinding.getName().toString().equals("DummyDad")){
			System.out.println("enter");
		}
		*/

		if (this.declaringTypeBinding == null) {
			return false;
		}

		IBinding binding = node.resolveBinding();
		if (binding == null) {
			return false;
		}

		/*
		 * Checks if the binding refers to a variable's access. If so,
		 * it checks whether the variable is a field in the class.
		 */
		if (binding.getKind() == IBinding.VARIABLE) {
			IVariableBinding variableBinding = (IVariableBinding) binding;

			if (!wasAlreadyCollected(variableBinding) && this.allVariables.contains(variableBinding)) {
				this.addCollectedNode(variableBinding);
			}
		}
		return true;
	}

	private Set<IVariableBinding> getVariablesInHierarchy() {
		Set<IVariableBinding> variables = new HashSet<>();
		ITypeBinding type = this.declaringTypeBinding;

		while (type != null) {
			IVariableBinding[] localVariables = type.getDeclaredFields();
			variables.addAll(Arrays.asList(localVariables));
			type = type.getSuperclass();
		}
		return variables;
	}

}
