package com.andreamazzon.handout6;

/**
 * This class takes care of the computations of the classical price (i.e., for  payment date T_2)
 * and of the price in arrears (i.e., for payment date T_1) of  a Floater under the Black model. 
 * Note that the computation of the convexity adjustment and consequently of the price in arrears are
 * implemented in the parent, abstract class EuropeanOptionPossiblyInArrears: these quantities can
 * be computed once the value of the classical price as a function of the initial value of the Libor
 * is known.
 *
 * @author Andrea Mazzon
 *
 */
public class FloaterWithBlack extends EuropeanOptionPossiblyInArrears {

	public FloaterWithBlack(double firstTime, double secondTime, double firstBond, double secondBond,
			double liborVolatility, double notional, Boolean giveLibor) {
		// parent class
		super(firstTime, secondTime, firstBond, secondBond, liborVolatility, notional, giveLibor);
	}

	@Override
	public double getValueInClassicUnits(double initialValueLibor) {
		/*
		 * This is not the simplest formula for the price of a classical floater
		 * (theoretically we could compute it as notional * (firstBond - secondBond)),
		 * but remember that we want to be able to use this method also for the
		 * computation of the convexity adjustment in the parent class, exploiting the
		 * derivation in "Solution to theoretical part of exercise 2" in com.andreamazzon.handou5t.
		 * In order to do that, we have to express it as a function of the initial value of the Libor.
		 */
		final double timeInterval = getTimeInterval();
		return getNotional() * getSecondBond() * initialValueLibor * timeInterval;
	}
}
