package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.metrics.MetricName;

/**
 * LCOM3 = (m - sum(mA) / a) / (m - 1)
 * m:	number of methods in a class
 * a:	number of attributes (fields) in class
 * mA:	number of methods that access an attribute
 * sum(mA):	sum of mA over attributes of a class
 */
public class LCOM3Calculator extends LCOMGeneralCalculator {

    @Override
    public MetricName getMetricName() {
        return MetricName.LCOM3;
    }

    @Override
    protected Double calcLCOM(Integer m, Integer a) {
        if (m != 1 && a != 0) {
            return (m.doubleValue() - this.sumMA.doubleValue() / a.doubleValue()) / (m.doubleValue() - 1);
        }
        return .0;
    }
}
