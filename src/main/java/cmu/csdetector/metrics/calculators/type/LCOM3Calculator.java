package cmu.csdetector.metrics.calculators.type;

import cmu.csdetector.metrics.MetricName;
import org.eclipse.jdt.core.dom.ASTNode;

/**
 * Class to calculate the LCOM3 lack of cohesion of methods metrics.
 *
 * The equation to calculate the LCOM2 is:
 * 		m = #declaredMethods(C)
 * 		a = #declaredAttributes(C)
 * 		m(A) = # of methods in C that reference attribute A
 * 		s = sum(m(A)) for A in declaredAttributes(C)
 * 		LCOM3(C) = (m - s/a)/(m - 1)
 *
 * Observation = timesAccessedAttributes comprises the s
 *
 * The metric definition as well as its implementation are available at:
 * http://www.aivosto.com/project/help/pm-oo-cohesion.html and
 * http://www.cs.sjsu.edu/~pearce/cs251b/DesignMetrics.htm
 *
 * @author leonardo
 */
public class LCOM3Calculator extends BaseLCOM {

	public LCOM3Calculator(){
		super();
	}

	@Override
	protected Double computeValue(ASTNode target) {
		boolean isPossibleLCOM = instantiateAttributes(target);	//call the method first in order to initialize the attributes

		if (!isPossibleLCOM){
			return 0.0;
		}

		/*
		 * If there are no more than one method in a class, LCOM3 is undefined.
		 * An undefined LCOM3 is displayed as zero.
		 */
		if(nMethods == 1){
			return 0.0;
		}

		return (nMethods - timesAccessedAttributes/nAttributes) / (nMethods - 1);
	}

	@Override
	public MetricName getMetricName() {
		return MetricName.LCOM3;
	}

}
