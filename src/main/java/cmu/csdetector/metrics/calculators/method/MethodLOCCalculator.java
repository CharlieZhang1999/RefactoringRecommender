package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.ast.visitors.LinesOfCodeVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

public class MethodLOCCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		LinesOfCodeVisitor visitor = new LinesOfCodeVisitor();
		target.accept(visitor);
		return visitor.getLoc().doubleValue();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.MLOC;
	}
	
	@Override
	public boolean shouldComputeAggregate() {
		return true;
	}

}
