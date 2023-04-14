package cmu.csdetector.smells.detectors;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellDetector;
import cmu.csdetector.smells.SmellName;

import java.util.ArrayList;
import java.util.List;

/**
 * All chains of method's calls longer than three
 *
 */
public class MessageChain extends SmellDetector {
    private static final double MAXCALLCHAIN = 3;

    @Override
    public List<Smell> detect(Resource resource) {
        Double callChain = resource.getMetricValue(MetricName.MaxCallChain);

        if (callChain > MAXCALLCHAIN) {
            Smell smell = createSmell(resource);
            smell.setReason("MAX_CALL_CHAIN = " + callChain);

            return List.of(smell);
        }

        return new ArrayList<>();
    }

    @Override
    protected SmellName getSmellName() {
        return SmellName.MessageChain;
    }
}
