package io.rong.imkit.util;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

/**
 * Created by zhjchen on 3/16/15.
 */

/**
 * 用于消息提供者获取用户信息使用
 */
public class MessageProviderUserInfoHelper {

    private ConcurrentHashMap<MessageContent, List<String>> mMessageIdUserIdsMap = new ConcurrentHashMap<>();
    private static MessageProviderUserInfoHelper mHelper;
    ArrayList<String> cacheUserIds = new ArrayList<>();

    HandlerThread mWorkThread;
    Handler mUserInfoHandler;


    public static MessageProviderUserInfoHelper getInstance() {
        if (mHelper == null) {
            synchronized (MessageProviderUserInfoHelper.class) {
                if (mHelper == null) {
                    mHelper = new MessageProviderUserInfoHelper();

                }
            }
        }
        return mHelper;
    }

    MessageProviderUserInfoHelper() {
        mWorkThread = new HandlerThread("MessageProviderUserInfoHelper");
        mWorkThread.start();
        mUserInfoHandler = new Handler(mWorkThread.getLooper());
    }

    synchronized void setCacheUserId(String userId) {
        if (!cacheUserIds.contains(userId))
            cacheUserIds.add(userId);
    }

    synchronized void removeCacheUserId(String userId) {
        if (cacheUserIds.contains(userId))
            cacheUserIds.remove(userId);
    }

    public synchronized boolean isCacheUserId(String userId) {
        return cacheUserIds.contains(userId);
    }

    /**
     * @param message
     * @param userId
     */
    public void registerMessageUserInfo(MessageContent message, String userId) {

        RLog.i(this, "registerMessageUserInfo", "userId:" + userId);

        List<String> userIdList = mMessageIdUserIdsMap.get(message);

        if (userIdList == null) {
            userIdList = new ArrayList<>();
            mMessageIdUserIdsMap.put(message, userIdList);
        }

        if (!userIdList.contains(userId)) {
            userIdList.add(userId);
        }

        setCacheUserId(userId);

    }

    /**
     * @param userId
     */
    public void notifyMessageUpdate(final String userId) {

        Iterator messageUserIdsIterator = mMessageIdUserIdsMap.entrySet().iterator();

        mUserInfoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                removeCacheUserId(userId);
            }
        }, 500);


        while (messageUserIdsIterator.hasNext()) {
            Map.Entry userIdMessageEntry = (Map.Entry) messageUserIdsIterator.next();
            List<String> userIdList = (List<String>) userIdMessageEntry.getValue();

            if (userIdList != null) {
                if (userIdList.contains(userId)) {
                    userIdList.remove(userId);
                }

                if (userIdList.isEmpty()) {
                    RongContext.getInstance().getEventBus().post(userIdMessageEntry.getKey());
                    mMessageIdUserIdsMap.remove(userIdMessageEntry.getKey());
                    RLog.d(this, "notifyMessageUpdate", "--notify--" + userIdMessageEntry.getKey().toString());
                } else {
                    RLog.d(this, "notifyMessageUpdate", "--wait--" + userId);
                }
            }
        }
    }


    public boolean isRequestGetUserInfo() {
        return mMessageIdUserIdsMap.isEmpty() ? false : true;
    }


}
