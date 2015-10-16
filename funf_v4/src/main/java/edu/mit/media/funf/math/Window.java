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

public class Window
{
	public double[] window;
	public int n;
	
	public Window(int windowSize)
	{
		n = windowSize;
		
		// Make a Hamming window
		window = new double[n];
		for(int i = 0; i < n; i++)
		{
			window[i] = 0.54 - 0.46*Math.cos(2*Math.PI*(double)i/((double)n-1));
		}
	}

	public void applyWindow(double[] buffer)
	{
		for (int i = 0; i < n; i ++)
		{
			buffer[i] *= window[i];
		}
	}

}
