package cmu.csdetector.extractor;

import cmu.csdetector.ast.CollectorVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;


/**
 * Visits a method body
 *
 * @author
 */
public class MethodVariableCollector extends CollectorVisitor<SimpleName> {

    /**
     * Fields declared in the type associated with this field.
     * Using these declarations we can determine whether a
     * simple name is a field access or not
     */


    public MethodVariableCollector() {

    }

    public boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        /*
         * Checks if the binding refers to a variable access. If yes,
         * checks if the variable is a field.
         */
        if (binding != null && binding.getKind() == IBinding.VARIABLE) {
            this.addCollectedNode(node);
        }
        return true;
    }
}
