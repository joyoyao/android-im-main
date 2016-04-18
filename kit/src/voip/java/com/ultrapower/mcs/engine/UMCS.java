package com.ultrapower.mcs.engine;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.ultrapower.mcs.engine.video.IOrientationChangedObserver;
import com.ultrapower.mcs.engine.video.OrientationChangedNotify;
import com.ultrapower.mcs.engine.video.ViERenderer;
import com.ultrapower.mcs.engine.video.VideoCaptureDeviceInfoAndroid;
import com.ultrapower.mcs.engine.video.VideoCaptureDeviceInfoAndroid.CameraSupportVideoSize;

import java.io.File;
import java.util.ArrayList;

public class UMCS implements IOrientationChangedObserver {
	private Context mContext;
	private static final int PARTTIMERMS = 10; // 30;//
	private int currentCameraIndex = -1;
	ViewGroup localContainer = null;
	SurfaceView localSv = null;
	private IVideoObserver remoteVideoObserver = null;
	private IVideoObserver localVideoObserver = null;
	private ITransportListener transportListener = null;
	private ITransportCallback transportCallback = null;
	private int oldMode = 0;
	private TransportType transportType;

	private VideoCodecLevel intToLevel(int level) {
		switch (level) {
		case 1:
			return VideoCodecLevel.kCodecLevelLSD;
		case 2:
			return VideoCodecLevel.kCodecLevelSD;
		case 3:
			return VideoCodecLevel.kCodecLevelHD;
		default:
			return VideoCodecLevel.kCodecLevelLSD;
		}
	}

	private NetworkState intToNetworkState(int state) {

		switch (state) {
		case 1:
			return NetworkState.kNetworkStateLow;
		case 2:
			return NetworkState.kNetworkStateNormal;
		case 3:
			return NetworkState.kNetworkStateGood;
		default:
			return NetworkState.kNetworkStateLow;
		}
	}

	private void OnLocalVideoLevelChanged(int level) {
		if (localVideoObserver == null) {
			return;
		}

		localVideoObserver.OnVideoLevelChanged(intToLevel(level));
	}

	private void OnLocalNetworkStateChanged(int state) {
		if (localVideoObserver == null) {
			return;
		}

		localVideoObserver.OnNetworkStateChanged(intToNetworkState(state));
	}

	private void OnRemoteVideoLevelChanged(int level) {

		if (localVideoObserver == null) {
			return;
		}

		remoteVideoObserver.OnVideoLevelChanged(intToLevel(level));
	}

	private void OnRemoteNetworkStateChanged(int state) {
		if (remoteVideoObserver == null)
			return;
		remoteVideoObserver.OnNetworkStateChanged(intToNetworkState(state));

	}

	private void OnDataNeedSend(boolean isAudio, boolean isRtcp, byte[] data) {
		if (transportCallback == null) {
			Log.e("umcs", "transport callback is null");
			return;
		}
		if (transportType == TransportType.kExternal) {
			transportCallback.SendData(data);
		} else if (transportType == TransportType.kExternalOriginal) {
			if (isAudio) {
				if (isRtcp) {
					transportCallback.SendAudioRtcp(data);
				} else {
					transportCallback.SendAudioRtp(data);
				}
			} else {
				if (isRtcp) {
					transportCallback.SendVideoRtcp(data);
				} else {
					transportCallback.SendVideoRtp(data);
				}
			}
		}
		else {
			Log.e("umcs", "transport type is not external");
		}
	}

	static {
		System.loadLibrary("umcs");
	}

	public UMCS(Context context) {
		mContext = context;
	}

