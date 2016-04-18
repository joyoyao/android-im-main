package io.rong.imkit.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.message.VoiceMessage;

public class UIConversation implements Parcelable {
    private String conversationTitle;
    private Uri portrait;
    private Spannable conversationContent;
    private MessageContent messageContent;
    private long conversationTime;
    private int unReadMessageCount;
    private boolean isTop;
    private Conversation.ConversationType conversationType;
    private Message.SentStatus sentStatus;
    private String targetId;
    private String senderId;
    private boolean isGathered; //该会话是否处于聚合显示的状态
    private boolean notificationBlockStatus;
    private String draft;
    private int latestMessageId;
    private boolean extraFlag;

    public boolean getExtraFlag() {
        return extraFlag;
    }

    public void setExtraFlag(boolean extraFlag) {
        this.extraFlag = extraFlag;
    }

    private ArrayList<String> nicknameIds;

    public UIConversation() {
        nicknameIds = new ArrayList<>();
    }


    public void setUIConversationTitle(String title) {
        conversationTitle = title;
    }

    public String getUIConversationTitle() {
        return conversationTitle;
    }

    public void setIconUrl(Uri iconUrl) {
        portrait = iconUrl;
    }

    public Uri getIconUrl() {
        return portrait;
    }

    public void setConversationContent(Spannable content) {
        conversationContent = content;
    }

    public Spannable getConversationContent() {
        return conversationContent;
    }

    public void setMessageContent(MessageContent content) {
        messageContent = content;
    }

    public MessageContent getMessageContent() {
        return messageContent;
    }

    public void setUIConversationTime(long time) {
        conversationTime = time;
    }

    public long getUIConversationTime() {
        return conversationTime;
    }

    public void setUnReadMessageCount(int count) {
        unReadMessageCount = count;
    }

    public int getUnReadMessageCount() {
        return unReadMessageCount;
    }

    public void setTop(boolean value) {
        isTop = value;
    }

    public boolean isTop() {
        return isTop;
    }

    public void setConversationType(Conversation.ConversationType type) {
        conversationType = type;
    }

    public Conversation.ConversationType getConversationType() {
        return conversationType;
    }

    public void setSentStatus(Message.SentStatus status) {
        sentStatus = status;
    }

    public Message.SentStatus getSentStatus() {
        return sentStatus;
    }

    public void setConversationTargetId(String id) {
        targetId = id;
    }

    public String getConversationTargetId() {
        return targetId;
    }

    public void setConversationSenderId(String id) {
        senderId = id;
    }

    public String getConversationSenderId() {
        return senderId;
    }

    public void setConversationGatherState(boolean state) {
        isGathered = state;
    }

    public boolean getConversationGatherState() {
        return isGathered;
    }

    public void setNotificationBlockStatus(boolean status) {
        notificationBlockStatus = status;
    }

    public boolean getNotificationBlockStatus() {
        return notificationBlockStatus;
    }

    public void setDraft(String content) {
        draft = content;
    }

    public String getDraft() {
        return draft;
    }

    public void setLatestMessageId(int id) {
        this.latestMessageId = id;
    }

    public int getLatestMessageId() {
        return latestMessageId;
    }

    public void addNickname(String userId) {
        nicknameIds.add(userId);
    }

    public void removeNickName(String userId) {
        nicknameIds.remove(userId);
    }

    public boolean hasNickname(String userId) {
        return nicknameIds.contains(userId);
    }

