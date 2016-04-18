package io.rong.voipkit.message;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.rong.common.ParcelUtils;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;

//@MessageTag(value = "RC:VoipCallMsg", flag = MessageTag.ISCOUNTED
//		| MessageTag.ISPERSISTED)
@MessageTag(value = "RC:VoipCallMsg", flag = 0)
public class VoIPCallMessage extends MessageContent implements Parcelable {

    private String sessionId;
    private String ip;
    private int remoteTransferPort;
    private int remoteControlPort;
    private String toId;
    private String toUserName;
    private String fromId;
    private String fromUserName;


    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("sessionId", sessionId);
            jsonObj.put("ip", ip);
            jsonObj.put("remoteTransferPort", remoteTransferPort);
            jsonObj.put("remoteControlPort", remoteControlPort);
            jsonObj.put("toId", toId);

            if (!TextUtils.isEmpty(toUserName))
                jsonObj.put("toUserName", toUserName);

            jsonObj.put("fromId", fromId);

            if (!TextUtils.isEmpty(fromUserName))
                jsonObj.put("fromUserName", fromUserName);

            Log.d("econde", "-----" + jsonObj.toString());
        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }

        return jsonObj.toString().getBytes();
    }

    public VoIPCallMessage(byte[] data) {
        String jsonStr = new String(data);

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            setSessionId(jsonObj.getString("sessionId"));
            setIp(jsonObj.getString("ip"));
            setRemoteTransferPort(jsonObj.getInt("remoteTransferPort"));
            setRemoteControlPort(jsonObj.getInt("remoteControlPort"));

            setToId(jsonObj.getString("toId"));

            if (jsonObj.has("fromUserName"))
                setToUserName(jsonObj.getString("toUserName"));

            setFromId(jsonObj.getString("fromId"));

            if (jsonObj.has("fromUserName"))
                setFromUserName(jsonObj.getString("fromUserName"));

        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }
    }

    public VoIPCallMessage() {
    }

    public VoIPCallMessage(String sessionId, String ip, int remoteTransferPort, int remoteControlPort, String toId,
                           String toUserName, String fromId, String fromUserName) {
        super();
        this.sessionId = sessionId;
        this.ip = ip;
        this.remoteTransferPort = remoteTransferPort;
        this.remoteControlPort = remoteControlPort;
        this.toId = toId;
        this.toUserName = toUserName;
        this.fromId = fromId;
        this.fromUserName = fromUserName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }


    public int getRemoteTransferPort() {
        return remoteTransferPort;
    }

    public void setRemoteTransferPort(int remoteTransferPort) {
        this.remoteTransferPort = remoteTransferPort;
    }

    public int getRemoteControlPort() {
        return remoteControlPort;
    }

    public void setRemoteControlPort(int remoteControlPort) {
        this.remoteControlPort = remoteControlPort;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public VoIPCallMessage(Parcel in) {
        this.sessionId = ParcelUtils.readFromParcel(in);
        this.ip = ParcelUtils.readFromParcel(in);
        ;
        this.remoteTransferPort = ParcelUtils.readIntFromParcel(in);
        this.remoteControlPort = ParcelUtils.readIntFromParcel(in);
        this.toId = ParcelUtils.readFromParcel(in);
        this.toUserName = ParcelUtils.readFromParcel(in);
        this.fromId = ParcelUtils.readFromParcel(in);
        this.fromUserName = ParcelUtils.readFromParcel(in);
    }

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        ParcelUtils.writeToParcel(dest, sessionId);
        ParcelUtils.writeToParcel(dest, ip);
        ParcelUtils.writeToParcel(dest, remoteTransferPort);
        ParcelUtils.writeToParcel(dest, remoteControlPort);
        ParcelUtils.writeToParcel(dest, toId);
        ParcelUtils.writeToParcel(dest, toUserName);
        ParcelUtils.writeToParcel(dest, fromId);
        ParcelUtils.writeToParcel(dest, fromUserName);
    }

    public static final Creator<VoIPCallMessage> CREATOR = new Creator<VoIPCallMessage>() {
        @SuppressWarnings("unchecked")
        @Override
        public VoIPCallMessage createFromParcel(Parcel source) {
            VoIPCallMessage c = new VoIPCallMessage(source);
            return c;
        }

        @Override//创建一个类型为T，长度为size的数组。
        public VoIPCallMessage[] newArray(int size) {
            return new VoIPCallMessage[size];
        }
    };
}