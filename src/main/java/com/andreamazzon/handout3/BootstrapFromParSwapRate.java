package com.andreamazzon.handout3;

import java.util.ArrayList;

import net.finmath.rootfinder.BisectionSearch;

/**
 * This class implements the zero coupon bond curve bootstrapping from the
 * values of par swap rates: the idea is that you take the formula for the par
 * swap rate and you use it -knowing the value of the swap rate in the market-
 * to determine the value of one or two bonds, also knowing the value of the
 * other bonds. Here we suppose that the time step of the tenure structure is
 * constant. If some values of the swap rate are missing (for example, if we
 * have annual swap rates and semi-annual payments) a root finder algorithm is
 * used in order to bootstrap the curve, together with a linear interpolation of
 * the logarithm of the bonds.
 *
 * @author: Andrea Mazzon
 */
public class BootstrapFromParSwapRate {

	private final ArrayList<Double> computedBonds = new ArrayList<Double>();
	/*
	 * we use it in order to compute the bootstrapped bonds. We want it to be
	 * updated every time we get a new bond (or two new bonds)
	 */
	private double sumOfComputedBonds;

	private int numberOfComputedBonds;// it is used in nextTwoBondsFromParSwapRate

	private final double yearFraction;

	private final double firstBond;

	public BootstrapFromParSwapRate(double firstBond, double secondBond, double yearFraction) {
		computedBonds.add(firstBond);// the first two bonds are given
		computedBonds.add(secondBond);
		numberOfComputedBonds = 2;
		sumOfComputedBonds = secondBond;// the sum is initialized. Note: the first bond is not included!
		this.yearFraction = yearFraction;
		this.firstBond = firstBond;
	}

	/**
	 * Computes a new bond from the previously computed ones and from the par swap
	 * rate: look at the form of the swap rate in the script. Internally, it also
	 * adds the new bond to the bond list and updates the sum
	 *
	 * @param parSwapRate, the par swap rate for the given period
	 */
	public void nextBondFromParSwapRate(double parSwapRate) {
		final double newBond = (firstBond - yearFraction * parSwapRate * sumOfComputedBonds) / (1 + parSwapRate * yearFraction);
		sumOfComputedBonds += newBond;// note: the sum is updated!
		computedBonds.add(newBond);
		numberOfComputedBonds++;
	}

	/**
	 * Here it is assumed that the value of one par swap rate is missing, so that
	 * the values of two bonds have to be computed (for example, this is the case of
	 * semi-annual payments when only annual swap rates are given). In practice, you
	 * know the bonds until time T_{k-2}, and you know the par swap rate S_k at time
	 * T_k. You have to compute P(T_{k-1}) and P(T_k). In order to do this, a root
	 * finder algorithm is used, together with an interpolation for the second last
	 * bond (that is, P(T_{k-1}) in the example above): the objective function given
	 * to the root finder algorithm is the difference between the given par swap rate
	 * S_k and the one computed when the two missing bonds are the one computed at the
	 * present iteration and the bond given by interpolation.
	 *
	 * @param swapRate,  the par swap rate for the given period
	 * @param tolerance, the tolerance to be given to the root finder algorithm: the
	 *                   algorithm gives us x_n as a root of our function if
	 *                   |x_n-x_{n-1}| < tolerance, at the n-th iteration
	 */
	public void nextTwoBondsFromParSwapRate(double swapRate) {

		final double lastBond = computedBonds.get(numberOfComputedBonds - 1);// P(T_{k-2};0)
		//or
		//final double lastBond = computedBonds.get(computedBonds.size() - 1);// P(T_{k-2};0)

		/*
		 * We want to find x such that the theoretical value of the par swap rate equals
		 * the given vale of the swapRate, i.e., such that
		 * (P(T_1;0)-x))/(yearFraction*(\sum_{k=2}^{n-2}P(T_k;0)+f(P(T_{n-2}),x))+x)-parSwapRate=0
		 * We use the BisectionSearch class of the Finmath library. It can
		 * be used to find the zero of monotone functions on some interval, whose
		 * extremes are given in the constructor of the class. Here we know that the
		 * value of the bond has to be positive, but smaller than the value of the last
		 * computed bond (as it has an higher maturity)
		 */
		final BisectionSearch rootFinder = new BisectionSearch(0.0001, // left point of the interval where we search
				lastBond// right point.
		);

		/*
		 * look at the class in the Finmath library: what is contained in the while is a
		 * Boolean which is True when the points are close enough
		 */
		while (!rootFinder.isDone()) {
			/*
			 * next "try" to get the value of the new bond by which the difference of the
			 * par swap rate is close to zero
			 */
			final double x = rootFinder.getNextPoint();
			
			/*
			 * value of the difference between the (observed) parSwapRate and 
			 * (P(T_1;0)-x))/(Delta*(\sum_{k=2}^{n-2}P(T_k;0)+f(P(T_{n-2}),x))+x)
			 * for the new try
			 */	
			final double y = differenceSwapRateAtMissingBond(swapRate, x);

			rootFinder.setValue(y); // the algorithm is repeated for the new difference
		}
		final double computedBond = rootFinder.getBestPoint();// (approximation of) P(T_n;0)
		// P(T_{n-1}) is computed by interpolation of P(T_{n-2};0) and P(T_n;0)
		final double interpolatedBond = interpolate(lastBond, computedBond);// (approximation of) P(T_{n-1};0)
		sumOfComputedBonds += interpolatedBond + computedBond;
		computedBonds.add(interpolatedBond);
		computedBonds.add(computedBond);
		numberOfComputedBonds += 2;
	}

	/*
	 * This method computes the value of a bond for a sub-period, through the linear
	 * interpolation of the logarithm of the discount factors.
	 */
	private double interpolate(double bondTnminus2, double bondTn) {
		/*
		 * In general, the logarithmic interpolation works as follows: if t \in [S, T], f(t)
		 * gets approximated as
		 * exp((T-t)/(T-S)log(f(S))+(t-S)/(T-S)log(f(T))
		 */
		return Math.exp(0.5 * (Math.log(bondTnminus2) + Math.log(bondTn)));
	}

	/*
	 * Valuation of the difference between a given swap rate and the one calculated
	 * for a vector of already computed bonds, to which the new bond is added
	 * together with an interpolated one. It enters in the root finder algorithm
	 * above.
	 */
	private double differenceSwapRateAtMissingBond(double swapRate, double missingBond) {
		/*
		 * By means of the rootfinder algorithm, a value of missingBond will be computed
		 * in order to the following quantity to be close to zero
		 */
		// (P(T_1;0)-x))/(yearFraction*(\sum_{k=2}^{n-2}P(T_k;0)+f(P(T_{n-2}),x))+x)-parSwapRate
		return (computedBonds.get(0) - missingBond) / (yearFraction
				* (sumOfComputedBonds + interpolate(computedBonds.get(numberOfComputedBonds - 1), missingBond) + missingBond))
				- swapRate;
	}

	/**
	 * It returns the values of the bonds which have been bootstrapped up to now
	 *
	 * @return the values of the bootstrapped bonds, as an ArrayList
	 */
	public ArrayList<Double> getBonds() {
		return computedBonds;
	}
}