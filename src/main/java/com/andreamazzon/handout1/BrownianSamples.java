package com.andreamazzon.handout1;

import java.text.DecimalFormat;
import java.util.Random;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class has a main method whose goal is to compute some statistics about
 * the average of the values of a Brownian motion at a given point in time: in
 * order to do that, we first simulate trajectories of a Brownian motion for a given
 * seed using the implementation of the Finmath library and we compute the average,
 * according to the Monte-Carlo approach. Then, we repeat the experiment several times 
 * randomly changing the seed. All the values of the average that we get in this way
 * form an array of doubles. This array gets then wrapped into a RandomVariable object,
 * in order to compute average, variance, maximum and minimum using the methods
 * implemented in the Finmath library.
 *
 * @author Andrea Mazzon
 *
 */
public class BrownianSamples {
	


	public static void main(String[] args) throws CalculationException {

		//this helps to print choosing the number of decimal digits we want to be shown
		final DecimalFormat printWithFourDecimalDigits = new DecimalFormat("0.0000");
		
		/*
		 * This is a Java class that we use here in order to get random integer numbers
		 * that will represent the seeds.
		 */
		final Random randomGenerator = new Random();
		final int numberOfAverageComputations = 1000;

		/*
		 * It is supposed to contain all the averages for given seeds. It gets then
		 * wrapped into a RandomVariable.
		 */
		final double[] vectorOfAverages = new double[numberOfAverageComputations];

		// time discretization parameters
		final double initialTime = 0;
		final double finalTime = 1.0;
		final int numberOfTimeSteps = 100;
		final double timeStep = finalTime / numberOfTimeSteps;
		
		final TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);
		
		// simulation parameter: the number of paths simulated for every experiment
		final int numberOfSimulations = 100000;

		//an object of type "BrownianMotion"
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(times, 1, numberOfSimulations, 
				1897 // the seed that is needed to generate the Mersenne random numbers
		);

		//this will get updated with the values of the increments
		RandomVariable brownianIncrement;
		
		
		//this will get updated with the values of the Brownian motion itself
		RandomVariable brownianMotionCurrentValue = new RandomVariableFromDoubleArray(0.0 /* the time */,
				0.0 /* the value */);

		/*
		 * We now construct the Brownian motion time by time, getting the increment and
		 * summing it (omega-wise) to the last realization of the Brownian motion
		 */
		for (int timeIndex = 1; timeIndex < numberOfTimeSteps + 1; timeIndex++) {

			brownianIncrement = brownianMotion.getBrownianIncrement(timeIndex - 1, 0);

			// B_(t_i)=B_(t_(i-1))+(B_(t_(i))-B_(t_(i-1)))
			brownianMotionCurrentValue = brownianMotionCurrentValue.add(brownianIncrement);
		}

		final double average = brownianMotionCurrentValue.getAverage();
		
		System.out.println("Average of the first simulated Brownian motions: " + printWithFourDecimalDigits.format(average));
		System.out.println();
		
		// first entry of the array: the average of this sample of simulations
		vectorOfAverages[0] = average;
		
		
		BrownianMotion brownianMotionWithModifiedSeed;

		
		// Now we get all the averages for all the random seeds. We store them in the array	
		int seed;// it's better to create it once for all here, outside the for loop.

		for (int i = 1; i < numberOfAverageComputations; i++) {
			seed = randomGenerator.nextInt();// random int
			/*
			 * Note here the getCloneWithModifiedSeed method: we don't have to bother
			 * constructing the object from scratch as before
			 */
			brownianMotionWithModifiedSeed = brownianMotion.getCloneWithModifiedSeed(seed);

			RandomVariable brownianMotionWithModifiedSeedCurrentValue = new RandomVariableFromDoubleArray(
					0.0 /* the time */, 0.0 /* the value */);
			;
			for (int timeIndex = 1; timeIndex < numberOfTimeSteps + 1; timeIndex++) {

				brownianIncrement = brownianMotionWithModifiedSeed.getBrownianIncrement(timeIndex - 1, 0);
				// B_(t_i)=B_(t_(i-1))+(B_(t_(i))-B_(t_(i-1)))
				brownianMotionWithModifiedSeedCurrentValue = brownianMotionWithModifiedSeedCurrentValue
						.add(brownianIncrement);

			}
			vectorOfAverages[i] = brownianMotionWithModifiedSeedCurrentValue.getAverage();// we store the price
		}

		/*
		 * Now we wrap the array into one object of type RandomVariable. There are
		 * multiple ways to do this, here you can see two
		 */
		final RandomVariable priceRandomVariable = new RandomVariableFromDoubleArray(0.0, vectorOfAverages);
		// or:
		// final RandomVariable priceRandomVariable = (new
		// RandomVariableFromArrayFactory()).createRandomVariable(0.0, vectorOfPrices);

		System.out.println("Average of the averages: " + printWithFourDecimalDigits.format(priceRandomVariable.getAverage()));
		System.out.println("Variance of the averages: " + printWithFourDecimalDigits.format(priceRandomVariable.getVariance()));
		System.out.println("Min average: " + printWithFourDecimalDigits.format(priceRandomVariable.getMin()));
		System.out.println("Max average: " + printWithFourDecimalDigits.format(priceRandomVariable.getMax()));
		
		ProcessModel bachelierWhichIsBrownianMotion = new BachelierModel(0,0,1);
		
		MonteCarloAssetModel simulator = new MonteCarloAssetModel(bachelierWhichIsBrownianMotion, brownianMotion);
		double averageOfBachelierBrownianMotion = simulator.getAssetValue(finalTime, 0).getAverage();
		System.out.println("Average of the first simulated Brownian motions: " + printWithFourDecimalDigits.format(average));
	}

}