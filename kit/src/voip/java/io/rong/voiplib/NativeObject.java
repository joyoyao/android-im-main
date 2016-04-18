package io.rong.voiplib;

public class NativeObject {

    static {
        System.loadLibrary("RongIMVoip");
    }

    NativeObject() {
        setJNIEnv(this);
    }

    public static interface StartVoIPCallback {
        public void OnSuccess(String sessionId, String ip, String remoteTransferPort, String remoteControlPort);

        public void OnError(int errorcode, String description);
    }

    public static interface AcceptVoIPCallback {
        public void OnSuccess();

        public void OnError(int errorcode, String description);
    }

    /**
     * @param nativeObj
     */
    protected native void setJNIEnv(NativeObject nativeObj);

    /**
     * @param strAppId
     * @param strToken
     * @param strFromId
     * @param strToId
     * @param cb
     */
    protected native void StartVoip(String strAppId, String strToken, String strFromId, String strToId, int nLocalPort, StartVoIPCallback cb);

    /**
     * @param strAppId
     * @param strSessionId
     * @param strRemoteIp
     * @param nRemoteTransferPort
     * @param strUserId
     * @param nLocalPort
     * @param nRemoteControlPort
     * @param cb
     */
    protected native void AcceptVoip(String strAppId, String strSessionId, String strRemoteIp, int nRemoteTransferPort, String strUserId, int nLocalPort, int nRemoteControlPort, AcceptVoIPCallback cb);

    /**
     * @param strAppId
     * @param strSessionId
     * @param strUserId
     * @param cb
     */
    protected native void EndVoip(String strAppId, String strSessionId, String strUserId, AcceptVoIPCallback cb);
}
