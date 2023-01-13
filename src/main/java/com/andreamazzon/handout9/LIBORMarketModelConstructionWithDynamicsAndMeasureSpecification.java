package com.andreamazzon.handout9;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.model.volatilities.SwaptionMarketData;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class creates a LIBOR market model, basing on the classes of the Finmath
 * library. Here the user can specify if the simulation has to be created under
 * log-normal or normal dynamics, and if the measure considered has to be the
 * terminal or the spot one.
 *
 * @author Andrea Mazzon
 *
 */
public class LIBORMarketModelConstructionWithDynamicsAndMeasureSpecification {

	public enum Measure {
		SPOT, TERMINAL
	};

	public enum Dynamics {
		LOGNORMAL, NORMAL
	};

	/**
	 * It specifies and creates a Rebonato volatility structure, represented by a
	 * matrix, for the LIBOR Market Model. In particular, we have
	 * dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j) with
	 * \sigma_i(t_j)=(a+b(T_i-t_j))\exp(-c(T_i-t_j))+d, for t_j < T_i,
	 * for four parameters a,b,c,d with b,c>0.
	 *
	 * @param a
	 * @param b
	 * @param c
	 * @param d
	 * @param simulationTimeDiscretization,  the time discretization for the
	 *                                       evolution of the processes
	 * @param tenureStructureDiscretization, the tenure structure T_0 < T_1< ...<T_n
	 * @return the matrix that represents the volatility structure:
	 *         volatility[i,j]=sigma_j(t_i)
	 */
	private static double[][] createVolatilityStructure(double a, double b, double c, double d,
			TimeDiscretization simulationTimeDiscretization, TimeDiscretization tenureStructureDiscretization) {
		// volatility[i,j]=sigma_j(t_i)		
		final int numberOfSimulationTimes = simulationTimeDiscretization.getNumberOfTimeSteps();
		final int numberOfTenureStructureTimes = tenureStructureDiscretization.getNumberOfTimeSteps();
		final double[][] volatility = new double[numberOfSimulationTimes][numberOfTenureStructureTimes];

		for (int timeIndex = 0; timeIndex < numberOfSimulationTimes; timeIndex++) {
			for (int LIBORIndex = 0; LIBORIndex < numberOfTenureStructureTimes; LIBORIndex++) {
				final double currentTime = simulationTimeDiscretization.getTime(timeIndex);// t_j
				final double currentMaturity = tenureStructureDiscretization.getTime(LIBORIndex);// T_i
				final double timeToMaturity = currentMaturity - currentTime;
				double instVolatility;
				if (timeToMaturity <= 0) {
					instVolatility = 0; // This forward rate is already fixed, no volatility
				} else {
					instVolatility = d + (a + b * timeToMaturity) * Math.exp(-c * timeToMaturity);// \sigma_i(t)=(a+b(T_i-t))\exp(-c(T_i-t))+d
				}
				// Store
				volatility[timeIndex][LIBORIndex] = instVolatility;
			}
		}
		return volatility;
	}

