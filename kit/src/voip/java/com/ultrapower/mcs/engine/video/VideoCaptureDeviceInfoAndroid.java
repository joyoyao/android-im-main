/*
 * Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
 * 
 * Use of this source code is governed by a BSD-style license that can be found in the LICENSE file in the root of the source tree. An additional intellectual property rights grant can be found in the file PATENTS. All contributing project authors may be found in the AUTHORS file in the root of the source tree.
 */

package com.ultrapower.mcs.engine.video;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dalvik.system.DexClassLoader;

public class VideoCaptureDeviceInfoAndroid {

    // Context
    Context context;

    // Set VERBOSE as the default logging level because camera device info
    // is very useful information and doesn't degrade performance normally
    private final static String TAG = "WEBRTC";
    private static int PIXEL_FORMAT = ImageFormat.NV21;

    // Private class with info about all available cameras and the capabilities
    public class AndroidVideoCaptureDevice {
        AndroidVideoCaptureDevice() {
            frontCameraType = FrontFacingCameraType.None;
            index = 0;
        }

        public String deviceUniqueName;
        public CaptureCapabilityAndroid captureCapabilies[];
        public FrontFacingCameraType frontCameraType;

        // Orientation of camera as described in
        // android.hardware.Camera.CameraInfo.Orientation
        public int orientation;
        // Camera index used in Camera.Open on Android 2.3 and onwards
        public int index;
    }

    public enum FrontFacingCameraType {
        None, // This is not a front facing camera
        GalaxyS, // Galaxy S front facing camera.
        HTCEvo, // HTC Evo front facing camera
        Android23, // Android 2.3 front facing camera.
    }

    String currentDeviceUniqueId;
    int id;
    List<AndroidVideoCaptureDevice> deviceList;

    public static VideoCaptureDeviceInfoAndroid CreateVideoCaptureDeviceInfoAndroid(int in_id, Context in_context) {

        Log.d(TAG, String.format(Locale.US, "VideoCaptureDeviceInfoAndroid"));
        VideoCaptureDeviceInfoAndroid self = new VideoCaptureDeviceInfoAndroid(in_id, in_context);
        if (self != null && self.Init() == 0) {
            return self;
        } else {
            Log.d(TAG, "Failed to create VideoCaptureDeviceInfoAndroid.");
        }
        return null;
    }

    private VideoCaptureDeviceInfoAndroid(int in_id, Context in_context) {
        id = in_id;
        context = in_context;
        deviceList = new ArrayList<AndroidVideoCaptureDevice>();
    }

