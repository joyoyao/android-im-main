package io.rong.imkit.notification;

import android.content.Context;
import android.text.TextUtils;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import io.rong.common.SystemUtils;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongNotificationManager;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.utils.CommonUtils;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;


/**
 * 控制弹通知和消息音
 * <p/>
 * 1、应用是否在后台
 * 2、新消息提醒设置
 * 3、安静时间设置
 */
public class MessageNotificationManager {

    static boolean isInNeglectTime = false;
    static final int NEGLECT_TIME = 3000;
    static Timer timer = new Timer();

    /**
     * 创建单实例。
     */
    private static class SingletonHolder {
        static final MessageNotificationManager instance = new MessageNotificationManager();
    }

    public static MessageNotificationManager getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 是否设置了消息免打扰，新消息提醒是否关闭？
     *
     * @param context 上下文
     * @param message 要通知的消息
     * @param left    剩余的消息
     * @return
     */
    public void notifyIfNeed(final Context context, final Message message, final int left) {

        if (isInQuietTime(context)) {
            return;
        }

        if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
            if (RongContext.getInstance() != null) {
                ConversationKey key = ConversationKey.obtain(message.getTargetId(), message.getConversationType());
                Conversation.ConversationNotificationStatus notificationStatus = RongContext.getInstance().getConversationNotifyStatusFromCache(key);
                if (notificationStatus != null) {
                    if (notificationStatus == Conversation.ConversationNotificationStatus.NOTIFY) {
                        notify(context, message, left);
                    }
                    return;
                }
            }

            RongIM.getInstance().getRongIMClient().getConversationNotificationStatus(message.getConversationType(), message.getTargetId(), new RongIMClient.ResultCallback<Conversation.ConversationNotificationStatus>() {

                @Override
                public void onSuccess(Conversation.ConversationNotificationStatus status) {
                    if (Conversation.ConversationNotificationStatus.NOTIFY == status) {
                        MessageNotificationManager.getInstance().notify(context, message, left);
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {

                }
            });
        }
    }

    private void notify(Context context, Message message, int left) {
        boolean isInBackground = !SystemUtils.isAppRunningOnTop(context, context.getPackageName());
        if (left != 0 || isInNeglectTime) {
            if (isInBackground) {
                RongNotificationManager.getInstance().onReceiveMessageFromApp(message, true);
            }
            return;
        }

        if (message.getConversationType() == Conversation.ConversationType.CHATROOM) {
            return;
        }

        if (isInBackground) {
            if (!isInNeglectTime) {
                RongNotificationManager.getInstance().onReceiveMessageFromApp(message, false);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        isInNeglectTime = false;
                    }
                }, NEGLECT_TIME);
                isInNeglectTime = true;
            }
        } else if (!CommonUtils.isInConversationPager(message.getTargetId(), message.getConversationType())) {
            MessageTag msgTag = message.getContent().getClass().getAnnotation(MessageTag.class);
            if (msgTag != null && (msgTag.flag() & MessageTag.ISCOUNTED) == MessageTag.ISCOUNTED) {
                MessageSounder.getInstance().messageReminder();
                if (!isInNeglectTime) {
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            isInNeglectTime = false;
                        }
                    }, NEGLECT_TIME);
                    isInNeglectTime = true;
                }
            }
        }
    }

    private boolean isInQuietTime(Context context) {

        String startTimeStr = CommonUtils.getNotificationQuietHoursForStartTime(context);

        int hour = -1;
        int minute = -1;
        int second = -1;

        if (!TextUtils.isEmpty(startTimeStr) && startTimeStr.indexOf(":") != -1) {
            String[] time = startTimeStr.split(":");

            try {
                if (time.length >= 3) {
                    hour = Integer.parseInt(time[0]);
                    minute = Integer.parseInt(time[1]);
                    second = Integer.parseInt(time[2]);
                }
            } catch (NumberFormatException e) {
                RLog.e(MessageNotificationManager.class, "getConversationNotificationStatus", "NumberFormatException");
            }
        }

        if (hour == -1 || minute == -1 || second == -1) {
            return false;
        }

        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, hour);
        startCalendar.set(Calendar.MINUTE, minute);
        startCalendar.set(Calendar.SECOND, second);


        long spanTime = CommonUtils.getNotificationQuietHoursForSpanMinutes(context) * 60;
        long startTime = startCalendar.getTimeInMillis() / 1000;

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTimeInMillis(startTime * 1000 + spanTime * 1000);

        Calendar currentCalendar = Calendar.getInstance();
        if (currentCalendar.get(Calendar.DAY_OF_MONTH) == endCalendar.get(Calendar.DAY_OF_MONTH)) {

            if (currentCalendar.after(startCalendar) && currentCalendar.before(endCalendar))
                return true;
            else
                return false;
        } else  {

            if (currentCalendar.before(startCalendar)) {

                endCalendar.set(Calendar.DAY_OF_MONTH, currentCalendar.get(Calendar.DAY_OF_MONTH));

                if (currentCalendar.get(Calendar.HOUR_OF_DAY) <= endCalendar.get(Calendar.HOUR_OF_DAY))
                    return true;
                else
                    return false;

            } else {

                return false;
            }
        }

    }
}
