package com.andreamazzon.handout10;

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
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
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
 * It is possible to specify the dynamics (normal or log-normal) and the
 * measure (spot or terminal).
 * 
 * The organization of this class is different with respect to the others we have seen lately;
 * here some objects are stored as feilds of the class and given in the constructor. 
 * For this reason, the methods are not static. 
 *
 * @author Andrea Mazzon
 *
 */
public class NonStaticLIBORMarketModelConstructionWithDynamicsAndMeasureSpecification {

	public enum Measure {
		SPOT, TERMINAL
	};

	public enum Dynamics {
		LOGNORMAL, NORMAL
	};

	/*
	 * These objects are used by both the method to create the volatility structure and the LIBOrmarket model:
	 * in order to avoid that the second method gives all of them to the first one, we can make them fields of
	 * the class, so that they are directly accessible.
	 */
	
	private TimeDiscretization simulationTimeDiscretization;
	private TimeDiscretization tenureStructureDiscretization;

	private double firstParameterForVolatility, secondParameterForVolatility, thirdParameterForVolatility, fourthParameterForVolatility;
	private double correlationDecayParameter;

	private ForwardCurve forwardCurve;
	private Measure measureType;
	private Dynamics dynamicsType;
	private final DiscountCurve discountCurve;


	/**
	 * It constructs an object to build a LIBOR market model
	 * 
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
	 * @param firstParameterForVolatility:                      
	 * 								  the first term for the volatility structure:
	 *                                the volatility in the SDEs above is given by
	 *                                \sigma_i(t_j)=(a+b(T_i-t_j))\exp(-c(T_i-t_j))+d,
	 *                                for t_j < T_i.
	 * @param secondParameterForVolatility:                      
	 * @param thirdParameterForVolatility:                      
	 * @param fourthParameterForVolatility:                      
	 * @throws CalculationException
	 */
	NonStaticLIBORMarketModelConstructionWithDynamicsAndMeasureSpecification(double simulationTimeStep,
			double LIBORPeriodLength, // T_i-T_{i-1}, we suppose it to be fixed
			double LIBORRateTimeHorizon, // T_n
			double[] fixingForGivenForwards, double[] givenForwards, double correlationDecayParameter,
			Dynamics dynamicsType, Measure measureType, double firstParameterForVolatility,
			double secondParameterForVolatility, double thirdParameterForVolatility,
			double fourthParameterForVolatility){

		this.firstParameterForVolatility = firstParameterForVolatility;
		this.secondParameterForVolatility = secondParameterForVolatility;
		this.thirdParameterForVolatility = thirdParameterForVolatility;
		this.fourthParameterForVolatility = fourthParameterForVolatility;

		this.correlationDecayParameter = correlationDecayParameter;

		this.measureType = measureType;
		this.dynamicsType = dynamicsType;
		simulationTimeDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / simulationTimeStep), simulationTimeStep);

		tenureStructureDiscretization = new TimeDiscretizationFromArray(0.0,
				(int) (LIBORRateTimeHorizon / LIBORPeriodLength), LIBORPeriodLength);

		forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("forwardCurve",
				fixingForGivenForwards, // fixings of the forward
				givenForwards, // the forwards we have
				LIBORPeriodLength);

		/*
		 * We didn't have it before: here we use it to call the constructor LIBORMarketModelFromCovarianceModel,
		 * which we need to specify the properties about the dynamics and the measure.
		 */
		discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);

	}


	/*
	 * It specifies and creates a Rebonato volatility structure, represented by a
	 * matrix, for the LIBOR Market Model. In particular, we have
	 * dL_i(t_j)=\sigma_i(t_j)L_i(t_j)dW_i(t_j) with
	 * \sigma_i(t_j)=(a+b(T_i-t_j))\exp(-c(T_i-t_j))+d, for t_j < T_i,
	 * for four parameters a,b,c,d with b,c>0.
	 *
	 * @return the matrix that represents the volatility structure:
	 *         volatility[i,j]=sigma_j(t_i)
	 */
	private  double[][] createVolatilityStructure(){
		
		//easier to call them this way, internally
		double a = firstParameterForVolatility;
		double b = secondParameterForVolatility;
		double c = thirdParameterForVolatility;
		double d = fourthParameterForVolatility;
		
		// volatility[i,j]=sigma_j(t_i)

		final int numberOfSimulationTimes = simulationTimeDiscretization.getNumberOfTimeSteps();
		final int numberOfTenureStructureTimes = tenureStructureDiscretization.getNumberOfTimeSteps();
		final double[][] volatility = new double[numberOfSimulationTimes][numberOfTenureStructureTimes];

		for (int LIBORIndex = 0; LIBORIndex < numberOfTenureStructureTimes; LIBORIndex++) {
			

			final double currentMaturity = tenureStructureDiscretization.getTime(LIBORIndex);// T_i
			final double scalingFactor = (dynamicsType == Dynamics.LOGNORMAL) ? 1.0: forwardCurve.getForward(null, currentMaturity);

			for (int timeIndex = 0; timeIndex < numberOfSimulationTimes; timeIndex++) {
				final double currentTime = simulationTimeDiscretization.getTime(timeIndex);// t_j
				final double timeToMaturity = currentMaturity - currentTime;
				double instVolatility;
				if (timeToMaturity <= 0) {
					instVolatility = 0; // This forward rate is already fixed, no volatility
				} else {
					instVolatility = d + (a + b * timeToMaturity) * Math.exp(-c * timeToMaturity);// \sigma_i(t)=(a+b(T_i-t))\exp(-c(T_i-t))+d
				}
				
				instVolatility *= scalingFactor;
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
	 * @return 						  an object implementing LIBORModelMonteCarloSimulationModel,
	 * 								  i.e., representing the simulation of a LMM
	 * @throws CalculationException
	 */
	public final TermStructureMonteCarloSimulationModel createLIBORMarketModel(int numberOfPaths)
			throws CalculationException {

		// the volatility model: we only have to provide the matrix
		double[][] volatility = createVolatilityStructure();

		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFromGivenMatrix(simulationTimeDiscretization,
				tenureStructureDiscretization, volatility);
		
		// We create a correlation model rho_{i,j} = exp(−a ∗ |T_i −T_j|)
		final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(simulationTimeDiscretization,
				tenureStructureDiscretization, tenureStructureDiscretization.getNumberOfTimes() - 1, //no factor reduction for now
				correlationDecayParameter);

		/*
		 * We combine volatility model and correlation model, together with the two
		 * time discretizations, to get a covariance model
		 */
		final LIBORCovarianceModel covarianceModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(
				simulationTimeDiscretization, tenureStructureDiscretization, volatilityModel, correlationModel);



		// We now create the model (i.e., the object of type LiborMarketModel)

		// Choose the simulation measure
		final String nameOfTheMeasure = (measureType == Measure.TERMINAL) ? "terminal" : "spot";

		// Choose the state state space transform
		final String nameOfTheStateSpaceTransform = (dynamicsType == Dynamics.LOGNORMAL) ? "lognormal" : "normal";

		// Set model properties based on our choices
		final Map<String, String> properties = new HashMap<>();

		properties.put("measure", nameOfTheMeasure);		

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
		final ProcessModel LIBORMarketModel = new LIBORMarketModelFromCovarianceModel(tenureStructureDiscretization,
				forwardCurve, discountCurve, covarianceModel, swaptions, properties);

		// We create an Euler scheme of the LIBOR model defined above
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(simulationTimeDiscretization,
				tenureStructureDiscretization.getNumberOfTimes() - 1, // no factor reduction for now
				numberOfPaths, 1897 // seed
				);

		final MonteCarloProcess process = new EulerSchemeFromProcessModel(LIBORMarketModel, brownianMotion);

		// We give the Euler scheme to the constructor of LIBORMonteCarloSimulationFromLIBORModel
		return new LIBORMonteCarloSimulationFromLIBORModel(process);
	}
}
