/*
 * Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 * 
 * Use of this source code is governed by a BSD-style license that can be found in the LICENSE file in the root of the source tree. An additional intellectual property rights grant can be found in the file PATENTS. All contributing project authors may be found in the AUTHORS file in the root of the source tree.
 */

// 待同步到印度版begin
package com.ultrapower.mcs.engine.video;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

import com.ultrapower.mcs.engine.video.VideoCaptureDeviceInfoAndroid.AndroidVideoCaptureDevice;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

class VideoCaptureAndroid implements PreviewCallback, Callback,IOrientationChangedObserver {

	private final static String TAG = "WEBRTC-JC";

	private Camera camera;
	private AndroidVideoCaptureDevice currentDevice = null;
	public ReentrantLock previewBufferLock = new ReentrantLock();
	// This lock takes sync with StartCapture and SurfaceChanged
	private final ReentrantLock captureLock = new ReentrantLock();
	public static int PIXEL_FORMAT = ImageFormat.NV21;
	PixelFormat pixelFormat = new PixelFormat();
	// True when the C++ layer has ordered the camera to be started.
	private boolean isCaptureStarted = false;
	private boolean isCaptureRunning = false;
	private boolean isSurfaceReady = false;
	private SurfaceHolder surfaceHolder = null;

	private final int numCaptureBuffers = 3;
	private int expectedFrameSize = 0;
	private final int orientation = 0;
	private int id = 0;
	// C++ callback context variable.
	private long context = 0;
	private SurfaceHolder localPreview = null;
	// True if this class owns the preview video buffers.
	private boolean ownsBuffers = false;

	private int mCaptureWidth = -1;
	private int mCaptureHeight = -1;
	private int mCaptureFPS = -1;

	// public final static int FX_MAX_FRAME_RATE_SUPPORT = 15;
	public final static int FX_MAX_FRAME_RATE_SUPPORT = 10;
	public final static int BEST_CAMERA_SIZE_WIDTH = 320;
	public final static int BEST_CAMERA_SIZE_HEIGHT = 240;

	private byte[] rotatedData;

	public static void DeleteVideoCaptureAndroid(VideoCaptureAndroid captureAndroid) {

		captureAndroid.StopCapture();
		captureAndroid.camera.release();
		captureAndroid.camera = null;
		captureAndroid.context = 0;
		captureAndroid.localPreview = null;
	}

	public VideoCaptureAndroid(int in_id, long in_context, Camera in_camera, AndroidVideoCaptureDevice in_device) {
		id = in_id;
		context = in_context;
		camera = in_camera;
		currentDevice = in_device;
	}

	private int tryStartCapture(int width, int height, int frameRate) {

		if (camera == null) {
			return -1;
		}

		if (isCaptureRunning || !isSurfaceReady || !isCaptureStarted) {
			return 0;
		}

		try {
			camera.setPreviewDisplay(surfaceHolder);

			CaptureCapabilityAndroid currentCapability = new CaptureCapabilityAndroid();
			currentCapability.width = width;
			currentCapability.height = height;
			if (frameRate > FX_MAX_FRAME_RATE_SUPPORT) {
				currentCapability.maxFPS = FX_MAX_FRAME_RATE_SUPPORT;
			} else {
				currentCapability.maxFPS = frameRate;
			}

			PixelFormat.getPixelFormatInfo(PIXEL_FORMAT, pixelFormat);

			Camera.Parameters parameters = camera.getParameters();
			parameters.setPreviewSize(currentCapability.width, currentCapability.height);
	
			parameters.setPreviewFormat(PIXEL_FORMAT);
			
			List<int[]> listfps = parameters.getSupportedPreviewFpsRange();
			int min = listfps.get(0)[0];
			int max = listfps.get(0)[1];
			if((currentCapability.maxFPS>=min/1000)&&(currentCapability.maxFPS<=max/1000))
			{
				parameters
					.setPreviewFrameRate(currentCapability.maxFPS);
			}else {
				parameters
				.setPreviewFrameRate(min/1000);
			}

			//parameters.setRotation(90);

			// ////////////////add by lyh
			// parameters.set("orientation", "portrait"); //parameters.set("orientation", "landscape");
			// parameters.setRotation(90); //parameters.setRotation(0);
			// /////////////////////////////

			// ////////////////add by lyh
			
			//camera.setDisplayOrientation(90);
			
			// ////////////////

			// camera.setParameters(parameters);
			
				try {
					camera.setParameters(parameters);
				} catch (Exception ex) {
					if (currentCapability.maxFPS >= frameRate) {
						ex.printStackTrace();
						return -1;
					}
					if (currentCapability.maxFPS == FX_MAX_FRAME_RATE_SUPPORT) {
						currentCapability.maxFPS = 15;
					} else if (currentCapability.maxFPS == 15) {
						currentCapability.maxFPS = frameRate;
					} else {
						currentCapability.maxFPS = frameRate;
					}
					parameters.setPreviewFrameRate(currentCapability.maxFPS);
					try {
						camera.setParameters(parameters);
					} catch (Exception e) {
						return -1;
					}
				}

			int bufSize = width * height * pixelFormat.bitsPerPixel / 8;
			byte[] buffer = null;
			for (int i = 0; i < numCaptureBuffers; i++) {
				buffer = new byte[bufSize];
				camera.addCallbackBuffer(buffer);

				// /by lyh
				rotatedData = new byte[bufSize];
			}
			camera.setPreviewCallbackWithBuffer(this);
			ownsBuffers = true;
			OrientationChangedNotify.Instances().AddObserver(this);
			camera.startPreview();
			previewBufferLock.lock();
			expectedFrameSize = bufSize;
			isCaptureRunning = true;
			previewBufferLock.unlock();
		} catch (Exception ex) {
			return -1;
		}

		isCaptureRunning = true;

		return 0;
	}

