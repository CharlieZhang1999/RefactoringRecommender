package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.metrics.MetricName;
import org.eclipse.jdt.core.dom.ASTNode;

public class CouplingDispersionCalculator extends CouplingIntensityCalculator {

	@Override
	protected Double computeValue(ASTNode target) {
		Double cint = super.computeValue(target);
		if (cint == null || cint == 0) {
			return 0d;
		}
		Double differentClasses = new Double(super.methodCalls.keySet().size());
		return differentClasses/cint;
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.CDISP;
	}

}
