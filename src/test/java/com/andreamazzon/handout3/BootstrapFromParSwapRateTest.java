package com.andreamazzon.handout3;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Here we "test" the class BootstrapFromParSwapRate by giving to its
 * constructor two initial bonds along with a time step (semi-annual). We then
 * iteratively call the methods nextBondFromParSwapRate and
 * nextTwoBondsFromParSwapRate: first we have semi-annual par swap rates, then
 * only annual swap rates.
 *
 * @author Andrea Mazzon
 *
 */
public class BootstrapFromParSwapRateTest {

	public static void main(String[] args) {
		final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");
		final double[] firstBonds = { 0.98, 0.975 };
		final double yearFraction = 0.5;

		// we give the two initial bonds and the step of the tenure structure
		final BootstrapFromParSwapRate bootstrap = new BootstrapFromParSwapRate(firstBonds[0], firstBonds[1],
				yearFraction);

		final double[] semiAnnualSwapRates = { 0.0086, 0.0077, 0.0073, 0.0084 };

		// here every par swap rate is known
		for (final double semiAnnualSwapRate : semiAnnualSwapRates) {
			bootstrap.nextBondFromParSwapRate(semiAnnualSwapRate);
		}

		final double[] annualSwapRates = { 0.0075, 0.0085, 0.0095, 0.0092 };

		// now only the par swap rates for yearly maturities are given
		for (final double annualSwapRate : annualSwapRates) {
			bootstrap.nextTwoBondsFromParSwapRate(annualSwapRate);
		}

		final ArrayList<Double> computedBonds = bootstrap.getBonds();

		System.out.println("Computed bonds: \n");

		// print the value of the bonds
		for (int i = 0; i < computedBonds.size(); i++) {
			System.out.println("The value of the time " + yearFraction * (i + 1) + " bond is : "
					+ printNumberWithFourDecimalDigits.format(computedBonds.get(i)));
		}
	}

}
