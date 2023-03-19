package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.ast.visitors.ClassFieldAccessCollector;
import cmu.csdetector.ast.visitors.MethodCollector;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class to support the calculation of the LCOM lack of cohesion of
 * methods metrics.
 *
 * This class contains the fields required to calculate the metric and its variations:
 * 		nMethods = number of procedures (methods) in class
 * 		nAttributes= number of variables (attributes) in class
 * 		timesAccessedAttributes = sum of the number of times that each attribute is accessed by all methods
 *
 * The metric definition as well as its implementation are available at:
 * http://www.aivosto.com/project/help/pm-oo-cohesion.html
 *
 * @author leonardo
 */


public abstract class BaseLCOM extends MetricValueCalculator {
	protected double nMethods;
	protected double nAttributes;
	protected double timesAccessedAttributes;

	/**
	 * This method initializes the attributes required to calculate the LCOM The
	 * method does not compute the value of the metric, instead it initializes
	 * the variables required to calculate the LCOM
	 *
	 * If the return is false, then the class doesn't have methods or
	 * attribute. Thus, the LCOM is 0. Otherwise, if the return is true, then is
	 * possible to calculate the LCOM
	 */
	protected boolean instantiateAttributes(ASTNode target) {
		TypeDeclaration type = (TypeDeclaration) target;
		ITypeBinding binding = type.resolveBinding();

		if (binding == null) {
			return false;
		}
		List<MethodDeclaration> methods = getMethods(target);

		this.nAttributes = this.getVariablesInHierarchy(binding).size();


		// get the number of methods within a class
		this.nMethods = methods.size();

		// Sum the number of times that each attribute is accessed by all methods
		for (MethodDeclaration md : methods) {
			ClassFieldAccessCollector fieldVisitor = new ClassFieldAccessCollector(type);
			md.accept(fieldVisitor);

			timesAccessedAttributes += fieldVisitor.getNodesCollected().size();
		}

		if (nMethods == 0 || nAttributes == 0) {
			return false; // it is impossible to calculate lcom
		}

		return true;
	}

	private Set<IVariableBinding> getVariablesInHierarchy(ITypeBinding type) {
		IVariableBinding[] localVariables = type.getDeclaredFields();
		Set<IVariableBinding> variables = new HashSet<>(Arrays.asList(localVariables));

		type = type.getSuperclass();


		//begin to go in the superclasses
		while (type != null) {
			localVariables = type.getDeclaredFields();
			for(IVariableBinding variable: localVariables){

				if(variable.getModifiers() != Modifier.PRIVATE){
					variables.add(variable);
				}
			}

			type = type.getSuperclass();
		}
		return variables;
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.LCOM;
	}


	/*
	 * Get the list with all the methods implemented inside the class
	 */
	private List<MethodDeclaration> getMethods(ASTNode target) {
		MethodCollector methodCollector = new MethodCollector();
		target.accept(methodCollector);

		return methodCollector.getNodesCollected();
	}

}
