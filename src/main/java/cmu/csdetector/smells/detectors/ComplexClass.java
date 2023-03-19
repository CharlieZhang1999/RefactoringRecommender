package cmu.csdetector.smells.detectors;

import cmu.csdetector.metrics.MetricName;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.resources.Type;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellDetector;
import cmu.csdetector.smells.SmellName;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex Class: A class having at least one method with McCabe Cyclomatic complexity greater than 10
 */
public class ComplexClass extends SmellDetector {
    private final static int OVERCOMPLEXITY = 10;

    @Override
    public List<Smell> detect(Resource resource) {
        List<Smell> smells = new ArrayList<>();

        Type type = (Type) resource;

        for (Method method : type.getMethods()) {
            Double cc = method.getMetricValue(MetricName.CC);

            if (cc != null && cc > OVERCOMPLEXITY) {
                Smell smell = createSmell(resource);
                smell.setReason("CC = " + cc);

                smells.add(smell);
            }
        }

        return smells;
    }

    @Override
    protected SmellName getSmellName() {
        return SmellName.ComplexClass;
    }
}
