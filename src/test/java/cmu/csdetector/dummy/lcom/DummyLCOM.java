package cmu.csdetector.dummy.lcom;

import java.util.Date;

/**
 * Dummy class to calculate LCOM variations
 *
 * @author leonardo
 *
 */

public class DummyLCOM {
	public String client;
	public double principal;
	public Date hiringDate;
	public Date paymentDueDate;
	public double interestRatePerYear;

	public int installments;
	public Date paymentDate;
	
	
	public DummyLCOM(String client, double principal, Date hiringDate, Date paymentDate, double interestRatePerYear) {
		this.client = client;
		this.principal = principal;
		this.hiringDate = hiringDate;
		this.paymentDueDate = paymentDate;
		this.interestRatePerYear = interestRatePerYear;
	}
	
	public boolean checkDueDate(){
		boolean due = false;
		
		Date date = new Date();
		
		if (this.paymentDueDate.compareTo(date) < 0){
			due = true;
		}
		
		return due;
	}


	public double notCohesiveMethod(int years){
		return 0;
	}
}
