package com.ultrapower.mcs.engine;

/**
 * 视频清晰度等级
 *
 */
public enum VideoCodecLevel {
    /**
     * 低清
     */
    kCodecLevelLSD(1), 
    /**
     * 高清
     */
    kCodecLevelSD(2),  
    /**
     * 超高清，保留
     */
    kCodecLevelHD(3);  
    private final int value;
    private VideoCodecLevel(int value) {
        this.value = value;
    }
    
    /**
     * Get the value.
     * @return the value
     */
    public int getValue() {
        return value;
    }
}
