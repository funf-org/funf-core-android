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
package edu.mit.media.funf.probe.builtin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.math.FFT;
import edu.mit.media.funf.math.Window;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredProbes;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AccelerometerFeaturesKeys;
import edu.mit.media.funf.time.TimeUtil;

/**
 * 
 * @author Max Little and Alan Gardner
 *
 */
@RequiredFeatures("android.hardware.sensor.accelerometer")
@RequiredProbes(AccelerometerSensorProbe.class)
@Schedule.DefaultSchedule(interval=120, duration=15)
public class AccelerometerFeaturesProbe extends Base implements ContinuousProbe, AccelerometerFeaturesKeys {
	
	@Configurable
	private double frameDuration = 1.0;
	
	@Configurable
	private int fftSize = 128;
	
	@Configurable
	private double[] freqBandEdges = {0,1,3,6,10};

	// Assumed maximum accelerometer sampling rate
	private static final double SENSOR_MAX_RATE = 100.0;	

	
	private AccelerometerListener listener;
	private double prevSecs;
	private double prevFrameSecs;
	private double frameTimer = 0;
	private double[][] frameBuffer = null;
	private double[] fftBufferR = null;
	private double[] fftBufferI = null;
	private int frameSamples = 0;
	private int frameBufferSize = 0;
	
	private FFT featureFFT = null;
    private Window featureWin = null;
    private static int[] freqBandIdx = null;
	
    private class AccelerometerListener implements DataListener {

    	private Gson gson = getGson();
    	
		@Override
		public void onDataReceived(IJsonObject completeProbeUri, IJsonObject acclerometerData) {
			double currentSecs = acclerometerData.get(AccelerometerSensorProbe.TIMESTAMP).getAsDouble();
			double x = acclerometerData.get(AccelerometerSensorProbe.X).getAsDouble();
			double y = acclerometerData.get(AccelerometerSensorProbe.Y).getAsDouble();
			double z = acclerometerData.get(AccelerometerSensorProbe.Z).getAsDouble();
			
			if (prevSecs == 0)
			{
				prevSecs = currentSecs;
			}
        	double diffSecs = currentSecs - prevSecs;
        	prevSecs = currentSecs;

        	frameBuffer[frameSamples][0] = x;
        	frameBuffer[frameSamples][1] = y;
        	frameBuffer[frameSamples][2] = z;
        	frameSamples ++;
        	frameTimer += diffSecs;
        	
        	if ((frameTimer >= frameDuration) || (frameSamples == (frameBufferSize - 1))) {
			
        		JsonObject data = new JsonObject();
        		double fN = (double)frameSamples;
        		if (prevFrameSecs == 0) {
                	prevFrameSecs = currentSecs;
                }
        		
        		double diffFrameSecs = currentSecs - prevFrameSecs;
                prevFrameSecs = currentSecs;
                data.addProperty(TIMESTAMP, currentSecs);
                data.addProperty(DIFF_FRAME_SECS, new BigDecimal(diffFrameSecs).setScale(TimeUtil.MICRO, RoundingMode.HALF_EVEN));
                data.addProperty(NUM_FRAME_SAMPLES, frameSamples);
                
                data.add(X, getFeatures(0, fN));
                data.add(Y, getFeatures(1, fN));
                data.add(Z, getFeatures(2, fN));
                
                sendData(data);
                
                // Reset frame buffer counters
        		frameSamples = 0;
        		frameTimer = 0;

        		// Ensure buffer is zero-padded
        		for (double[] row: frameBuffer) {
        			Arrays.fill(row, 0);
        		}
        	}
		}
		
