package cmu.csdetector.extractor.evaluator;

import java.io.IOException;
import java.util.Map;

public abstract interface IRefactoringEvaluator {

    void evaluate() throws IOException;

    Double getReduction();

    void printEvaluation();

    Map<String, Double> getBeforeRefactorMetrics();
    Map<String, Double> getAfterRefactorMetrics();
}
