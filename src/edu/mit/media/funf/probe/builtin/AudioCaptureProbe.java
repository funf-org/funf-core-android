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

import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;

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

@DisplayName("Audio Capture Probe")
@RequiredPermissions({android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO})
@RequiredFeatures("android.hardware.microphone")
@Schedule.DefaultSchedule(interval=1800)
public class AudioCaptureProbe extends ImpulseProbe implements PassiveProbe, HighBandwidthKeys {

    @Configurable
    private String fileNameBase = "audiorectest";

    @Configurable
    private String folderName = "myaudios";
    
    @Configurable
    private int recordingLength = 5; // Duration of recording in seconds

    private String mFileName;
    private String mFolderPath;
    
    private MediaRecorder mRecorder;    
    private NameGenerator mNameGenerator;

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
            //Log.d(LogUtil.TAG, "Audio capture: seconds remaining = " + millisUntilFinished / 1000);
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
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LogUtil.TAG, "AudioCaptureProbe: Probe initialization");
        mFileName = mFolderPath + "/" + mNameGenerator.generateName(fileNameBase) + ".mp4";
        
        mCountDown = new RecordingCountDown(TimeUtil.secondsToMillis(recordingLength), 1000);
        if (startRecording())
            mCountDown.start();
        else {
            abortRecording();
        }
    }
    
    private boolean startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            Log.d(LogUtil.TAG, "AudioCaptureProbe: Recording audio start");
            mRecorder.start();
        } catch (IOException e) {
            Log.e(LogUtil.TAG, "AudioCaptureProbe: Error in preparing MediaRecorder");
            Log.e(LogUtil.TAG, e.getLocalizedMessage());
            return false;
        }
        
        return true;
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
        
        Log.d(LogUtil.TAG, "AudioCaptureProbe: Recording audio stop");
        JsonObject data = new JsonObject();
        data.addProperty(FILENAME, mFileName);
        sendData(data);
        stop();
    }
    
    private void abortRecording() {
        Log.e(LogUtil.TAG, "AudioCaptureProbe: Recording audio abort");
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
        stop();
    }

}
