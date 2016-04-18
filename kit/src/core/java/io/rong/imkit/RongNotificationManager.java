package io.rong.imkit;

import android.text.Spannable;

import java.util.concurrent.ConcurrentHashMap;

import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.notification.PushNotificationManager;
import io.rong.notification.PushNotificationMessage;

public class RongNotificationManager {
    private static RongNotificationManager sS;
    RongContext mContext;
    ConcurrentHashMap<String, Message> messageMap = new ConcurrentHashMap<>();

    static {
        sS = new RongNotificationManager();
    }

    private RongNotificationManager(){}

    public void init(RongContext context) {
        mContext = context;
        if(!context.getEventBus().isRegistered(this)) {
            context.getEventBus().register(this);
        }
    }

    public static RongNotificationManager getInstance() {
        if (sS == null) {
            sS = new RongNotificationManager();
        }
        return sS;
    }

    public void onReceiveMessageFromApp(Message message, boolean isKeepSilent) {

        Conversation.ConversationType type = message.getConversationType();
        String targetUserName = null;
        PushNotificationMessage pushMsg;

        IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(message.getContent().getClass());
        if (provider == null)
            return;

        Spannable content = provider.getContentSummary(message.getContent());

        ConversationKey key = ConversationKey.obtain(message.getTargetId(), message.getConversationType());

        RLog.i(RongNotificationManager.this, "onReceiveMessageFromApp", "start");

        if (content == null) {
            RLog.i(RongNotificationManager.this, "onReceiveMessageFromApp", "Content is null. Return directly.");
            return;
        }

        if (type.equals(Conversation.ConversationType.PRIVATE) || type.equals(Conversation.ConversationType.CUSTOMER_SERVICE)
                || type.equals(Conversation.ConversationType.CHATROOM) || type.equals(Conversation.ConversationType.SYSTEM)) {

            UserInfo userInfo = message.getContent().getUserInfo();
            if(userInfo == null || userInfo.getName() == null)
                userInfo = RongContext.getInstance().getUserInfoFromCache(message.getTargetId());

            if (userInfo != null)
                targetUserName = userInfo.getName();
            else
                targetUserName = message.getTargetId();

            if (targetUserName != null) {
                pushMsg = PushNotificationMessage.obtain(content.toString(), message.getConversationType(), message.getTargetId(), targetUserName);
                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, isKeepSilent);
            } else {
                messageMap.put(key.getKey(), message);
            }

        } else if (type.equals(Conversation.ConversationType.GROUP)) {
            Group groupInfo = RongContext.getInstance().getGroupInfoFromCache(message.getTargetId());
            String targetName = null;
            if (groupInfo != null) {
                targetUserName = groupInfo.getName();
            }

            if (targetUserName != null) {
                MessageContent messageContent = message.getContent();
                if (messageContent.getUserInfo() != null) {
                    targetName = messageContent.getUserInfo().getName() + " : ";
                } else {
                    UserInfo userInfo;
                    GroupUserInfo groupUserInfo = RongContext.getInstance().getGroupUserInfoFromCache(message.getTargetId(), message.getSenderUserId());
                    if(groupUserInfo != null) {
                        targetName = groupUserInfo.getNickname() + " : ";
                    } else {
                        userInfo = message.getContent().getUserInfo();
                        if(userInfo == null || userInfo.getName() == null) {
                            userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());
                        }
                        if (userInfo != null)
                            targetName = userInfo.getName() + " : ";
                        else
                            targetName = message.getSenderUserId() + " : ";
                    }
                }
                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), message.getConversationType(), message.getTargetId(), targetUserName);
                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            } else {
                messageMap.put(key.getKey(), message);
            }

        } else if (type.equals(Conversation.ConversationType.DISCUSSION)) {
            Discussion discussionInfo = RongContext.getInstance().getDiscussionInfoFromCache(message.getTargetId());

            UserInfo userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());

            String targetName = null;

            if (discussionInfo != null) {
                targetUserName = discussionInfo.getName();
            }
            if (targetUserName != null) {
                if (userInfo != null) {
                    targetName = userInfo.getName() + " : ";
                } else {
                    targetName = message.getSenderUserId() + " : ";
                }

                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), message.getConversationType(), message.getTargetId(), targetUserName);
                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            } else {
                messageMap.put(key.getKey(), message);
            }
        } else if (type.getName().equals(Conversation.ConversationType.PUBLIC_SERVICE.getName()) ||
                type.getName().equals(Conversation.PublicServiceType.APP_PUBLIC_SERVICE.getName())) {

            PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(key.getKey());

            if (info != null) {
                targetUserName = info.getName();
            }

            if (targetUserName != null) {
                pushMsg = PushNotificationMessage.obtain(content.toString(), message.getConversationType(), message.getTargetId(), targetUserName);
                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            } else {
                messageMap.put(key.getKey(), message);
            }
        }
    }


    public void onRemoveNotification() {
        messageMap.clear();
        PushNotificationManager.getInstance().onRemoveNotificationMsgFromCache();
    }

    public void onEventMainThread(UserInfo userInfo) {

        Message message;
        PushNotificationMessage pushMsg;

        Conversation.ConversationType[] types = new Conversation.ConversationType[]{Conversation.ConversationType.PRIVATE,
                Conversation.ConversationType.CUSTOMER_SERVICE, Conversation.ConversationType.CHATROOM, Conversation.ConversationType.SYSTEM};

        for (Conversation.ConversationType type : types) {

            String key = ConversationKey.obtain(userInfo.getUserId(), type).getKey();

            if (messageMap.containsKey(key)) {
                message = messageMap.get(key);

                Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                        .getContentSummary(message.getContent());

                if (userInfo.getName() == null) {
                    pushMsg = PushNotificationMessage.obtain(content.toString(), type, userInfo.getUserId(), userInfo.getUserId());
                } else {
                    pushMsg = PushNotificationMessage.obtain(content.toString(), type, userInfo.getUserId(), userInfo.getName());
                }

                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
                messageMap.remove(key);
            }
        }
    }

    public void onEventMainThread(Group groupInfo) {

        Message message;
        PushNotificationMessage pushMsg;
        String targetName = null;
        String key = ConversationKey.obtain(groupInfo.getId(), Conversation.ConversationType.GROUP).getKey();

        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);

            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());

            UserInfo userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());

            if (userInfo != null) {
                targetName = userInfo.getName() + ":";
            } else {
                targetName = groupInfo.getId().toString() + ":";
            }

            if (groupInfo.getName() == null) {
                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), Conversation.ConversationType.GROUP, groupInfo.getId(), groupInfo.getId());
            } else {
                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), Conversation.ConversationType.GROUP, groupInfo.getId(), groupInfo.getName());
            }

            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);
        }

    }

    public void onEventMainThread(Discussion discussion) {

        Message message;
        PushNotificationMessage pushMsg;
        String targetName = null;
        String key = ConversationKey.obtain(discussion.getId(), Conversation.ConversationType.DISCUSSION).getKey();
        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());

            UserInfo userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());

            if (userInfo != null) {
                targetName = userInfo.getName() + ":";
            } else {
                targetName = discussion.getId().toString() + ":";
            }

            if (discussion.getName() == null) {
                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), Conversation.ConversationType.DISCUSSION, discussion.getId(), discussion.getId());
            } else {
                pushMsg = PushNotificationMessage.obtain(targetName + content.toString(), Conversation.ConversationType.DISCUSSION, discussion.getId(), discussion.getName());
            }
            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);
        }
    }

    public void onEventMainThread(PublicServiceProfile info) {

        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(info.getTargetId(), info.getConversationType()).getKey();

        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());
            if (info.getName() == null) {
                pushMsg = PushNotificationMessage.obtain(content.toString(), info.getConversationType(), info.getTargetId(), info.getTargetId());
            } else {
                pushMsg = PushNotificationMessage.obtain(content.toString(), info.getConversationType(), info.getTargetId(), info.getName());
            }
            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);
        }
    }

    public void onEventMainThread(Event.NotificationUserInfoEvent event) {
        Message message;
        PushNotificationMessage pushMsg;

        Conversation.ConversationType[] types = new Conversation.ConversationType[]{Conversation.ConversationType.PRIVATE,
                Conversation.ConversationType.CUSTOMER_SERVICE, Conversation.ConversationType.CHATROOM, Conversation.ConversationType.SYSTEM};

        for (Conversation.ConversationType type : types) {

            String key = ConversationKey.obtain(event.getKey(), type).getKey();

            if (messageMap.containsKey(key)) {
                message = messageMap.get(key);

                Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                        .getContentSummary(message.getContent());

                String title = (String) mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo());

                pushMsg = PushNotificationMessage.obtain(content.toString(), type, event.getKey(), title);

                PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
                messageMap.remove(key);
            }
        }
    }

    public void onEventMainThread(Event.NotificationGroupInfoEvent event) {

        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(event.getKey(), Conversation.ConversationType.GROUP).getKey();

        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);

            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());

            String title = (String) mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo());

            pushMsg = PushNotificationMessage.obtain(content.toString(), Conversation.ConversationType.GROUP, event.getKey(), title);

            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);
        }
    }

    public void onEventMainThread(Event.NotificationDiscussionInfoEvent event) {

        Message message;
        PushNotificationMessage pushMsg;
        String key = ConversationKey.obtain(event.getKey(), Conversation.ConversationType.DISCUSSION).getKey();
        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);
            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());

            String title = (String) mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo());

            pushMsg = PushNotificationMessage.obtain("Hello :" + content.toString(), Conversation.ConversationType.DISCUSSION, event.getKey(), title);

            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);
        }
    }

    public void onEventMainThread(Event.NotificationPublicServiceInfoEvent event) {

        Message message;
        PushNotificationMessage pushMsg;
        String key = event.getKey();

        if (messageMap.containsKey(key)) {
            message = messageMap.get(key);

            Spannable content = RongContext.getInstance().getMessageTemplate(message.getContent().getClass())
                    .getContentSummary(message.getContent());

            String title = (String) mContext.getPackageManager().getApplicationLabel(mContext.getApplicationInfo());

            pushMsg = PushNotificationMessage.obtain(content.toString(), message.getConversationType(), message.getTargetId(), title);

            PushNotificationManager.getInstance().onReceiveMessage(pushMsg, false);
            messageMap.remove(key);

        }
    }
}
