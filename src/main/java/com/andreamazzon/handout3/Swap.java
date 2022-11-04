package com.andreamazzon.handout3;

/**
 * Interface with methods to compute the value of a swap rate and of a par swap
 * rate.
 *
 * @author Andrea Mazzon
 *
 */
public interface Swap {

	/**
	 * It computes and returns the value of a swap at evaluation time t = 0, for a
	 * given vector of swap rates and a general tenure structure, given in the
	 * constructor of the class implementing the interface
	 *
	 * @parameter swapRates, the array of doubles representing the swap rates
	 * @return the value of the swap
	 */
	double getSwapValue(double[] swapRates);

	/**
	 * It computes and returns the value of a swap at evaluation time t = 0, for a
	 * given vector of swap rates, in the case when the settlement dates of the term
	 * structure are evenly distributed with given time step
	 *
	 * @parameter swapRates, the array of doubles representing the swap rates
	 * @parameter yearFraction, the given time step
	 * @return the value of the swap
	 */
	double getSwapValue(double[] swapRates, double yearFraction);

	/**
	 * It computes and returns the value of a swap at evaluation time t = 0, for a
	 * given single swap rate (i.e., all entries of the vector of swap rates are
	 * equal to that single swap rate) and a general tenure structure, given in the
	 * constructor of the class implementing the interface
	 *
	 * @parameter swapRate, the single swap rate
	 * @return the value of the swap
	 */
	double getSwapValue(double singleSwapRate);

	/**
	 * It computes and returns the value of a swap at evaluation time t = 0, for a
	 * given single swap rate (i.e., all entries of the vector of swap rates are
	 * equal to that single swap rate) when the settlement dates of the term
	 * structure are evenly distributed with given time step
	 *
	 * @parameter swapRate, the single swap rate
	 * @parameter yearFraction, the given time step
	 * @return the value of the swap
	 */
	double getSwapValue(double singleSwapRate, double yearFraction);

	/**
	 * It computes and returns the par swap rate at evaluation time t = 0, for a
	 * general tenure structure, given in the constructor of the class implementing
	 * the interface.
	 *
	 * @return the par swap rate
	 */
	double getParSwapRate();

	/**
	 * It computes and returns the par swap rate at evaluation time t = 0, in the
	 * case when the settlement dates of the term structure are evenly distributed
	 * with given time step
	 *
	 * @parameter yearFraction, the given time step
	 * @return the par swap rate
	 */
	double getParSwapRate(double yearFraction);
}