package com.andreamazzon.handout6;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import com.andreamazzon.handout5.InterestRatesProductsEnhanced;

/**
 * This is a test class for the computation of the price for classical payment
 * in T_2 as well as of the price in arrears (i.e., with payment in T_1) of
 * European interest rate derivatives. Here we test a floater and a caplet under
 * the Black model. We want to see if the prices that we get by the abstract
 * class EuropeanOptionPossiblyInArrears and the derived classes are the same as
 * the ones we have computed in the previous exercises, with the formulas in the
 * test class com.andreamazzon.handout5.ConvexityAdjustmentsTests and the
 * methods in com.andreamazzon.handout5.InterestRateProductsEnhanced,
 * respectively.
 *
 * @author Andrea Mazzon
 *
 */
public class EuropeanOptionPossiblyInArrearsTest {

	final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");

	/*
	 * parameters for both the options: they give the value of the fields of the
	 * parent class
	 */
	final double firstTime = 1;
	final double secondTime = 2;

	final double firstBond = 0.95;
	final double secondBond = 0.91;

	final double notional = 10000;

	final double liborVolatility = 0.3;// parameter for the log-volatility of the process

	// value of L(T_1,T_2;0) from T_1, T_2, P(T_1;0), P(T_2;0).

	final double tolerance = 1E-10;

	@Test
	public void testFloater() {

		System.out.println("TESTING THE FLOATER..");
		System.out.println();

		// first we calculate the prices with the "new" implementation..
		final EuropeanOptionPossiblyInArrears floater = new FloaterWithBlack(firstTime, secondTime, firstBond,
				secondBond, liborVolatility, notional, false);

		final double initialForwardLibor = floater.getInitialValueLibor();

		final double priceOfTheFloater = floater.getValueInClassicUnits(initialForwardLibor);

		System.out.println(
				"Price of the floater with the new implementation: " + printNumberWithFourDecimalDigits.format(priceOfTheFloater));
		System.out.println();

		// ..and then the price in arrears..
		final double priceOfTheFloaterInArrears = floater.getValueInArrears();

		System.out.println("Price of the floater in arrears: " + printNumberWithFourDecimalDigits.format(priceOfTheFloaterInArrears));
		System.out.println();

		/*
		 * ..and the convexity adjustment (alternatively, we could also directly call
		 * the method of EuropeanOptionPossiblyInArrears
		 */
		System.out.println("Price of the convexity adjustment: "
				+ printNumberWithFourDecimalDigits.format(priceOfTheFloaterInArrears - priceOfTheFloater));
		System.out.println();

		// look at "Solution to theoretical part of exercise 2.pdf" in com.andreamazzon.handout5
		final double priceOfTheFloaterWithOldImplementation = notional * (firstBond - secondBond);

		final double priceOfTheFloaterInArrearsWithOldImplementation = priceOfTheFloaterWithOldImplementation
				+ notional * secondBond * initialForwardLibor * initialForwardLibor * (secondTime - firstTime)
						* (secondTime - firstTime) * Math.exp(liborVolatility * liborVolatility * firstTime);

		// we check if the prices look the same
		assertEquals(priceOfTheFloater, priceOfTheFloaterWithOldImplementation, tolerance);
		assertEquals(priceOfTheFloaterInArrears, priceOfTheFloaterInArrearsWithOldImplementation, tolerance);
	}
	
	@Test
	public void testCaplet() {

		System.out.println("TESTING THE CAPLET..");
		System.out.println();

		final double strikeOfTheCaplet = 0.044;// parameter specific to the caplet

		// first we calculate the prices with the "new" implementation..
		final EuropeanOptionPossiblyInArrears caplet = new CapletWithBlack(firstTime, secondTime, firstBond, secondBond,
				liborVolatility, notional, false, strikeOfTheCaplet);

		final double initialForwardLibor = caplet.getInitialValueLibor();

		final double priceOfTheCaplet = caplet.getValueInClassicUnits(initialForwardLibor);

		System.out
				.println("Price of the caplet with the new implementation: " + printNumberWithFourDecimalDigits.format(priceOfTheCaplet));
		System.out.println();

		// ..and then the price in arrears..
		final double priceOfTheCapletInArrears = caplet.getValueInArrears();

		System.out.println("Price of the caplet in arrears: " + printNumberWithFourDecimalDigits.format(priceOfTheCapletInArrears));
		System.out.println();

		/*
		 * ..and the convexity adjustment (alternatively, we could also directly call the method of EuropeanOptionPossiblyInArrears
		 */
		System.out.println("Price of the convexity adjustment: "
				+ printNumberWithFourDecimalDigits.format(priceOfTheCapletInArrears - priceOfTheCaplet));
		System.out.println();

		final double priceOfTheCapletWithOldImplementation = InterestRatesProductsEnhanced
				.calculateCapletValueBlackModel(initialForwardLibor, liborVolatility, strikeOfTheCaplet, firstTime,
						secondTime, secondBond, notional);

		final double priceOfTheCapletInArrearsWithOldImplementation = InterestRatesProductsEnhanced
				.calculateCapletInArrearsBlack(initialForwardLibor, liborVolatility, strikeOfTheCaplet, firstTime,
						secondTime, secondBond, notional);

		// we check if the prices look the same
		assertEquals(priceOfTheCaplet, priceOfTheCapletWithOldImplementation, tolerance);
		assertEquals(priceOfTheCapletInArrears, priceOfTheCapletInArrearsWithOldImplementation, tolerance);
	}
}
