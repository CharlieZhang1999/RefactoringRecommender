package cmu.csdetector.extractor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An iterator that iterates over the statements table by given step.
 * It returns the group of line numbers that has the next opportunity to extract a method.
 */
public class StepIterator {
    private final SortedMap<Integer, Set<String>> line2vars;
    private int startIndex;
    private final Integer[] line2varsKeys;
    private List<String> var2idx;
    private int startline;
    private int endline;
    private int[][] matrix;

    private int[] parent;


    public StepIterator(SortedMap<Integer, Set<String>> line2vars) {
        this.line2vars = line2vars;
        this.startline = this.line2vars.firstKey();
        this.endline  = this.line2vars.lastKey();
        this.line2varsKeys = line2vars.keySet().toArray(new Integer[0]);
        this.var2idx = this.varDict();
        this.matrix = this.convertMatrix();
        this.startIndex = 0;
    }


    private List<String> varDict (){
        HashSet<String> mergedSet = new HashSet<>();
        this.line2vars.forEach((k, value) -> {mergedSet.addAll(value);});
        List<String> mergedList = new ArrayList<>(mergedSet);
        return mergedList;
    }

    private int[][] convertMatrix() {
        int[][] matrix = new int[var2idx.size()][this.endline + 1];
        this.line2vars.forEach((key, value) -> {
            for(String var: this.line2vars.get(key)) {
                matrix[this.var2idx.indexOf(var)][key] = 1;
            }
        });
        return matrix;
    }

    private int findParent(int idx){
        if(parent[idx] != idx && parent[idx] != 0){
            return findParent(parent[idx]);
        }
        return idx;
    }

    private boolean hasOverlap(Set<String> variables_1, Set<String> variables_2){
        if(variables_1 == null || variables_2 == null) return false;
        Set<String> variables_1_copy = new HashSet<>(variables_1);
        variables_1_copy.retainAll(variables_2);
        return variables_1_copy.size() > 0;
    }

    public Set<List<Integer>> getAllOpportunities() {
        Set<List<Integer>> set = new HashSet<>();
        for (int i = 1; i < this.endline - this.startline; i++) {
            set.addAll(getIntervalsFromAllRows(i));
        }
        return set;
    }

    public List<List<Integer>> getIntervalsFromAllRows(int step) {
        List<List<Integer>> allIntervals = new ArrayList<>();
        for (int i = 0; i < this.var2idx.size(); i++ ) {
            allIntervals.addAll(getIntervals(i, step));
        }

        Collections.sort(allIntervals, new Comparator<List<Integer>>() {
            @Override
            public int compare(List<Integer> a, List<Integer> b) {
                return a.get(0) == b.get(0)? a.get(1)-b.get(1) : a.get(0)-b.get(0);
            }
        });

        List<List<Integer>> res = new ArrayList<>();

        if (allIntervals.size() == 0) return  res;
        res.add(allIntervals.get(0));

        for(int i=1; i<allIntervals.size(); i++){
            List<Integer> last = res.get(res.size()-1);
            if(last.get(1) < allIntervals.get(i).get(0)){
                res.add(allIntervals.get(i));
            }else{
                last.set(1, Math.max(allIntervals.get(i).get(1), last.get(1)) );
            }
        }

        return res;
    }


    private List<List<Integer>> getIntervals(int row, int step) {
        List<List<Integer>> intervals = new ArrayList<>();
        List<Integer> lastInterval = Arrays.asList(-step,-step);
        for (int j = 1; j <= this.endline + 1 - step; j++) {
            if (matrix[row][j] == 1) {
                if (lastInterval.get(1) < j - step) {
                    intervals.add(lastInterval);
                    lastInterval = Arrays.asList(j,j);;
                } else {
                    lastInterval.set(1, j);
                }
            }
        }

        intervals.add(lastInterval);
        intervals.remove(0);
        return intervals.stream().filter(in -> in.get(1) - in.get(0) >= step).collect(Collectors.toList());
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