	/**
	 * 初始化音视频库
	 * 
	 * @param isMultiMode
	 *            是否为多人模式
	 * @return 0 成功，-1 失败
	 */
	public int Init(UmcsConfig config) {
		TransportType transportType = config.getTransportType();
		if (transportType == TransportType.kExternal
				|| transportType == TransportType.kExternalOriginal) {
			if (config.getTransportCallback() == null) {
				Log.e("umcs", "transport callback is null");
				return -1;
			}
		}
		this.transportCallback = config.getTransportCallback();
		this.transportType = transportType;

		AudioManager _audioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		oldMode = _audioManager.getMode();
		int apiLevel = Integer.parseInt(Build.VERSION.SDK);

		if(apiLevel<=10)
		{
		   _audioManager.setMode(AudioManager.MODE_NORMAL);
		}
		else
		{
			_audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
		}
		OrientationChangedNotify.Instances().Start(mContext);
		SetTraceFilePath("not supported", config.getTraceFilter());

		File exterFileDir = mContext.getExternalFilesDir(null);
		String strDir = null;
		if(exterFileDir != null)
		{
			strDir= exterFileDir.toString();
			File destDir = new File(strDir);
			if (!destDir.exists()) {
					destDir.mkdirs();	
			}
		}
		return Init(mContext, config.getTransportType().getValue(),
				config.isMultiMode() ? 1 : 0,strDir);
	}

	private native int Init(Context context, int tranType, int multi,String filePath);

	private native int 	SetTraceFilePath(String filePath,int filter);

	/**
	 * 终止音视频
	 * 
	 * @return 0 成功，-1 失败
	 */
	public int Terminate() {
		AudioManager _audioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		_audioManager.setMode(oldMode);

		OrientationChangedNotify.Instances().Stop();
		int ret = Fini();
		localVideoObserver = null;
		remoteVideoObserver = null;
		transportListener = null;
		
		return ret;
	}

	private native int Fini();

	/**
	 * 设置扬声器
	 * 
	 * @param enable 是否启用
	 * @return 0 成功，-1 失败
	 */
	public int SetLoudSpeakerEnable(boolean enabled)
	{
		return SetLoudSpeakerEnable(enabled?1:0);
	}
	/**
	 * 设置扬声器
	 * 
	 * @param enable
	 *            是否启用 0 不启用 ： 1 启用
	 * @return 0 成功，-1 失败
	 */
	private native int SetLoudSpeakerEnable(int enable);

	/**
	 * 获取扬声器状态
	 * 
	 * @return 0 不启用 ， 1 启用
	 */
	public native int GetLoudSpeakerEnable();

	/**
	 * 获取音频编解码信息
	 * 
	 * @return 音频编解码信息 成功 ， null 失败
	 */
	public AudioCodecInfo[] GetAudioCodecs() {
		AudioCodecInfo[] cai = new AudioCodecInfo[5];
		cai[0] = new AudioCodecInfo();
		cai[0].codecType = AudioCodecType.iSAC;
		cai[0].plType = 103;
		cai[0].name = "ISAC/16000/1";

		cai[1] = new AudioCodecInfo();
		cai[1].codecType = AudioCodecType.PCMU;
		cai[1].plType = 0;
		cai[1].name = "PCMU/8000/1";

		cai[2] = new AudioCodecInfo();
		cai[2].codecType = AudioCodecType.iLBC;
		cai[2].plType = 102;
		cai[2].name = "ILBC/8000/1";

		cai[3] = new AudioCodecInfo();
		cai[3].codecType = AudioCodecType.AMRWB;
		cai[3].plType = 114;
		cai[3].name = "AMR-WB/16000/1";

		cai[4] = new AudioCodecInfo();
		cai[4].codecType = AudioCodecType.SILK;
		cai[4].plType = 124;
		cai[4].name = "SILK/16000/1";
		return cai;
	}

	/**
	 * 获得摄像头设备总数
	 * 
	 * @return >1 成功 ， 0 失败
	 */
	public native int GetCaptureDeviceNum();

