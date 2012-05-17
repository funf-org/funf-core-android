/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.math;


public class MFCC
{
	private static double minMelFreq = 0;
	private static double maxMelFreq = 4000;
	private static double lifterExp = 0.6;
	private int numCoeffs;
	private int melBands;
	private int numFreqs;
	private double sampleRate;
	public Matrix melWeights = null;
	public Matrix dctMat = null;
	public double[] lifterWeights;

	public MFCC(int fftSize, int numCoeffs, int melBands, double sampleRate)
	{
	    // Precompute mel-scale auditory perceptual spectrum
	    melWeights = new Matrix(melBands, fftSize, 0);
	    
	    // Number of non-redundant frequency bins
	    numFreqs = fftSize/2 + 1;
	    this.numCoeffs = numCoeffs;
	    this.melBands = melBands;
	    this.sampleRate = sampleRate;
	    
	    double fftFreqs[] = new double[fftSize];
	    for (int i = 0; i < fftSize; i ++)
	    {
	    	fftFreqs[i] = (double)i/(double)fftSize*this.sampleRate;
	    }
	    
	    double minMel = fhz2mel(minMelFreq);
	    double maxMel = fhz2mel(maxMelFreq);
	    
	    double binFreqs[] = new double[melBands + 2];
	    for (int i = 0; i < melBands + 2; i ++)
	    {
	    	binFreqs[i] = fmel2hz(minMel + (double)i/((double)melBands + 1.0) * (maxMel - minMel));
	    }
	    
	    for (int i = 0; i < melBands; i ++)
	    {
	    	for (int j = 0; j < fftSize; j ++)
	    	{
	    		double loSlope = (fftFreqs[j] - binFreqs[i])/(binFreqs[i+1] - binFreqs[i]);
		    	double hiSlope = (binFreqs[i+2] - fftFreqs[j])/(binFreqs[i+2] - binFreqs[i+1]);
		    	melWeights.A[i][j] = Math.max(0, Math.min(loSlope, hiSlope));
	    	}
	    }
	    
	    // Keep only positive frequency parts of Fourier transform
	    melWeights = melWeights.getMatrix(0, melBands - 1, 0, numFreqs - 1);
	    
	    // Precompute DCT matrix
	    dctMat = new Matrix(numCoeffs, melBands, 0);
	    double scale = Math.sqrt(2.0/melBands);
	    for (int i = 0; i < numCoeffs; i ++)
	    {
	    	for (int j = 0; j < melBands; j ++)
	    	{
	    		double phase = j*2 + 1;
		    	dctMat.A[i][j] = Math.cos((double)i*phase/(2.0*(double)melBands)*Math.PI)*scale;
	    	}
	    }
	    double root2 = 1.0/Math.sqrt(2.0);
	    for (int j = 0; j < melBands; j ++)
	    {
	    	dctMat.A[0][j] *= root2;
	    }
	    
	    // Precompute liftering vector
	    lifterWeights = new double[numCoeffs];
	    lifterWeights[0] = 1.0;
	    for (int i = 1; i < numCoeffs; i ++)
	    {
	    	lifterWeights[i] = Math.pow((double)i, lifterExp);
	    }
	}
	
	public double[] cepstrum(double[] re, double[] im)
	{
		Matrix powerSpec = new Matrix(numFreqs, 1);
		for (int i = 0; i < numFreqs; i ++)
		{
			powerSpec.A[i][0] = re[i]*re[i] + im[i]*im[i];
		}

		// melWeights - melBands x numFreqs
		// powerSpec  - numFreqs x 1
		// melWeights*powerSpec - melBands x 1
		// aSpec      - melBands x 1
		// dctMat     - numCoeffs x melBands
		// dctMat*log(aSpec) - numCoeffs x 1
		
		Matrix aSpec = melWeights.times(powerSpec);
		Matrix logMelSpec = new Matrix(melBands, 1);
		for (int i = 0; i < melBands; i ++)
		{
			logMelSpec.A[i][0] = Math.log(aSpec.A[i][0]);
		}

		Matrix melCeps = dctMat.times(logMelSpec);
			
		double[] ceps = new double[numCoeffs];
		for (int i = 0; i < numCoeffs; i ++)
		{
			ceps[i] = lifterWeights[i]*melCeps.A[i][0];
		}

		return ceps;
	}

	
	public double fmel2hz(double mel)
	{
		return 700.0*(Math.pow(10.0, mel/2595.0) - 1.0);
	}
	
	public double fhz2mel(double freq)
	{
		return 2595.0*Math.log10(1.0 + freq/700.0);
	}

}
