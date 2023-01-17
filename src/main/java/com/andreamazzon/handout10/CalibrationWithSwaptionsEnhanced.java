package com.andreamazzon.handout10;


import java.util.ArrayList;

import org.jblas.util.Random;


import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class performs a calibration of the LIBOR Market Model with respect to the covariance
 * structure. In particular, we assume an exponentially decaying correlation coupled with a
 * Rebonato volatility, and we want to calibrate its parameters. 
 * The calibration is based on prices of swaptions for different strikes and different swaption
 * tenure structures. The prices of the swaptions are produced by a LIBORMonteCarloSimulationFromLIBORModel
 * object given in the constructor of the class: in this example, this is seen as the "true Libor Market model".
 * The number of strikes is given as an argument of the method creating the calibration product, whereas
 * the swaption tenure structures are identified by the tenure structure that one gets from the 
 * LIBORMonteCarloSimulationFromLIBORModel object which gives the price.
 * 
 * It is also possible to calibrate only the variance parameters or only the correlation parameters.
 *
 * @author Andrea Mazzon
 */
public class CalibrationWithSwaptionsEnhanced {

	//this will be filled with the products that we construct in order to calibrate
	private ArrayList<CalibrationProduct> calibrationProducts;
	
	/*
	 * They will keep track of strikes, fixings and tenure structures we use for any calibration:
	 * it can be useful for our later analysis
	 */
	private ArrayList<Double> trackOfStrikesForSwaptions;
	private ArrayList<Double> trackOfFixingsForSwaptions;
	private ArrayList<ArrayList<Double>> trackOfTenureStructuresForSwaptions;

	
	private boolean isVolatilityCalibreatable;//if true, we calibrate the volatility parameters
	private boolean isCorrelationCalibreatable;//if true, we calibrate the correlation parameter

	/*
	 * This is the LIBOR Market model that we suppose (in our exercise) to give the prices that
	 * we observe: basically, we want that our calibration returns something close to this
	 */
	private LIBORMonteCarloSimulationFromLIBORModel trueLiborMarket;
	private LIBORModel trueLiborMarketModel;
	
	//if its value is given in the constructor, we don't calibrate it 
	private double decayingParameterForCorrelation;
	
