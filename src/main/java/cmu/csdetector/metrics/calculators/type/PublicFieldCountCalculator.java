package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.ast.visitors.PublicFieldCountVisitor;
import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;

public class PublicFieldCountCalculator extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		PublicFieldCountVisitor visitor = new PublicFieldCountVisitor();
		target.accept(visitor);
		return visitor.getPublicFieldsCount().doubleValue();
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.PublicFieldCount;
	}

}
