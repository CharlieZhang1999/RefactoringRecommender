package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.lang.reflect.Modifier;

public class IsClassAbstract extends MetricValueCalculator {
	
	@Override
	protected Double computeValue(ASTNode target) {
		TypeDeclaration type = (TypeDeclaration)target;
		if (Modifier.isAbstract(type.getModifiers())){
			return 1d;
		} else {
			return 0d;
		}
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.IsAbstract;
	}

}
