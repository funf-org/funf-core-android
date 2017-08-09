/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.media.funf.math;

public class FFT
{
	int n, m;

	// Lookup tables.  Only need to recompute when size of FFT changes.
	double[] cos;
	double[] sin;

	public FFT(int n)
	{
		this.n = n;
		this.m = (int)(Math.log(n) / Math.log(2));

		// Make sure n is a power of 2
		if (n != (1<<m))
		{
			throw new RuntimeException("FFT length must be power of 2");
		}

		// precompute tables
		cos = new double[n/2];
		sin = new double[n/2];

		for(int i=0; i<n/2; i++)
		{
			cos[i] = Math.cos(-2*Math.PI*i/n);
			sin[i] = Math.sin(-2*Math.PI*i/n);
		}
	}



	/***************************************************************
	 * fft.c
	 * Douglas L. Jones 
	 * University of Illinois at Urbana-Champaign 
	 * January 19, 1992 
	 * http://cnx.rice.edu/content/m12016/latest/
	 * 
	 *   fft: in-place radix-2 DIT DFT of a complex input 
	 * 
	 *   input: 
	 * n: length of FFT: must be a power of two 
	 * m: n = 2**m 
	 *   input/output 
	 * x: double array of length n with real part of data 
	 * y: double array of length n with imag part of data 
	 * 
	 *   Permission to copy and use this program is granted 
	 *   as long as this header is included. 
	 ****************************************************************/
	public void fft(double[] re, double[] im)
	{
		int i,j,k,n1,n2,a;
		double c,s,t1,t2;

		// Bit-reverse
		j = 0;
		n2 = n/2;
		for (i=1; i < n - 1; i++)
		{
			n1 = n2;
			while ( j >= n1 )
			{
				j = j - n1;
				n1 = n1/2;
			}
			j = j + n1;

			if (i < j)
			{
				t1 = re[i];
				re[i] = re[j];
				re[j] = t1;
				t1 = im[i];
				im[i] = im[j];
				im[j] = t1;
			}
		}

		// FFT
		n1 = 0;
		n2 = 1;

		for (i=0; i < m; i++)
		{
			n1 = n2;
			n2 = n2 + n2;
			a = 0;

			for (j=0; j < n1; j++)
			{
				c = cos[a];
				s = sin[a];
				a +=  1 << (m-i-1);

				for (k=j; k < n; k=k+n2)
				{
					t1 = c*re[k+n1] - s*im[k+n1];
					t2 = s*re[k+n1] + c*im[k+n1];
					re[k+n1] = re[k] - t1;
					im[k+n1] = im[k] - t2;
					re[k] = re[k] + t1;
					im[k] = im[k] + t2;
				}
			}
		}
	}                          

}