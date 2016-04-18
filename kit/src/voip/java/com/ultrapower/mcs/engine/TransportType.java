package com.ultrapower.mcs.engine;
public enum TransportType {
	
    /**
     * 普通UDP传输，使用内置传输层进行传输，调用UMCS的StartTransport方法开始传输、接收数据；
     */
    kUdpGernal(0),
    /**
     * 增加了TURN协议头的传输，配合Transfer服务使用，使用内置传输层进行传输，调用UMCS的StartTransport方法开始传输、接收数据；
     */
    kUdpRelay(1),
    /**
     * 外部传输，所有数据可通过一个端口传输
     * 发送：
     *    引擎产生的数据将回调ITransportCallback接口的SendData方法，外部实现此方法发送数据；
     * 接收：
     *    当外部收到数据后调用UMCS.OnReceiveData方法投递给引擎；
     */
    kExternal(4),
    
    /**
     * 外部传输，
     * 正常情况下，数据传输将分为audio rtp,audio rtcp,video rtp,video rtcp四种类型发送
     * 其中audio rtp 跟 video rtp 流可以多路复用，即可以将audio rtcp 包投递到 audio rtp流中
     * 发送：
     *    引擎产生的数据将回调ITransportCallback	的SendAudioRtp，SendAudioRtcp，SendVideoRtp，SendVideoRtcp方法；
     * 接收：
     *    当外部收到数据后调用UMCS的OnReceiveAudioRtp,OnReceiveAudioRtcp,OnReceiveVideoRtp,OnReceiveVideoRtcp方法投递到引擎；
     */
    kExternalOriginal(5);
    private final int value;
    private TransportType(int value) {
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