    public static UIConversation obtain(Conversation conversation, boolean gatherState) {
        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongContext hasn't been initialized !!");

        if (RongContext.getInstance().getConversationTemplate(conversation.getConversationType().getName()) == null) {
            throw new IllegalArgumentException("the conversation type hasn't been registered! type:" + conversation.getConversationType());
        }

        MessageContent msgContent = conversation.getLatestMessage();

        UserInfo userInfo;
        String title;
        Uri uri = null;
        if (!TextUtils.isEmpty(conversation.getPortraitUrl()))
            uri = Uri.parse(conversation.getPortraitUrl());
        title = conversation.getConversationTitle();

        // 私聊或者系统会话时，根据message里携带的用户信息，更新会话的标题为用户名称，会话头像为用户头像。
        if (uri == null
                && msgContent != null
                && (conversation.getConversationType().equals(Conversation.ConversationType.PRIVATE)
                || conversation.getConversationType().equals(Conversation.ConversationType.SYSTEM))) {
            userInfo = msgContent.getUserInfo();
            String targetId = conversation.getTargetId();
            if (targetId != null && userInfo != null && userInfo.getUserId().equals(targetId)) {
                title = userInfo.getName();
                uri = userInfo.getPortraitUri();
            }

            if (!gatherState && uri != null) {
                RongIMClient.getInstance().updateConversationInfo(conversation.getConversationType(), conversation.getTargetId(), title, uri.toString(), null);
            }
        }
        // 根据用户信息提供者获取用户信息
        if (uri == null || title == null) {
            title = RongContext.getInstance().getConversationTemplate(conversation.getConversationType().getName())
                    .getTitle(conversation.getTargetId());
            uri = RongContext.getInstance().getConversationTemplate(conversation.getConversationType().getName())
                    .getPortraitUri(conversation.getTargetId());
        }

        //配置会话相关信息
        UIConversation uiConversation = new UIConversation();
        uiConversation.setMessageContent(msgContent);
        uiConversation.setUnReadMessageCount(conversation.getUnreadMessageCount());
        uiConversation.setUIConversationTime(conversation.getSentTime());
        uiConversation.setConversationGatherState(gatherState);
        if (gatherState && RongContext.getInstance() != null) {
            uiConversation.setUIConversationTitle(RongContext.getInstance().getGatheredConversationTitle(conversation.getConversationType()));
            uiConversation.setIconUrl(null);
        } else {
            uiConversation.setUIConversationTitle(title);
            uiConversation.setIconUrl(uri);
        }
        uiConversation.setConversationType(conversation.getConversationType());
        uiConversation.setTop(conversation.isTop());
        uiConversation.setSentStatus(conversation.getSentStatus());
        uiConversation.setConversationTargetId(conversation.getTargetId());
        uiConversation.setConversationSenderId(conversation.getSenderUserId());
        uiConversation.setLatestMessageId(conversation.getLatestMessageId());
        uiConversation.setDraft(conversation.getDraft());
        if (!TextUtils.isEmpty(conversation.getDraft())) {
            uiConversation.setSentStatus(null);
        }
        uiConversation.setConversationContent(uiConversation.buildConversationContent(uiConversation));//进一步根据聚合信息等，完善会话内容显示

        return uiConversation;
    }

