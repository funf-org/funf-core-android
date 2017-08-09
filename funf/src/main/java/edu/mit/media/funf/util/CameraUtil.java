/**
 * 
 * Funf: Open Sensing Framework Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner Contact: nadav@media.mit.edu
 * 
 * Author(s): Pararth Shah (pararthshah717@gmail.com)
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Funf. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
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
