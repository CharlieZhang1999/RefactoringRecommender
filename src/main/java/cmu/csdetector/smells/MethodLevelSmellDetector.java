package cmu.csdetector.smells;

import cmu.csdetector.smells.detectors.FeatureEnvy;

public class MethodLevelSmellDetector extends CompositeSmellDetector {

	public MethodLevelSmellDetector() {
		addDetector(new FeatureEnvy());
	}

	@Override
	protected SmellName getSmellName() {
		return null;
	}

}
