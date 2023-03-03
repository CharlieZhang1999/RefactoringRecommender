package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.ast.visitors.PublicMethodCollector;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * The number of non-assessors public methods divided by the total number of public methods.
 * 
 * @author Diego Cedrim
 */
public class WOCCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		NOAMCalculator noamCalculator = new NOAMCalculator();
		Double noam = noamCalculator.computeValue(target);
		Double publicMethods = getPublicMethodsCount(target).doubleValue();
		if (publicMethods == 0) {
			return 0d;
		}
		Double woc = (publicMethods - noam)/publicMethods;
		return woc;
	}
	
	private Integer getPublicMethodsCount(ASTNode target) {
		PublicMethodCollector methocCollector = new PublicMethodCollector();
		target.accept(methocCollector);
		return methocCollector.getNodesCollected().size();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.WOC;
	}

}
