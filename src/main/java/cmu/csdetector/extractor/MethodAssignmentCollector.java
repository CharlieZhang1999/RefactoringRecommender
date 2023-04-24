package cmu.csdetector.extractor;

import cmu.csdetector.ast.CollectorVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.SimpleName;


public class MethodAssignmentCollector extends CollectorVisitor<SimpleName> {



    public MethodAssignmentCollector() {

    }

    public boolean visit(Assignment node) {
        if (node.getLeftHandSide() instanceof SimpleName) {
            this.addCollectedNode((SimpleName) node.getLeftHandSide());
        }

        return true;
    }
}