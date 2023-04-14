package cmu.csdetector.smells.detectors;

import cmu.csdetector.ast.visitors.ClassMethodInvocationVisitor;
import cmu.csdetector.resources.Method;
import cmu.csdetector.resources.Resource;
import cmu.csdetector.smells.Smell;
import cmu.csdetector.smells.SmellDetector;
import cmu.csdetector.smells.SmellName;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureEnvy extends SmellDetector {
    @Override
    public List<Smell> detect(Resource resource) {
        List<Smell> smells = new ArrayList<>();
        Method method = (Method) resource;
        IMethodBinding binding = method.getBinding();
        // not a method
        if (binding == null) {
            return smells;
        }
        ITypeBinding declaringClass = binding.getDeclaringClass();

        // collect all method calls
        ClassMethodInvocationVisitor visitor = new ClassMethodInvocationVisitor(declaringClass);
        resource.getNode().accept(visitor);
        Map<ITypeBinding, Integer> methodCalls = visitor.getMethodsCalls();

        // get internal calls, if `this` is not in the map, the internal calls is 0
        Integer internalCalls = methodCalls.getOrDefault(declaringClass, 0);
        methodCalls.remove(declaringClass); // now the map only contains external calls

        // check if any external method calls > internal calls
        methodCalls.forEach((tb, externalCalls) -> {
            if (externalCalls > internalCalls) {
                Smell smell = super.createSmell(resource);
                smell.setReason("EXTERNAL_METHOD_CALLS to " + tb.getQualifiedName() + " (" + externalCalls + ") > INTERNAL_CALLS (" + internalCalls + ")");
                smells.add(smell);
            }
        });

        return smells;
    }

    @Override
    protected SmellName getSmellName() {
        return SmellName.FeatureEnvy;
    }
}
