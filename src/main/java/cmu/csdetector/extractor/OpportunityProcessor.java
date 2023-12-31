package cmu.csdetector.extractor;

import java.util.ArrayList;
import java.util.List;

public class OpportunityProcessor {
    private final List<ExtractedMethod> extractedMethods;
    /**
     * The maximum allowed difference in size between two opportunities to be considered valid for grouping
     */
    private final double maxSizeDifference = .2;
    /**
     * The minimum allowed overlap in the range of two opportunities to be considered valid for grouping
     */
    private final double minOverlap = .1;
    /**
     * The minimum difference in the benefit incurred by the two opportunities, to decide which one is the optimal
     */
    private final double significantDifferenceThreshold = .01;


    public OpportunityProcessor(List<ExtractedMethod> extractedMethods) {
        this.extractedMethods = extractedMethods;
    }

    public List<ExtractedMethod> process() {
        List<ExtractedMethod> result = new ArrayList<>();
        boolean[] grouped = new boolean[extractedMethods.size()];
        for (int i = 0; i < extractedMethods.size(); i++) {
            if (grouped[i]) {
                continue;
            }
            ExtractedMethod current = extractedMethods.get(i);
            for (int j = i + 1; j < extractedMethods.size(); j++) {
                if (grouped[j]) {
                    continue;
                }
                ExtractedMethod other = extractedMethods.get(j);
                if (isSimilarSize(current, other) && isSignificantlyOverlapping(current, other)) {
                    if (calcBenefit(current) - calcBenefit(other) > significantDifferenceThreshold) {
                        grouped[j] = true;
                    } else {
                        grouped[i] = true;
                        current = other;
                    }
                }
            }
        }
        for (int i = 0; i < grouped.length; i++) {
            if (!grouped[i]) {
                result.add(extractedMethods.get(i));
            }
        }
        return result;
    }

    private boolean isSimilarSize(ExtractedMethod a, ExtractedMethod b) {
        Integer[] aRange = a.getLineRange();
        int aSize = aRange[1] - aRange[0];
        Integer[] bRange = b.getLineRange();
        int bSize = bRange[1] - bRange[0];

        var minSize = Math.min(aSize, bSize);
        var diff = Math.abs(aSize - bSize);
        return diff < minSize * maxSizeDifference;
    }

    private boolean isSignificantlyOverlapping(ExtractedMethod a, ExtractedMethod b) {
        Integer[] aRange = a.getLineRange();
        int aSize = aRange[1] - aRange[0];
        Integer[] bRange = b.getLineRange();
        int bSize = bRange[1] - bRange[0];

        var maxSize = Math.max(aSize, bSize);
        var intersectionSize = Math.min(aRange[1], bRange[1]) - Math.max(aRange[0], bRange[0]);
        return intersectionSize > minOverlap * maxSize;
    }

    private double calcBenefit(ExtractedMethod extractedMethod) {
        double originalLCOM = extractedMethod.getOriginalLCOM();
        double newLCOM = extractedMethod.getOpportunityLCOM(); // opportunity lcom
        double refactoredLCOM = extractedMethod.getRefactoredLCOM();

        return originalLCOM - Math.max(newLCOM, refactoredLCOM);
    }
}