		private JsonObject getFeatures(int i, double fN) {
			JsonObject data = new JsonObject();
			// Mean
			double mean = 0;
			for (int j = 0; j < frameSamples; j ++)
				mean += frameBuffer[j][i];
			mean /= fN;
			data.addProperty(MEAN, mean);
    		
			double accum;

			// Absolute central moment
			accum = 0;
			for (int j = 0; j < frameSamples; j ++)
				accum += Math.abs(frameBuffer[j][i] - mean);
			data.addProperty(ABSOLUTE_CENTRAL_MOMENT, accum/fN);
			
			// Standard deviation
			accum = 0;
			for (int j = 0; j < frameSamples; j ++)
				accum += (frameBuffer[j][i] - mean)*(frameBuffer[j][i] - mean);
			data.addProperty(STANDARD_DEVIATION, Math.sqrt(accum/fN));

			// Max deviation
			accum = 0;
			for (int j = 0; j < frameSamples; j ++)
				accum = Math.max(Math.abs(frameBuffer[j][i] - mean),accum);
			data.addProperty(MAX_DEVIATION, accum);
			
            // Frequency analysis with zero-padding
        	Arrays.fill(fftBufferR, 0);
        	Arrays.fill(fftBufferI, 0);
        	
        	// Drop accel. values into FFT buffer
        	for (int j = 0; j < frameSamples; j++)
        	{
        		fftBufferR[j] = frameBuffer[j][i] - mean;
        	}

        	// In-place windowing
        	featureWin.applyWindow(fftBufferR);
        	
        	// In-place FFT
        	featureFFT.fft(fftBufferR, fftBufferI);

        	// Get PSD across frequency band ranges
        	double[] psdAcrossFrequencyBands = new double[freqBandEdges.length - 1];
        	for (int b = 0; b < (freqBandEdges.length - 1); b ++)
        	{
        		int j = freqBandIdx[b];
        		int k = freqBandIdx[b+1];
        		accum = 0;
        		for (int h = j; h < k; h ++)
        		{
        			accum += fftBufferR[h]*fftBufferR[h] + fftBufferI[h]*fftBufferI[h];
        		}
        		psdAcrossFrequencyBands[b] = accum/((double)(k - j));
        	}
        	data.add(PSD_ACROSS_FREQUENCY_BANDS, gson.toJsonTree(psdAcrossFrequencyBands));
        	return data;
		}

		@Override
		public void onDataCompleted(IJsonObject completeProbeUri, JsonElement checkpoint) {
			// TODO Auto-generated method stub
			
		}
    	
    }
    
	@Override
	protected void onEnable() {
		super.onEnable();
		frameBufferSize = (int)Math.ceil(SENSOR_MAX_RATE/frameDuration);
        frameBuffer = new double[frameBufferSize][3];
        //writeLogTextLine("Accelerometer maximum frame size (samples): " + frameBufferSize);
        //writeLogTextLine("Accelerometer maximum frame duation (secs): " + SENSOR_FRAME_DURATION);
        
        //allocateFrameFeatureBuffer(STREAM_FEATURES);

        featureFFT = new FFT(fftSize);
	    featureWin = new Window(frameBufferSize);

        fftBufferR = new double[fftSize];
    	fftBufferI = new double[fftSize];

    	freqBandIdx = new int[freqBandEdges.length];
	    for (int i = 0; i < freqBandEdges.length; i ++) {
	    	freqBandIdx[i] = Math.round((float)freqBandEdges[i]*((float)fftSize/(float)SENSOR_MAX_RATE));
	    }
	    listener = new AccelerometerListener();
	    getGson().fromJson("{}", AccelerometerSensorProbe.class).registerPassiveListener(listener);
		// TODO: Register listener for accelerometer probe
	}

	@Override
	protected void onStart() {
		super.onStart();
		reset();
		getGson().fromJson(DEFAULT_CONFIG, AccelerometerSensorProbe.class).registerListener(listener);
	}

	@Override
	protected void onStop() {
		super.onStop();
		getGson().fromJson(DEFAULT_CONFIG, AccelerometerSensorProbe.class).unregisterListener(listener);
		reset();
	}

	@Override
	protected void onDisable() {
		// TODO Auto-generated method stub
		super.onDisable();
	}
	
	private void reset() {
		prevSecs = 0;
		prevFrameSecs = 0;
	    frameTimer = 0;
	    frameSamples = 0;
	    // Ensure frame buffer is cleared
	 	for (double[] row: frameBuffer)
	 			Arrays.fill(row, 0);
	}
	
}
