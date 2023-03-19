package cmu.csdetector.smells;

import cmu.csdetector.smells.detectors.MessageChain;

public class MethodLevelSmellDetector extends CompositeSmellDetector {
	
	public MethodLevelSmellDetector() {
		addDetector(new MessageChain());
	}

	@Override
	protected SmellName getSmellName() {
		return null;
	}

}
