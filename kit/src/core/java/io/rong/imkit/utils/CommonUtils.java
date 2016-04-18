package io.rong.imkit.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

import io.rong.imkit.RongContext;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by zhjchen on 4/10/15.
 */

public class CommonUtils {


    /**
     * 本地化通知免打扰时间。
     *
     * @param startTime   默认  “-1”
     * @param spanMinutes 默认 -1
     */
    public static void saveNotificationQuietHours(Context mContext, String startTime, int spanMinutes) {

        SharedPreferences mPreferences = null;

        if (mContext != null)
            mPreferences = mContext.getSharedPreferences("RONG_SDK", Context.MODE_PRIVATE);

        if (mPreferences != null) {
            SharedPreferences.Editor editor = mPreferences.edit();
            editor.putString("QUIET_HOURS_START_TIME", startTime);
            editor.putInt("QUIET_HOURS_SPAN_MINUTES", spanMinutes);
            editor.commit();
        }
    }

    /**
     * 获取通知免打扰开始时间
     *
     * @return
     */
    public static String getNotificationQuietHoursForStartTime(Context mContext) {
        SharedPreferences mPreferences = null;

        if (mPreferences == null && mContext != null)
            mPreferences = mContext.getSharedPreferences("RONG_SDK", Context.MODE_PRIVATE);

        if (mPreferences != null) {
            return mPreferences.getString("QUIET_HOURS_START_TIME", "");
        }

        return "";
    }

    /**
     * 获取通知免打扰时间间隔
     *
     * @return
     */
    public static int getNotificationQuietHoursForSpanMinutes(Context mContext) {
        SharedPreferences mPreferences = null;

        if (mPreferences == null && mContext != null)
            mPreferences = mContext.getSharedPreferences("RONG_SDK", Context.MODE_PRIVATE);

        if (mPreferences != null) {
            return mPreferences.getInt("QUIET_HOURS_SPAN_MINUTES", 0);
        }

        return 0;
    }


    public static void refreshUserInfoIfNeed(RongContext context, UserInfo userInfo){
        if(userInfo == null)
            return;

        UserInfo cacheUserInfo = RongContext.getInstance().getUserInfoCache().get(userInfo.getUserId());

        if (cacheUserInfo == null) {
            RongContext.getInstance().getUserInfoCache().put(userInfo.getUserId(), userInfo);
            context.getEventBus().post(userInfo);
        } else if ((userInfo.getName() != null && cacheUserInfo.getName() != null && !userInfo.getName().equals(cacheUserInfo.getName())) ||
                (userInfo.getPortraitUri() != null && cacheUserInfo.getPortraitUri() != null && !userInfo.getPortraitUri().toString().equals(cacheUserInfo.getPortraitUri().toString()))) {
            RongContext.getInstance().getUserInfoCache().put(userInfo.getUserId(), userInfo);
            context.getEventBus().post(userInfo);
        }
    }

    public static boolean isInConversationPager(String id, Conversation.ConversationType type) {
        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();
        //如果处于所在会话界面，不响铃。
        for (ConversationInfo conversationInfo : list) {
            return id.equals(conversationInfo.getTargetId()) && type == conversationInfo.getConversationType();
        }
        return false;
    }
}