    private int Init() {
        // Populate the deviceList with available cameras and their capabilities.
        Log.d(TAG, "VideoCaptureDeviceInfoAndroid Init() into... ");
        Camera camera = null;
        try {
            if (android.os.Build.VERSION.SDK_INT > 8) {
                Class cameraClazz = Class.forName("android.hardware.Camera");
                Method getNumberOfCamerasMethod = cameraClazz.getMethod("getNumberOfCameras");
                Integer num = (Integer) getNumberOfCamerasMethod.invoke(cameraClazz);

                Method openMethod = cameraClazz.getMethod("open", new Class[]{int.class});
                // From Android 2.3 and onwards
                for (int i = 0; i < num; ++i) {
                    AndroidVideoCaptureDevice newDevice = new AndroidVideoCaptureDevice();

                    Class cameraInfoClazz = Class.forName("android.hardware.Camera$CameraInfo");
                    Object obj = cameraInfoClazz.newInstance();
                    Field facField = obj.getClass().getDeclaredField("facing");
                    Field orientationField = obj.getClass().getDeclaredField("orientation");
                    int faing = facField.getInt(obj);
                    int orientation = orientationField.getInt(obj);

                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Method getCameraInfoMethod = cameraClazz.getMethod("getCameraInfo", new Class[]{int.class, cameraInfoClazz});
                    getCameraInfoMethod.invoke(cameraClazz, new Object[]{i, obj});

                    Camera.getCameraInfo(i, info);
                    newDevice.index = i;
                    newDevice.orientation = info.orientation;
                    faing = info.facing;
                    if (faing == 0) {
                        newDevice.deviceUniqueName = "Camera " + i + ", Facing back, Orientation " + orientation;
                        Log.d(TAG, "Camera " + i + ", Facing back, Orientation " + orientation);
                    } else {
                        newDevice.deviceUniqueName = "Camera " + i + ", Facing front, Orientation " + orientation;
                        newDevice.frontCameraType = FrontFacingCameraType.Android23;
                        Log.d(TAG, "Camera " + i + ", Facing front, Orientation " + orientation);
                    }

                    // camera = Camera.open(i);
                    camera = (Camera) openMethod.invoke(cameraClazz, new Object[]{i});
                    Camera.Parameters parameters = camera.getParameters();
                    AddDeviceInfo(newDevice, parameters);
                    camera.release();
                    camera = null;
                    deviceList.add(newDevice);
                }
            } else {
                int i = 0;
                int camera_orientation = 0;
                AndroidVideoCaptureDevice newDevice = new AndroidVideoCaptureDevice();

                // Camera.CameraInfo info = new Camera.CameraInfo();
                // Camera.getCameraInfo(i, info);
                newDevice.index = i;
                newDevice.orientation = camera_orientation; // 0,90,180,270
                // if(info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                newDevice.deviceUniqueName = "Camera_" + i + "_FB_Orien_" + camera_orientation;
                Log.d(TAG, "Camera " + i + ", Facing back, Orientation " + camera_orientation);
                // }
                // else {
                // newDevice.deviceUniqueName =
                // "Camera " + i +", Facing front, Orientation "+
                // camera_orientation;
                // newDevice.frontCameraType = FrontFacingCameraType.Android23;
                // camera_orientation);
                // }

                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                AddDeviceInfo(newDevice, parameters);
                camera.release();
                camera = null;
                deviceList.add(newDevice);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Failed to init VideoCaptureDeviceInfo ex" + ex.getLocalizedMessage() + "   ,ex.toString=" + ex.toString());
            return 0;
        }
        VerifyCapabilities();
        Log.d(TAG, "VideoCaptureDeviceInfoAndroid Init() out... ");
        return 0;
    }

    // Adds the capture capabilities of the currently opened device
    private void AddDeviceInfo(AndroidVideoCaptureDevice newDevice, Camera.Parameters parameters) {

        List<Size> sizes = parameters.getSupportedPreviewSizes();
        List<Integer> frameRates = parameters.getSupportedPreviewFrameRates();
        int maxFPS = 0;
        for (Integer frameRate : frameRates) {
            if (frameRate > maxFPS) {
                maxFPS = frameRate;
            }
        }

        newDevice.captureCapabilies = new CaptureCapabilityAndroid[sizes.size()];
        for (int i = 0; i < sizes.size(); ++i) {
            Size s = sizes.get(i);
            newDevice.captureCapabilies[i] = new CaptureCapabilityAndroid();
            newDevice.captureCapabilies[i].height = s.height;
            newDevice.captureCapabilies[i].width = s.width;
            newDevice.captureCapabilies[i].maxFPS = maxFPS;
            Log.d(TAG, "VideoCaptureDeviceInfo " + "maxFPS:" + maxFPS + " width:" + s.width + " height:" + s.height);
        }
    }

    // Function that make sure device specific capabilities are
    // in the capability list.
    // Ie Galaxy S supports CIF but does not list CIF as a supported capability.
    // Motorola Droid Camera does not work with frame rate above 15fps.
    // http://code.google.com/p/android/issues/detail?id=5514#c0
    private void VerifyCapabilities() {
        // Nexus S or Galaxy S
        if (android.os.Build.DEVICE.equals("GT-I9000") || android.os.Build.DEVICE.equals("crespo")) {
            CaptureCapabilityAndroid specificCapability = new CaptureCapabilityAndroid();
            specificCapability.width = 352;
            specificCapability.height = 288;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapabilityAndroid();
            specificCapability.width = 176;
            specificCapability.height = 144;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);

            specificCapability = new CaptureCapabilityAndroid();
            specificCapability.width = 320;
            specificCapability.height = 240;
            specificCapability.maxFPS = 15;
            AddDeviceSpecificCapability(specificCapability);
        }
        // Motorola Milestone Camera server does not work at 30fps
        // even though it reports that it can
        if (android.os.Build.MANUFACTURER.equals("motorola") && android.os.Build.DEVICE.equals("umts_sholes")) {
            for (AndroidVideoCaptureDevice device : deviceList) {
                for (CaptureCapabilityAndroid capability : device.captureCapabilies) {
                    capability.maxFPS = 15;
                }
            }
        }
    }

