package cmu.csdetector.metrics;

import cmu.csdetector.ast.visitors.ClassFieldAccessCollector;
import cmu.csdetector.metrics.calculators.type.*;

public class TypeMetricValueCollector extends MetricValueCollector {

	public TypeMetricValueCollector() {
		addCalculator(new ClassLOCCalculator());
		addCalculator(new IsClassAbstract());
		addCalculator(new NOAMCalculator());
		addCalculator(new OverrideRatioCalculator());
		addCalculator(new PublicFieldCountCalculator());
		addCalculator(new TCCMetricValueCalculator());
		addCalculator(new WMCCalculator());
		addCalculator(new WOCCalculator());
	}
	
	
}
