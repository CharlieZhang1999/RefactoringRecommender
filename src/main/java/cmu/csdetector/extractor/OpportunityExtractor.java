package cmu.csdetector.extractor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpportunityExtractor is an iterator that iterates over the statements table by given step.
 * It returns the group of line numbers that has the next opportunity to extract a method.
 */
public class OpportunityExtractor implements Iterator<List<List<Integer>>> {
    private final SortedMap<Integer, Set<String>> line2vars;
    private final int step; // window size
    private int startIndex;
    private final Integer[] line2varsKeys;

    public OpportunityExtractor(SortedMap<Integer, Set<String>> line2vars, int step) {
        this.line2vars = line2vars;
        this.step = step;
        this.startIndex = 0;

        this.line2varsKeys = line2vars.keySet().toArray(new Integer[0]);
    }

    @Override
    public boolean hasNext() {
        return this.startIndex < line2vars.size();
    }

    /**
     * Return the opportunities with the given step and start index by applying sliding window.
     *
     * @return the line numbers of the opportunities. Each opportunity is a list of line numbers whose length is greater than 1.
     */
    @Override
    public List<List<Integer>> next() {
        Set<List<Integer>> opportunitySet = new HashSet<>();
        for (int left = startIndex; left < line2vars.size() - step; left++) {
            // slice the sorted map by the window size
            SortedMap<Integer, Set<String>> ln2VarWindow = line2vars.subMap(line2varsKeys[left], line2varsKeys[left + step]);
            if (haveOverlap(new ArrayList<>(ln2VarWindow.values()))) {
                opportunitySet.add(new ArrayList<>(ln2VarWindow.keySet()));
            }
        }

        // move the start index to the next line
        this.startIndex++;

        return opportunitySet.stream()
                // keep the opportunities whose length is greater than 1
                .filter(opportunity -> opportunity.size() > 1)
                // sort by the first line number, then by the last line number
                .sorted(Comparator.comparingInt((List<Integer> o) -> o.get(0)).thenComparingInt(o -> o.get(o.size() - 1)))
                .collect(Collectors.toList());
    }

    /**
     * Check if a list of sets share the same elements.
     */
    private boolean haveOverlap(List<Set<String>> list) {
        if (list.size() == 0) {
            return false;
        }
        // make a copy of the first set
        Set<String> first = new HashSet<>(list.get(0));
        // keep intersecting until the first set is empty or all sets are checked
        for (int i = 1; i < list.size(); i++) {
            first.retainAll(list.get(i));
            if (first.size() == 0) {
                return false;
            }
        }
        return true;
    }
}
