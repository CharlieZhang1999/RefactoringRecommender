package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.ast.visitors.MaxCallChainVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

public class MaxCallChainCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		MaxCallChainVisitor visitor = new MaxCallChainVisitor();
		target.accept(visitor);
		return visitor.getMaxCallChain().doubleValue();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.MaxCallChain;
	}

}