	/**
	 * 获取视频编码信息
	 * 
	 * @return VideoCodecInfo 成功 ， null 失败
	 */
	public VideoCodecInfo[] GetVideoCodecs() {
		if (mContext == null) {
			return null;
		}
		VideoCodecInfo[] vcis = new VideoCodecInfo[1];
		vcis[0] = new VideoCodecInfo();
		vcis[0].codecName = "VP8";
		vcis[0].codecType = VideoCodecType.VP8;
		vcis[0].pyTpye = 120;
		PhoneInfo pInfo = new PhoneInfo(mContext);
		long cpuMaxFrequency = pInfo.getCpuFrequence();
		int cpuNumber = pInfo.getNumCores();
		if ((cpuNumber >= 2) && (cpuMaxFrequency > 1200000)) {
			vcis[0].maxCodecLevel = VideoCodecLevel.kCodecLevelSD;
		} else {
			vcis[0].maxCodecLevel = VideoCodecLevel.kCodecLevelLSD;
		}
		return vcis;
	}

	/**
	 * 创建本地音频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int CreateLocalAudio(int sessionId);

	/**
	 * 创建本地视频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public int CreateLocalVideo(int sessionId) {
		return CreateLocalVideo(sessionId, null);
	}

	private native int CreateLocalVideo(int sessionId, Object localRender);

	/**
	 * 创建远端音频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int CreateRemoteAudio(int sessionId);

	/**
	 * 创建远端视频
	 * 
	 * @param sessionId
	 *            远端视频会话ID
	 * @param container
	 *            远端视频展示的容器
	 * @return 0 成功，-1 失败
	 */
	public int CreateRemoteVideo(int sessionId, ViewGroup container,
			boolean onTop) {
		SurfaceView sView = ViERenderer.CreateRenderer(mContext, true);
		int ret = CreateRemoteVideo(sessionId, sView);
		if (ret != -1) {
			container.addView(sView);
			sView.setZOrderOnTop(onTop);
		}
		return ret;
	}

	private native int CreateRemoteVideo(int sessionId, Object remoteRender);

	/**
	 * 删除本地音频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int DeleteLocalAudio(int sessionId);

	/**
	 * 删除本地视频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int DeleteLocalVideo(int sessionId);

	/**
	 * 删除远端音频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int DeleteRemoteAudio(int sessionId);

	/**
	 * 删除远端视频通道
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int DeleteRemoteVideo(int sessionId);

	/**
	 * 开始发送本地音频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StartSendLocalAudio(int sessionId);

	/**
	 * 开始发送本地视频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StartSendLocalVideo(int sessionId);

	/**
	 * 停止发送本地音频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StopSendLocalAudio(int sessionId);

	/**
	 * 停止发送本地视频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StopSendLocalVideo(int sessionId);

	/**
	 * 开始接受远端音频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StartRecvRemoteAudio(int sessionId);

	/**
	 * 开始接受远端视频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StartRecvRemoteVideo(int sessionId);

	/**
	 * 停止接受远端音频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StopRecvRemoteAudio(int sessionId);

	/**
	 * 停止接受远端视频
	 * 
	 * @param sessionId
	 *            会话ID，两人通话时为 1
	 * @return 0 成功 ， -1 失败
	 */
	public native int StopRecvRemoteVideo(int sessionId);

	/**
	 * 关闭当前打开的摄像头
	 * 
	 * @return 0 成功，-1 失败
	 */
	public int StopCamera() {
		if (currentCameraIndex == -1) {
			return -1;
		}
		localContainer.removeAllViews();
		OrientationChangedNotify.Instances().RemoveObserver(this);
		int ret = StopCamera(currentCameraIndex);
		if (ret == 0) {
			currentCameraIndex = -1;
		}
		return ret;
	}

	private native int StopCamera(int index);

	/**
	 * 打开指定的摄像头，并设置预览容器
	 * 
	 * @param index
	 *            摄像头索引
	 * @param container
	 *            预览容器
	 * @return 0 成功，-1 失败
	 */
	boolean localOnTop = false;

