package com.andreamazzon.handout2;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * The main goal of this class is to handle the conversion from a zero coupon
 * bond curve to a Libor rate curve and vice-versa. In particular, we have one
 * method that converts a zero coupon bond curve (given as an array of doubles)
 * in a Libor rate curve with same tenure structure, and returns the Libor rate
 * curve again as an array of doubles, and one method that does the opposite.
 * Note that here the tenure structure is supposed to be the same for both the
 * methods, and is given as a TimeDiscretization object. Because of this, we
 * make it a field of the class.
 *
 * @author Andrea Mazzon
 *
 */
public class BondsAndLibors {

	/*
	 * Note: it is used by both the methods, so it's the same for both the methods.
	 * It is given in the constructor below.
	 */
	private TimeDiscretization tenureStructure;

	/**
	 * Public constructor setting the tenure structure as a TimeDiscretization
	 * object
	 *
	 * @param tenureStructure, an object of type TimeDiscretization, representing
	 *                         the tenure structure of the curves.
	 */
	public BondsAndLibors(TimeDiscretization tenureStructure) {
		this.tenureStructure = tenureStructure;
	}

	/**
	 * It converts a zero coupon bond curve in a Libor curve, and returns the latter
	 * as an array of doubles
	 *
	 * @param bonds, the zero coupon bond curve given as an array of doubles. The
	 *               first element of the array is P(T_1;0).
	 * @return the array of doubles representing the Libor curve. The first element
	 *         of the array is L(0,T_1;0).
	 */
	public double[] fromBondsToLibors(double[] bonds) {
		//In the following, we use Definition 142 of the script (page 60)
		
		// We need it to be able to construct the array containing the values of the libors
		final int curveLength = bonds.length;
		final double[] derivedLiborsCurve = new double[curveLength];// vector that will store the libors
		final double firstBond = 1.0;// P(0;0)=1, we use it to calculate L(0,T_1;0)
		final double secondBond = bonds[0];
		// L(0,T_1;0)=1/T_1*(P(0;0)-P(T_1;0))/P(T_1;0) (since T_0=0)
		derivedLiborsCurve[0] = (firstBond - secondBond) / (secondBond * tenureStructure.getTime(0));
		for (int periodIndex = 1; periodIndex < curveLength; periodIndex++) {
			// L(T_{i-1},T_i;0)=1/(T_i-T_{i-1})(P(T_{i-1};0)-P(T_i;0))/P(T_i;0)
			derivedLiborsCurve[periodIndex] = (bonds[periodIndex - 1] - bonds[periodIndex])
					/ (bonds[periodIndex] * tenureStructure.getTimeStep(periodIndex - 1));
		}
		return derivedLiborsCurve;
	}

	/**
	 * It converts a Libor curve in a zero coupon bond curve, and returns the latter
	 * as an array of doubles
	 *
	 * @param libors, the Libors curve given as an array of doubles. The first
	 *                element of the array is L(0,T_1;0).
	 * @return the array of doubles representing the zero coupon bond curve. The
	 *         first element of the array is P(T_1;0).
	 */
	public double[] fromLiborsToBonds(double[] libors) {
		//In the following, we use Definition 142 of the script (page 60)

		/*
		 * We need it to be able to construct the array containing the values of the
		 * zero coupon bonds
		 */
		final int curveLength = libors.length;
		final double[] derivedBondsCurve = new double[curveLength];// vector that will store the zero coupon bond curve
		final double firstBond = 1.0;// P(0;0)=1, we use it to calculate P(0;T_1) from L(0,T_1;0)
		double lastLibor = libors[0];
		double timeStep = tenureStructure.getTime(0);
		// P(T_1;0) = P(0;0)/(1+L(0,T_1;0)*T_1) (since T_0 = 0)
		derivedBondsCurve[0] = firstBond / (1 + lastLibor * timeStep);
		double lastComputedBond;
		for (int periodIndex = 1; periodIndex < curveLength; periodIndex++) {
			timeStep = tenureStructure.getTimeStep(periodIndex - 1);
			lastLibor = libors[periodIndex];// L(T_{i-1},T_i;0)
			lastComputedBond = derivedBondsCurve[periodIndex - 1];
			// P(T_i;0) = P(T_{i-1};0)/(1+L(T_{i-1},T_i;0)*(T_i-T_{i-1}))
			derivedBondsCurve[periodIndex] = lastComputedBond / (1 + lastLibor * timeStep);

		}
		return derivedBondsCurve;
	}

	/*
	 * We want now to do a small test to check if we did everything fine: of course,
	 * if we convert from bonds to libors and then back to bonds, we have to get the
	 * original curve
	 */
	public static void main(String[] args) throws CalculationException {
		final double[] bonds = { 0.98, 0.975, 0.97, 0.965, 0.959, 0.954, 0.95, 0.945 };

		// time discretization parameters
		double initialTime = 0.5;
		int numberOfTimeSteps = 7;
		double timeStep = 0.5;
		TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		BondsAndLibors converter = new BondsAndLibors(times);
		final double[] libors = converter.fromBondsToLibors(bonds);
		
		// time discretization parameters
		initialTime = 0.5;
		numberOfTimeSteps = 7;
		timeStep = 0.7;
				
		final TimeDiscretization newTimes = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		converter.tenureStructure = newTimes;
		
		final double[] newBonds = converter.fromLiborsToBonds(libors);
		// note how to print an array of doubles
		System.out.println("The new bonds are " + Arrays.toString(newBonds));
	}

}