	//if its value is given in the constructor, we don't calibrate it 
	private double[] volatilityParameters;
	
	
	/**
	 * It creates an object used to calibrate the covariance structure of the LIBOR Market model
	 * based on prices of swaptions with different strikes, fixings and tenure structures. 
	 *
	 * @param trueLiborMarket, it gives the underlying LIBOR Market model we want to calibrate: it
	 * 		  provides the prices that we observe
	 */
	public CalibrationWithSwaptionsEnhanced(LIBORMonteCarloSimulationFromLIBORModel trueLiborMarket) {
		this.trueLiborMarket = trueLiborMarket;
		isVolatilityCalibreatable = true;
		isCorrelationCalibreatable = true;
		
		/*
		 * They will be calibrated, so this value actually does not matter. However, we will have
		 * to give it to the constructor of the volatility model we have to calibrate.
		 */
		volatilityParameters = new double[] {0.1, 0.1, 0.1, 0.1};		
		/*
		 * It will be calibrated, so its value actually does not matter. However, we will have
		 * to give it to the constructor of the correlation model we have to calibrate.
		 */
		decayingParameterForCorrelation = 0.3;
		
		
		//the model which is linked to the Brownian motion to construct the simulation
		trueLiborMarketModel =  trueLiborMarket.getModel();
		
		/*
		 * We initialize the ArrayLists that will keep track of our calibration products,
		 * strikes, fixings and tenure structures
		 */
		calibrationProducts = new ArrayList<CalibrationProduct>();
		trackOfStrikesForSwaptions = new ArrayList<Double>();
		trackOfFixingsForSwaptions = new ArrayList<Double>();
		trackOfTenureStructuresForSwaptions = new ArrayList<ArrayList<Double>>();
	}
	
	
	/**
	 * It creates an object used to calibrate the correlation structure of the LIBOR Market model
	 * based on prices of swaptions with different strikes, fixings and tenure structures. 
	 *
	 * @param trueLiborMarket, it gives the underlying LIBOR Market model we want to calibrate: it
	 * 		  provides the prices that we observe
	 * @param decayingParameterForCorrelation, the value assigned to the correlation decay parameter
	 */
	public CalibrationWithSwaptionsEnhanced(LIBORMonteCarloSimulationFromLIBORModel trueLiborMarket, 
			double decayingParameterForCorrelation) {
		this.trueLiborMarket = trueLiborMarket;
		isVolatilityCalibreatable = true;
		isCorrelationCalibreatable = false;//it is given, so we don't calibrate it
		this.decayingParameterForCorrelation = decayingParameterForCorrelation;
		
		/*
		 * They will be calibrated, so this value actually does not matter. However, we will have
		 * to give it to the constructor of the volatility model we have to calibrate.
		 */
		volatilityParameters = new double[] {0.1, 0.1, 0.1, 0.1};
		
		
		//the model which is linked to the Brownian motion to construct the simulation
		trueLiborMarketModel =  trueLiborMarket.getModel();
		
		/*
		 * We initialize the ArrayLists that will keep track of our calibration products,
		 * strikes, fixings and tenure structures
		 */
		calibrationProducts = new ArrayList<CalibrationProduct>();
		trackOfStrikesForSwaptions = new ArrayList<Double>();
		trackOfFixingsForSwaptions = new ArrayList<Double>();
		trackOfTenureStructuresForSwaptions = new ArrayList<ArrayList<Double>>();
	}
	
	
	/**
	 * It creates an object used to calibrate the correlation structure of the LIBOR Market model
	 * based on prices of swaptions with different strikes, fixings and tenure structures. 
	 * The volatility parameters are given and not calibrated
	 *
	 * @param trueLiborMarket, it gives the underlying LIBOR Market model we want to calibrate: it
	 * 		  provides the prices that we observe
	 * @param volatilityParameters, the values assigned to the volatility parameters
	 */
	public CalibrationWithSwaptionsEnhanced(LIBORMonteCarloSimulationFromLIBORModel trueLiborMarket, 
			double[] volatilityParameters) {
		this.trueLiborMarket = trueLiborMarket;
		isVolatilityCalibreatable = false;//it is given, so we don't calibrate it
		isCorrelationCalibreatable = true;
		this.volatilityParameters = volatilityParameters;
		
		/*
		 * It will be calibrated, so its value actually does not matter. However, we will have
		 * to give it to the constructor of the correlation model we have to calibrate.
		 */
		decayingParameterForCorrelation = 0.3;
		
		
		//the model which is linked to the Brownian motion to construct the simulation
		trueLiborMarketModel =  trueLiborMarket.getModel();
		
		/*
		 * We initialize the ArrayLists that will keep track of our calibration products,
		 * strikes, fixings and tenure structures
		 */
		calibrationProducts = new ArrayList<CalibrationProduct>();
		trackOfStrikesForSwaptions = new ArrayList<Double>();
		trackOfFixingsForSwaptions = new ArrayList<Double>();
		trackOfTenureStructuresForSwaptions = new ArrayList<ArrayList<Double>>();
	}
	
	
	
	
	
