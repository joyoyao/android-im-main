package io.rong.app.message;

import android.os.Parcel;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import io.rong.common.ParcelUtils;
import io.rong.imlib.MessageTag;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;

/**
 * 自定义融云IM消息类
 *
 * @author lsy
 * @date 2015-11-19
 */
@MessageTag(value = "app:custom", flag = MessageTag.ISCOUNTED
        | MessageTag.ISPERSISTED)
public class RongRedPacketMessage extends MessageContent {

    private String userid;
    private String message;

    public RongRedPacketMessage() {

    }

    public static RongRedPacketMessage obtain(String userid, String message) {
        RongRedPacketMessage rongRedPacketMessage = new RongRedPacketMessage();
        rongRedPacketMessage.userid = userid;
        rongRedPacketMessage.message = message;
        return rongRedPacketMessage;
    }

    // 给消息赋值。
    public RongRedPacketMessage(byte[] data) {

        try {
            String jsonStr = new String(data, "UTF-8");
            JSONObject jsonObj = new JSONObject(jsonStr);
            setUserid(jsonObj.getString("userid"));
            setMessage(jsonObj.getString("message"));
            if (jsonObj.has("user")) {
                setUserInfo(parseJsonToUserInfo(jsonObj.getJSONObject("user")));
            }
        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        } catch (UnsupportedEncodingException e1) {

        }
    }

    /**
     * 构造函数。
     *
     * @param in 初始化传入的 Parcel。
     */
    public RongRedPacketMessage(Parcel in) {
        setUserid(ParcelUtils.readFromParcel(in));
        setMessage(ParcelUtils.readFromParcel(in));
        setUserInfo(ParcelUtils.readFromParcel(in, UserInfo.class));
    }

    /**
     * 读取接口，目的是要从Parcel中构造一个实现了Parcelable的类的实例处理。
     */
    public static final Creator<RongRedPacketMessage> CREATOR = new Creator<RongRedPacketMessage>() {

        @Override
        public RongRedPacketMessage createFromParcel(Parcel source) {
            return new RongRedPacketMessage(source);
        }

        @Override
        public RongRedPacketMessage[] newArray(int size) {
            return new RongRedPacketMessage[size];
        }
    };

    /**
     * 描述了包含在 Parcelable 对象排列信息中的特殊对象的类型。
     *
     * @return 一个标志位，表明Parcelable对象特殊对象类型集合的排列。
     */
    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * 将类的数据写入外部提供的 Parcel 中。
     *
     * @param dest  对象被写入的 Parcel。
     * @param flags 对象如何被写入的附加标志。
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // 这里可继续增加你消息的属性
        ParcelUtils.writeToParcel(dest, message);
        ParcelUtils.writeToParcel(dest, userid);
        ParcelUtils.writeToParcel(dest, getUserInfo());

    }

    /**
     * 将消息属性封装成 json 串，再将 json 串转成 byte 数组，该方法会在发消息时调用
     */
    @Override
    public byte[] encode() {
        JSONObject jsonObj = new JSONObject();
        try {

            jsonObj.put("userid", userid);
            jsonObj.put("message", message);

            if (getJSONUserInfo() != null)
                jsonObj.putOpt("user", getJSONUserInfo());

        } catch (JSONException e) {
            Log.e("JSONException", e.getMessage());
        }

        try {
            return jsonObj.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
