package cmu.csdetector.metrics.calculators.type;


import cmu.csdetector.ast.visitors.LinesOfCodeVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

public class ClassLOCCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		LinesOfCodeVisitor visitor = new LinesOfCodeVisitor();
		target.accept(visitor);

		return visitor.getLoc().doubleValue();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.CLOC;
	}
	
	@Override
	public boolean shouldComputeAggregate() {
		return true;
	}

}