	/**
	 * This method creates a list of CalibrationProducts, and stores them in the
	 * field of the class: such a field is accessed by the swaptionCalibration()
	 * method. It also keeps track of the strikes, fixings and tenure structures
	 * corresponding to this products.
	 *
	 * @param numberOfStrikes: the number of strikes for which we observe the prices
	 *                         of swaptions 
	 * @throws CalculationException
	 */
	@SuppressWarnings("unchecked")
	public void createCalibrationItems(int numberOfStrikes) throws CalculationException {

		/*
		 * This is used:
		 * - to calculate the par swap rate (we need it to center the strikes on it)
		 * - to construct the swaptions on which we base our calibration
		 */
		final TimeDiscretization tenureStructure = trueLiborMarket.getLiborPeriodDiscretization();

		//here we use directly the method of the Finmath library
		final double swapRate = Swap.getForwardSwapRate(
				tenureStructure,
				// discretization for fixed and floating leg, in principle they are different!
				tenureStructure,
				// we get forwards and bonds from the model
				trueLiborMarketModel.getForwardRateCurve(), trueLiborMarketModel.getDiscountCurve());

		// our choice: how far out and in the money we want the strikes to be
		final double minStrike = 4.0/5.0*swapRate;
		final double maxStrike = 5.0/4.0*swapRate;
		final double strikeStep = (maxStrike - minStrike)/numberOfStrikes;

		/*
		 * Maybe we don't observe the exact prices that the model would give, but we
		 * have some noise (in addition to the Monte-Carlo error)
		 */
		double noise;

		/*
		 * Now we want to identify the fixing times we will consider for our swaptions: these
		 * are all the times of the tenure structure except for the first one (which is T_0)
		 * and the last one (which is T_n). In order to do that, we work with the times of
		 * the time discretization expressed as an ArrayList.
		 */
		ArrayList<Double> arrayListForFixingTimes =  tenureStructure.getAsArrayList();
		arrayListForFixingTimes.remove(tenureStructure.getNumberOfTimeSteps());//T_n
		arrayListForFixingTimes.remove(0);//T_0

		
		/*
		 * Now the swaption tenure structure: if T_i is the fixing, we start with T_i, ..., T_n,
		 * and then we start to delete the last elements: first T_n, then T_{n-1}, etc. Now the fixing
		 * is T_1, so we start with T_1, ..., T_n.
		 */
		
		//T_0,...,T_n
		ArrayList<Double> completeSwaptionTenureStructureForGivenFixingAsArrayList =  tenureStructure.getAsArrayList();
		completeSwaptionTenureStructureForGivenFixingAsArrayList.remove(0);//T_0

		/*
		 * Now we have three nested for loops: the most external one identifies the fixings, the middle one all the
		 * tenure structures for given fixing (i.e., for fixing T_i, all the tenure structures from T_i,...,T_n to T_i,T_{i+1})
		 * and the most internal one the strikes. 
		 */
		for (double fixingTimeSwaption : arrayListForFixingTimes) {

			
			// This is T_i,...,T_n for fixing T_i. At every iteration of this most external for loop, we will delete T_i
			TimeDiscretization completeSwaptionTenureStructureForGivenFixing =
					new TimeDiscretizationFromArray(completeSwaptionTenureStructureForGivenFixingAsArrayList);
			
			/*
			 * Now we already work looking at the second loop: we start by T_i,...,T_n, and at every iteration of
			 * the next for loop we will delete the last element. So first we have T_i,...,T_n, then T_i,...,T_{n-1},
			 * and so on. See the next loop
			 */
			ArrayList<Double> dynamicalSwaptionTenureStructureAsArrayList =	
					completeSwaptionTenureStructureForGivenFixing.getAsArrayList();	
			
			/*
			 * Second nested loop: we manage the tenure structures. At the end of the for loop we delete the last entry
			 * of dynamicalSwaptionTenureStructureAsArrayList 
			 */
			for (int indexForEndOfSwaptionTenureStructure = completeSwaptionTenureStructureForGivenFixing.getNumberOfTimes() - 1;
					indexForEndOfSwaptionTenureStructure >= 1; indexForEndOfSwaptionTenureStructure--) {

				//we create a TimeDiscretization object representing our swaption tenure structures
				TimeDiscretization dynamicalSwaptionTenureStructure = new TimeDiscretizationFromArray(dynamicalSwaptionTenureStructureAsArrayList);

				//third for loop: the one representing the strike
				for (double strike = minStrike; strike <= maxStrike; strike += strikeStep) {


					// the product that we use to calibrate
					final AbstractTermStructureMonteCarloProduct monteCarloSwaption = new Swaption(
							fixingTimeSwaption, dynamicalSwaptionTenureStructure, strike);

					// we assume to perturb the true prices with some noise

					// remember: Random.nextDouble() gives a number uniformly distributed in (0,1)
					noise = 0.05*(-0.5+Random.nextDouble());

					/*
					 * This is the value of the swaption we suppose to observe in the market: in our
					 * case, we produce it by Monte-Carlo
					 */
					final double targetValueSwaption = monteCarloSwaption.getValue(trueLiborMarket)*(1 + noise);
					
					/*
					 * Look at this constructor of CalibrationProduct, where the important inputs
					 * for now are the first two: the product we are considering (so type of
					 * pruduct, tenure structure, strike ecc) and the value we observe for this
					 * product. The calibration will be made with the scope to match these values as
					 * much as possible.
					 */
					calibrationProducts.add(new CalibrationProduct(monteCarloSwaption,
							targetValueSwaption,
							1.0 /* calibration weight*/));
					
					//we update the lists keeping track of our data
					trackOfStrikesForSwaptions.add(strike);
					trackOfFixingsForSwaptions.add(fixingTimeSwaption);
					
					/*
					 * Pay attention here:
					 * trackOfTenureStructuresForSwaptions.add(dynamicalSwaptionTenureStructureAsArrayList)
					 * would give a big! Why?
					 */
					trackOfTenureStructuresForSwaptions.add((ArrayList<Double>) dynamicalSwaptionTenureStructureAsArrayList.clone());
				}
				/*
				 * We remove the last element of the swaption tenure structure: if it is T_i, .., T_j, where T_i is the fixing,
				 * we remove T_j
				 */
				dynamicalSwaptionTenureStructureAsArrayList.remove(indexForEndOfSwaptionTenureStructure);
			}
			completeSwaptionTenureStructureForGivenFixingAsArrayList.remove(0);
		}
	}