	public int StartCapture(int width, int height, int frameRate) {

		// Get the local preview SurfaceHolder from the static render class
		localPreview = ViERenderer.GetLocalRenderer();
		if (localPreview != null) {
			localPreview.addCallback(this);
		}

		
		captureLock.lock();
		isCaptureStarted = true;
		mCaptureWidth = width;
		mCaptureHeight = height;
		mCaptureFPS = frameRate;

		int res = tryStartCapture(mCaptureWidth, mCaptureHeight, mCaptureFPS);

		captureLock.unlock();

		return res;
	}

	public int StopCapture() {
		try {
			previewBufferLock.lock();
			isCaptureRunning = false;
			previewBufferLock.unlock();
			camera.stopPreview();
			camera.setPreviewCallbackWithBuffer(null);
			OrientationChangedNotify.Instances().RemoveObserver(this);
			OrientationChangedNotify.Instances().AddObserver(this);


		} catch (Exception ex) {
			OrientationChangedNotify.Instances().RemoveObserver(this);
			return -1;

		}

		isCaptureStarted = false;
		return 0;
	}

	native void ProvideCameraFrame(byte[] data, int length, long captureObject);

	private void saveJPEG(byte[] data) {
		FileOutputStream outStream = null;
		try {
			YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, mCaptureWidth, mCaptureHeight, null);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			yuvimage.compressToJpeg(new Rect(0, 0, mCaptureWidth, mCaptureHeight), 80, baos);

			outStream = new FileOutputStream(String.format("/sdcard/capture_%d.jpg", System.currentTimeMillis()));
			outStream.write(baos.toByteArray());
			outStream.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void RouteData(byte[] data, int total) {
		int len = total * 2 / 3; // pixelFormat.bitsPerPixel
		int half_len = len / 2;

		for (int i = 0; i < half_len; i++) {
			byte tmp = data[i];
			data[i] = data[len - 1 - i];
			data[len - 1 - i] = tmp;
		}

		int uv_len = total - len;
		int half_uv_len = uv_len / 4;
		for (int i = 0; i < half_uv_len; i++) {
			byte tmp_u = data[len + i * 2];
			byte tmp_v = data[len + i * 2 + 1];
			data[len + i * 2] = data[total - 1 - i * 2 - 1];
			data[len + i * 2 + 1] = data[total - 1 - i * 2];
			data[total - 1 - i * 2 - 1] = tmp_u;
			data[total - 1 - i * 2] = tmp_v;
		}

	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		// The following line is for debug only

		previewBufferLock.lock();

		if (isCaptureRunning) {
			// If StartCapture has been called but not StopCapture
			// Call the C++ layer with the captured frame
			if (data.length == expectedFrameSize) {
				// ///back camera need route 180 degree, by lyh
				// if(null!=currentDevice &&
				// //(currentDevice.frontCameraType == VideoCaptureDeviceInfoAndroid.FrontFacingCameraType.None)){
				// (currentDevice.index == 0)){
				// RouteData(data, expectedFrameSize);
				// }

				ProvideCameraFrame(data, expectedFrameSize, context);
				if (ownsBuffers) {
					// Give the video buffer to the camera service again.
					camera.addCallbackBuffer(data);
				}
			}
		}
		previewBufferLock.unlock();
	}

	// Sets the rotation of the preview render window.
	// Does not affect the captured video image.
	public void SetPreviewRotation(int rotation) {

		if (camera!=null) {
			int resultRotation = 90;
			resultRotation = (rotation-90+360+(currentDevice.orientation-90))%360;
			if(currentDevice.index==0)
			{
					if (rotation==0) {
						resultRotation=90;
					}
					else if (rotation==90) {
						resultRotation=180;
					}
					else if (rotation==270) {
						resultRotation=0;
					}
					else{
						resultRotation = 270;
					}
			}
		    Log.d("rotation", "rocation ="+ rotation +"set local rotaion "+resultRotation);

			camera.setDisplayOrientation(resultRotation);
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

		captureLock.lock();
		isSurfaceReady = true;
		surfaceHolder = holder;

		tryStartCapture(mCaptureWidth, mCaptureHeight, mCaptureFPS);
		captureLock.unlock();

		return;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isSurfaceReady = false;
	}
	// 待同步到印度版end

	@Override
	public void OnOrientationChanged(int orientation) {
		SetPreviewRotation(orientation);
	}
}
