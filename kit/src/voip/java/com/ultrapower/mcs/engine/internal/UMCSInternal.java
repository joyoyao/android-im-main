package com.ultrapower.mcs.engine.internal;


public class UMCSInternal {
	public native int GetLocalVideoInfo(SendVideoInfo info);
	public native int GetRemoteVideoInfo(ReceiveVideoInfo info);
	public native int GetLocalAudioInfo(SendAudioInfo info);
	public native int GetRemoteAudioInfo(ReceiveAudioInfo info);
	
	
	public native int SetECEnable(int enable, int mode);


	public native int GetECEnable();


	public native int SetNSEnable(int enable, int mode);


	public native int GetNSEnable();

	public native int SetHighPassFilterEnable(int enable);

	public native int GetHighPassFilterEnable();

	public static final int FX_AVLIB_VIDEO_PROTECTION_NONE = 0;
	public static final int FX_AVLIB_VIDEO_PROTECTION_NACK = 1;
	public static final int FX_AVLIB_VIDEO_PROTECTION_FCE = 2;
	public static final int FX_AVLIB_VIDEO_PROTECTION_NACK_FCE = 3;

	public native int SetVideoProtectionMethod(int method);
	
	public native int SetVADStatus(int channel, int enable, int mode, int disableDTX);
	static {
		System.loadLibrary("umcs");
	}
}
