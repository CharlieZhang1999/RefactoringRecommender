package cmu.csdetector.metrics;

import cmu.csdetector.metrics.calculators.method.*;
import cmu.csdetector.metrics.calculators.method.*;

public class MethodMetricValueCollector extends MetricValueCollector{
    public MethodMetricValueCollector() {
        addCalculator(new ChangingClassesCalculator());
        addCalculator(new ChangingMethodsCalculator());
        addCalculator(new CouplingDispersionCalculator());
        addCalculator(new CouplingIntensityCalculator());
        addCalculator(new CyclomaticComplexityCalculator());
        addCalculator(new MaxCallChainCalculator());
        addCalculator(new MaxNestingCalculator());
        addCalculator(new MethodLOCCalculator());
        addCalculator(new MethodParameterCountCalculator());
        addCalculator(new NOAVCalculator());
    }
}
