package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.ast.visitors.CyclomaticComplexityVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

public class CyclomaticComplexityCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		CyclomaticComplexityVisitor visitor = new CyclomaticComplexityVisitor();
		target.accept(visitor);
		return visitor.getCyclomaticComplexity().doubleValue();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.CC;
	}
	
	@Override
	public boolean shouldComputeAggregate() {
		return true;
	}

}
