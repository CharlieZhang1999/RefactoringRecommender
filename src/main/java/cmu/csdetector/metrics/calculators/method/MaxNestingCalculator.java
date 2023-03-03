package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.ast.visitors.MaximumNestingLevelVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Computes the max nesting level of a method
 * @author Diego Cedrim
 */
public class MaxNestingCalculator extends MetricValueCalculator {

	@Override
	protected Double computeValue(ASTNode target) {
		MaximumNestingLevelVisitor visitor = new MaximumNestingLevelVisitor();
		target.accept(visitor);
		return visitor.getMNL();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.MaxNesting;
	}

}
