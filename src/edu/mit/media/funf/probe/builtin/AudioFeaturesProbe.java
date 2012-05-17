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
package edu.mit.media.funf.probe.builtin;

import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.mit.media.funf.math.FFT;
import edu.mit.media.funf.math.MFCC;
import edu.mit.media.funf.math.Window;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.ContinuousProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AudioFeaturesKeys;

/**
 * @author Max Little and Alan Gardner
 *
 */
@RequiredFeatures("android.hardware.microphone")
@RequiredPermissions(android.Manifest.permission.RECORD_AUDIO)
public class AudioFeaturesProbe extends Base implements ContinuousProbe, AudioFeaturesKeys {

	// TODO: may need to change this to 44100 sampling to make it more compatible across devices
	// Alternatively, we could dynamically discover it 
	// http://stackoverflow.com/questions/6745344/record-audio-using-audiorecord-in-android
	
	private static int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static int RECORDER_SAMPLERATE = 8000;
	
	private static int FFT_SIZE = 8192;
	private static int MFCCS_VALUE = 12;
	private static int MEL_BANDS = 20;
	private static double[] FREQ_BANDEDGES = {50,250,500,1000,2000};
	
	private Thread recordingThread = null;
	private int bufferSize = 0;
	private int bufferSamples = 0;
	private static int[] freqBandIdx = null;
	
    private FFT featureFFT = null;
    private MFCC featureMFCC = null;
    private Window featureWin = null;
    
    private AudioRecord audioRecorder = null;
	
    public double prevSecs = 0;
	public double[] featureBuffer = null;
	
	@Override
	protected void onStart() {
		super.onStart();
		

    	bufferSize = AudioRecord.getMinBufferSize(
        		RECORDER_SAMPLERATE,
        		RECORDER_CHANNELS,
        		RECORDER_AUDIO_ENCODING);

	    bufferSize = Math.max(bufferSize, RECORDER_SAMPLERATE*2);
	    bufferSamples = bufferSize/2;
	    
	    
	    //allocateFrameFeatureBuffer(STREAM_FEATURES);
	    
	    featureFFT = new FFT(FFT_SIZE);
	    featureWin = new Window(bufferSamples);
	    featureMFCC = new MFCC(FFT_SIZE, MFCCS_VALUE, MEL_BANDS, RECORDER_SAMPLERATE);
	    
	    freqBandIdx = new int[FREQ_BANDEDGES.length];
	    for (int i = 0; i < FREQ_BANDEDGES.length; i ++)
	    {
	    	freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)RECORDER_SAMPLERATE));
	    	//writeLogTextLine("Frequency band edge " + i + ": " + Integer.toString(freqBandIdx[i]));
	    }
	    
	    audioRecorder = new AudioRecord(
	    		RECORDER_SOURCE,
				RECORDER_SAMPLERATE,
				RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING,
				bufferSize);
	    prevSecs = (double)System.currentTimeMillis()/1000.0d;
	    audioRecorder.startRecording();
	    recordingThread = new Thread(new Runnable()
	    {
	        @Override
	        public void run()
	        {
	            handleAudioStream();
	        }
	    }, "AudioRecorder Thread");
	    recordingThread.start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		audioRecorder.stop();
        audioRecorder.release();
        audioRecorder = null;
        recordingThread = null;
	}

	
	private void handleAudioStream()
	{
        short data16bit[] = new short[bufferSamples];
    	byte data8bit[] = new byte[bufferSize];
    	double fftBufferR[] = new double[FFT_SIZE];
    	double fftBufferI[] = new double[FFT_SIZE];
    	double featureCepstrum[] = new double[MFCCS_VALUE];
    	
	    int readAudioSamples = 0;
	    while (State.RUNNING.equals(getState()))
	    {
	    	readAudioSamples = audioRecorder.read(data16bit, 0, bufferSamples);
	    	double currentSecs = (double)(System.currentTimeMillis())/1000.0d;
	    	double diffSecs = currentSecs - prevSecs;
	    	prevSecs = currentSecs;
	    	
	    	JsonObject data = new JsonObject();
	    	if (readAudioSamples > 0)
	    	{
	    		double fN = (double)readAudioSamples;

	    		data.addProperty(DIFF_SECS, diffSecs);

	    		// Convert shorts to 8-bit bytes for raw audio output
	    		for (int i = 0; i < bufferSamples; i ++)
	    		{
	    			data8bit[i*2] = (byte)data16bit[i];
	    			data8bit[i*2+1] = (byte)(data16bit[i] >> 8);
	    		}
	    		//		        	writeLogTextLine("Read " + readAudioSamples + " samples");

	    		// L1-norm
	    		double accum = 0;
	    		for (int i = 0; i < readAudioSamples; i ++)
	    		{
	    			accum += Math.abs((double)data16bit[i]);
	    		}
	    		data.addProperty(L1_NORM, accum/fN);

	    		// L2-norm
	    		accum = 0;
	    		for (int i = 0; i < readAudioSamples; i ++)
	    		{
	    			accum += (double)data16bit[i]*(double)data16bit[i];
	    		}
	    		data.addProperty(L2_NORM, Math.sqrt(accum/fN));

	    		// Linf-norm
	    		accum = 0;
	    		for (int i = 0; i < readAudioSamples; i ++)
	    		{
	    			accum = Math.max(Math.abs((double)data16bit[i]),accum);
	    		}
	    		data.addProperty(LINF_NORM, Math.sqrt(accum));

	    		// Frequency analysis
	    		Arrays.fill(fftBufferR, 0);
	    		Arrays.fill(fftBufferI, 0);

	    		// Convert audio buffer to doubles
	    		for (int i = 0; i < readAudioSamples; i++)
	    		{
	    			fftBufferR[i] = data16bit[i];
	    		}

	    		// In-place windowing
	    		featureWin.applyWindow(fftBufferR);

	    		// In-place FFT
	    		featureFFT.fft(fftBufferR, fftBufferI);

	    		// Get PSD across frequency band ranges
	    		double[] psdAcrossFrequencyBands = new double[FREQ_BANDEDGES.length - 1];
	    		for (int b = 0; b < (FREQ_BANDEDGES.length - 1); b ++)
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
	    		Gson gson = getGson();
	    		data.add(PSD_ACROSS_FREQUENCY_BANDS, gson.toJsonTree(psdAcrossFrequencyBands));

	    		// Get MFCCs
	    		featureCepstrum = featureMFCC.cepstrum(fftBufferR, fftBufferI);
	    		data.add(MFCCS, gson.toJsonTree(featureCepstrum));

	    		// Write out features
	    		sendData(data);
	    		
	    	}
	    }

	}
	
}
