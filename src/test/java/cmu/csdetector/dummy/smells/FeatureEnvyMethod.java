package cmu.csdetector.dummy.smells;

public class FeatureEnvyMethod {

	private String fPrivate;

	private FieldAccessedByMethod foreign1;

	private RefusedBequestSample foreign2;

	public FeatureEnvyMethod() {
		foreign1 = new FieldAccessedByMethod();
		foreign2 = new RefusedBequestSample();
	}
	
	protected String getfPrivate() {
		return fPrivate;
	}

	public void localA() {
		this.fPrivate = "";
	}

	public void localB() {
		System.out.println("");
		this.fPrivate = "";
	}

	public void localC() {
		new String().length();
		this.fPrivate = "";
	}
	
	public void localD() {
		Object.class.getName().toLowerCase().toUpperCase().toLowerCase().trim().length();
	}

	public void superLocal() {
		this.localA();
		this.localB();
		this.localC();
		this.localD();
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
	}
	
	public void superForeign(int a) {
		foreign1.a();
		foreign1.b();
		foreign1.c();
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
	}
	
	public void mostLocal(int a, int b) {
		this.localA();
		this.localB();
		this.localC();
		this.localD();
		foreign1.a();
		foreign1.b();
		foreign2.b();
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
	}
	
	public void mostForeign(int a, int b, int c) {
		this.mostLocal(a, b);
		this.localD();
		new String().toCharArray();
		new String().toLowerCase();
		Integer.bitCount(12);
		foreign1.a();
		foreign1.b();
		foreign1.b();
		foreign1.b();
		foreign1.b();
		foreign2.b();
	}
}
