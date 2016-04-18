package com.ultrapower.mcs.engine;

/**
 * 视频编码类型
 *
 */
public enum VideoCodecType {
	/**
	 * VP8类型
	 */
	VP8(0),    
	/**
	 * H264类型
	 */
	H264(1);   
	private final int value;

	private VideoCodecType(int value) {
		this.value = value;
	}

	/**
	 * Get the value.
	 * 
	 * @return the value
	 */
	public int getValue() {
		return value;
	}
}