    private void AddDeviceSpecificCapability(CaptureCapabilityAndroid specificCapability) {
        for (AndroidVideoCaptureDevice device : deviceList) {
            boolean foundCapability = false;
            for (CaptureCapabilityAndroid capability : device.captureCapabilies) {
                if (capability.width == specificCapability.width && capability.height == specificCapability.height) {
                    foundCapability = true;
                    break;
                }
            }
            if (foundCapability == false) {
                CaptureCapabilityAndroid newCaptureCapabilies[] = new CaptureCapabilityAndroid[device.captureCapabilies.length + 1];
                for (int i = 0; i < device.captureCapabilies.length; ++i) {
                    newCaptureCapabilies[i + 1] = device.captureCapabilies[i];
                }
                newCaptureCapabilies[0] = specificCapability;
                device.captureCapabilies = newCaptureCapabilies;
            }
        }
    }

    // Returns the number of Capture devices that is supported
    public int NumberOfDevices() {
        return deviceList.size();
    }

    public String GetDeviceUniqueName(int deviceNumber) {
        if (deviceNumber < 0 || deviceNumber >= deviceList.size()) {
            return null;
        }
        return deviceList.get(deviceNumber).deviceUniqueName;
    }

    public CaptureCapabilityAndroid[] GetCapabilityArray(String deviceUniqueId) {
        for (AndroidVideoCaptureDevice device : deviceList) {
            if (device.deviceUniqueName.equals(deviceUniqueId)) {
                return device.captureCapabilies;
            }
        }
        return null;
    }

    // Returns the camera orientation as described by
    // android.hardware.Camera.CameraInfo.orientation
    public int GetOrientation(String deviceUniqueId) {
        for (AndroidVideoCaptureDevice device : deviceList) {
            if (device.deviceUniqueName.equals(deviceUniqueId)) {
                return device.orientation;
            }
        }
        return -1;
    }

    // Returns an instance of VideoCaptureAndroid.
    public VideoCaptureAndroid AllocateCamera(int id, long context, String deviceUniqueId) {

        Log.d(TAG, "VideoCaptureDeviceInfoAndroid AllocateCamera() into... ");
        try {
            Log.d(TAG, "AllocateCamera " + deviceUniqueId);
            Camera camera = null;
            AndroidVideoCaptureDevice deviceToUse = null;
            for (AndroidVideoCaptureDevice device : deviceList) {
                if (device.deviceUniqueName.equals(deviceUniqueId)) {
                    // Found the wanted camera
                    deviceToUse = device;
                    switch (device.frontCameraType) {
                        case GalaxyS:
                            camera = AllocateGalaxySFrontCamera();
                            break;
                        case HTCEvo:
                            camera = AllocateEVOFrontFacingCamera();
                            break;
                        default:
                            // From Android 2.3 and onwards)
                            if (android.os.Build.VERSION.SDK_INT > 8) {
                                Class cameraClazz = Class.forName("android.hardware.Camera");
                                Method openMethod = cameraClazz.getMethod("open", new Class[]{int.class});
                                // camera= Camera.open(device.index);
                                camera = (Camera) openMethod.invoke(cameraClazz, new Object[]{device.index});
                            } else {
                                camera = Camera.open(); // Default camera
                            }
                    }
                }
            }

            if (camera == null) {
                Log.d(TAG, "VideoCaptureAndroid::AllocateCamera - create camera fail");
                return null;
            }
            Log.d(TAG, "VideoCaptureAndroid::AllocateCamera - creating success");
            return new VideoCaptureAndroid(id, context, camera, deviceToUse);

        } catch (Exception ex) {
            Log.e(TAG, "AllocateCamera Failed to open camera- ex " + ex.getLocalizedMessage());
        }

        return null;
    }