	/**
	 * This method performs the calibration of the LIBOR Market model to the
	 * swaption prices.
	 *
	 * @return the calibrated LIBOR market model
	 */
	public LIBORMarketModel swaptionCalibration() throws CalculationException {

		/*
		 * We have to construct the LIBOR Market model we want to calibrate: what we
		 * want to calibrate are the parameters identifying the covariance structure,
		 * but of course we have to specify all the rest.
		 */
		final TimeDiscretization timeDiscretization = trueLiborMarket.getTimeDiscretization();
		final TimeDiscretization liborDiscretization = trueLiborMarket.getLiborPeriodDiscretization();


		/*
		 * The volatility model: note the specification if it is calibreatable or not. If yes, the four parameters
		 * get calibrated so we don't really care about the values we have given
		 */
		final LIBORVolatilityModel volatilityModel =
				new LIBORVolatilityModelFourParameterExponentialForm(new RandomVariableFromArrayFactory(), timeDiscretization,
				liborDiscretization, volatilityParameters[0],volatilityParameters[1],volatilityParameters[2],
				volatilityParameters[3], isVolatilityCalibreatable);

		/*
		 * The correlation model: note the specification if it is calibreatable or not. If yes, the parameter
		 * gets calibrated so we don't really care about the values we have given
		 */
		final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization,
				liborDiscretization, liborDiscretization.getNumberOfTimes() - 1, // no factor reduction
				decayingParameterForCorrelation, isCorrelationCalibreatable );

		
		final LIBORCovarianceModel covarianceModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(
				timeDiscretization, liborDiscretization, volatilityModel, correlationModel);
		
		// again things we need for the LIBOR Market model
		final ForwardCurve forwardCurve = trueLiborMarketModel.getForwardRateCurve();
		final DiscountCurve discountCurve = trueLiborMarketModel.getDiscountCurve();

		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		// new CalibrationProduct[0] is given to specify we want to have an array of objects of type CalibrationProdoct
		CalibrationProduct[] calibrationProductsAsArray = calibrationProducts.toArray(new CalibrationProduct[0]);

		// Calibrated LIBOR Market Model: same as the "true" one but with calibrated parameters
		final LIBORMarketModel liborMarketModelCalibrated = new LIBORMarketModelFromCovarianceModel(
				liborDiscretization,
				null,
				forwardCurve,
				discountCurve,
				randomVariableFactory,
				//The parameters will be in fact computed by calibration
				covarianceModel, calibrationProductsAsArray,
				null);

		return liborMarketModelCalibrated;
	}

	/**
	 * It returns the calibration products
	 * 
	 * @return the array of the calibration products
	 */
	public CalibrationProduct[] getCalibrationProducts() {
		CalibrationProduct[] calibrationProductsAsArray = (CalibrationProduct[]) calibrationProducts.toArray(new CalibrationProduct[0]);
		return calibrationProductsAsArray;
	}

	/**
	 * It returns all the strikes used to price the calibration products: in this way, for every product we have its strike
	 * 
	 * @return the ArrayList of strikes
	 */
	public ArrayList<Double> getStrikes() {
		return trackOfStrikesForSwaptions;
	}

	/**
	 * It returns all the fixing times used to price the calibration products: in this way, for every product we have its fixing
	 * 
	 * @return the ArrayList of fixings
	 */
	public ArrayList<Double> getFixings() {
		return trackOfFixingsForSwaptions;
	}
	
	/**
	 * It returns all the swaptions tenure structures used to price the calibration products: in this way, for every
	 * product we have its tenure structure
	 * 
	 * @return the ArrayList of swaptions tenure structures
	 */
	public ArrayList<ArrayList<Double>> getTenureStructures() {
		return trackOfTenureStructuresForSwaptions;
	}
}