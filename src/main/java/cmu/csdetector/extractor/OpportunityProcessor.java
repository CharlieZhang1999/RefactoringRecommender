package cmu.csdetector.extractor;

import java.util.List;
import java.util.Set;

public class OpportunityProcessor {
    private final Set<List<Integer>> opportunitySet;

    // Settings
    // The maximum allowed difference in size between two opportunities to be considered valid for grouping
    private final double maxSizeDifference = .2;
    // The minimum allowed overlap in the range of two opportunities to be considered valid for grouping
    private final double minOverlap = .1;
    // The minimum difference in the benefit incurred by the two opportunities, to decide which one is the optimal
    private final double significantDifferenceThreshold = .01;

    public OpportunityProcessor(Set<List<Integer>> opportunitySet) {
        this.opportunitySet = opportunitySet;
    }

    private boolean isSimilarSize(List<Integer> a, List<Integer> b) {
        var aSize = a.size();
        var bSize = b.size();
        var minSize = Math.min(aSize, bSize);
        var diff = Math.abs(aSize - bSize);
        return diff < minSize * maxSizeDifference;
    }

    private boolean isSignificantlyOverlapping(List<Integer> a, List<Integer> b) {
        var aSize = a.size();
        var bSize = b.size();
        var maxSize = Math.max(aSize, bSize);
        var intersectionSize = a.stream().filter(b::contains).count();
        return intersectionSize > minOverlap * maxSize;
    }

    private int calcBenefit(List<Integer> a, List<Integer> b) {
        return 0;
    }
}
