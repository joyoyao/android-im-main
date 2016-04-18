/*
 * Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 * 
 * Use of this source code is governed by a BSD-style license that can be found in the LICENSE file in the root of the source tree. An additional intellectual property rights grant can be found in the file PATENTS. All contributing project authors may be found in the AUTHORS file in the root of the source tree.
 */

package com.ultrapower.mcs.engine.video;

// The following four imports are needed saveBitmapToJPEG which
// is for debug only

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

class ViESurfaceRenderer implements Callback {

	private final static String TAG = "WEBRTC";

	// the bitmap used for drawing.
	private Bitmap bitmap = null;
	private ByteBuffer byteBuffer = null;
	private SurfaceHolder surfaceHolder;
	// Rect of the source bitmap to draw
	private final Rect srcRect = new Rect();
	// Rect of the destination canvas to draw to
	private final Rect dstRect = new Rect();
	private float dstTopScale = 0;
	private float dstBottomScale = 1;
	private float dstLeftScale = 0;
	private float dstRightScale = 1;

	// /add by lyh
	private final Rect srcRectRoute = new Rect();

	public ViESurfaceRenderer(SurfaceView view) {
		surfaceHolder = view.getHolder();
		if (surfaceHolder == null) {
			return;
		}
		surfaceHolder.addCallback(this);
	}

	// surfaceChanged and surfaceCreated share this function
	private void changeDestRect(int dstWidth, int dstHeight) {
		dstRect.right = (int) (dstRect.left + dstRightScale * dstWidth);
		dstRect.bottom = (int) (dstRect.top + dstBottomScale * dstHeight);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int in_width, int in_height) {

		changeDestRect(in_width, in_height);

		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas != null) {
			Rect dst = surfaceHolder.getSurfaceFrame();
			if (dst != null) {
				changeDestRect(dst.right - dst.left, dst.bottom - dst.top);
				Log.d(TAG, "ViESurfaceRender::surfaceCreated" + " dst.left:" + dst.left + " dst.top:" + dst.top + " dst.right:" + dst.right + " dst.bottom:" + dst.bottom + " srcRect.left:"
						+ srcRect.left + " srcRect.top:" + srcRect.top + " srcRect.right:" + srcRect.right + " srcRect.bottom:" + srcRect.bottom + " dstRect.left:" + dstRect.left + " dstRect.top:"
						+ dstRect.top + " dstRect.right:" + dstRect.right + " dstRect.bottom:" + dstRect.bottom);
			}
			surfaceHolder.unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "ViESurfaceRenderer::surfaceDestroyed");
		bitmap = null;
		byteBuffer = null;
	}

	public Bitmap CreateBitmap(int width, int height) {
		Log.d(TAG, "CreateByteBitmap " + width + ":" + height);
		if (bitmap == null) {
			try {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
		srcRect.left = 0;
		srcRect.top = 0;
		srcRect.bottom = height;
		srcRect.right = width;

		Log.d(TAG, "ViESurfaceRender::CreateBitmap: " + " srcRect.left:" + srcRect.left + " srcRect.top:" + srcRect.top + " srcRect.right:" + srcRect.right + " srcRect.bottom:" + srcRect.bottom
				+ " dstRect.left:" + dstRect.left + " dstRect.top:" + dstRect.top + " dstRect.right:" + dstRect.right + " dstRect.bottom:" + dstRect.bottom);

		return bitmap;
	}

	public ByteBuffer CreateByteBuffer(int width, int height) {
		Log.d(TAG, "CreateByteBuffer " + width + ":" + height);
		// if (bitmap == null) {
		if (bitmap != null) {
			bitmap = null;
		}
		bitmap = CreateBitmap(width, height);
		if (byteBuffer != null) {
			byteBuffer = null;
		}
		byteBuffer = ByteBuffer.allocateDirect(width * height * 2);
		// }
		return byteBuffer;
	}

	public void SetCoordinates(float left, float top, float right, float bottom) {
		Log.d(TAG, "SetCoordinates " + left + "," + top + ":" + right + "," + bottom);
		dstLeftScale = left;
		dstTopScale = top;
		dstRightScale = right;
		dstBottomScale = bottom;
	}

	// It saves bitmap data to a JPEG picture, this function is for debug only.
	private void saveBitmapToJPEG(int width, int height) {
		ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteOutStream);

		try {
			FileOutputStream output = new FileOutputStream(String.format("/sdcard/render_%d.jpg", System.currentTimeMillis()));
			output.write(byteOutStream.toByteArray());
			output.flush();
			output.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void DrawByteBuffer() {
		if (byteBuffer == null) {
			return;
		}
		byteBuffer.rewind();
		bitmap.copyPixelsFromBuffer(byteBuffer);
		DrawBitmap();
	}

	public void DrawBitmap() {
		if (bitmap == null) {
			return;
		}

		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas != null) {
			// The follow line is for debug only
			// saveBitmapToJPEG(srcRect.right - srcRect.left,srcRect.bottom - srcRect.top);
			// //add by lyh
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();
			Matrix mtx = new Matrix();
			mtx.postRotate(-90);
			Bitmap rotateBmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
			if (null != rotateBmp) {
				srcRectRoute.left = 0;
				srcRectRoute.top = 0;
				srcRectRoute.bottom = rotateBmp.getHeight();
				srcRectRoute.right = rotateBmp.getWidth();
				canvas.drawBitmap(rotateBmp, srcRectRoute, dstRect, null);
			} else {
				canvas.drawBitmap(bitmap, srcRect, dstRect, null);
			}
			// //

			// /canvas.drawBitmap(bitmap, srcRect, dstRect, null);
			surfaceHolder.unlockCanvasAndPost(canvas);
		}
	}

}
