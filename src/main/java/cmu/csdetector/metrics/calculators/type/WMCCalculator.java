package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.ast.visitors.CyclomaticComplexityVisitor;
import cmu.csdetector.ast.visitors.MethodCollector;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.util.List;

/**
 * Computes the Weighted Method Count of a given class. This metric is defined
 * as the sum of Cyclomatic Complexity of all methods declared in the given class
 * @author Diego Cedrim
 */
public class WMCCalculator extends MetricValueCalculator {

	@Override
	protected Double computeValue(ASTNode target) {
		List<MethodDeclaration> methods = getMethods(target);
		
		Double wmc = 0d;
		for (MethodDeclaration methodDeclaration : methods) {
			CyclomaticComplexityVisitor ccVisitor = new CyclomaticComplexityVisitor();
			methodDeclaration.accept(ccVisitor);
			Double cc = ccVisitor.getCyclomaticComplexity().doubleValue();
			wmc += cc;
		}
		return wmc;
	}

	private List<MethodDeclaration> getMethods(ASTNode target) {
		MethodCollector methocCollector = new MethodCollector();
		target.accept(methocCollector);
		return methocCollector.getNodesCollected();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.WMC;
	}
	
	@Override
	public boolean shouldComputeAggregate() {
		return true;
	}

}