	public int StartCamera(int index, ViewGroup container, boolean onTop) {
		if (index < 0 || index > GetCaptureDeviceNum() - 1) {
			Log.e("camera", "camera index is error , index = " + index);
			return -1;
		}
		localContainer = container;
		localSv = ViERenderer.CreateLocalRenderer(mContext);

		int ret = StartCamera(index);
		if (ret == -1) {
			Log.e("camera", "start camera error,index = " + index);
			return ret;
		}
		currentCameraIndex = index;
		localSv.setZOrderOnTop(onTop);
		localOnTop = onTop;
		container.removeAllViews();
		container.addView(localSv);
		OrientationChangedNotify.Instances().RemoveObserver(this);
		OrientationChangedNotify.Instances().AddObserver(this);
		return ret;
	}

	/**
	 * 获取当前设置的摄像头索引，未设置时返回 -1
	 * 
	 * @return 摄像头索引，-1 表示当前未打开摄像头
	 */
	public int CurrentCameraIndex() {
		return currentCameraIndex;
	}

	/**
	 * 切换摄像头
	 * 
	 * @param index
	 *            需要打开的新摄像头索引
	 * @return 0 成功，-1 失败
	 */
	public int ChangeCamera(int index) {
		if (index < 0 || index > GetCaptureDeviceNum() - 1) {
			Log.e("camera", "camera index is error , index = " + index);
			return -1;
		}
		if (currentCameraIndex == -1) {
			Log.e("camera", "change camera error,camera is not started");
			return -1;
		}
		if (currentCameraIndex == index) {
			Log.e("camera", "change camera error,current = "
					+ currentCameraIndex + " , change to = " + index);
			return -1;
		}
		int ret = StopCamera(currentCameraIndex);
		if (ret == -1) {
			Log.e("camera", "stop camera error,current = " + currentCameraIndex);
			return -1;
		}
		ret = StartCamera(index, localContainer, localOnTop);
		if (ret == -1) {
			Log.e("camera", "start camera error,index = " + index);
			return -1;
		}
		return 0;
	}

	private int StartCamera(int index) {
		ArrayList<CameraSupportVideoSize> sizeList = VideoCaptureDeviceInfoAndroid
				.GetCameraSupportVideoSize();
		if (sizeList.size() > index) {
			CameraSupportVideoSize csvs = sizeList.get(index);
			StartCamera(index, csvs.width, csvs.height);
			return 0;
		}
		return -1;
	}

	private native int StartCamera(int index, int width, int height);

	private native int SetRotation(int index, int degrees);

	private void OnTransportFailed() {
		if (transportListener != null) {
			transportListener.OnTransportFailed();
		}
	}

	/**
	 * 开始音视频数据传输
	 * 
	 * @param tran_type
	 *            传输协议类型
	 * @param local_ip
	 *            本地IP
	 * @param local_port
	 *            本地端口
	 * @param remote_ip
	 *            远端IP
	 * @param remote_port
	 *            远端端口
	 * @param data
	 *            多人音视频认证数据
	 * @param failListener
	 *            传输失败回调
	 * @return 0 成功 ， -1 失败
	 */
	public int StartTransport(String local_ip, int local_port,
			String remote_ip, int remote_port, Object data,
			ITransportListener failListener) {
		if(transportType == TransportType.kExternal||transportType == TransportType.kExternalOriginal)
		{
			Log.e("umcs", "external transport does not support start transport");
			return -1;
		}
		this.transportListener = failListener;
		Log.i("UMCS.java StartTransport ","localIp: "+local_ip+" ,remoteIp: "+remote_ip);
		return StartTransport(transportType.getValue(), local_ip, local_port, remote_ip,
				remote_port, data);
	}

	private native int StartTransport(int tran_type, String local_ip,
			int local_port, String remote_ip, int remote_port, Object data);

	/**
	 * 停止音视频数据传输
	 * 
	 * @return 0 成功，-1 失败
	 */
	public native int StopTransport();

