package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.metrics.MetricName;

/**
 * LCOM2 = 1 - sum(mA) / (m * a)
 * m:	number of methods in a class
 * a:	number of attributes (fields) in class
 * mA:	number of methods that access an attribute
 * sum(mA):	sum of mA over attributes of a class
 */
public class LCOM2Calculator extends LCOMGeneralCalculator {

    @Override
    public MetricName getMetricName() {
        return MetricName.LCOM2;
    }

    @Override
    protected Double calcLCOM(Integer m, Integer a) {
        if (m != 0 && a != 0) {
            return 1 - (this.sumMA.doubleValue() / (m.doubleValue() * a.doubleValue()));
        }
        return .0;
    }
}
