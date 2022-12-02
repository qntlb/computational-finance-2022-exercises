package com.andreamazzon.handout6;

import java.util.Arrays;


import net.finmath.exception.CalculationException;

/**
 * This class offers some methods involving matrices. In particular, via matrix inversion, 
 * it also permits to solve a linear system Ax=y, where the n \times n matrix A and the vector
 * y of length n are known. In order to do this, we have to invert the matrix.
 *  
 * @author Andrea Mazzon
 *
 */
public class UsefulMethodsMatrices {
	
	
	/**
	 * Computes the product of two matrices of doubles, and returns it as another matrix
	 * of doubles. Note: the number of columns of the first matrix must be equal to the 
	 * number of rows of the second matrix, otherwise an exception is thrown.
	 * 
	 * @param firstMatrix
	 * @param secondMatrix
	 * @return the product matrix
	 */
    public static double[][] multiply(double[][] firstMatrix, double[][] secondMatrix) {
    	
    	/*
    	 * firstMatrix[0] is the first row of the matrix. Its length represents the number
    	 * of columns of the matrix. On the other hand, secondMatrix.length is the number of rows.
    	 */
    	int numberOfColumnsFirstMatrix = firstMatrix[0].length;
    	int numberOfRowsFirstMatrix = firstMatrix.length;
    	
    	int numberOfColumnsSecondMatrix = secondMatrix[0].length;
    	int numberOfRowsSecondMatrix = secondMatrix.length;
    	
        if (numberOfColumnsFirstMatrix != numberOfRowsSecondMatrix) {
            throw new IllegalStateException("invalid dimensions");
        }

        /*
         * Dimensions are of course: number of rows of the first matrix and number of columns
         * of the second one
         */
        double[][] productMatrix = new double[numberOfRowsFirstMatrix][numberOfColumnsSecondMatrix];
        for (int i = 0; i < numberOfRowsFirstMatrix; i++) {
            for (int j = 0; j < numberOfColumnsSecondMatrix; j++) {
            	//we want to compute productMatrix[i][j]
                double sumOfElements = 0;
                for (int k = 0; k < numberOfColumnsFirstMatrix; k++) {
                	sumOfElements += firstMatrix[i][k] * secondMatrix[k][j];
                }
                productMatrix[i][j] = sumOfElements;
            }
        }

        return productMatrix;
    }
    
    /**
	 * Computes the product of a matrix of doubles with a vector of doubles, and returns
	 * it as another vector of doubles. Note: the number of columns of the matrix must be
	 * equal to the length of the vector, otherwise an exception is thrown.
	 * 
	 * @param matrix
	 * @param vector
	 * @return the product vector
	 */
    public static double[] multiply(double[][] matrix, double[] vector) {
    	
    	int numberOfColumnsMatrix = matrix[0].length;
    	int numberOfRowsMatrix = matrix.length;
    	
    	int vectorLength = vector.length;
    	
        if (numberOfColumnsMatrix !=  vectorLength) {
            throw new IllegalStateException("invalid dimensions");
        }

        double[] vectorRepresentingProduct = new double[numberOfRowsMatrix];
        
        for (int i = 0; i < numberOfRowsMatrix; i++) {
                double sum = 0;
                for (int k = 0; k < numberOfColumnsMatrix; k++) {
                    sum += matrix[i][k] * vector[k];
                }
                vectorRepresentingProduct[i] = sum;
            }
        return vectorRepresentingProduct;
    }
    
    
 
    
    /**
	 * Computes the element-wise product of a matrix of doubles with a double, and returns
	 * it as another matrix of doubles. 
	 * 
	 * @param matrix
	 * @param element
	 * @return the product vector
	 */
    public static double[][] multiply(double[][] matrix, double multiplier) {
    	int numberOfColumnsMatrix = matrix[0].length;
    	int numberOfRowsMatrix = matrix.length;
    	
        double[][] productMatrix = new double[numberOfRowsMatrix][numberOfColumnsMatrix];
		// double for loop to multiply every element with the double multiplier
        for (int i = 0; i < numberOfRowsMatrix; i++) {
            for (int j = 0; j < numberOfColumnsMatrix; j++) {
            	productMatrix[i][j] += matrix[i][j] * multiplier;
            }
        }
        return productMatrix;
    }
    
    
	/**
	 * It returns the transpose of a matrix of doubles
	 *
	 * @param matrix
	 * @return transpose of matrix
	 */
	public static double[][] transpose(double matrix[][]) {

		int numberOfRows = matrix.length;
		// matrix[0] is the first row of the matrix
		int numberOfColumns = matrix[0].length;// number of columns: length of the row
		double[][] transpose = new double[numberOfColumns][numberOfRows];

		// double for loop to transpose the matrix
		for (int i = 0; i < numberOfColumns; i++) {
			for (int j = 0; j < numberOfRows; j++) {
				transpose[i][j] = matrix[j][i];
			}
		}
		return transpose;
	}
	
	
	/*
     * For a given n \times n matrix, it returns a (n-1) \times (n-1) matrix which is the original
     * one "minus" the row and the column whose indices are specified as arguments of this method. 
     */
    private static double[][] submatrixExcludingRowAndColumn(double[][] matrix, int row, int column) {
    	
    	int dimensionOfTheMatrix = matrix.length;
    	
        double[][] submatrix = new double[dimensionOfTheMatrix - 1][dimensionOfTheMatrix - 1];

        for (int i = 0; i < dimensionOfTheMatrix; i++) {
            for (int j = 0; j < dimensionOfTheMatrix; j++) {
            	//we want to exclude the i-th row and j-th column
            	if ( i != row && j != column){
                    submatrix[i < row ? i : i - 1][j < column ? j : j - 1] = matrix[i][j];
            	}
            }
        }
        return submatrix;
    }
    
    
	/**
	 * Computes the determinant of a matrix of doubles and returns it as double. 
	 * Note: the matrix must be a square one, otherwise an exception is thrown. 
	 * 
	 * @param matrix
	 * @return the determinant of the matrix
	 */
    public static double determinant(double[][] matrix) {
        
    	if (matrix.length != matrix[0].length) {
            throw new IllegalStateException("invalid dimensions");
        }
    	
    	int dimensionOfTheMatrix = matrix.length;
    	
        if (dimensionOfTheMatrix == 1) {
            return matrix[0][0];
        }
        
        if (dimensionOfTheMatrix == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0];
        }

