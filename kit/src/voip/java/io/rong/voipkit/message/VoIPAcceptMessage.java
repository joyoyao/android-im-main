package io.rong.voipkit.message;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;


@MessageTag(value = "RC:VoipAcceptMsg", flag = 0)
public class VoIPAcceptMessage extends MessageContent implements Parcelable {

    private String toId;


    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("toId", toId);
        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }

        return jsonObj.toString().getBytes();
    }

    public VoIPAcceptMessage(byte[] data) {
        String jsonStr = new String(data);

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            setToId(jsonObj.getString("toId"));
        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }
    }

    public VoIPAcceptMessage() {
    }

    public VoIPAcceptMessage(String toId) {
        super();
        this.toId = toId;
    }

    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(toId);
    }

    public static final Creator<VoIPAcceptMessage> CREATOR = new Creator<VoIPAcceptMessage>() {
        @SuppressWarnings("unchecked")
        @Override
        public VoIPAcceptMessage createFromParcel(Parcel source) {
            VoIPAcceptMessage c = new VoIPAcceptMessage(source.readString());
            return c;
        }

        @Override//创建一个类型为T，长度为size的数组。
        public VoIPAcceptMessage[] newArray(int size) {
            return new VoIPAcceptMessage[size];
        }
    };
}