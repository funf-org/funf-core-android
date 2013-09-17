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

import android.content.Context;
import android.graphics.PixelFormat;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OutputFormat;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
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
import edu.mit.media.funf.util.CameraUtil;
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

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
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
        Log.d(LogUtil.TAG, "VideoCaptureProbe: Probe initialization");

        mSurfaceView = new SurfaceView(getContext());
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        mWindowManager.addView(mSurfaceView, mParams);

        mSurfaceView.setZOrderOnTop(false);
        mHolder.setFormat(PixelFormat.TRANSPARENT);        
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.d(LogUtil.TAG, "Video capture: surfaceCreated");
        if (CameraUtil.safeCameraOpen(selectedCamera)) {
            try {
                CameraUtil.getCamera().setPreviewDisplay(mHolder);
            } catch (IOException exception) {
                CameraUtil.safeCameraRelease();
                Log.e(LogUtil.TAG, "VideoCaptureProbe: error in surface initialization");
                Log.e(LogUtil.TAG, exception.getLocalizedMessage());
            }  
        } else {
            abortRecording();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        //Log.d(LogUtil.TAG, "Video capture: surfaceChanged");
        CameraUtil.configureCameraParameters(getContext(), mWindowManager.getDefaultDisplay().getRotation());
        CameraUtil.getCamera().startPreview();
        // to allow time for camera initiation on certain devices
        try {
            Thread.sleep(TimeUtil.secondsToMillis(cameraOpenDelay));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mCountDown = new RecordingCountDown(TimeUtil.secondsToMillis(recordingLength), 1000);
        CameraUtil.getCamera().unlock();
        if (startRecording())
            mCountDown.start();
        else {
            abortRecording();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.d(LogUtil.TAG, "Video capture: surfaceDestroyed");
        CameraUtil.safeCameraClose();
    }

    private boolean startRecording() {
        Log.d(LogUtil.TAG, "VideoCaptureProbe: Recording video init");
        mRecorder = new MediaRecorder();
        mRecorder.setCamera(CameraUtil.getCamera());

        CamcorderProfile camProfile;
        int videoQuality = getConfiguredProfile(timeLapse, videoProfile);
        if (CamcorderProfile.hasProfile(CameraUtil.getCameraId(), videoQuality)) {
            Log.d(LogUtil.TAG, "VideoCaptureProbe: using camcorder profile " + videoProfile);
            camProfile = CamcorderProfile.get(CameraUtil.getCameraId(), videoQuality);
        } else {
            Log.d(LogUtil.TAG, "VideoCaptureProbe: profile unavailable, using LOW");
            camProfile = CamcorderProfile.get(CameraUtil.getCameraId(), CamcorderProfile.QUALITY_LOW);
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

        mRecorder.setOrientationHint(CameraUtil.computePictureRotation());
        try {
            mRecorder.prepare();
            Log.d(LogUtil.TAG, "VideoCaptureProbe: Recording video start");
            mRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LogUtil.TAG, "VideoCaptureProbe: Error in preparing mediaRecorder");
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
        Log.d(LogUtil.TAG, "VideoCaptureProbe: Recording video stop");

        JsonObject data = new JsonObject();
        data.addProperty(FILENAME, mFileName);
        sendData(data);
        stop();
    }

    private void abortRecording() {
        Log.e(LogUtil.TAG, "VideoCaptureProbe: Recording video abort");
        CameraUtil.safeCameraClose();
        if (mHolder != null) {
            mHolder.removeCallback(this);
        }
        if (mWindowManager != null) {
            mWindowManager.removeView(mSurfaceView);
        } 
        stop();  
    }

    public static int getConfiguredProfile(boolean isTimeLapse, String profileType) {
        if (isTimeLapse) {
            String profileName = "QUALITY_TIME_LAPSE_" + profileType;
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
            String profileName = "QUALITY_" + profileType;
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

    private static String getFileFormat(int fileFormat){
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
