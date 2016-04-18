package com.ultrapower.mcs.engine;

public interface ITransportCallback {
	public void SendAudioRtp(byte[] data);
	public void SendAudioRtcp(byte[] data);
	public void SendVideoRtp(byte[] data);
	public void SendVideoRtcp(byte[] data);
	public void SendData(byte[] data);

}