    // Searches for a front facing camera device. This is device specific code.
    private Camera.Parameters SearchOldFrontFacingCameras(AndroidVideoCaptureDevice newDevice) throws SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException,
            IllegalAccessException, InvocationTargetException {
        // Check the id of the opened camera device
        // Returns null on X10 and 1 on Samsung Galaxy S.
        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        String cameraId = parameters.get("camera-id");
        if (cameraId != null && cameraId.equals("1")) {
            // This might be a Samsung Galaxy S with a front facing camera.
            try {
                parameters.set("camera-id", 2);
                camera.setParameters(parameters);
                parameters = camera.getParameters();
                newDevice.frontCameraType = FrontFacingCameraType.GalaxyS;
                newDevice.orientation = 0;
                camera.release();
                return parameters;
            } catch (Exception ex) {
                // Nope - it did not work.
                Log.e(TAG, "Init Failed to open front camera camera - ex " + ex.getLocalizedMessage());
            }
        }
        camera.release();

        // Check for Evo front facing camera
        File file = new File("/system/framework/com.htc.hardware.twinCamDevice.jar");
        boolean exists = file.exists();
        if (!exists) {
            file = new File("/system/framework/com.sprint.hardware.twinCamDevice.jar");
            exists = file.exists();
        }
        if (exists) {
            newDevice.frontCameraType = FrontFacingCameraType.HTCEvo;
            newDevice.orientation = 0;
            Camera evCamera = AllocateEVOFrontFacingCamera();
            parameters = evCamera.getParameters();
            evCamera.release();
            return parameters;
        }
        return null;
    }

    // Returns a handle to HTC front facing camera.
    // The caller is responsible to release it on completion.
    private Camera AllocateEVOFrontFacingCamera() throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        String classPath = null;
        File file = new File("/system/framework/com.htc.hardware.twinCamDevice.jar");
        classPath = "com.htc.hardware.twinCamDevice.FrontFacingCamera";
        boolean exists = file.exists();
        if (!exists) {
            file = new File("/system/framework/com.sprint.hardware.twinCamDevice.jar");
            classPath = "com.sprint.hardware.twinCamDevice.FrontFacingCamera";
            exists = file.exists();
        }
        if (!exists) {
            return null;
        }

        String dexOutputDir = "";
        if (context != null) {
            dexOutputDir = context.getFilesDir().getAbsolutePath();
            File mFilesDir = new File(dexOutputDir, "dexfiles");
            if (!mFilesDir.exists()) {
                if (!mFilesDir.mkdirs()) {
                }
            }
        }

        dexOutputDir += "/dexfiles";

        DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), dexOutputDir, null, ClassLoader.getSystemClassLoader());

        Method method = loader.loadClass(classPath).getDeclaredMethod("getFrontFacingCamera", (Class[]) null);
        Camera camera = (Camera) method.invoke((Object[]) null, (Object[]) null);
        return camera;
    }

    // Returns a handle to Galaxy S front camera.
    // The caller is responsible to release it on completion.
    private Camera AllocateGalaxySFrontCamera() {
        Camera camera = Camera.open();
        Camera.Parameters parameters = camera.getParameters();
        parameters.set("camera-id", 2);
        camera.setParameters(parameters);
        return camera;
    }

    public static class CameraSupportVideoSize {
        public int index; // ??????头索�?
        // public String size_str_min; //??????头�???????????�?尺�?? �?�? "QCIF" "CIF", �????使�??
        // public String size_str; //??????头�????????�???�尺�? �?�? "QCIF" "CIF", �?�?
        // public String size_str_max; //??????头�???????????大尺�? �?�? "QCIF" "CIF"�? �?�?�?
        public int min_width = VideoCaptureAndroid.BEST_CAMERA_SIZE_WIDTH;
        public int min_height = VideoCaptureAndroid.BEST_CAMERA_SIZE_HEIGHT;
        public int width = VideoCaptureAndroid.BEST_CAMERA_SIZE_WIDTH;
        public int height = VideoCaptureAndroid.BEST_CAMERA_SIZE_HEIGHT;
        public int max_width = VideoCaptureAndroid.BEST_CAMERA_SIZE_WIDTH;
        public int max_height = VideoCaptureAndroid.BEST_CAMERA_SIZE_HEIGHT;
    }

    public static int FindSupportSize(Camera.Parameters para, CameraSupportVideoSize cs) {

        List<Size> sizes_old = para.getSupportedPreviewSizes();
        ArrayList<Size> sizes = new ArrayList<Size>();
        for (int i = 0; i < sizes_old.size(); ++i) {
            if (sizes_old.get(i).width > sizes_old.get(i).height && sizes_old.get(i).width >= 128) {
                boolean find = false;
                for (int j = 0; j < sizes.size(); j++) {
                    if (sizes.get(j).width == sizes_old.get(i).width && sizes.get(j).height == sizes_old.get(i).height) {
                        find = true;
                    }
                }
                if (!find /* && null!= CameraVideoSizeToStr(sizes_old.get(i).width, sizes_old.get(i).height) */) {
                    sizes.add(sizes_old.get(i));
                }
            }
        }
        Log.d(TAG, "FindSupportSize new size len =" + sizes.size());
        // /find min
        int min_w = 9999, min_h = 0, min_index = 0;
        for (int i = 0; i < sizes.size(); ++i) {
            Size s = sizes.get(i);
            if (min_w > s.width) {
                min_w = s.width;
                min_h = s.height;
                min_index = i;
            }
        }
        Log.d(TAG, "FindSupportSize " + "min_w:" + min_w + " min_h:" + min_h);
        // cs.size_str_min = CameraVideoSizeToStr(min_w, min_h);
        cs.min_width = min_w;
        cs.min_height = min_h;

        // /find best
        int best_index = 0;
        if (0 == min_index) {
            // /顺�??
            if (min_w > 176) {
                // cs.size_str = cs.size_str_min;
                cs.width = cs.min_width;
                cs.height = cs.min_height;
                // cs.size_str_max = cs.size_str_min;
                cs.max_width = cs.min_width;
                cs.max_height = cs.min_height;
            } else {
                Size s = null;
                for (int i = 0; i < sizes.size(); i++) {
                    s = sizes.get(i);
                    best_index = i;
                    if (s.width >= 176) {
                        break;
                    }
                }
                if (null == s) {
                    Log.d(TAG, "FindSupportSize s=null");
                    return -1;
                }
                if (s.width < 176) {
                    // cs.size_str = cs.size_str_min;
                    cs.width = cs.min_width;
                    cs.height = cs.min_height;
                    // cs.size_str_max = cs.size_str_min;
                    cs.max_width = cs.min_width;
                    cs.max_height = cs.min_height;
                } else if (s.width == 176) {
                    // cs.size_str = CameraVideoSizeToStr(s.width, s.height);
                    cs.width = s.width;
                    cs.height = s.height;
                    if ((best_index + 1) <= sizes.size() - 1) {
                        s = sizes.get(best_index + 1);
                        // cs.size_str_max = CameraVideoSizeToStr(s.width, s.height);
                        cs.max_width = s.width;
                        cs.max_height = s.height;
                    } else {
                        // cs.size_str_max = cs.size_str;
                        cs.max_width = cs.width;
                        cs.max_height = cs.height;
                    }
                } else {
                    // cs.size_str_max = CameraVideoSizeToStr(s.width, s.height);
                    cs.max_width = s.width;
                    cs.max_height = s.height;
                    if ((best_index - 1) >= 0) {
                        s = sizes.get(best_index - 1);
                        // cs.size_str = CameraVideoSizeToStr(s.width, s.height);
                        cs.width = s.width;
                        cs.height = s.height;
                    } else {
                        // cs.size_str = cs.size_str_max;
                        cs.width = cs.max_width;
                        cs.height = cs.max_height;
                    }
                }
            }
        } else if (sizes.size() - 1 == min_index) {
            // /???�?
            if (min_w > 176) {
                // cs.size_str = cs.size_str_min;
                cs.width = cs.min_width;
                cs.height = cs.min_height;
                // cs.size_str_max = cs.size_str_min;
                cs.max_width = cs.min_width;
                cs.max_height = cs.min_height;
            } else {
                Size s = null;
                for (int i = (sizes.size() - 1); i >= 0; i--) {
                    s = sizes.get(i);
                    best_index = i;
                    if (s.width >= 176) {
                        break;
                    }
                }
                if (null == s) {
                    return -1;
                }
                if (s.width < 176) {
                    // cs.size_str = cs.size_str_min;
                    cs.width = cs.min_width;
                    cs.height = cs.min_height;
                    // cs.size_str_max = cs.size_str_min;
                    cs.max_width = cs.min_width;
                    cs.max_height = cs.min_height;
                } else if (s.width == 176) {
                    // cs.size_str = CameraVideoSizeToStr(s.width, s.height);
                    cs.width = s.width;
                    cs.height = s.height;
                    if ((best_index - 1) <= (sizes.size() - 1)) {
                        s = sizes.get(best_index - 1);
                        // cs.size_str_max = CameraVideoSizeToStr(s.width, s.height);
                        cs.max_width = s.width;
                        cs.max_height = s.height;
                    } else {
                        // cs.size_str_max = cs.size_str;
                        cs.max_width = cs.width;
                        cs.max_height = cs.height;
                    }
                } else {
                    // cs.size_str_max = CameraVideoSizeToStr(s.width, s.height);
                    cs.max_width = s.width;
                    cs.max_height = s.height;
                    if ((best_index + 1) <= (sizes.size() - 1)) {
                        s = sizes.get(best_index + 1);
                        // cs.size_str = CameraVideoSizeToStr(s.width, s.height);
                        cs.width = s.width;
                        cs.height = s.height;
                    } else {
                        // cs.size_str = cs.size_str_max;
                        cs.width = cs.max_width;
                        cs.height = cs.max_height;
                    }
                }
            }
        } else {
            Log.d(TAG, "FindSupportSize min_index wrong," + " should never happen");
        }

        return 0;
    }

    public static ArrayList<CameraSupportVideoSize> GetCameraSupportVideoSize() {
        ArrayList lst = new ArrayList();
        Log.d(TAG, "GetCameraSupportVideoSize into... ");
        Camera camera = null;
        try {
            // From Android 2.3 and onwards
            if (android.os.Build.VERSION.SDK_INT > 8) {
                Class cameraClazz = Class.forName("android.hardware.Camera");
                Method getNumberOfCamerasMethod = cameraClazz.getMethod(
                        "getNumberOfCameras");
                Integer num = (Integer) getNumberOfCamerasMethod.invoke(
                        cameraClazz);
                Method openMethod = cameraClazz.getMethod("open",
                        new Class[]{int.class});
                for (int i = 0; i < num; ++i) {
                    CameraSupportVideoSize cs = new CameraSupportVideoSize();
                    cs.index = i;
                    int ret = 0;
                    camera = (Camera) openMethod.invoke(cameraClazz,
                            new Object[]{i});
                    Camera.Parameters parameters = camera.getParameters();
                    List<Size> sizes_old = parameters.getSupportedPreviewSizes();
                    Boolean hasFind = false;
                    Boolean isReverse = sizes_old.get(0).width > sizes_old
                            .get(sizes_old.size() - 1).width;
                    if (isReverse) {
                        for (int j = 0; j < sizes_old.size() / 2; j++) {
                            Size tmpSize0 = sizes_old.get(j);
                            Size tmpSizen = sizes_old.get(sizes_old.size() - 1
                                    - j);
                            sizes_old.set(j, tmpSizen);
                            sizes_old.set(sizes_old.size() - 1 - j, tmpSize0);
                        }
                    }
                    for (int j = 0; j < sizes_old.size(); j++) {
                        Size size = sizes_old.get(j);
                        if (size.height >= 480 && size.width >= 640) {
                            cs.height = size.height;
                            cs.width = size.width;
                            // ////////////////
                            // 尝试是否成功
                            try {
                                parameters.setPreviewSize(size.width,
                                        size.height);
                                parameters
                                        .setPreviewFormat(VideoCaptureAndroid.PIXEL_FORMAT);
                                List<int[]> listfps = parameters.getSupportedPreviewFpsRange();
                                int min = listfps.get(0)[0];
                                int max = listfps.get(0)[1];
                                if ((VideoCaptureAndroid.FX_MAX_FRAME_RATE_SUPPORT >= min / 1000) && (VideoCaptureAndroid.FX_MAX_FRAME_RATE_SUPPORT <= max / 1000)) {
                                    parameters
                                            .setPreviewFrameRate(VideoCaptureAndroid.FX_MAX_FRAME_RATE_SUPPORT);
                                } else {
                                    parameters
                                            .setPreviewFrameRate(min / 1000);
                                }
                                camera.setParameters(parameters);
                                cs.width = size.width;
                                cs.height = size.height;
                            } catch (Exception e) {
                                continue;
                            }
                            lst.add(cs);
                            hasFind = true;
                            break;
                        }
                    }
                    if (!hasFind) {
                        cs.width = sizes_old.get(0).width;
                        cs.height = sizes_old.get(0).height;
                    }
                    camera.release();
                    camera = null;
                }
            } else {
                CameraSupportVideoSize cs = new CameraSupportVideoSize();
                cs.index = 0;
                camera = Camera.open();
                Camera.Parameters parameters = camera.getParameters();
                List<Size> sizes_old = parameters.getSupportedPreviewSizes();
                Boolean hasFind = false;
                Boolean isReverse = sizes_old.get(0).width > sizes_old
                        .get(sizes_old.size() - 1).width;
                if (isReverse) {
                    for (int j = 0; j < sizes_old.size() / 2; j++) {
                        Size tmpSize0 = sizes_old.get(j);
                        Size tmpSizen = sizes_old.get(sizes_old.size() - 1 - j);
                        sizes_old.set(j, tmpSizen);
                        sizes_old.set(sizes_old.size() - 1 - j, tmpSize0);
                    }
                }
                for (int j = 0; j < sizes_old.size(); j++) {
                    Size size = sizes_old.get(j);
                    if (size.height >= 480 && size.width >= 640) {
                        cs.height = size.height;
                        cs.width = size.width;
                        // ////////////////
                        // 尝试是否成功
                        try {
                            parameters.setPreviewSize(size.width, size.height);
                            parameters
                                    .setPreviewFormat(VideoCaptureAndroid.PIXEL_FORMAT);
                            parameters
                                    .setPreviewFrameRate(VideoCaptureAndroid.FX_MAX_FRAME_RATE_SUPPORT);
                            camera.setParameters(parameters);
                            cs.width = size.width;
                            cs.height = size.height;
                        } catch (Exception e) {
                            continue;
                        }
                        lst.add(cs);
                        hasFind = true;
                        break;
                    }
                }
                if (!hasFind) {
                    cs.width = sizes_old.get(0).width;
                    cs.height = sizes_old.get(0).height;
                }
                camera.release();
                camera = null;
            }
        } catch (Exception ex) {
            Log.e(TAG,
                    "GetCameraSupportVideoSize fail for exception:"
                            + ex.getLocalizedMessage() + " ,ex.toString="
                            + ex.toString());
        }
        // /
        return lst;
    }

    public static String CameraVideoSizeToStr(int width, int height) {
        String size_str;
        if (128 == width && 96 == height) {
            size_str = "SQCIF";
        } else if (160 == width && 120 == height) {
            size_str = "QQVGA";
        } else if (176 == width && 144 == height) {
            size_str = "QCIF";
        } else if (240 == width && 160 == height) {
            size_str = "W240P";
        } else if (320 == width && 200 == height) {
            size_str = "CGA";
        } else if (320 == width && 240 == height) {
            size_str = "QVGA";
        } else if (352 == width && 240 == height) {
            size_str = "SIF";
        } else if (400 == width && 240 == height) {
            size_str = "WQVGA";
        } else if (352 == width && 288 == height) {
            size_str = "CIF";
        } else if (512 == width && 288 == height) {
            size_str = "W288P";
        } else if (640 == width && 368 == height) {
            size_str = "W368P";
        } else if (576 == width && 448 == height) {
            size_str = "S_448P";
        } else if (640 == width && 480 == height) {
            size_str = "VGA";
        } else if (720 == width && 432 == height) {
            size_str = "S_432P";
        } else if (768 == width && 432 == height) {
            size_str = "W432P";
        } else if (704 == width && 480 == height) { // , // 704*480 = 337 920
            size_str = "S_4SIF";
        } else if (768 == width && 448 == height) { // , // 768*448 = 344 064
            size_str = "W448P";
        } else if (720 == width && 480 == height) {
            size_str = "NTSC";
        } else if (800 == width && 448 == height) { // , // 800*448 = 358 400
            size_str = "FW448P";
        } else if (768 == width && 480 == height) { // 768*480 = 368 640 (a.k.a WVGA 16:10)
            size_str = "S_768x480P";
        } else if (800 == width && 480 == height) { // 800*480 = 384 000
            size_str = "WVGA";
        } else if (704 == width && 576 == height) { // , // 704*576 = 405 504
            size_str = "S_4CIF";
        } else if (800 == width && 600 == height) { // 800*600 = 480 000
            size_str = "SVGA";
        } else if (960 == width && 544 == height) { // 960*544 = 522 240
            size_str = "W544P";
        } else if (1024 == width && 576 == height) { // , // 1024*576 = 589 824 (W4CIF)
            size_str = "W576P";
        } else if (960 == width && 720 == height) { // 960*720 = 691 200
            size_str = "HD";
        } else if (1024 == width && 768 == height) { // 1024*768 = 786 432
            size_str = "XGA";
        } else if (1280 == width && 720 == height) {
            size_str = "WHD";
        } else if (1440 == width && 1080 == height) {
            size_str = "FULL_HD";
        } else if (1600 == width && 1200 == height) { // 1600*1200 = 1 920 000
            size_str = "UXGA";
        } else if (1920 == width && 1080 == height) { // 1920*1080 = 2 073 600
            size_str = "WFULL_HD";
        } else {
            return "QCIF";
        }

        return size_str;
    }

}