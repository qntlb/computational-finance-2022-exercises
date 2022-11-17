package com.andreamazzon.handout4;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;

/**
 * In this class we test the implementation of the methods computing the value
 * of a caplet (both with Monte-Carlo and using the analytic formula of a call 
 * option) and a quanto. Note that we have two methods: we run both of them when
 * running the class. 
 * 
 * @author Andrea Mazzon
 *
 */
public class CapletAndQuantoTest {

	//parameters which are useful for both the methods
	
	// discount factor
	final double discountFactorAtMaturity = 0.91;

	// parameters for the Libor dynamics
	final double initialForwardLibor = 0.05;
	final double liborVolatility = 0.3;

	// parameters for the both the options
	final double fixingDate = 1;
	final double paymentDate = 2;

	final double strike = 0.05;
	final double notional = 10000;

	final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");

	
	
	@Test
	void capletTest() throws CalculationException {
		
		// tolerances for assertEqual
		
		final double toleranceForMonteCarlo = 5*1E-2;
		
		//it makes sense to make it smaller than the Monte-Carlo one
		final double toleranceForAnalytic = 1E-15;

		final double maturityOfTheOption = fixingDate;
		final double periodLength = paymentDate - fixingDate;
		
		// the benchmark value
		final double finmathLibraryValue = notional * AnalyticFormulas.blackModelCapletValue(initialForwardLibor,
				liborVolatility, maturityOfTheOption, strike, periodLength, discountFactorAtMaturity);

		System.out.println("Value of the caplet computed by the Finmath library: " 
				+ printNumberWithFourDecimalDigits.format(finmathLibraryValue) + "\n");

		
		// the value computed using the analytic BS formula from the finmath library
		final double ourAnalyticValue = InterestRatesProducts.calculateCapletValueBlackModel(initialForwardLibor,
						liborVolatility, strike, fixingDate, paymentDate, discountFactorAtMaturity, notional);

		System.out.println("Value of the caplet computed using the analytic formula for a call option: "
						+ printNumberWithFourDecimalDigits.format(ourAnalyticValue) + "\n");
		
		// We check the relative error
		assertEquals(0.0, (ourAnalyticValue-finmathLibraryValue)/finmathLibraryValue, toleranceForAnalytic);	

		
		// parameters for the Monte Carlo simulation
		final int numberOfTimeSteps = 100;
		final int numberOfSimulations = 100000;

		// the value computed using the analytic BS formula from the finmath library
		final double ourMonteCarloValue = InterestRatesProducts.calculateCapletValueBlackModel(initialForwardLibor,
				liborVolatility, strike, fixingDate, paymentDate, discountFactorAtMaturity, notional, numberOfTimeSteps, numberOfSimulations);

		System.out.println("Value of the caplet computed using Monte Carlo valuation of a call option: "
				+ printNumberWithFourDecimalDigits.format(ourMonteCarloValue) + "\n");

		
		// We check the relative error
		assertEquals(0, (ourMonteCarloValue-finmathLibraryValue)/finmathLibraryValue, toleranceForMonteCarlo);	
	}



	@Test
	void quantoTest() {
		// foreign Libor rate dynamics
		final double initialForwardForeignLibor = initialForwardLibor;
		final double liborForeignVolatility = liborVolatility;

		// forward ffx rate dynamics
		final double ffxVolatility = 0.2;

		// correlation between the forward fx rate process and the Libor rate process
		final double correlationFxLibor = 0.4;

		// the quanto rate (i.e., the the constant conversion factor)
		final double quantoRate = 0.9;

		final double quantoPrice = InterestRatesProducts.calculateQuantoCapletValue(
				initialForwardForeignLibor, liborForeignVolatility, ffxVolatility, correlationFxLibor, fixingDate,
				paymentDate, strike, discountFactorAtMaturity, notional, quantoRate);

		System.out.println("Price of the Quanto Caplet: " + printNumberWithFourDecimalDigits.format(quantoPrice));
	}

}
