package com.andreamazzon.handout10;

import java.text.DecimalFormat;
import java.util.ArrayList;

import com.andreamazzon.handout9.LIBORMarketModelConstructionWithDynamicsAndMeasureSpecificationViaDirectVolatilityScaling;
import com.andreamazzon.handout9.LIBORMarketModelConstructionWithDynamicsAndMeasureSpecificationViaDirectVolatilityScaling.Dynamics;
import com.andreamazzon.handout9.LIBORMarketModelConstructionWithDynamicsAndMeasureSpecificationViaDirectVolatilityScaling.Measure;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;

/**
 * This class performs a calibration test: we construct a LIBOR market model which
 * produces some target prices of swaptions, and then we calibrate the covariance
 * structure of a LIBOR market model based on these prices. We check the parameters
 * and the differences between the target prices and the prices produced by the calibrated
 * LIBOR Market Model.
 * 
 * @author Andrea Mazzon
 *
 */
public class CalibrationEnhancedTest {

	static final DecimalFormat formatterRealWithTwoDecimalDigits = new DecimalFormat("0.00");
	static final DecimalFormat formatterRealWithFourDecimalDigits = new DecimalFormat(" 0.0000;-0.0000");
	static final DecimalFormat formatterRealPercentage = new DecimalFormat(" 0.00%;");
	
	
	public static void main(String[] args) throws CalculationException {

		System.out.println("Calibration to Swaptions:");
		System.out.println();
		final int numberOfPaths = 50000;// the number of simulated processes

		final double simulationTimeStep = 0.1;//for the SIMULATION discretization
		final double liborPeriodLength = 0.5;//for the TENURE STRUCTURE discretization
		final double liborRateTimeHorizon = 5;
		
		/*
		 * These are the parameters of the LIBOR Market model that we use to calibrate:
		 * will we get something similar after the calibration?
		 */
		final double correlationDecayParameter = 0.3;//alpha such that rho_{i,j}=\exp(-alpha|T_i-T_j|)
		final double a = 0.5, b = 0.7, c = 0.35, d = 0.1; // volatility structure
		/*
		 * The fixing (or maturity) dates for which the initial values of the forwards/Libors are given.
		 * For example, in our case we have the value of L(0.5,1;0), L(1,1.5;0),L(3,3.5;0), L(4,4.5;0),
		 * L(9.5,10;0)
		 */
		final double[] fixingForForwards = { 0.5, 1.0, 3.0, 4.0, liborRateTimeHorizon - liborPeriodLength};
		//times for the forwards: the others will be interpolated (in our case, this is simple :) )
		final double[] forwardsForCurve = { 0.05, 0.05, 0.05, 0.05, 0.05 };


		/*
		 * First step: construction of the calibration products: we have to create a LIBOR market model
		 * and to pass it as a parameter to Calibration.createCalibrationItems
		 */
		final LIBORMonteCarloSimulationFromLIBORModel originalLiborMarketModelSimulation = (LIBORMonteCarloSimulationFromLIBORModel) 
				LIBORMarketModelConstructionWithDynamicsAndMeasureSpecificationViaDirectVolatilityScaling.createLIBORMarketModel(numberOfPaths,
				simulationTimeStep,
				liborPeriodLength, liborRateTimeHorizon,
				fixingForForwards, forwardsForCurve,
				correlationDecayParameter /* Correlation */,
				Dynamics.LOGNORMAL,
				Measure.SPOT,
				a,b,c,d
				);

		//We construct the calibration object. We give the correlation decaying parameter: this will not get calibrated 
		final CalibrationWithSwaptionsEnhanced calibration =
				new CalibrationWithSwaptionsEnhanced(originalLiborMarketModelSimulation, correlationDecayParameter);

		final int numberOfStrikesForTheCalibration = 10;

		//creation of the items: products + target prices given by these products
		calibration.createCalibrationItems(numberOfStrikesForTheCalibration);

		// second step: we calibrate using the constructed LIBOR market model
		final LIBORMarketModel liborMarketModelCalibrated = calibration.swaptionCalibration();

		/*
		 * We get the calibrated parameters by downcasting the covariance of the
		 * calibrated model to the parametric type, and use the getter
		 */
		final double[] parameters = ((AbstractLIBORCovarianceModelParametric)
				liborMarketModelCalibrated.getCovarianceModel()).getParameterAsDouble();
		/*
		 * getter in the AbstractLIBORCovarianceModelParametric class (downcasting),
		 * implemented in the specific covariance model
		 */
		
		System.out.println("Rebonato volatility:");
		System.out.println("a = " + formatterRealWithTwoDecimalDigits.format(parameters[0]));
		System.out.println("b = " + formatterRealWithTwoDecimalDigits.format(parameters[1]));
		System.out.println("c = " + formatterRealWithTwoDecimalDigits.format(parameters[2]));
		System.out.println("d = " + formatterRealWithTwoDecimalDigits.format(parameters[3]));

		System.out.println();

		//System.out.println("Covariance structure: decay parameter = " + formatterRealWithTwoDecimalDigits.format(parameters[4]));
		System.out.println();
		
		/*
		 * third step: we construct the calibrated simulation, linking together the
		 * (calibrated) model and the Euler scheme as usual
		 */
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(
				originalLiborMarketModelSimulation.getTimeDiscretization(),
				originalLiborMarketModelSimulation.getNumberOfComponents(),//no factor reduction for now
				numberOfPaths,
				1897 // seed
				);
		
		final MonteCarloProcess process = new
				EulerSchemeFromProcessModel(liborMarketModelCalibrated, brownianMotion);

		final LIBORMonteCarloSimulationFromLIBORModel calibratedModelSimulator = new
				LIBORMonteCarloSimulationFromLIBORModel(process);


		
		//we need them to produce the prices for these products, given now by the calibrated model
		final CalibrationProduct[] calibrationProducts = calibration.getCalibrationProducts();

		//we also want to print the corresponding strikes, fixings and swaptions tenure structures
		final ArrayList<Double> strikesForSwaptions = calibration.getStrikes();
		final ArrayList<Double> fixingsForSwaptions = calibration.getFixings();
		final ArrayList<ArrayList<Double>> tenureStructuresForSwaptions = calibration.getTenureStructures();

		
		
		//it will be given by calibrationProduct.getProduct()
		AbstractTermStructureMonteCarloProduct derivative;

		System.out.println(" Fixing" + "\t" + "       Tenure structure begin-end" + "\t" + "     Strike"  + "\t" + "        Model price"
		+  "\t" + "      Target price" +  "\t" + "       Percentage Error");

		System.out.println();
		
		int strikeAndSwaptionIndex = 0;
		
		double sumError= 0.0;
		for (final CalibrationProduct calibrationProduct : calibrationProducts) {

			// getter of the i-th calibration product: a Swaption object with a specific strike
			derivative = calibrationProduct.getProduct();
			
			final double strikeForSwaption = strikesForSwaptions.get(strikeAndSwaptionIndex);
			final double fixingForSwaption = fixingsForSwaptions.get(strikeAndSwaptionIndex);
			
			final ArrayList<Double> currentTenureStructure = tenureStructuresForSwaptions.get(strikeAndSwaptionIndex);
			final double firstTimeTenureStructure = currentTenureStructure.get(0);
			final double lastTimeTenureStructure = currentTenureStructure.get(currentTenureStructure.size()-1);
				
			/* 
			 * usual getValue method of an object of type AbstractLIBORMonteCarloProduct: it
			 * gives the Black volatility of the Swaption for the calibrated parameters
			 */
			final double valueModel = derivative.getValue(calibratedModelSimulator);
			// other getter from the arrayList: here we get the target volatility
			final double valueTarget = derivative.getValue(originalLiborMarketModelSimulation);
			// calibration error
			final double diff = Math.abs(valueModel - valueTarget)/valueTarget;
			sumError += diff;
			System.out.println(" " + formatterRealWithTwoDecimalDigits.format(fixingForSwaption) + "\t \t" +
					formatterRealWithTwoDecimalDigits.format(firstTimeTenureStructure) + "-" + 
					formatterRealWithTwoDecimalDigits.format(lastTimeTenureStructure) + "\t \t \t    " +
					formatterRealWithFourDecimalDigits.format(strikeForSwaption) + "\t \t" +
					formatterRealWithFourDecimalDigits.format(valueModel) + "\t \t     " +
					formatterRealWithFourDecimalDigits.format(valueTarget) + "\t \t \t"
					+ formatterRealPercentage.format(diff));
			strikeAndSwaptionIndex ++;
		}
		
		double averageError = sumError / strikesForSwaptions.size();
		
		System.out.println();
		System.out.println("Average percentage error: " + formatterRealPercentage.format(averageError));

	}

}
