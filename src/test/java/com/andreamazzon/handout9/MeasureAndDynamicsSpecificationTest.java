package com.andreamazzon.handout9;

import java.util.function.DoubleUnaryOperator;


import com.andreamazzon.handout9.WrongLIBORMarketModelConstructionWithDynamicsAndMeasureSpecification.Dynamics;
import com.andreamazzon.handout9.WrongLIBORMarketModelConstructionWithDynamicsAndMeasureSpecification.Measure;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.plots.Plot2D;

/**
 * This class tests the implementation of the simulation of a LIBOR Market Model specifying
 * measure (spot or terminal) and dynamics (normal or lognormal). In particular, we focus
 * on caplet prices.
 *
 * @author Andrea Mazzon
 *
 */
public class MeasureAndDynamicsSpecificationTest {


	/**
	 * It plots the prices of a caplet based on a LIBOR market model, both when we consider normal and log-normal dynamics,
	 * for different strikes. Here we consider the simulation of the processes under the terminal measure.
	 *
	 * @throws CalculationException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws CalculationException, InterruptedException {
		
		final int	numberOfPaths	= 50000;

		//parameters for the two time discretizations
		final double simulationTimeStep = 0.1;
		final double LIBORTimeStep = 0.5;
		final double LIBORRateTimeHorizon = 5;

		final double notional = 1000;


		//fixing times for the forwards: the forwards corresponding to other fixing times will be interpolated
		final double[] fixingForGivenForwards = { 0.5, 1.0, 2.0, 3.0};
		final double[] forwardsForCurve = { 0.05, 0.05, 0.05, 0.05};

		final double correlationDecayParameter = 0.5;

		final double a = 0.2, b = 0.1, c = 0.15, d = 0.3; //volatility structure

		//parameters to be given to the constructor of the finmath library Caplet class
		final double maturityOfTheCaplet = 4.0;
		final double periodLengthOfTheCaplet = LIBORTimeStep;

		//parameters for the tests and for the plots
		final double minStrike = 0.025, maxStrike = 0.1;
		
		
		
		String dynamicsString;//it will be used just for the plots

		//we first consider log-normal dynamics, and then normal dynamics (note the values() method of enum types)
		for (final Dynamics typeOfDynamics : Dynamics.values()) {

			final TermStructureMonteCarloSimulationModel liborMarketModelSimulation =
					WrongLIBORMarketModelConstructionWithDynamicsAndMeasureSpecification.createLIBORMarketModel(
							numberOfPaths,
							simulationTimeStep,
							LIBORTimeStep, //T_i-T_{i-1}, we suppose it to be fixed
							LIBORRateTimeHorizon, //T_n
							fixingForGivenForwards,
							forwardsForCurve,
							correlationDecayParameter, // decay of the correlation between LIBOR rates
							typeOfDynamics, //first log-normal dynamics then normal dynamics
							Measure.TERMINAL, //we specify that we consider the dynamics under the terminal measure
							a, b, c, d
							);

			DoubleUnaryOperator capletFunction = (strike) -> {
				//we use the Finmath library implementation
				final TermStructureMonteCarloProduct caplet = new Caplet(maturityOfTheCaplet, periodLengthOfTheCaplet, strike);
				/*
				 * The getValue method might give a CalculationException. It looks like the only option here
				 * is to use the try..catch way. Why?
				 */
				try {
						//we get the price with usual getValue method (and we multiply by the notional)
						return notional * caplet.getValue(liborMarketModelSimulation);
					} catch (CalculationException e) {
						e.printStackTrace();
						return 0.0;
					}
			};

			Plot2D myPlot = new Plot2D(minStrike, maxStrike, 100, capletFunction);

			//now we want to plot the prices, with the appropriate titles according to the type of the dynamics
			dynamicsString = (typeOfDynamics == Dynamics.LOGNORMAL) ? "log-normal" : "normal";


			myPlot.setTitle("Caplet price using " + dynamicsString + " dynamics");
			myPlot.setXAxisLabel("strike");
			myPlot.setYAxisLabel("price");
			myPlot.show();
			Thread.sleep(1000);
		}
	}

}
