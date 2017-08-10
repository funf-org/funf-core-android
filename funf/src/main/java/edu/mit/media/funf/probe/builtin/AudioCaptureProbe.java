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
