package edu.mit.media.funf.probe.builtin;

import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import edu.mit.media.funf.FFT;
import edu.mit.media.funf.MFCC;
import edu.mit.media.funf.Window;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.builtin.ProbeKeys.AudioFeaturesKeys;

public class AudioFeaturesProbe extends Probe implements AudioFeaturesKeys {
	private static String STREAM_NAME = "hdl_audio";
	
	private static int RECORDER_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
	private static int RECORDER_SAMPLERATE = 8000;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private static int FFT_SIZE = 8192;
	private static int MFCCS_VALUE = 12;
	private static int MEL_BANDS = 20;
	private static int STREAM_FEATURES = 20;
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
	public Parameter[] getAvailableParameters() {
		return new Parameter[] {
				new Parameter(Parameter.Builtin.DURATION, 30L),
				new Parameter(Parameter.Builtin.PERIOD, 300L),
				new Parameter(Parameter.Builtin.START, 0L),
				new Parameter(Parameter.Builtin.END, 0L)
		};
	}

	@Override
	public String[] getRequiredFeatures() {
		return null;
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[] {
				android.Manifest.permission.RECORD_AUDIO
		};
	}

	@Override
	protected void onDisable() {
		if (null != audioRecorder)
	    {
	        audioRecorder.stop();
	        audioRecorder.release();
	        audioRecorder = null;
	        recordingThread = null;
	    }
	}

	@Override
	protected void onEnable() {
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
	}

	@Override
	protected void onRun(Bundle params) {
	    prevSecs = (double)System.currentTimeMillis()/1000.0d;
	   // String timeStamp = timeString(startTime);

	    // Create new stream file(s)
	    //if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
	    //{
	    //	audioStreamRaw = new DataOutputStream(openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_RAW));
	    //}
	    //audioStreamFeatures = new DataOutputStream(openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN));
	    
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
	    
	    //writeLogTextLine("Audio recording started");
	}

	@Override
	protected void onStop() {
		audioRecorder.stop();
	}
	
	

	@Override
	public void sendProbeData() {
		// TODO Auto-generated method stub

	}

	private void handleAudioStream()
	{
        short data16bit[] = new short[bufferSamples];
    	byte data8bit[] = new byte[bufferSize];
    	double fftBufferR[] = new double[FFT_SIZE];
    	double fftBufferI[] = new double[FFT_SIZE];
    	double featureCepstrum[] = new double[MFCCS_VALUE];
    	
	    int readAudioSamples = 0;
	    while (isRunning())
	    {
	    	readAudioSamples = audioRecorder.read(data16bit, 0, bufferSamples);
	    	double currentSecs = (double)(System.currentTimeMillis())/1000.0d;
	    	double diffSecs = currentSecs - prevSecs;
	    	prevSecs = currentSecs;
	    	
	    	Bundle data = new Bundle();
	    	if (readAudioSamples > 0)
	    	{
	    		double fN = (double)readAudioSamples;

	    		data.putDouble(DIFF_SECS, diffSecs);

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
	    		data.putDouble(L1_NORM, accum/fN);

	    		// L2-norm
	    		accum = 0;
	    		for (int i = 0; i < readAudioSamples; i ++)
	    		{
	    			accum += (double)data16bit[i]*(double)data16bit[i];
	    		}
	    		data.putDouble(L2_NORM, Math.sqrt(accum/fN));

	    		// Linf-norm
	    		accum = 0;
	    		for (int i = 0; i < readAudioSamples; i ++)
	    		{
	    			accum = Math.max(Math.abs((double)data16bit[i]),accum);
	    		}
	    		data.putDouble(LINF_NORM, Math.sqrt(accum));

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
	    		data.putDoubleArray(PSD_ACROSS_FREQUENCY_BANDS, psdAcrossFrequencyBands);

	    		// Get MFCCs
	    		featureCepstrum = featureMFCC.cepstrum(fftBufferR, fftBufferI);
	    		data.putDoubleArray(MFCCS, featureCepstrum);

	    		// Write out raw audio, if enabled
//	    		if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
//	    		{
//	    			try
//	    			{
//	    				audioStreamRaw.write(data8bit, 0, readAudioSamples*2);
//	    				audioStreamRaw.flush();
//	    			}
//	    			catch (IOException e)
//	    			{
//	    				e.printStackTrace();
//	    			}
//	    		}

	    		// Write out features
	    		sendProbeData((long)currentSecs, data);
	    		
	    	}
	    }

	}
}
