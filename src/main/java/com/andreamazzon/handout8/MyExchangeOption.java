package com.andreamazzon.handout8;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * This class represents an exchange option on two LIBOR rates: one LIBOR rate
 * from time periodStartFirstLibor to time periodEndFirstLibor, and one from
 * periodStartSecondLibor to periodEndSecondLibor. The product is payed at
 * periodEndSecondLibor. Note the implementation of the getValue method.
 *
 * @author Andrea Mazzon
 */
public class MyExchangeOption extends AbstractLIBORMonteCarloProduct {
	private final double periodStartFirstLibor; // T_i
	private final double periodEndFirstLibor; // T_{i+1}

	private final double periodStartSecondLibor; // T_k
	private final double periodEndSecondLibor; // T_{k+1}

	public MyExchangeOption(double periodStartFirstLibor, double periodEndFirstLibor, double periodStartSecondLibor,
			double periodEndSecondLibor) {
		super();
		this.periodStartFirstLibor = periodStartFirstLibor;
		this.periodEndFirstLibor = periodEndFirstLibor;
		this.periodStartSecondLibor = periodStartSecondLibor;
		this.periodEndSecondLibor = periodEndSecondLibor;
	}

	/**
	 * Computes and returns the payoff of the product
	 *
	 * @param evaluationTime The time on which this product value is evaluated.
	 * @param model          The underlying of the product. From this underlying, we
	 *                       take the two LIBOR rates involved.
	 * @return The random variable representing the value of the product discounted
	 *         to evaluation time
	 */
	@Override
	public RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model)
			throws CalculationException {

		// Get the value of the first LIBOR L_i at T_i: L(T_i,T_{i+1};T_i)
		final RandomVariable firstLibor = model.getLIBOR(periodStartFirstLibor, periodStartFirstLibor,
				periodEndFirstLibor);

		// Get the value of the first LIBOR L_k at T_k: L(T_k,T_{k+1};T_k)
		final RandomVariable secondLibor = model.getLIBOR(periodStartSecondLibor, periodStartSecondLibor,
				periodEndSecondLibor);

		RandomVariable values = firstLibor.sub(secondLibor).floor(0.0); // payoff

		// Get numeraire at payment time: you then divide by N(T_{k+1})
		final RandomVariable numeraire = model.getNumeraire(periodEndSecondLibor);

		values = values.div(numeraire);

		// Get numeraire at evaluation time: you multiply by N(0)
		final RandomVariable numeraireAtEvaluationTime = model.getNumeraire(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime);

		return values;
	}
}