	/**
	 * It simulates a LIBOR Market Model, by using the implementation of the Finmath
	 * library. It is possible to specify the dynamics (normal or log-normal) and the
	 * measure (spot or terminal) 
	 *
	 * @param numberOfPaths:          number of simulations
	 * @param simulationTimeStep:     the time step for the simulation of the LIBOR
	 *                                processes
	 * @param LIBORPeriodLength:      the length of the interval between times of
	 *                                the tenure structure
	 * @param LIBORRateTimeHorizon:   final LIBOR maturity
	 * @param fixingForGivenForwards: the times of the tenure structure where the
	 *                                initial forwards (also called LIBORs if you
	 *                                want, here we stick to the name of the Finmath
	 *                                library) are given
	 * @param givenForwards:          the given initial forwards (from which the
	 *                                others are interpolated)
	 * @param correlationDecayParam:  parameter \alpha>0, for the correlation of the
	 *                                LIBORs: in particular, we have
	 *                                dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j) with
	 *                                d<W_i,W_k>(t)= \rho_{i,k}(t)dt where
	 *                                \rho_{i,j}(t)=\exp(-\alpha|T_i-T_k|)
	 * @param dynamicsType:			  the type of the dynamics of the processes modelling
	 * 								  the libors. This is an enum type, and can be "normal"
	 * 								  or "lognormal".   
	 * @param measureType:			  the measure under which the processes modelling
	 * 								  the libors are simulated. This is an enum type,
	 * 								  and can be "spot" or "terminal".                              
	 * @param a:                      the first term for the volatility structure:
	 *                                the volatility in the SDEs above is given by
	 *                                \sigma_i(t_j)=(a+b(T_i-t_j))\exp(-c(T_i-t_j))+d,
	 *                                for t_j < T_i.
	 * @param b:                      the second term for the volatility structure
	 * @param c:                      the third term for the volatility structure
	 * @param d:                      the fourth term for the volatility structure
	 * @return 						  an object implementing LIBORModelMonteCarloSimulationModel,
	 * 								  i.e., representing the simulation of a LMM
	 * @throws CalculationException
	 */
	public static final TermStructureMonteCarloSimulationModel createLIBORMarketModel(int numberOfPaths,
			double simulationTimeStep, double LIBORPeriodLength, // T_i-T_{i-1}, we suppose it to be fixed
			double LIBORRateTimeHorizon, // T_n
			double[] fixingForGivenForwards, double[] givenForwards, double correlationDecayParam,
			Dynamics dynamicsType, Measure measureType, double a, double b, double c, double d)
					throws CalculationException {
		/*
		 * In order to simulate a LIBOR market model, we need to proceed along the
		 * following steps:
		 * 1) provide the time discretization for the evolution of the
		 * processes
		 * 2) provide the time discretization of the tenure structure
		 * 3) provide the observed term structure of the initial LIBOR rates (also called
		 * forwards, using the terminology of the Finmath library) and if needed
		 * interpolate the ones missing: in this way we obtain the initial values for
		 * the LIBOR processes
		 * 4) create the volatility structure, i.e., the terms sigma_i(t_j) in
		 * dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j) or dL_i(t_j)=\sigma_i(t_j)dW_i(t_j).
		 * 5) create the correlation structure, i.e.,
		 * define the terms \rho_{i,j}(t) such that d<W_i,W_k>(t)= \rho_{i,k}(t)dt
		 * 6) combine all steps 1, 2, 4, 5 to create a covariance model
		 * 7) give the covariance model to the constructor of BlendedLocalVolatilityModel, to get
		 * another covariance model that now takes into account if the dynamics are
		 * normal (in this case the volatility gets rescaled) or log-normal
		 * 8) combine steps 2, 3, 7 to create the LIBOR model, also adding the appropriate
		 * properties about dynamics and measure
		 * 9) create a Euler discretization of the model we defined in step 8, specifying the model
		 * itself and a Brownian motion that uses the time discretization defined in step 1
		 * 10) give the Euler scheme to the constructor of LIBORMonteCarloSimulationFromLIBORModel, to create an
		 * object of type LIBORModelMonteCarloSimulationModel
		 */

		// Step 1: create the time discretization for the simulation of the processes
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / simulationTimeStep), simulationTimeStep);


		//Step 2: create the time discretization for the tenure structure (i.e., the dates T_1,..,T_n)
		final TimeDiscretization LIBORPeriodDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / LIBORPeriodLength), LIBORPeriodLength);

		/*
		 * Step 3 Create the forward curve (initial values for the LIBOR market model).
		 * We suppose not to have all the forwards: the others are interpolated using
		 * the specific method of the Finmath library
		 */
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("forwardCurve",
				fixingForGivenForwards, // fixings of the forward
				givenForwards, // the forwards we have
				LIBORPeriodLength);

		/*
		 * We didn't have it before: here we use it to call the constructor LIBORMarketModelFromCovarianceModel,
		 * which we need to specify the properties about the dynamics and the measure.
		 */
		final DiscountCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);

		// Step 4, the volatility model: we only have to provide the matrix
		final double[][] volatility = createVolatilityStructure(a, b, c, d, timeDiscretization,
				LIBORPeriodDiscretization);

		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization,
				LIBORPeriodDiscretization, volatility);
		/*
		 * Step 5 Create a correlation model rho_{i,j} = exp(−a ∗ |T_i −T_j|)
		 */
		final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization,
				LIBORPeriodDiscretization, LIBORPeriodDiscretization.getNumberOfTimes() - 1, //no factor reduction for now
				correlationDecayParam);

		/*
		 * Step 6 Combine volatility model and correlation model, together with the two
		 * time discretizations, to get a covariance model
		 */
		
		final AbstractLIBORCovarianceModelParametric covarianceModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(
				timeDiscretization, LIBORPeriodDiscretization, volatilityModel, correlationModel);


		/*
		 * Step 7 Here we substitute our covariance model with a new one built on top
		 * of it. The new model is given by (d L0 + (1-d)L) F where
		 * d = parameterForBlended is the displacement parameter, L is the component of
		 * the stochastic process, L_0 is its value at time zero and F is the factor
		 * loading from the "old" covariance model. 
		 * Our focus here is on parameterForBlended: we construct it is such a way
		 * that it is 0 if the dynamics are log-normal and 1 if they are normal.
		 */
		final double parameterForBlended = (dynamicsType == Dynamics.LOGNORMAL) ? 0.0 : 1.0;

		final AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModel,
				forwardCurve, parameterForBlended, false);

		// Step 8: we now create the model (i.e., the object of type LiborMarketModel)
		// Set model properties
		final Map<String, String> properties = new HashMap<>();


		final String nameOfTheMeasure = (measureType == Measure.TERMINAL) ? "terminal" : "spot";

		// Choose the simulation measure
		properties.put("measure", nameOfTheMeasure);

		
		//final String nameOfTheStateSpaceTransform = (dynamicsType == Dynamics.LOGNORMAL) ? "lognormal" : "normal";
		final String nameOfTheStateSpaceTransform = "normal";
		// Choose the state space transform
		properties.put("stateSpace", nameOfTheStateSpaceTransform);

		
		/*
		 * We use the "simplest" constructor allowing us to give the properties. Still, we have to provide
		 * an object of type SwaptionMarketData in case one wants to calibrate.
		 */
		final SwaptionMarketData swaptions = null;
		
		/*
		 * LIBORMarketModelFromCovarianceModel is another class implementing
		 * LiborMarketModel, like LIBORMarketModelStandard. It has the feature that you
		 * can specify some properties
		 */
		final ProcessModel LIBORMarketModel = new LIBORMarketModelFromCovarianceModel(LIBORPeriodDiscretization,
				forwardCurve, discountCurve, covarianceModelBlended, swaptions, properties);

		// Step 9: create an Euler scheme of the LIBOR model defined above
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization,
				LIBORPeriodDiscretization.getNumberOfTimes() - 1, // no factor reduction for now
				numberOfPaths, 1897 // seed
				);

		final MonteCarloProcess process = new EulerSchemeFromProcessModel(LIBORMarketModel, brownianMotion);

		// Step 10: give the Euler scheme to the constructor of LIBORMonteCarloSimulationFromLIBORModel
		return new LIBORMonteCarloSimulationFromLIBORModel(process);
	}
}
