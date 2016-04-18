package com.ultrapower.mcs.engine;

/**
 * 音频编码类型
 * @author Administrator
 *
 */
public enum AudioCodecType {
	iSAC(0),
	PCMU(7),
	iLBC(11),
	AMRWB(12),
	SILK(22);
	private final int value;

	private AudioCodecType(int value) {
		this.value = value;
	}
	public int getValue() {
		return value;
	}
}