package com.andreamazzon.handout3;

import java.util.Arrays;

import com.andreamazzon.handout2.BondsAndLibors;

import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class represents a swap for a given tenure structure and a curve, might
 * it be of zero coupon bonds or Libor rates. The tenure structure (as a
 * TimeDiscretization object) and the curve are given in the constructor, along
 * with a boolean value specifying if the given curve is a zero coupon bond
 * curve or not. It is also possible, in the case when the time step of the
 * tenure structure is constant, to only give the time step and not all the
 * tenure structure. Methods are implemented that allow to compute the value of
 * the swap for given swap rates (or only for one swap rate) and the par swap
 * rate. In both cases, if the time step of the tenure structure is constant, an
 * overloaded, more efficient version of the methods is provided.
 *
 * @author Andrea Mazzon
 */

public class SwapImplementation implements Swap {

	private final TimeDiscretization swapDates; // tenure structure, starting from T_1
	/*
	 * We only work with the zero coupon bond curve. This is possible since we can
	 * always express a Libor rate as a function of two zero coupon bonds. However,
	 * we want to allow the user to give us a Libor rate curve. Then we implement a
	 * PRIVATE method, liborToBondCurve, that converts the Libor curve to the zcb
	 * curve. We call this method directly inside the constructor, if the user
	 * specifies that he/she is giving the Libor rate curve, and we store the
	 * resulting vector in the field zeroBondCurve. This is a clear example of
	 * ENCAPSULATION! Do you remember the examples in the Java course, when we
	 * wanted to convert from Fahrenheit to Kelvin, from miles to km, etc? This is
	 * the same idea.
	 */
	private final double[] zeroBondCurve;
	private final int curveLength;

	/**
	 * It builds an object of type SwapImplementation, with the option of giving a
	 * zero coupon bond curve or a Libor curve.
	 *
	 * @param swapDates:   the tenure structure, given as a TimeDiscretization
	 *                     object
	 * @param curve:       the curve the user wants to give: it is represented by a
	 *                     vector of doubles, and can be the zero coupon bond curve
	 *                     or the Libor curve
	 * @param isBondCurve: a boolean object, whose value is True if the curve given
	 *                     by the user is a zero coupon bond curve, false if it is a
	 *                     Libor curve
	 */
	public SwapImplementation(TimeDiscretization swapDates, double[] curve,
			Boolean isBondCurve/* if false, we convert from Libors to bonds */) {

		this.swapDates = swapDates;
		curveLength = curve.length;
		/*
		 * If isBondCurve is True, we leave the vector as it is, if it is False, we
		 * convert to bonds
		 */
		zeroBondCurve = isBondCurve ? curve : liborToBondCurve(curve);
	}

	/**
	 * It builds an object of type SwapWithoutFinmath, with the option of giving a
	 * zero coupon bond curve or a Libor curve, in the case when the user wants to
	 * give the tenure structure not as a TimeDiscretization but as an array of
	 * doubles.
	 *
	 * @param swapDates:   the tenure structure, given as an array of doubles
	 * @param curve:       the curve the user wants to give: it is represented by a
	 *                     vector of doubles, and can be the zero coupon bond curve
	 *                     or the Libor curve
	 * @param isBondCurve: a boolean object, whose value is True if the curve given
	 *                     by the user is a zero coupon bond curve, false if it is a
	 *                     Libor curve
	 */
	public SwapImplementation(double[] times, double[] curve,
			Boolean isBondCurve/* if false, we convert from Libors to bonds */) {
		this(new TimeDiscretizationFromArray(times), curve, isBondCurve);
	}

	/**
	 * It builds an object of type SwapWithoutFinmath, with the option of giving a
	 * zero coupon bond curve or a Libor curve, in the case when all the time steps
	 * of the tenure structure have same length.
	 *
	 * @param swapDates:   the length of the step from one swap date to the other
	 * @param curve:       the curve the user wants to give: it is represented by a
	 *                     vector of doubles, and can be the zero coupon bond curve
	 *                     or the Libor curve
	 * @param isBondCurve: a boolean object, whose value is True if the curve given
	 *                     by the user is a zero coupon bond curve, false if it is a
	 *                     Libor curve
	 */
	public SwapImplementation(double yearFraction, double[] curve,
			Boolean isBondCurve/* if false, we convert from Libors to bonds */) {
		// the tenure structure must have same length as the curve
		this(new TimeDiscretizationFromArray(yearFraction/* first time */, curve.length - 1/* number of time steps */,
				yearFraction/* time step */), curve, isBondCurve);

	}

