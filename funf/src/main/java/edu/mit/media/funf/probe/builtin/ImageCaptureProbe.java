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
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
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

@DisplayName("Image Capture Probe")
@RequiredPermissions({android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA})
@RequiredFeatures("android.hardware.camera")
@Schedule.DefaultSchedule(interval=1800)
public class ImageCaptureProbe extends ImpulseProbe implements PassiveProbe, HighBandwidthKeys, SurfaceHolder.Callback {

    @Configurable
    private String fileNameBase = "imgcapturetest";

    @Configurable
    private String folderName = "myimages";

    @Configurable
    private int selectedCamera = 1; // BACK_FACING = 0, FRONT_FACING = 1

    @Configurable
    private int jpegCompressionRatio = 90; // 0 - min size, 100 - max quality

    @Configurable
    private int cameraOpenDelay = 1; // allow delay for camera open in seconds

    private String mFileName;
    private String mFolderPath;
    private NameGenerator mNameGenerator;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private WindowManager mWindowManager;
    private LayoutParams mParams;
    private Bitmap mBmp;

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
        Log.d(LogUtil.TAG, "ImageCaptureProbe: Probe initialization");

        mSurfaceView = new SurfaceView(getContext());
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);

        mWindowManager.addView(mSurfaceView, mParams);

        mSurfaceView.setZOrderOnTop(false);
        mHolder.setFormat(PixelFormat.TRANSPARENT);   

        mFileName = mFolderPath + "/" + mNameGenerator.generateName(fileNameBase) + ".jpg";
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Log.d(LogUtil.TAG, "Image capture: surfaceCreated");
        if (CameraUtil.safeCameraOpen(selectedCamera)) {
            try {
                CameraUtil.getCamera().setPreviewDisplay(mHolder);
            } catch (IOException exception) {
                CameraUtil.safeCameraRelease();
                Log.e(LogUtil.TAG, "ImageCaptureProbe: error in surface initialization");
                Log.e(LogUtil.TAG, exception.getLocalizedMessage());
            }  
        } else {
            abortCapture();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        //Log.d(LogUtil.TAG, "Image capture: surfaceChanged");
        CameraUtil.configureCameraParameters(getContext(), mWindowManager.getDefaultDisplay().getRotation());
        CameraUtil.getCamera().startPreview();

        // to allow time for camera initiation on certain devices
        try {
            Thread.sleep(TimeUtil.secondsToMillis(cameraOpenDelay));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Camera.PictureCallback mCall = new Camera.PictureCallback()
        {
            @Override
            public void onPictureTaken(byte[] data, Camera camera)
            {
                mBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                finishCapture();
            }
        };

        CameraUtil.getCamera().takePicture(null, null, mCall);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Log.d(LogUtil.TAG, "Image capture: surfaceDestroyed");
        CameraUtil.safeCameraClose();
    }

    protected void finishCapture() {
        try {
            File file = new File(mFileName);
            if (file.exists()) 
                file.delete();

            FileOutputStream out = new FileOutputStream(file);

            mBmp = rotate(mBmp, CameraUtil.computePictureRotation());
            mBmp.compress(Bitmap.CompressFormat.JPEG, jpegCompressionRatio, out);
            out.flush();
            out.close();

            Log.d(LogUtil.TAG, "ImageCaptureProbe: image capture finish");
            JsonObject data = new JsonObject();
            data.addProperty(FILENAME, mFileName);
            sendData(data);
            mWindowManager.removeView(mSurfaceView);
            stop();
        } catch (Exception e) {
            Log.e(LogUtil.TAG, "ImageCaptureProbe: image capture error");
            Log.e(LogUtil.TAG, e.getLocalizedMessage());
        }
    }
    
    private void abortCapture() {
        Log.e(LogUtil.TAG, "ImageCaptureProbe: image capture abort");
        CameraUtil.safeCameraClose();
        if (mHolder != null) {
            mHolder.removeCallback(this);
        }
        if (mWindowManager != null) {
            mWindowManager.removeView(mSurfaceView);
        } 
        stop();
    }

    private static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }
}
