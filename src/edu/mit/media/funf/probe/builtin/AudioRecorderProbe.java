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


import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonObject;

import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.probe.Probe.Base;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.probe.Probe.PassiveProbe;
import edu.mit.media.funf.probe.Probe.RequiredFeatures;
import edu.mit.media.funf.probe.Probe.RequiredPermissions;
import edu.mit.media.funf.probe.builtin.ProbeKeys.HighBandwidthKeys;
import edu.mit.media.funf.util.LogUtil;
import edu.mit.media.funf.util.NameGenerator;
import edu.mit.media.funf.util.NameGenerator.SystemUniqueTimestampNameGenerator;

@DisplayName("Audio Recorder Probe")
@RequiredPermissions({android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.RECORD_AUDIO})
@RequiredFeatures("android.hardware.microphone")
@Schedule.DefaultSchedule(interval=1800, duration=10)
public class AudioRecorderProbe extends Base implements PassiveProbe, HighBandwidthKeys {

    @Configurable
    private String fileNameBase = "audiorectest";

    @Configurable
    private String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    private String mFileName;
    private MediaRecorder mRecorder;    
    private NameGenerator mNameGenerator;

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LogUtil.TAG, "prepare() failed");
            stop();
        }

        mRecorder.start();
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.reset();
        mRecorder.release();
        mRecorder = null;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        mNameGenerator = new SystemUniqueTimestampNameGenerator(getContext());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFileName = folderPath + "/" + mNameGenerator.generateName(fileNameBase) + ".mp4";
        Log.e(LogUtil.TAG, "Recording audio: start");
        startRecording();
    }

    @Override
    protected void onStop() {
        stopRecording();
        Log.e(LogUtil.TAG, "Recording audio: stop");
        JsonObject data = new JsonObject();
        data.addProperty(FILENAME, mFileName);
        sendData(data);
    }
    
    @Override
    protected void onDisable() {
        super.onDisable();

    }

}
