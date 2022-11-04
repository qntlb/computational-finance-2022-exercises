package com.andreamazzon.handout2;

import java.util.Arrays;

import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class TestLiborsAndBonds {

	public static void main(String[] args) {
		final double[] bonds = { 0.98, 0.975, 0.97, 0.965, 0.959, 0.954, 0.95, 0.945 };

		// time discretization parameters
		double initialTime = 0.5;
		int numberOfTimeSteps = 7;
		double timeStep = 0.5;
		TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		BondsAndLibors converter = new BondsAndLibors(times);
		final double[] libors = converter.fromBondsToLibors(bonds);
		
		
		/*
		 * try to uncomment the following  lines: since tenureStructure is private, an error message is returned:
		 * this is good according to the encapsulation principle (we could get unwanted results)
		 */
//		initialTime = 0.5;
//		numberOfTimeSteps = 7;
//		timeStep = 0.7;
		
//		final TimeDiscretization newTimes = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);
//
//		converter.tenureStructure = newTimes;
//		
		final double[] newBonds = converter.fromLiborsToBonds(libors);
//		// note how to print an array of doubles
		System.out.println("The new bonds are " + Arrays.toString(newBonds));
	}

}
