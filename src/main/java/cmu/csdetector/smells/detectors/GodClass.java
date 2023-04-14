package cmu.csdetector.smells.detectors;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.metrics.calculators.AggregateMetricValues;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellDetector;
import cmu.csdetector.smells.SmellName;

import java.util.ArrayList;
import java.util.List;

public class GodClass extends SmellDetector {
    final static int CLOC_THRESHOLD = 500;
    @Override
    public List<Smell> detect(Resource resource) {
        List<Smell> smells = new ArrayList<>();
        // CLOC
        Double cloc = resource.getMetricValue(MetricName.CLOC);
        if (cloc == null || cloc <= CLOC_THRESHOLD) {
            return smells;
        }
        // TCC
        Double tcc = resource.getMetricValue(MetricName.TCC);
        AggregateMetricValues aggregateMetricValues = AggregateMetricValues.getInstance();
        Double tccAvg = aggregateMetricValues.getAverageValue(MetricName.TCC);
        if (tcc == null || tccAvg == null || tcc >= tccAvg) {
            return smells;
        }
        // God_Class = CLOC > 500 AND TCC < TCCAvg
        Smell smell = super.createSmell(resource);
        smell.setReason("CLOC(" + cloc + ") > " + CLOC_THRESHOLD + " AND TCC(" + tcc + ") < TCCAvg(" + tccAvg + ")");
        smells.add(smell);

        return smells;
    }

    @Override
    protected SmellName getSmellName() {
        return SmellName.GodClass;
    }
}
