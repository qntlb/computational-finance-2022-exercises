package com.andreamazzon.handout8;

import java.util.HashMap;
import java.util.Map;

import com.andreamazzon.handout7.LIBORMarketModelConstruction;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;

public class LIBORMarketModelConstructionWithAddedClone extends LIBORMarketModelConstruction {
	/**
	 * It returns a simulation of a LIBOR Market model which is the same of the
	 * simulation given as argument (i.e., also same realizations of the Brownian
	 * motions) with a (possibly) different correlation structure, given by an
	 * exponential decay model with decay parameter given as input.
	 *
	 * @param oldModel,    the simulation of the model whose LIBORCorrelationModel
	 *                     we want to change
	 * @param correlation, the value of the decay parameter of the new
	 *                     LIBORCorrelationModel: this LIBORCorrelationModel will be
	 *                     indeed of type LIBORCorrelationModelExponentialDecay.
	 * @return the simulation of the model with the new LIBORCorrelationModel
	 * @throws CalculationException
	 */
	public static TermStructureMonteCarloSimulationModel getCloneWithModifiedCorrelation(
			TermStructureMonteCarloSimulationModel oldLIBORSimulation, double decayParameter)
			throws CalculationException {
		/*
		 * Steps: 
		 * - check if the model (i.e. the TermStructureModel object) we get from
		 * the simulation (i.e., from LIBORModelMonteCarloSimulationModel) is of type
		 * LIBORMarketModel; 
		 * - get this model as LIBORMarketModel (downcasting) 
		 * - get the LIBORCovarianceModel object from the LIBORMarketModel object (that's why
		 * we want it to be LIBORMarketModel) 
		 * - create a new LIBORCorrelationModel as a LIBORCorrelationModelExponentialDecay
		 * object with the given correlation decay  parameter, and the other arguments taken
		 * from the LIBORCovarianceModel object
		 * - create a new LIBORCovarianceModel with the new LIBORCorrelationModel -
		 * create a new LIBORMarketModel with the new LIBORCovarianceModel - link the
		 * LIBORMarketModel with the BrownianMotion of the old simulation to get a
		 * MonteCarloProcess with the constructor of EulerSchemeFromProcessModel - pass
		 * this MonteCarloProcess to the constructor of
		 * LIBORMonteCarloSimulationFromLIBORModel to get a
		 * LIBORModelMonteCarloSimulationModel.
		 */

		/*
		 * We check if oldLIBORSimulation.getModel() is of type LIBORMarketModel. If
		 * not, we know that we will get a class Exception few lines below. At least, if
		 * this is the case, here we print something in order to help the user
		 * understanding what's going wrong.
		 */
		if (!(oldLIBORSimulation.getModel() instanceof LIBORMarketModel)) {
			System.out.println("The model returned by oldLIBORSimulation must be of type LIBORMarketModel!");
		}

		/*
		 * We downcast: we want the model to be of type LIBORMarketModel, because then
		 * we want it to be able to call the method getCovarianceModel(). If this is not
		 * the case, a ClassCastException is authomatically thrown (Java does this). The 
		 * message we print above might help.
		 */
		final LIBORMarketModel model = (LIBORMarketModel) oldLIBORSimulation.getModel();

		/*
		 * Covariance model: it is returned as an object of type LIBORCovarianceModel
		 * (the interface). Now, we use it in order to get the time discretizations and the number
		 * of factors we use to construct the new correlation model. Then, we substitute it with
		 * a new covariance model which has the new correlation model we construct below.
		 */
		final LIBORCovarianceModel oldCovarianceModel = model.getCovarianceModel();

		/*
		 * New correlation model: constructor with the same fields of the old
		 * LIBORCovarianceModel, except for the decay parameter
		 */
		final LIBORCorrelationModel newCorrelationModel = new LIBORCorrelationModelExponentialDecay(
				oldCovarianceModel.getTimeDiscretization(), oldCovarianceModel.getLiborPeriodDiscretization(),
				oldCovarianceModel.getNumberOfFactors(), decayParameter);

		final Map<String, Object> changeMap = new HashMap<String, Object>();
		// name of the field and new value
		changeMap.put("correlationModel", newCorrelationModel);
		// new covariance model
		final LIBORCovarianceModel newCovarianceModel = oldCovarianceModel.getCloneWithModifiedData(changeMap);

		// new LIBOR model
		final ProcessModel newLiborMarketModel = model.getCloneWithModifiedCovarianceModel(newCovarianceModel);

		final IndependentIncrements brownianMotion = oldLIBORSimulation.getBrownianMotion();

		final MonteCarloProcess newMonteCarloProcess = new EulerSchemeFromProcessModel(newLiborMarketModel,
				brownianMotion);
		/*
		 * New simulation: the model is linked with a clone of the Euler Scheme of the
		 * old simulation. Note: this is a clone and not the same object, since this
		 * would give a running time error.
		 */
		return new LIBORMonteCarloSimulationFromLIBORModel(newMonteCarloProcess);
	}
}
