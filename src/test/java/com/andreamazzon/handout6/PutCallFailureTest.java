package com.andreamazzon.handout6;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

/**
 * This class tests the implementation of the methods to recover forward and discount factor from differences
 * between call and put prices
 * 
 * @author Andrea Mazzon
 *
 */
class PutCallFailureTest {

	@Test
	void testLinearRegressionForwardAndDiscounting() {
		final DecimalFormat printNumberWithTwoDecimalDigits = new DecimalFormat("0.00");
		
		double[] strikes = {8.0, 11.0};
		
		double initialValue = 10.0;
		double forward = 0.85 * initialValue;
		double volatility = 0.7;
		double maturity = 2.0;
		double discountFactor = 0.77;
		
		double[] differences = PutCallParityFailure.getCallPutDifference(forward, volatility, maturity, discountFactor, strikes);
		double[] factorsWeGet = PutCallParityFailure.getForwardAndDiscountFactor(differences, strikes);
		
		System.out.println("Recovered forward: " + printNumberWithTwoDecimalDigits.format(factorsWeGet[0]));
		System.out.println("Recovered discount factor: " + printNumberWithTwoDecimalDigits.format(factorsWeGet[1]));

		
	}

}
