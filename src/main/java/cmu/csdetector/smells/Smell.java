package cmu.csdetector.smells;

import cmu.csdetector.resources.Resource;

public class Smell {

	private SmellName name;

	private String reason;

	/**
	 * Line of code where the smell starts
	 */
	private Integer startingLine;

	private Integer endingLine;

	/**
	 * Related resource (Class (Type) or Method).
	 * No serialization for this field.
	 */
	private transient Resource resource;

	public Smell(SmellName name) {
		this.name = name;
	}

	public Smell(SmellName name, String reason) {
		this.name = name;
		this.reason = reason;
	}

	public Smell(SmellName name, String reason, Integer line) {
		this.name = name;
		this.reason = reason;
		this.startingLine = line;
	}

	public SmellName getName() {
		return name;
	}

	public void setName(SmellName name) {
		this.name = name;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public Integer getStartingLine() {
		return startingLine;
	}

	public void setStartingLine(Integer line) {
		this.startingLine = line;
	}

	public Integer getEndingLine() {
		return endingLine;
	}

	public void setEndingLine(Integer endingLine) {
		this.endingLine = endingLine;
	}

	public <T extends Resource> T getResource() {
		return (T) resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
