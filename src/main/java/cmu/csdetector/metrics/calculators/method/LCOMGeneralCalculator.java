package cmu.csdetector.metrics.calculators.method;

import cmu.csdetector.ast.visitors.ClassFieldAccessCollector;
import cmu.csdetector.ast.visitors.LCOMGeneralVisitor;
import cmu.csdetector.metrics.calculators.MetricValueCalculator;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayList;

public abstract class LCOMGeneralCalculator extends MetricValueCalculator {
    protected Integer sumMA;

    protected abstract Double calcLCOM(Integer m, Integer a);

    protected void setSumMA(ASTNode target, ArrayList<MethodDeclaration> methodDeclarations) {
        this.sumMA = 0;
        methodDeclarations.forEach(md -> {
            // use ClassFieldAccessCollector to sum up mA
            ClassFieldAccessCollector classFieldAccessCollector = new ClassFieldAccessCollector((TypeDeclaration) target);
            md.accept(classFieldAccessCollector);
            this.sumMA += classFieldAccessCollector.getNodesCollected().size();
        });
    }

    @Override
    protected Double computeValue(ASTNode target) {
        LCOMGeneralVisitor visitor = new LCOMGeneralVisitor((TypeDeclaration) target);
        target.accept(visitor);
        ArrayList<MethodDeclaration> methodDeclarations = visitor.getMethodDeclarations();

        Integer m = visitor.getM();
        Integer a = visitor.getA();
        this.setSumMA(target, methodDeclarations);
        return this.calcLCOM(m, a);
    }

    @Override
    public boolean shouldComputeAggregate() {
        return true;
    }
}
