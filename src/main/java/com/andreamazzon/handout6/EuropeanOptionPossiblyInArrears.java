package com.andreamazzon.handout6;

/**
 * This is an abstract class taking care of the computations of the classical
 * price (i.e., for payment date T_2) and of the price in arrears (i.e., for
 * payment date T_1) of a general European option with two dates T_1 and T_2 on
 * interest rates, under the Black model for the Libor. In particular, the
 * computation of the convexity adjustment and consequently of the price in
 * arrears is implemented here: these quantities can be effectively computed
 * once the value of the classical price as a function of the initial value of
 * the Libor is known, see the derivation in "Solution to theoretical part of exercise 2" 
 * in com.andreamazzon.handout5. This last computation is of course specific to any
 * particular derivative, and it is therefore implemented in the derived
 * classes: for this reason, here it is defined as an abstract method.
 *
 * @author Andrea Mazzon
 *
 */
public abstract class EuropeanOptionPossiblyInArrears {

	/*
	 * All these parameters are common to all the interest rates derivatives we
	 * consider
	 */
	private final double firstTime, secondTime;
	private double firstBond;
	private final double secondBond;
	private double initialLibor;
	private final double liborVolatility;

	private final double notional;

	/**
	 * It constructs an object to compute the value in arrears and in natural unit
	 * of an option. Here P(T_2;0) must be given. It can be decided to give
	 * P(T_1;0), and in this case L(T_1,T_2;0) is computed from P(T_1;0) and
	 * P(T_2;0), or L(T_1,T_2;0): in this case, P(T_1;0) is computed from
	 * L(T_1,T_2;0) and P(T_2;0).
	 *
	 * @param firstTime,               T_1
	 * @param secondTime,              T_2
	 * @param secondBond,              P(T_2;0)
	 * @param firstBondOrInitialLibor, P(T_1 0);
	 * @param liborVolatility,         the volatility appearing in the dynamics of
	 *                                 the Libor
	 * @param notional
	 * @param giveLibor,               a Boolean which is True if one wants to give
	 *                                 L(T_1,T_2;0), false if one wants to give
	 *                                 P(T_1;0)
	 */
	public EuropeanOptionPossiblyInArrears(double firstTime, double secondTime, double firstBondOrInitialLibor,
			double secondBond, double liborVolatility, double notional, Boolean giveLibor) {
		this.firstTime = firstTime;
		this.secondTime = secondTime;
		this.secondBond = secondBond;
		this.liborVolatility = liborVolatility;
		this.notional = notional;
		// in this case, we get directly L(T_1,T_2;0) and compute P(T_1;0)
		if (giveLibor) {
			initialLibor = firstBondOrInitialLibor;
			computeFirstBond();
		} else {// in this case, we get directly P(T_1;0) and compute L(T_1,T_2;0)
			firstBond = firstBondOrInitialLibor;
			computeInitialLibor();
		}
	}

	private void computeInitialLibor() {
		final double timeInterval = getTimeInterval();
		initialLibor = 1 / timeInterval * (firstBond / secondBond - 1);
	}

	private void computeFirstBond() {
		final double timeInterval = secondTime - firstTime;
		firstBond = secondBond * (initialLibor * timeInterval + 1);

	}

	/*
	 * The fields should be private, but we might need their value in the derived
	 * class (or even in not derived classes): we then need some getters
	 */

	public double getFirstTime() {
		return firstTime;
	}

	public double getSecondTime() {
		return secondTime;
	}

	public double getFirstBond() {
		return firstBond;
	}

	public double getSecondBond() {
		return secondBond;
	}

	public double getLiborVolatility() {
		return liborVolatility;
	}

	public double getNotional() {
		return notional;
	}

	public double getTimeInterval() {
		return secondTime - firstTime;
	}

	public double getInitialValueLibor() {
		return initialLibor;
	}

	/**
	 * It computes and returns the classical price of the contract (i.e., if the
	 * payment date is T_2) as a function of the initial value of the Libor
	 *
	 * @param initialValue, initial value of the Libor
	 * @return the classical price of the contract (i.e., if the payment date is
	 *         T_2) as a function of the initial value of the Libor.
	 */
	/*
	 * We want to see the "natural" price (i.e., the price in units of P(T_2;0)) as
	 * a function of the initial value of the Libor because we want to use this
	 * formula to derive the convexity adjustment: in this case, we give the initial
	 * value of the Libor multiplied by the exponential of sigma_L^2 T_1
	 */
	public abstract double getValueInClassicUnits(double initialValueLibor);

	/**
	 * It computes and returns the convexity adjustment using the derivation you can
	 * see in "Solution to exercise 1.pdf", in com.andreamazzon.exercise7. Here the
	 * point is that the formula is identical for all the contracts, once you know
	 * the classical price as a function of the initial value of the Libor.
	 *
	 * @return the convexity adjustment
	 */
	public double computeConvexityAdjustment() {
		final double valueInUnitsOfM = getValueInClassicUnits(
				initialLibor * Math.exp(liborVolatility * liborVolatility * firstTime));
		return getTimeInterval() * initialLibor * valueInUnitsOfM;
	}

	/**
	 * It computes and returns the price in arrears of the contract (i.e., if the
	 * payment date is T_1) using the derivation you can see in "Solution to
	 * exercise 1.pdf", in com.andreamazzon.exercise7. Here the point is that the
	 * formula is identical for all the contracts, once you know the classical price
	 * as a function of the initial value of the Libor.
	 *
	 * @return the price in arrears of the contract (i.e., if the payment date is
	 *         T_1)
	 */
	public double getValueInArrears() {
		return getValueInClassicUnits(initialLibor) + computeConvexityAdjustment();
	}
}
