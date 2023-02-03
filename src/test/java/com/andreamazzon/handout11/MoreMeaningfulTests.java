package com.andreamazzon.handout11;

import java.text.DecimalFormat;

import com.andreamazzon.handout11.LIBORMarketModelConstructionWithFactorReduction.Dynamics;
import com.andreamazzon.handout11.LIBORMarketModelConstructionWithFactorReduction.Measure;
import com.andreamazzon.handout8.MyExchangeOption;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;

/**
 * In this test class we print the value of an exchange option involving two
 * LIBOR rates taken from a LIBOR market model, when the correlation structure
 * between the LIBORs is given by an exponential decay model with changing
 * exponential decay parameter. We do that both for a model with full correlation
 * structure and for one with factor reduction.
 *
 * @author Andrea Mazzon
 *
 */
public class MoreMeaningfulTests {

	static final DecimalFormat formatterDoubleWithTwoDigits = new DecimalFormat("0.00");

	static final DecimalFormat formatterDoubleWithFourDigits = new DecimalFormat(" 0.0000;-0.0000");
	
	public static void main(String[] args) throws CalculationException {
		// parameters for the option
		final double maturityDateFirstLibor = 2.0;
		final double paymentDateFirstLibor = 2.5;

		final double maturityDateSecondLibor = 4.0;
		final double paymentDateSecondLibor = 4.5;

		final AbstractLIBORMonteCarloProduct exchangeOption = new MyExchangeOption(maturityDateFirstLibor,
				paymentDateFirstLibor, maturityDateSecondLibor, paymentDateSecondLibor);


		final double notional = 100;

		// parameters for the LIBOR market model simulation
		final int numberOfPaths = 5000;


		final double simulationTimeStep = 0.1;
		final double LIBORTimeStep = 0.5;
		final double LIBORRateTimeHorizon = 10;
		
		final int numberOfFactorsForFullModel = (int) (LIBORRateTimeHorizon/LIBORTimeStep - 1);

		// parameters for the model
		final double a = 0.2, b = 0.1, c = 0.15, d = 0.3; // volatility structure

		/*
		 * Fixing times for the forwards: the forwards corresponding to other fixing
		 * times will be interpolated
		 */
		final double[] fixingForGivenForwards = { 0.5, 1.0, 3.0, 4.0, 9.0 };
		final double[] forwardsForCurve = { 0.05, 0.05, 0.05, 0.05, 0.05 };

		final double initialCorrelationDecayParameter = 0;
		
		final int numberOfFactors = 1;


		//model without factor reduction
		final TermStructureMonteCarloSimulationModel liborMarketModelFull = LIBORMarketModelConstructionWithFactorReduction
				.createLIBORMarketModel(numberOfPaths, simulationTimeStep, LIBORTimeStep, LIBORRateTimeHorizon,
						fixingForGivenForwards, forwardsForCurve, initialCorrelationDecayParameter, 
						Dynamics.LOGNORMAL, Measure.SPOT,a, b, c, d, numberOfFactorsForFullModel);

		double optionValueForFullModel = notional * exchangeOption.getValue(liborMarketModelFull);
		
		
		//model with factor reduction
		final TermStructureMonteCarloSimulationModel liborMarketModelReduced = LIBORMarketModelConstructionWithFactorReduction
				.createLIBORMarketModel(numberOfPaths, simulationTimeStep, LIBORTimeStep, LIBORRateTimeHorizon,
						fixingForGivenForwards, forwardsForCurve, initialCorrelationDecayParameter, 
						Dynamics.LOGNORMAL, Measure.SPOT,a, b, c, d, numberOfFactors);

		double optionValueForReducedModel = notional * exchangeOption.getValue(liborMarketModelReduced);
		
		
		double currentCorrelationDecayParameter = initialCorrelationDecayParameter;
		
		// rho_{i,k}=e^(-alpha|T_i-T_k|
		final double differenceBetweenMaturities = maturityDateSecondLibor - maturityDateFirstLibor;
		double correlationBetweenTheTwoLIBORs = Math
				.exp(-currentCorrelationDecayParameter * differenceBetweenMaturities);

		// for loop where the correlation decay parameter increases by 0.1 every time
		final double correlationStep = 0.1;

		System.out.println(
				"Correlation decay parameter" + "\t" + "Correlation between the two LIBORs" + "\t" + "Option value for full model"
						 + "\t" + "Option value for reduced model");

		System.out.println();


		for (int correlationIndex = 1; correlationIndex <= 10; correlationIndex++) {

			System.out.println(formatterDoubleWithTwoDigits.format(currentCorrelationDecayParameter) + "\t"
					+ "                         " + formatterDoubleWithTwoDigits.format(correlationBetweenTheTwoLIBORs)
					+ "                                   " + formatterDoubleWithFourDigits.format(optionValueForFullModel)
					+ "                                   " + formatterDoubleWithFourDigits.format(optionValueForReducedModel));

			// rho_i = rho_0 + i * 0.1
			currentCorrelationDecayParameter += correlationStep;

			correlationBetweenTheTwoLIBORs = Math.exp(-currentCorrelationDecayParameter * differenceBetweenMaturities);

			final TermStructureMonteCarloSimulationModel newLiborMarketModelFull = LIBORMarketModelConstructionWithFactorReduction
					.getCloneWithModifiedCorrelation(liborMarketModelFull, currentCorrelationDecayParameter);
			
			final TermStructureMonteCarloSimulationModel newLiborMarketModelReduced = LIBORMarketModelConstructionWithFactorReduction
					.getCloneWithModifiedCorrelation(liborMarketModelReduced, currentCorrelationDecayParameter);

			optionValueForFullModel = notional * exchangeOption.getValue(newLiborMarketModelFull);
			optionValueForReducedModel = notional * exchangeOption.getValue(newLiborMarketModelReduced);
		}
		
		// we print the last ones
		System.out.println(formatterDoubleWithTwoDigits.format(currentCorrelationDecayParameter) + "\t" + "                         "
				+ formatterDoubleWithTwoDigits.format(correlationBetweenTheTwoLIBORs) + "                                   "
				+ formatterDoubleWithFourDigits.format(optionValueForFullModel) + "                                   "
				+ formatterDoubleWithFourDigits.format(optionValueForReducedModel));
	}
}
