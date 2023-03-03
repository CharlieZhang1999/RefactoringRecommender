package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.graph.CallGraph;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.Set;

/**
 * Computed the changing class metric value for a method. Same as FanIn
 *  
 * @author Diego Cedrim
 */
public class ChangingMethodsCalculator extends MetricValueCalculator {

	@Override
	protected Double computeValue(ASTNode target) {
		MethodDeclaration method = (MethodDeclaration)target;
		IMethodBinding binding = method.resolveBinding();
		if (binding == null) {
			//TODO log!
			return null;
		}
		
		CallGraph callGraph = CallGraph.getInstance();
		Set<IMethodBinding> callers = callGraph.getCallers(binding);
		return new Double(callers.size());
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.ChangingMethods;
	}

}
