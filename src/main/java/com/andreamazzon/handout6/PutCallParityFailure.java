package com.andreamazzon.handout6;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.functions.NormalDistribution;



/**
 * This class investigates the put-call parity failure die to collateralization, described at pages
 * 344-350 of the script. In particular, we perform the following experiment:
 * - we compute both the generalized put and call price, via analytic formulas, for fixed maturity and
 * different strikes
 * - we compute the difference between the two
 * - we write a method which recovers forward and discount factor from two differences between call and put
 * 	 prices, for two strikes: theoretically, this is enough to solve the related linear system
 * 
 * 
 * @author Andrea Mazzon
 *
 */
public class PutCallParityFailure {

	
	/**
	 * Calculates the generalized Black-Scholes option value of a put, i.e., the payoff max(K-S(T),0) P, where S follows a log-normal process with
	 * constant log-volatility.
	 *
	 * @param forward The forward of the underlying.
	 * @param volatility The Black-Scholes volatility.
	 * @param optionMaturity The option maturity T.
	 * @param optionStrike The option strike. If the option strike is &le; 0.0 the method returns the value of the forward contract paying S(T)-K in T.
	 * @param payoffUnit The payoff unit (e.g., the discount factor)
	 * @return Returns the value of a European call option under the Black-Scholes model.
	 */
	public static double blackScholesPutGeneralizedOptionValue(
			final double forward,
			final double volatility,
			final double optionMaturity,
			final double optionStrike,
			final double payoffUnit)
	{
		if(optionMaturity < 0) {
			return 0;
		}

		// We calculate analytic value
		final double dPlus = (Math.log(forward / optionStrike) + 0.5 * volatility * volatility * optionMaturity) / (volatility * Math.sqrt(optionMaturity));
		final double dMinus = dPlus - volatility * Math.sqrt(optionMaturity);

		final double valueAnalytic = (-forward * NormalDistribution.cumulativeDistribution(-dPlus) + optionStrike * NormalDistribution.cumulativeDistribution(-dMinus)) * payoffUnit;

		return valueAnalytic;
	}

	
	/**
	 * It computes the difference between a generalized call and a generalized put option, computing the prices with both with the suitable
	 * analytic formula. It does it for a vector of strikes given as an argument
	 * 
	 * @param forward
	 * @param volatility
	 * @param optionMaturity
	 * @param payoffUnit
	 * @param strikes, the strikes for which the differences are computed
	 * @return the difference between the call and the put price, as a vector of doubles (every value corresponds to a given strike,
	 * 		in the vector which is a field of the class).
	 */
	public static double[] getCallPutDifference(final double forward, final double volatility, final double optionMaturity, final double payoffUnit, final double[] strikes) {
		
		int numberOfStrikes = strikes.length;
		double[] results = new double[numberOfStrikes];
		
		//we compute the difference for every strike
		for (int strikeIndex = 0; strikeIndex < numberOfStrikes; strikeIndex ++) {
			results[strikeIndex] = AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatility, optionMaturity,strikes[strikeIndex],payoffUnit)
					- blackScholesPutGeneralizedOptionValue(forward, volatility, optionMaturity,strikes[strikeIndex],payoffUnit);
		}
		return results;
	}

	/**
	 * It returns the forward F(0) and the discount factor M(0) computed via a linear regression (solving a linear system)
	 * knowing two values of the difference between a call and a put for two different values of the strikes.
	 * Specifically (look at page 348 of the script) we have:
	 * F(0) = -a/b; 
	 * M(0) = -b,
	 * where a and b solve the linear system
	 * a + b K_1 = V(K_1)
	 * a + b K_2 = V(K_2)
	 * with V(K_1)=valueDifference for strike K_1 and V(K_2) valueDifference for strike K_2.
	 * That is,
	 * ((1, K_1); (1, K_2))(a;b)=(V(K_1);V(K_2))
	 * 
	 * @param differencesBetweenCallAndPut
	 * @param strikes
	 * @return a vector of two doubles: the first double is the forward F(0), the second double is the discount factor M(0)
	 */
	public static double[] getForwardAndDiscountFactor(double[] differencesBetweenCallAndPut, final double[] strikes) {
		
		if (strikes.length != 2) {
            throw new IllegalStateException("invalid dimensions");
        }
		
		//this is the matrix A in Ax=y
		double[][] matrixForStrikes = { {1, strikes[0]}, {1, strikes[1]}};
		
		//this is the vector y in Ax=y
		double[] solutionOfLinearRegression = UsefulMethodsMatrices.solveLinearSystem(matrixForStrikes, differencesBetweenCallAndPut);
		
		//now we recover F(0) and M(0): look at page 348 of the script 
		double forward = - solutionOfLinearRegression[1]; 
		double discountFactor = solutionOfLinearRegression[0] / forward; 
		double[] vectorToReturn = {forward, discountFactor};
		return vectorToReturn;
		
	}
	
}
