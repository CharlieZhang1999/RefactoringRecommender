package cmu.csdetector.smells.detectors;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.AggregateMetricValues;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellDetector;
import cmu.csdetector.smells.SmellName;
import cmu.csdetector.smells.Thresholds;

import java.util.ArrayList;
import java.util.List;

public class LongParameterList extends SmellDetector {
    final static int PARAM_COUNT_THRESHOLD = Thresholds.THREE.intValue();
    @Override
    public List<Smell> detect(Resource resource) {
        List<Smell> smells = new ArrayList<>();
        Double paramCount = resource.getMetricValue(MetricName.ParameterCount);

        // check if method Parameter Count > 3
        if (paramCount == null || paramCount <= PARAM_COUNT_THRESHOLD) {
            return smells;
        }

        // check if method Parameter Count > avg Parameter Count
        AggregateMetricValues aggregateMetricValues = AggregateMetricValues.getInstance();
        Double avgParamCount = aggregateMetricValues.getAverageValue(MetricName.ParameterCount);
        if (avgParamCount == null || paramCount <= avgParamCount) {
            return smells;
        }

        // Long_Parameter_List = method Parameter Count > avg Parameter Count && method Parameter Count > 3
        Smell smell = super.createSmell(resource);
        smell.setReason("ParameterCount(" + paramCount + ") > avg ParameterCount(" + avgParamCount + ") AND ParameterCount(" + paramCount + ") > " + PARAM_COUNT_THRESHOLD);
        smells.add(smell);

        return smells;
    }

    @Override
    protected SmellName getSmellName() {
        return SmellName.LongParameterList;
    }
}
