package io.rong.imkit.model;

import android.text.SpannableStringBuilder;

import io.rong.imkit.util.AndroidEmoji;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.TextMessage;

/**
 * Created by zhjchen on 7/21/15.
 */

public class UIMessage extends Message {

    private SpannableStringBuilder textMessageContent;
    private UserInfo mUserInfo;
    private int mProgress;


    public Message getMessage() {

        Message message = new Message();

        message.setConversationType(getConversationType());
        message.setTargetId(getTargetId());
        message.setMessageId(getMessageId());
        message.setObjectName(getObjectName());
        message.setContent(getContent());
        message.setSentStatus(getSentStatus());
        message.setSenderUserId(getSenderUserId());
        message.setReceivedStatus(getReceivedStatus());
        message.setMessageDirection(getMessageDirection());
        message.setReceivedTime(getReceivedTime());
        message.setSentTime(getSentTime());
        message.setExtra(getExtra());

        return message;
    }

    public static UIMessage obtain(Message message) {
        UIMessage uiMessage = new UIMessage();

        uiMessage.setConversationType(message.getConversationType());
        uiMessage.setTargetId(message.getTargetId());
        uiMessage.setMessageId(message.getMessageId());
        uiMessage.setObjectName(message.getObjectName());
        uiMessage.setContent(message.getContent());
        uiMessage.setSentStatus(message.getSentStatus());
        uiMessage.setSenderUserId(message.getSenderUserId());
        uiMessage.setReceivedStatus(message.getReceivedStatus());
        uiMessage.setMessageDirection(message.getMessageDirection());
        uiMessage.setReceivedTime(message.getReceivedTime());
        uiMessage.setSentTime(message.getSentTime());
        uiMessage.setExtra(message.getExtra());
        uiMessage.setUserInfo(message.getContent().getUserInfo());

        return uiMessage;
    }

    public SpannableStringBuilder getTextMessageContent() {

        if(textMessageContent==null){

            if (getContent() instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) getContent();
                if(textMessage.getContent() != null) {
                    SpannableStringBuilder spannable = new SpannableStringBuilder(textMessage.getContent());
                    AndroidEmoji.ensure(spannable);
                    setTextMessageContent(spannable);
                }
            }
        }

        return textMessageContent;
    }

    public void setTextMessageContent(SpannableStringBuilder textMessageContent) {
        this.textMessageContent = textMessageContent;
    }


    public UserInfo getUserInfo() {
        return mUserInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        mUserInfo = userInfo;
    }

    public void setProgress(int progress) {
        mProgress = progress;
    }

    public int getProgress() {
        return mProgress;
    }
}
