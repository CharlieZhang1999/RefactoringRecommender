package cmu.csdetector.smells;

import cmu.csdetector.smells.detectors.ComplexClass;

public class ClassLevelSmellDetector extends CompositeSmellDetector {
	
	public ClassLevelSmellDetector() {
		addDetector(new ComplexClass());
	}

	@Override
	protected SmellName getSmellName() {
		return null;
	}

}
