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
package edu.mit.media.funf.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

public class CameraUtil {

    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";
    
    private static Camera mCamera;
    private static int mCameraType;
    private static int mCameraId;
    private static int mCameraAngle;
    
    @SuppressLint("NewApi")
    public static Camera openSelectedCamera(int type) {
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
                    Log.e(LogUtil.TAG, "CameraUtil: camera failed to open");
                    Log.e(LogUtil.TAG, e.getLocalizedMessage());
                }
            }
        }

        return cam;
    }
    
    public static boolean safeCameraOpen(int selectedCamera) {
        if (mCamera != null) {
            Log.e(LogUtil.TAG, "CameraUtil: camera already open");
            return false;
        }
        mCameraType = selectedCamera;
        mCamera = openSelectedCamera(mCameraType);
        if (mCamera == null) {
            Log.e(LogUtil.TAG, "CameraUtil: failed to access camera");
            return false;
        } else {
            return true;
        }
    }
    
    public static void safeCameraClose() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    
    public static void safeCameraRelease() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
    
    public static Camera getCamera() {
        return mCamera;
    }
    
    public static int getCameraId() {
        return mCameraId;
    }

    public static void configureCameraParameters(Context context, int rotation) {
        Parameters cameraParams = mCamera.getParameters();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
            if (isPortrait(context)) {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
                mCameraAngle = 90;
            } else {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
                mCameraAngle = 0;
            }
        } else { // for 2.2 and later
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

    public static boolean isPortrait(Context context) {
        return (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public static int computePictureRotation() {
        if (mCameraType == 1 && (mCameraAngle == 90 || mCameraAngle == 270)) {
            // for front-facing camera, portrait and reverse-portrait orientation 
            // gives mirror image of actual picture, which needs to be corrected
            return (180 + mCameraAngle) % 360;
        }
        return mCameraAngle;
    }   

}
