/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
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

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OutputFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.HighBandwidthKeys;
import edu.mit.media.funf.time.TimeUtil;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.NameGenerator;
import edu.mit.media.funf.util.NameGenerator.SystemUniqueTimestampNameGenerator;

@DisplayName("Video Capture Probe")
@RequiredPermissions({android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA})
@RequiredFeatures("android.hardware.camera")
@Schedule.DefaultSchedule(interval=1800)
public class VideoCaptureProbe extends ImpulseProbe implements PassiveProbe, HighBandwidthKeys, SurfaceHolder.Callback {

	@Configurable
	private String fileNameBase = "videorec";
	
	@Configurable
	private String folderName = "videocapturetest";
		
	@Configurable
	private int selectedCamera = 1; // BACK_FACING = 0, FRONT_FACING = 1
		
	@Configurable
	private int cameraOpenDelay = 1; // allow delay for camera open in seconds
	
	@Configurable
	private int recordingLength = 5; // Duration of recording in seconds
	
	@Configurable
	private String videoProfile = "LOW"; // LOW, HIGH, QCIF, CIF, 480P, 720P, 1080P, QVGA
	// If the camera does not support the configured profile, 
	// by default the "LOW" profile will be selected
	
	@Configurable
	private boolean timeLapse = false;
	
	@Configurable
	private double captureRate = 24;
		
	private enum CameraProfile {
      QUALITY_LOW,
      QUALITY_QCIF,
      QUALITY_CIF,
      QUALITY_480P,
      QUALITY_720P,
      QUALITY_1080P,
      QUALITY_QVGA,
      QUALITY_HIGH,
      QUALITY_TIME_LAPSE_LOW,
      QUALITY_TIME_LAPSE_QCIF,
      QUALITY_TIME_LAPSE_CIF,
      QUALITY_TIME_LAPSE_480P,
      QUALITY_TIME_LAPSE_720P,
      QUALITY_TIME_LAPSE_1080P,
      QUALITY_TIME_LAPSE_QVGA,
      QUALITY_TIME_LAPSE_HIGH
    }
	
	private String mFileName;	
	private String mFolderPath;
	private NameGenerator mNameGenerator;

	private Camera mCamera;
	private int mCameraId;
	private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private Parameters mParameters;
    private int mCameraAngle;
    
    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";

    private WindowManager mWindowManager;
	private LayoutParams mParams;
	
	private MediaRecorder mRecorder;
	
	private class RecordingCountDown extends CountDownTimer {

		public RecordingCountDown(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
		}

		@Override
		public void onFinish() {
			stopRecording();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			//Log.d(LogUtil.TAG, "Video capture: seconds remaining = " + millisUntilFinished / 1000);
		}
		
	}
	
	private RecordingCountDown mCountDown;

	@Override
	protected void onEnable() {
		super.onEnable();
		mNameGenerator = new SystemUniqueTimestampNameGenerator(getContext());
		mFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() 
            + "/" + folderName;
        File folder = new File(mFolderPath);
        if (!folder.exists()) {
          folder.mkdirs();
        } else if (!folder.isDirectory()) {
          folder.delete();
          folder.mkdirs();
        }
        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
        Log.d(LogUtil.TAG, "Video capture: start");
        
        mSurfaceView = new SurfaceView(getContext());
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
        
        mWindowManager.addView(mSurfaceView, mParams);
        
        mSurfaceView.setZOrderOnTop(false);
        mHolder.setFormat(PixelFormat.TRANSPARENT);        
	}
	
	@Override
	protected void onDisable() {
		super.onDisable();
		
	}
	
	private boolean startRecording() {
		mRecorder = new MediaRecorder();
		mRecorder.setCamera(mCamera);
				
		CamcorderProfile camProfile;
        int videoQuality = getConfiguredProfile();
		if (CamcorderProfile.hasProfile(mCameraId, videoQuality)) {
		    Log.d(LogUtil.TAG, "using camcorder profile: " + videoProfile);
		    camProfile = CamcorderProfile.get(mCameraId, videoQuality);
		} else {
		    Log.d(LogUtil.TAG, "profile unavailable, using: " + "LOW");
		    camProfile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
		    timeLapse = false;
		}

        mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		if (!timeLapse)
		    mRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		
		mRecorder.setProfile(camProfile);
        if (timeLapse) 
            mRecorder.setCaptureRate(captureRate);
		
		mFileName = mFolderPath + "/" + mNameGenerator.generateName(fileNameBase);
		mFileName += getFileFormat(camProfile.fileFormat); 
		mRecorder.setOutputFile(mFileName);
		
		mRecorder.setOrientationHint(computePictureRotation(mCameraAngle, selectedCamera));
		try {
			mRecorder.prepare();
	        mRecorder.start();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(LogUtil.TAG, "Video capture: error in preparing mediaRecorder");
			stop();
			return false;
		}
        return true;
	}
	
	private void stopRecording() {
		mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
		mWindowManager.removeView(mSurfaceView);
		Log.e(LogUtil.TAG, "Recording video: stop");
		
		JsonObject data = new JsonObject();
		data.addProperty(FILENAME, mFileName);
		sendData(data);
	}
	