	public native int OnReceiveData(byte[] data);

	public int OnReceiveAudioRtp(byte[] data) {
		return OnReceiveOriginalData(true, false, data);
	}

	public int OnReceiveVideoRtp(byte[] data) {
		return OnReceiveOriginalData(false, false, data);

	}

	public int OnReceiveAudioRtcp(byte[] data) {
		return OnReceiveOriginalData(true, true, data);

	}

	public int OnReceiveVideoRtcp(byte[] data) {
		return OnReceiveOriginalData(false, true, data);
	}

	private native int OnReceiveOriginalData(boolean isAudio, boolean isRtcp,
			byte[] data);

	/**
	 * 改变本地视频显示的视图
	 * 
	 * @param new_render
	 *            新的视图对象
	 * @return 0 成功，-1 失败
	 */
	public native int ChangeLocalRender(Object new_render);

	/**
	 * 改变远端视频显示的视图
	 * 
	 * @param new_render
	 *            新的视图对象
	 * @return 0 成功，-1 失败
	 */
	public native int ChangeRemotelRender(Object new_render);

	/**
	 * 设置本地音频发送的音频编码类型
	 * 
	 * @param codecType
	 *            设置的编码类型
	 * @return 0 成功，-1 失败
	 */
	public int SetAudioSendCodec(AudioCodecType codecType) {
		return SetSendAudioCodec(codecType.getValue());
	}

	private native int SetSendAudioCodec(int index);

	/**
	 * 设置本地视频发送的编码类型
	 * 
	 * @param codecType
	 *            设置的编码类型
	 * @param level
	 *            支持的最大级别，应该为本地最大级别跟远端支持最大级别中的最小值
	 * @return 0 成功，-1 失败
	 */
	public int SetVideoSendCodec(VideoCodecType codecType, VideoCodecLevel level) {
		// h264 is not supported in this version
		if (codecType == VideoCodecType.H264) {
			return -1;
		}
		return SetLocalVideoSendCodec(codecType.getValue(), level.getValue());
	}

	private native int SetLocalVideoSendCodec(int codectype, int level);

	/**
	 * 设置本地视频的信息监测者
	 * 
	 * @param sessionId
	 *            会话Id
	 * @param observer
	 *            信息监测者 ，回调的对象
	 * @return 0 成功 ， -1 失败
	 */
	public int SetLocalVideoObserver(int sessionId, IVideoObserver observer) {
		localVideoObserver = observer;
		return 0;
	}

	/**
	 * 设置远端视频的信息监测者
	 * 
	 * @param sessionId
	 *            会话Id
	 * @param observer
	 *            信息监测者 ，回调的对象
	 * @return 0 成功 ， -1 失败
	 */
	public int SetRemoteVideoObserver(int sessionId, IVideoObserver observer) {
		remoteVideoObserver = observer;
		return 0;
	}

	/**
	 * 设置本地音频静音，设置后远端将不能听到本地的声音
	 * 
	 * @param mute
	 *            是否设置静音
	 * @return 0 成功，-1 失败
	 */
	public native int SetLocalAudioMute(boolean mute);

	public void clear() {
		mContext = null;
	}

	public void SetRotation(int rotation) {

		int or = rotation;
		if (currentCameraIndex > 1 || currentCameraIndex < 0) {
			return;
		}
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(currentCameraIndex, info);
		if (currentCameraIndex == 1) {
			rotation = (360 + (info.orientation - rotation)) % 360;
		} else if (currentCameraIndex == 0) {
			rotation = (360 + (info.orientation + rotation)) % 360; // compensate															// mirror
		}
		Log.d("rotation", "rocation =" + or + "set send rotaion " + rotation);
		SetRotation(currentCameraIndex, rotation);
	}

	@Override
	public void OnOrientationChanged(int orientation) {
		SetRotation(orientation);
	}
}
