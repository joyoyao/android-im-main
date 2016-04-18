package io.rong.voipkit.message;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import io.rong.imlib.MessageTag;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

//@MessageTag(value = "RC:VoipCallMsg", flag = MessageTag.ISCOUNTED
//		| MessageTag.ISPERSISTED)
@MessageTag(value = "RC:VoipFinishMsg", flag = 0)
public class VoIPFinishMessage extends MessageContent implements Parcelable {


    public static final int FINISH_NORMAL = 0;//正常结束
    public static final int FINISH_REFUSE = 1;//拒绝

    private String toId;
    private int finish_state = FINISH_NORMAL;//结束状态


    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();

        try {
            jsonObj.put("toId", toId);
            jsonObj.put("finish_state", finish_state);
        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }

        return jsonObj.toString().getBytes();
    }

    public VoIPFinishMessage(byte[] data) {
        String jsonStr = new String(data);

        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            setToId(jsonObj.getString("toId"));
            setFinish_state(jsonObj.getInt("finish_state"));
        } catch (JSONException e) {
            Log.d("JSONException", e.getMessage());
        }
    }

    public VoIPFinishMessage() {
    }

    public VoIPFinishMessage(String toId) {
        super();
        this.toId = toId;
    }


    public String getToId() {
        return toId;
    }

    public void setToId(String toId) {
        this.toId = toId;
    }

    public int getFinish_state() {
        return finish_state;
    }

    public void setFinish_state(int finish_state) {
        this.finish_state = finish_state;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flag) {
        dest.writeString(toId);
        dest.writeInt(finish_state);
    }

    public static final Creator<VoIPFinishMessage> CREATOR = new Creator<VoIPFinishMessage>() {
        @SuppressWarnings("unchecked")
        @Override
        public VoIPFinishMessage createFromParcel(Parcel source) {
            VoIPFinishMessage c = new VoIPFinishMessage(source.readString());
            c.setFinish_state(source.readInt());
            return c;
        }

        @Override//创建一个类型为T，长度为size的数组。
        public VoIPFinishMessage[] newArray(int size) {
            return new VoIPFinishMessage[size];
        }
    };
}