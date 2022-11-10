package com.andreamazzon.handout3;

import static org.junit.jupiter.api.Assertions.*;

import java.text.DecimalFormat;

import org.junit.jupiter.api.Test;

/**
 * This is a test class, checking if the swap rate computed by the class
 * SwapImplementation is such that the value of the swap contract associated to
 * the computed swap rate is zero.
 *
 * @author Andrea Mazzon
 */

class SwapImplementationTest {

	@Test
	void testSwapRate() {
		final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");
		
		final double yearFraction = 0.5;

		final double[] zeroCouponBondCurve = { 0.9986509108, 0.9949129829, 0.9897033769, 0.9835370208, 0.9765298116,
				0.9689909565 };

		// we give the zero coupon bond curve: that's why the boolean is set to be true
		final Swap swapCalculator = new SwapImplementation(yearFraction, zeroCouponBondCurve, true);

		final double parSwapRate = swapCalculator
				.getParSwapRate(yearFraction/* in this way, we compute it more efficiently */);

		System.out.println("The par swap rate is " + printNumberWithFourDecimalDigits.format(parSwapRate));

		final double swapValue = swapCalculator.getSwapValue(parSwapRate, yearFraction);
		System.out.println("The value of the swap for the par swap rate is " + swapValue);

		final double tolerance = 1E-15;

		
		// We test if this value is zero, with a tolerance equal to the machine precision
		assertEquals(0, swapValue, tolerance);

		// now we do the same for a tenure structure whose times steps are not constant
		final double[] times = { 0.5, 1, 1.5, 2, 3, 3.5 };
		final Swap newSwapCalculator = new SwapImplementation(times, zeroCouponBondCurve, true);

		final double newParSwapRate = newSwapCalculator.getParSwapRate();
		System.out.println("The new par swap rate is " + printNumberWithFourDecimalDigits.format(newParSwapRate));

		final double newSwapValue = newSwapCalculator.getSwapValue(newParSwapRate);
		System.out.println("The value of the new swap for the par swap rate is " + newSwapValue);

		assertEquals(0, newSwapValue, tolerance);
		}

}