	/*
	 * This methods converts Libor rates into a zero coupon bond curve: the idea is
	 * that if we know P(T_{i};t) and L(T_i,T_{i+1},;t), we can infer P(T_{i};t).
	 * Then we can proceed iteratively starting from P(0;0)=1
	 */
	private double[] liborToBondCurve(double[] libors) {

		BondsAndLibors converter = new BondsAndLibors(swapDates);

		return converter.fromLiborsToBonds(libors);
		
		/*
		 * Note here how is convenient to "delegate" the implementation of a method
		 * to an object of an already existing class: we save all the lines of code
		 * you see below! 
		 */

//		final double[] derivedBondsCurve = new double[curveLength];// vector that will store the zero coupon bond curve
//		final double firstBond = 1.0;// P(0;0)=1, we use it to calculate P(0;T_1) from L(0,T_1;0)
//		double currentLibor = libors[0];
//		double timeStep = swapDates.getTimeStep(0);
//		// P(T_1;0) = P(0;0)/(1+L(0,T_1;0)*T_1 (since T_0 = 0)
//		derivedBondsCurve[0] = firstBond / (1 + currentLibor * timeStep);
//		for (int periodIndex = 1; periodIndex < curveLength; periodIndex++) {
//			timeStep = swapDates.getTimeStep(periodIndex);
//			currentLibor = libors[periodIndex];// L(T_{i-1},T_i;0)
//			// P(T_i;0) = P(T_{i-1};0)/(1+L(T_{i-1},T_i;0)*(T_i-T_{i-1})
//			derivedBondsCurve[periodIndex] = derivedBondsCurve[periodIndex - 1] / (1 + currentLibor * timeStep);
//		}
//		return derivedBondsCurve;
	}

	// This method computes and returns the annuity
	private double getAnnuity() {
		double annuity = 0.0;
		// we compute the sum of the bonds multiplied by the time intervals
		for (int couponIndex = 1; couponIndex < curveLength; couponIndex++) {
			// remember that zeroBondCurve[1]=P(T_2;0), ... ,zeroBondCurve[n-1]=P(T_n;0)
			annuity += zeroBondCurve[couponIndex] * swapDates.getTimeStep(couponIndex - 1);
		}
		return annuity;
	}

	// This method computes the annuity when the swap dates are evenly distributed
	private double getAnnuity(double yearFraction) {
		double annuity = 0.0;
		// we first compute the sum of the bonds..
		for (int couponIndex = 1; couponIndex < curveLength; couponIndex++) {
			annuity += zeroBondCurve[couponIndex];
		}
		// ..and then we multiply it by the length of the time intervals (constant here)
		return annuity * yearFraction;
	}

	/*
	 * Note, here and in the methods below, that the Javadoc documentation has been
	 * already given in the interface
	 */
	@Override
	public double getParSwapRate() {
		final double annuity = getAnnuity();
		
		// Computation of the swap rate: look the last equation of Remark 173		
		return (zeroBondCurve[0] - zeroBondCurve[curveLength - 1]) / annuity;
	}

	@Override
	public double getParSwapRate(double yearFraction) {
		final double annuity = getAnnuity(yearFraction);
		// computation of the swap rate
		return (zeroBondCurve[0] - zeroBondCurve[curveLength - 1]) / annuity;
	}

	@Override
	public double getSwapValue(double[] swapRates) {

		/*
		 * We compute the value of the swap as the sum of the value of the floating legs
		 * minus the sum of the value of the fixed legs. We use here Definition 142 to
		 * express L(T_i,L(T_{i+1}) as a function of P(T_i) and P(T_{i+1}), and then
		 * Theorem 172 to get the value of the swap
		 */
		final double sumOfFloatingLegs = zeroBondCurve[0] - zeroBondCurve[curveLength - 1];

		double sumOfFixedLegs = 0.0;

		for (int couponIndex = 0; couponIndex < curveLength - 1; couponIndex++) {
			sumOfFixedLegs += swapRates[couponIndex] * zeroBondCurve[couponIndex + 1]
					* swapDates.getTimeStep(couponIndex);
		}
		return sumOfFloatingLegs - sumOfFixedLegs;
	}

	@Override
	public double getSwapValue(double[] swapRates, double yearFraction) {

		/*
		 * We compute the value of the swap as the sum of the value of the floating legs
		 * minus the sum of the value of the fixed legs. We use here Definition 142 to
		 * express L(T_i,L(T_{i+1}) as a function of P(T_i) and P(T_{i+1}), and then
		 * Theorem 172 to get the value of the swap
		 */
		final double sumOfFloatingLegs = zeroBondCurve[0] - zeroBondCurve[curveLength - 1];

		double sumOfFixedLegs = 0.0;

		/*
		 * we now exploit the fact that the time step for the tenure structure is
		 * constant
		 */
		for (int couponIndex = 0; couponIndex < curveLength - 1; couponIndex++) {
			sumOfFixedLegs += swapRates[couponIndex] * zeroBondCurve[couponIndex + 1];
		}
		sumOfFixedLegs *= yearFraction;// that is, sumOfFixedLegs = sumOfFixedLegs * yearFraction
		return sumOfFloatingLegs - sumOfFixedLegs;
	}

	// case when the user gives just one swap rate
	@Override
	public double getSwapValue(double singleSwapRate) {
		final double[] swapRates = new double[curveLength];
		Arrays.fill(swapRates, singleSwapRate);// swapRates is now an array with all elements equal to singleSwapRate
		return getSwapValue(swapRates);
	}

	@Override
	public double getSwapValue(double singleSwapRate, double yearFraction) {
		final double[] swapRates = new double[curveLength];
		Arrays.fill(swapRates, singleSwapRate);// swapRates is now an array with all elements equal to singleSwapRate
		return getSwapValue(swapRates, yearFraction);
	}
}