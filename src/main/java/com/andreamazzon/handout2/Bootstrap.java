package com.andreamazzon.handout2;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import net.finmath.exception.CalculationException;

/**
 * This class bootstraps the zero coupon bond curve from the values of coupon
 * bonds: the idea is that you take the formula for the value of a coupon bond
 * (see Theorem 158 of the script) and you use it -knowing the value of the
 * associated coupons C_i- to determine the value of one bond, also knowing
 * the value of the other bonds. Here we suppose that the time step of the tenure
 * structure is constant. The class is assumed to be used in the following way:
 * first, one constructs an object giving the value of the first coupon bond and
 * of the first coupon. Then the method nextBondFromCouponBond gets called
 * iteratively, so that the bootstrapped bonds are stored in a List and used
 * again to compute the new bonds. Then the bonds can be got by calling the
 * method getBonds.
 *
 * @author: Andrea Mazzon
 */

public class Bootstrap {

	/*
	 * A list is basically a more flexible array: you don't have to set the length
	 * from the beginning, but you instead simply append an element after the other.
	 * Here we use it because we don't know how many bonds "the user" wants to get,
	 * and we don't want him/her to specify this information at the beginning. It will
	 * become clear just from how many times the method nextBondFromCouponBond gets called.
	 */
	private final List<Double> computedBonds = new ArrayList<Double>();
	private final double yearFraction;// the constant value T_{i+1}-T_i

	/*
	 * We use it in order to host the sum of the elements
	 * y*C_i*P(T_{i+1};0) from 1 to m-1. This will be used to get the
	 * value of P(T_{i+1};0). We want it to be updated every time we get a new bond
	 */
	private double sumOfProductTimeStepBondsAndCoupons;


	/**
	 * It constructs a new object to iteratively compute the value of zero coupon
	 * bonds from the values of coupon bonds and coupons.
	 *
	 * @param yearFraction,         T_2-T_1
	 * @param valueFirstCoupon,     C_1
	 * @param valueFirstCouponBond, (T_2-T_1)*C_1*P(T_2;0) + P(T_2;0)
	 */
	public Bootstrap(double yearFraction, double valueFirstCoupon, double valueFirstCouponBond) {

		Double valueFirstBond = valueFirstCouponBond / (1 + yearFraction * valueFirstCoupon);
		computedBonds.add(valueFirstBond);// we append the first element to our list
		// the sum is initialized. Note: P(T_1;0) is not included!
		sumOfProductTimeStepBondsAndCoupons = valueFirstBond * valueFirstCoupon * yearFraction;
		this.yearFraction = yearFraction;
	}

	/**
	 * Computes a new bond from the previously computed ones and from the new coupon
	 * bond. Internally, it also adds the new bond to the bond list and updates the
	 * sum
	 *
	 * @param valueNewCouponBond the new coupon bond given by the user (for example, the
	 * 		  first time the method is called this is 
	 * 		  (T_2-T_1)*C_1*P(T_2;0)+(T_3-T_2)*C_2*P(T_3;0) + P(T_3;0)
	 * @param valueNewCoupon the new coupon given by the user (for example, the
	 * 		  first time the method is called this is C_2)
	 */
	public void nextBondFromCouponBond(double valueNewCouponBond, double valueNewCoupon) {
		/*
		 * We write 
		 * CB_m= \sum_{i=1}^{m-2}C_i(T_{i+1}-T_i)P(T_{i+1};0)+C_{m-1}(T_m-T_{m-1})P(T_m;0)+P(T_m;0),
		 * where the first element is equal to sumOfProductTimeStepBondsAndCoupons
		 * so:
		 * CB_m - \sum_{i=1}^{m-2}C_i(T_{i+1}-T_i)P(T_{i+1};0)= C_{m-1}(T_m-T_{m-1})P(T_m;0)+P(T_m;0)
		 * P(T_m;0) = (CB_m - \sum_{i=1}^{m-2}C_i(T_{i+1}-T_i)P(T_{i+1};0))/(1+C_{m-1}(T_m-T_{m-1}))
		 */
		Double valueNewBond = (valueNewCouponBond - sumOfProductTimeStepBondsAndCoupons)
				/ (1 + yearFraction * valueNewCoupon);
		sumOfProductTimeStepBondsAndCoupons += valueNewBond * valueNewCoupon * yearFraction;// note: the sum is updated!
		computedBonds.add(valueNewBond);
	}

	/**
	 * It returns all the bonds bootstrapped (at the moment when the method is
	 * called) from the coupon bonds
	 *
	 * @return
	 */
	public List<Double> getBonds() {
		return computedBonds;
	}

	public static void main(String[] args) throws CalculationException {
		final DecimalFormat printNumberWithFourDecimalDigits = new DecimalFormat("0.0000");

		// these are the vales of the coupon bonds we will give, one by one
		final double[] couponBonds = {1.93, 2.77, 3.55, 4.45, 5.2, 5.9, 6.55, 7.15};
		// these are the vales of the coupons we will give, one by one
		final double[] coupons = {2.1, 1.9, 1.8, 2.2, 2.1, 1.95, 2, 2.05};
		final double yearFraction = 0.5;// the constant T_{i+1}-T_i

		final Bootstrap bootstrap = new Bootstrap(yearFraction, coupons[0], couponBonds[0]);

		final double curveLength = couponBonds.length;

		/*
		 * Now we call the nextBondFromCouponBond iteratively, to bootstrap the bonds
		 * and use them to compute the next ones
		 */
		for (int couponBondIndex = 1; couponBondIndex < curveLength; couponBondIndex++) {
			bootstrap.nextBondFromCouponBond(couponBonds[couponBondIndex], coupons[couponBondIndex]);
		}

		final List<Double> computedBonds = bootstrap.getBonds();


		// We print the value of the bonds
		for (int i = 0; i < curveLength; i++) {
			System.out.println("The value of the time " + yearFraction * (i + 1) + " bond is : "
					+ printNumberWithFourDecimalDigits.format(computedBonds.get(i)));
		}
	}
}
