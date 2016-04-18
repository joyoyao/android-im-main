package com.ultrapower.mcs.engine;


/**
 * 网络传输监控接口
 */
public interface ITransportListener {
	
	/**
	 * 当底层网络检测到15秒内没有收到任何数据时回调此方法
	 */
	public void OnTransportFailed();
}
