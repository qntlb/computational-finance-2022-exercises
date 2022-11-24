package com.andreamazzon.handout5;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * In this class we do some test about convexity adjustments: in particular, we
 * focus on the Floater in arrears and on the Caplet in arrears. We compute the
 * value of the convexity adjustment both analytically and by Monte-Carlo
 * simulations.
 *
 * @author Andrea Mazzon
 *
 */
public class ConvexityAdjustmentsTest {

	final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");

	final double initialTime = 0;
	final double fixingDate = 1;
	final double paymentDate = 2;
	final double notional = 10000;

	/**
	 * Test for the floater and floater in arrears
	 *
	 * @throws CalculationException
	 */
	@Test
	public void testFloater() {

		final double firstDiscountingFactor = 0.95;// P(T_1;0)
		final double secondDiscountingFactor = 0.9;// P(T_2;0)

		final double floaterTimeInterval = paymentDate - fixingDate;

		// floater price: N(P(T_1;0)-P(T_2;0))
		final double analyticFloater = notional * (firstDiscountingFactor - secondDiscountingFactor);
		// or if you want, N(T_2-T_1)P(T_2;0)L(T_1,T_2;0)

		System.out.println("Natural floater analytic price " + printNumberWithFourDecimalDigits.format(analyticFloater));

		/*
		 * We now want to compute the convexity adjustment: we need the LIBOR volatility
		 * and the initial LIBOR L(T_1,T_2;0)
		 */
		final double liborVolatility = 0.25;
		// we get L(T_1,T_2;0) from P(T_1;0) and P(T_2;0)
		final double initialForwardLibor = 1 / floaterTimeInterval
				* (firstDiscountingFactor / secondDiscountingFactor - 1);

		// convexity adjustment: N * P(T_2;0) * L(0)^2 * (T_2-T_1)^2 * exp(sigma^2 T_1)
		final double analyticConvexityAdjustment = notional * secondDiscountingFactor * initialForwardLibor
				* initialForwardLibor * floaterTimeInterval * floaterTimeInterval
				* Math.exp(liborVolatility * liborVolatility * fixingDate);

		System.out.println("Convexity Adjustment analytic price " + analyticConvexityAdjustment);

		// price of the natural floater + convexity adjustment
		final double analyticFloaterInArrears = analyticFloater + analyticConvexityAdjustment;

		System.out.println("Floater in arrears analytic price: " + printNumberWithFourDecimalDigits.format(analyticFloaterInArrears));
		System.out.println();

		// Monte Carlo implementation
		final int numberOfPaths = 100000;
		final double tolerance = 5/Math.sqrt(numberOfPaths); 

		final int numberOfTimeSteps = 100;
		final double stepSize = fixingDate / numberOfTimeSteps;
		// discretization of the time interval..
		final TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, stepSize);
		// and discretization of the simulated process, as usual
		final MonteCarloBlackScholesModel bsLiborModel = new MonteCarloBlackScholesModel(times, numberOfPaths,
				initialForwardLibor, 0.0, liborVolatility);

		
		// We get here all the realizations of the final value of the LIBOR, i.e., P(T_1,T_2;T_1)
		RandomVariable finalLibors = null;
		try {
			finalLibors = bsLiborModel.getAssetValue(fixingDate, 0);
		} catch (CalculationException e) {
			e.printStackTrace();
		}
		
		/*
		 * The payoff is N (T_2-T_1) L(T_1,T_2;T_1). We now want to approximate 
		 * N P(T_2;0)(T_2-T_1) E[L(T_1,T_2;T_1)]
		 */
		final double approximatedExpectation = finalLibors.getAverage();
		

		// We discount the average of the simulations of the floater value at final time
		final double montecarloFloater = secondDiscountingFactor * notional * floaterTimeInterval
				* approximatedExpectation;
		
		assertEquals(0,
				(montecarloFloater - analyticFloater) / analyticFloater, tolerance);
		
		System.out.println("Floater price with Monte Carlo: " + printNumberWithFourDecimalDigits.format(montecarloFloater));

		/*
		 * The convexity adjustment is 
		 * N * P(T_2;0) * (T_2-T_1)^2 * L(0)^2 * exp(sigma^2 T_1).
		 * Note (exercise) that 
		 * L(0)^2 * exp(sigma^2 T_1) = E[L(T)^2] 
		 * for L given by the Black-Scholes model with r = 0 and log-volatility equal to sigma.
		 */
		final RandomVariable finalLiborsSquare = finalLibors.mult(finalLibors);

		final double montecarloConvexityAdjustment = notional * secondDiscountingFactor * floaterTimeInterval
				* floaterTimeInterval * finalLiborsSquare.getAverage();

		final double montecarloFloaterInArrears = montecarloFloater + montecarloConvexityAdjustment;


		assertEquals(0,
				(montecarloConvexityAdjustment - analyticConvexityAdjustment) / analyticConvexityAdjustment, tolerance);
		
		System.out.println(
				"Convexity adjustment price with Monte Carlo " + printNumberWithFourDecimalDigits.format(montecarloConvexityAdjustment));

		System.out.println("Floater in arrears price with Monte Carlo " + printNumberWithFourDecimalDigits.format(montecarloFloaterInArrears));
		System.out.println();
	}

	/**
	 * Test for the caplet and caplet in arrears
	 */
	@Test
	public void testCaplet() {

		// parameters for the caplet and for the caplet in arrears
		final double initialForwardLibor = 0.05;
		final double liborVolatility = 0.3;

		final double strikeOfTheCaplet = 0.044;

		final double paymentDateDiscountFactor = 0.91;

		// this method is inherited from InterestRatesProducts
		final double capletPrice = InterestRatesProductsEnhanced.calculateCapletValueBlackModel(initialForwardLibor,
				liborVolatility, strikeOfTheCaplet, fixingDate, paymentDate, paymentDateDiscountFactor, notional);

		System.out.println("Price of the caplet: " + printNumberWithFourDecimalDigits.format(capletPrice));
		

		// this method is specific of InterestRatesProductsInArrears
		final double capletInArrearsPrice = InterestRatesProductsEnhanced.calculateCapletInArrearsBlack(
				initialForwardLibor, liborVolatility, strikeOfTheCaplet, fixingDate, paymentDate,
				paymentDateDiscountFactor, notional);

		System.out.println("Price of the caplet in arrears: " + printNumberWithFourDecimalDigits.format(capletInArrearsPrice));

		System.out.println(
				"Price of the convexity adjustment: " + printNumberWithFourDecimalDigits.format(capletInArrearsPrice - capletPrice));
	}
}
