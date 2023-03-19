package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.metrics.MetricName;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Class to calculate the LCOM2 lack of cohesion of methods metrics.
 *
 * The equation to calculate the LCOM2 is:
 * 		m = #declaredMethods(C)
 * 		a = #declaredAttributes(C)
 * 		m(A) = # of methods in C that reference attribute A
 * 		s = sum(m(A)) for A in declaredAttributes(C)
 * 		LCOM2(C) = 1 - s/(m * a)
 *
 * Observation = timesAccessedAttributes comprises the s
 *
 * The metric definition as well as its implementation are available at:
 * http://www.aivosto.com/project/help/pm-oo-cohesion.html and
 * http://www.cs.sjsu.edu/~pearce/cs251b/DesignMetrics.htm
 *
 * @author leonardo
 */
public class LCOM2Calculator extends BaseLCOM {

	public LCOM2Calculator(){
		super();
	}

	@Override
	protected Double computeValue(ASTNode target) {
		boolean isPossibleLCOM = instantiateAttributes(target);	//call the method first in order to initialize the attributes

		if (!isPossibleLCOM){
			return 0.0;
		}

		return 1 - (timesAccessedAttributes) / (nMethods * nAttributes);
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.LCOM2;
	}

}