    public static UIConversation obtain(Message message, boolean gather) {
        String title = "";
        Uri iconUri = null;
        UserInfo userInfo = message.getContent().getUserInfo();
        Conversation.ConversationType conversationType = message.getConversationType();
        // 私聊或者系统会话时，根据message里携带的用户信息，更新会话的标题为用户名称，会话头像为用户头像。
        if(userInfo != null
                && message.getTargetId().equals(userInfo.getUserId())
                && (conversationType.equals(Conversation.ConversationType.PRIVATE)
                || conversationType.equals(Conversation.ConversationType.SYSTEM))) {
            iconUri = userInfo.getPortraitUri();
            title = userInfo.getName();
            if (!gather) {
                RongIMClient.getInstance().updateConversationInfo(message.getConversationType(), message.getTargetId(), title, iconUri != null ? iconUri.toString() : "", null);
            }
        }
        // 根据用户信息提供者获取用户信息
        if (RongContext.getInstance() != null && (iconUri == null || title == null)) {
            title = RongContext.getInstance().getConversationTemplate(message.getConversationType().getName())
                    .getTitle(message.getTargetId());
            iconUri = RongContext.getInstance().getConversationTemplate(message.getConversationType().getName())
                    .getPortraitUri(message.getTargetId());
        }
        //配置会话相关信息
        MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
        UIConversation tempUIConversation = new UIConversation();
        if (tag != null && (tag.flag() & MessageTag.ISCOUNTED) == MessageTag.ISCOUNTED) {
            tempUIConversation.setUnReadMessageCount(1);
        }
        tempUIConversation.setMessageContent(message.getContent());
        tempUIConversation.setUIConversationTime(message.getSentTime());
        if (gather) {
            tempUIConversation.setUIConversationTitle(RongContext.getInstance().getGatheredConversationTitle(message.getConversationType()));
            tempUIConversation.setIconUrl(null);
        } else {
            tempUIConversation.setUIConversationTitle(title);
            tempUIConversation.setIconUrl(iconUri);
        }
        tempUIConversation.setConversationType(message.getConversationType());
        tempUIConversation.setConversationTargetId(message.getTargetId());
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null)
                tempUIConversation.setConversationSenderId(RongIM.getInstance().getRongIMClient().getCurrentUserId());
        } else {
            tempUIConversation.setConversationSenderId(message.getSenderUserId());
        }

        tempUIConversation.setSentStatus(message.getSentStatus());
        tempUIConversation.setLatestMessageId(message.getMessageId());
        tempUIConversation.setConversationGatherState(gather);
        tempUIConversation.setConversationContent(tempUIConversation.buildConversationContent(tempUIConversation));

        return tempUIConversation;
    }

    public SpannableStringBuilder buildConversationContent(UIConversation uiConversation) {
        boolean isGathered = uiConversation.getConversationGatherState();
        String type = uiConversation.getConversationType().getName();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean isShowName;
        final Spannable messageData;

        if (uiConversation.getMessageContent() == null) {
            builder.append("");
            return builder;
        }

        isShowName = RongContext.getInstance().getMessageProviderTag(uiConversation.getMessageContent().getClass()).showSummaryWithName();

        messageData = RongContext.getInstance().getMessageTemplate(uiConversation.getMessageContent().getClass()).getContentSummary(uiConversation.getMessageContent());

        if (messageData == null) {
            builder.append("");
            return builder;
        }

        if (uiConversation.getMessageContent() instanceof VoiceMessage) {
            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {

                Conversation conv = RongIM.getInstance().getRongIMClient().getConversation(uiConversation.getConversationType(), uiConversation.getConversationTargetId());
                if (conv != null) {
                    boolean isListened = conv.getReceivedStatus().isListened();
                    if (isListened) {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        if (!isShowName) {
            builder.append(messageData);
            return builder;
        }

        String senderId = uiConversation.getConversationSenderId();
        if (isGathered) {
            String targetName = RongContext.getInstance().getConversationTemplate(type)
                    .getTitle(uiConversation.getConversationTargetId());
            builder.append(targetName == null ? uiConversation.getConversationTargetId() : targetName)
                    .append(" : ")
                    .append(messageData);
        } else if (Conversation.ConversationType.GROUP.getName().equals(type)) {
            String senderName;
            GroupUserInfo info = RongContext.getInstance().getGroupUserInfoFromCache(uiConversation.targetId, senderId);
            if (RongContext.getInstance().getGroupUserInfoProvider() != null && info != null) {
                senderName = info.getNickname();
            } else {
                UserInfo userInfo = uiConversation.getMessageContent().getUserInfo();
                if (userInfo == null || userInfo.getName() == null) {
                    senderName = RongContext.getInstance()
                            .getConversationTemplate(Conversation.ConversationType.PRIVATE.getName())
                            .getTitle(senderId);
                } else {
                    senderName = userInfo.getName();
                }
            }
            builder.append(senderName == null ? (senderId == null ? "" : senderId) : senderName)
                    .append(" : ")
                    .append(messageData);
        } else if (Conversation.ConversationType.DISCUSSION.getName().equals(type)) {
            String senderName = RongContext.getInstance()
                    .getConversationTemplate(Conversation.ConversationType.PRIVATE.getName())
                    .getTitle(uiConversation.getConversationSenderId());
            builder.append(senderName == null ? (senderId == null ? "" : senderId) : senderName)
                    .append(" : ")
                    .append(messageData);
        } else {
            return builder.append(messageData);
        }
        return builder;
    }

    private UnreadRemindType mUnreadType = UnreadRemindType.REMIND_WITH_COUNTING;

    public void setUnreadType(UnreadRemindType type) {
        this.mUnreadType = type;
    }

    public UnreadRemindType getUnReadType() {
        return mUnreadType;
    }

    public enum UnreadRemindType {
        /**
         * 无未读提示
         */
        NO_REMIND,
        /**
         * 提示，但无计数
         */
        REMIND_ONLY,
        /**
         * 带计数的提示
         */
        REMIND_WITH_COUNTING
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static final Creator<UIConversation> CREATOR = new Creator<UIConversation>() {

        @Override
        public UIConversation createFromParcel(Parcel source) {
            return new UIConversation();
        }

        @Override
        public UIConversation[] newArray(int size) {
            return new UIConversation[size];
        }
    };
}
