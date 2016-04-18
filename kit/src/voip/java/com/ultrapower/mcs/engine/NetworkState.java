package com.ultrapower.mcs.engine;

/**
 * 网络状态
 * @author Administrator
 *
 */
public enum NetworkState {
	
    /**
     * 网络状态差
     */
    kNetworkStateLow(1),    
    /**
     * 网络状态一般
     */
    kNetworkStateNormal(2), 
    /**
     * 网络状态好
     */
    kNetworkStateGood(3);  
    
    private final int value;
    /**
     * Constructor.
     */
    private NetworkState(int value) {
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