        double determinant = 0;
        
        //same old rule for the determinant..
        for (int i = 0; i < dimensionOfTheMatrix; i++) {
            determinant += Math.pow(-1, i) * matrix[0][i]
                    * determinant(submatrixExcludingRowAndColumn(matrix, 0, i));
        }
        return determinant;
    }
    
    
    /**
     * It returns the inverse of a square matrix, and it returns it as a matrix
     * of doubles. Note: the matrix must be a square one, otherwise an exception is thrown.
     * 
     * @param matrix
     * @return the inverse of the matrix
     */
    public static double[][] inverse(double[][] matrix) {
    	
    	//check if the matrix is square
    	if (matrix.length != matrix[0].length) {
            throw new IllegalStateException("invalid dimensions");
        }

    	int dimensionOfTheMatrix = matrix.length;
    	
    	/*
    	 * The inverse is the transpose of the cofactor matrix divided (element-wise) by 
    	 * the determinant
    	 */
        double[][] cofactorMatrix = new double[dimensionOfTheMatrix][dimensionOfTheMatrix];

        // first, we compute the cofactor matrix
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
            	cofactorMatrix[i][j] = Math.pow(-1, i + j) * determinant(submatrixExcludingRowAndColumn(matrix, i, j));
            }
        }
        
        //then we get the adjugate matrix: transpose of the cofactor matrix
        double[][] adjugateMatrix = transpose(cofactorMatrix);
        
        // we compute the determinant of the original matrix and we invert it..
        double inverseOfDeterminant = 1.0 / determinant(matrix);
        
        //and we compute the inverse
        double[][] inverseMatrix = multiply(adjugateMatrix, inverseOfDeterminant);
        
        return inverseMatrix;
    }
    
 
    /**
     * It solves a linear system Ax=y, with A square matrix of doubles of dimension n and
     * y is a vector of doubles of length n, and it returns the solution as a vector of doubles.
     * Note: the matrix must be a square one, otherwise an exception is thrown.
     * 
     * @param matrixForVariables
     * @param knownTerms
     * @return the solution
     */
    public static double[] solveLinearSystem(double[][] matrix, double[] knownTerms) {
    	//check if the matrix is square
        if (matrix.length != knownTerms.length) {
            throw new IllegalStateException("Invalid dimensions");
        }
        
        //we compute the solution just inverting the matrix
        double[][] inverseOfMatrix = inverse(matrix);

        return multiply(inverseOfMatrix, knownTerms);
    }
    
    
    public static void main(String[] args) throws CalculationException {

		final double[][] matrix = { {2, 3, 1}, {1, 3, 7}, {2,1,4}};
		
		final double[][] inverse = inverse(matrix);
		final double[][] hopefullyIdentity = multiply(inverse, matrix);
		
		for (int i = 0; i<matrix.length; i++) {
			System.out.println(Arrays.toString(hopefullyIdentity[i]));
		}
    }
}
