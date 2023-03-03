package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.ast.visitors.MethodCollector;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.List;

/**
 * Computes how many assessors methods (getters and setters) exist in a given class
 * @author Diego Cedrim
 */
public class NOAMCalculator extends MetricValueCalculator {
	
	private static final String GET = "get";
	
	private static final String SET = "set"; 

	@Override
	protected Double computeValue(ASTNode target) {
		List<MethodDeclaration> methods = getMethods(target);
		
		Double noam = 0d;
		for (MethodDeclaration methodDeclaration : methods) {
			String name = methodDeclaration.getName().toString();
			if ( (name.startsWith(GET) || name.startsWith(SET)) 
					&& this.isPublicAndNonStatic(methodDeclaration)) {
				noam++;
			}
		}
		return noam;
	}
	
	private boolean isPublicAndNonStatic(MethodDeclaration method) {
		int modifiers = method.getModifiers();
		return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
	}
	
	private List<MethodDeclaration> getMethods(ASTNode target) {
		MethodCollector methodCollector = new MethodCollector();
		target.accept(methodCollector);
		return methodCollector.getNodesCollected();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.NOAM;
	}

}