	private void abortRecording() {
	  Log.e(LogUtil.TAG, "Recording video: abort");
	  if (mCamera != null) {
	      mCamera.stopPreview();
          mCamera.release();
          mCamera = null;
	  }
	  if (mHolder != null) {
          mHolder.removeCallback(this);
      }
	  if (mWindowManager != null) {
	      mWindowManager.removeView(mSurfaceView);
	  } 
	    
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		Log.d(LogUtil.TAG, "Video capture: surfaceChanged");
		mParameters = mCamera.getParameters();
		configureCameraParameters(mParameters);
		
		mCamera.startPreview();
		// to allow time for camera initiation on certain devices
		try {
			Thread.sleep(TimeUtil.secondsToMillis(cameraOpenDelay));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		mCountDown = new RecordingCountDown(TimeUtil.secondsToMillis(recordingLength), 1000);
		Log.e(LogUtil.TAG, "Recording video: start");
		mCamera.unlock();
		if (startRecording())
		    mCountDown.start();
		else {
		    abortRecording();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(LogUtil.TAG, "Video capture: surfaceCreated");
        if (safeCameraOpen()) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
                Log.e(LogUtil.TAG, "Video capture: error");
            }  
        } else {
            abortRecording();
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(LogUtil.TAG, "Video capture: surfaceDestroyed");
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
	}
	
	private boolean safeCameraOpen() {
		mCamera = openSelectedCamera(selectedCamera);
		if (mCamera == null) {
			Log.e(LogUtil.TAG, "Video capture: failed to access camera");
			stop();
			return false;
		} else {
		    return true;
		}
	}
	
	@SuppressLint("NewApi")
	private Camera openSelectedCamera(int type) {
	    int cameraCount = 0;
	    Camera cam = null;
	    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
	    cameraCount = Camera.getNumberOfCameras();
	    for ( int camIdx = 0; camIdx < cameraCount; camIdx++ ) {
	        Camera.getCameraInfo( camIdx, cameraInfo );
	        if (cameraInfo.facing == type) {
	            try {
	                cam = Camera.open( camIdx );
	                mCameraId = camIdx;
	            } catch (RuntimeException e) {
	                Log.e(LogUtil.TAG, "Camera failed to open: " + e.getLocalizedMessage());
	            }
	        }
	    }

	    return cam;
	}
		
	private void configureCameraParameters(Parameters cameraParams) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
            if (isPortrait()) {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
                mCameraAngle = 90;
            } else {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
                mCameraAngle = 0;
            }
        } else { // for 2.2 and later
            int rotation = mWindowManager.getDefaultDisplay().getRotation();
            switch (rotation) {
                case Surface.ROTATION_0: // This is display orientation
                    mCameraAngle = 90; // This is camera orientation
                    break;
                case Surface.ROTATION_90:
                    mCameraAngle = 0;
                    break;
                case Surface.ROTATION_180:
                    mCameraAngle = 270;
                    break;
                case Surface.ROTATION_270:
                    mCameraAngle = 180;
            		// image
                    break;
                default:
                    mCameraAngle = 90;
                    break;
            }
            Log.d(LogUtil.TAG, "angle: " + mCameraAngle);
            mCamera.setDisplayOrientation(mCameraAngle);
        }

        cameraParams.setRecordingHint(true);
        mCamera.setParameters(cameraParams);
    }

    public boolean isPortrait() {
        return (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }
    
    public static int computePictureRotation(int cameraAngle, int cameraType) {
    	if (cameraType == 1 && (cameraAngle == 90 || cameraAngle == 270)) {
    		// for front-facing camera, portrait and reverse-portrait orientation 
    		// gives mirror image of actual picture, which needs to be corrected
    		return (180 + cameraAngle) % 360;
    	}
    	return cameraAngle;
    }    
    
    public int getConfiguredProfile() {
        if (timeLapse) {
            String profileName = "QUALITY_TIME_LAPSE_" + videoProfile;
            switch (CameraProfile.valueOf(profileName)){
                case QUALITY_TIME_LAPSE_LOW: return CamcorderProfile.QUALITY_TIME_LAPSE_LOW; 
                case QUALITY_TIME_LAPSE_HIGH: return CamcorderProfile.QUALITY_TIME_LAPSE_HIGH;
                case QUALITY_TIME_LAPSE_QCIF: return CamcorderProfile.QUALITY_TIME_LAPSE_QCIF;
                case QUALITY_TIME_LAPSE_CIF: return CamcorderProfile.QUALITY_TIME_LAPSE_CIF;
                case QUALITY_TIME_LAPSE_480P: return CamcorderProfile.QUALITY_TIME_LAPSE_480P;
                case QUALITY_TIME_LAPSE_720P: return CamcorderProfile.QUALITY_TIME_LAPSE_720P;
                case QUALITY_TIME_LAPSE_1080P: return CamcorderProfile.QUALITY_TIME_LAPSE_1080P;
                case QUALITY_TIME_LAPSE_QVGA: return CamcorderProfile.QUALITY_TIME_LAPSE_QVGA;
                default: return CamcorderProfile.QUALITY_TIME_LAPSE_LOW;
            }
        } else {
            String profileName = "QUALITY_" + videoProfile;
            switch (CameraProfile.valueOf(profileName)){
                case QUALITY_LOW: return CamcorderProfile.QUALITY_LOW; 
                case QUALITY_HIGH: return CamcorderProfile.QUALITY_HIGH;
                case QUALITY_QCIF: return CamcorderProfile.QUALITY_QCIF;
                case QUALITY_CIF: return CamcorderProfile.QUALITY_CIF;
                case QUALITY_480P: return CamcorderProfile.QUALITY_480P;
                case QUALITY_720P: return CamcorderProfile.QUALITY_720P;
                case QUALITY_1080P: return CamcorderProfile.QUALITY_1080P;
                case QUALITY_QVGA: return CamcorderProfile.QUALITY_QVGA;
                default: return CamcorderProfile.QUALITY_LOW; 
            }
        }
    }
    
    private String getFileFormat(int fileFormat){
        switch(fileFormat){
            case OutputFormat.MPEG_4:
                return ".mp4";
            case OutputFormat.THREE_GPP:
                return ".3gp";
            case OutputFormat.DEFAULT:
                return ".mp4";
            default:
                return ".mp4";
      }  
     }
}
