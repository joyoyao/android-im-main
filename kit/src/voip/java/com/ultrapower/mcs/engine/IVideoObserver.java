package com.ultrapower.mcs.engine;

/**
 * 视频监控回调接口   
 *
 */
public interface IVideoObserver {
	/**
	 * 视频级别发送变化后的回调函数
	 * @param level 变化后视频级别
	 */
	void OnVideoLevelChanged(VideoCodecLevel level);
	/**
	 * 网络状态发送变化后的回调函数
	 * @param state 变化后网络状态
	 */
	void OnNetworkStateChanged(NetworkState state);
}
