package io.rong.imkit.model;

import android.content.Context;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.rong.imkit.RongIM;
import io.rong.imkit.util.AndroidEmoji;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.message.TextMessage;

public class EmojiMessageAdapter {
    private static EmojiMessageAdapter mLogic;

    public static void init(Context context) {
        mLogic = new EmojiMessageAdapter();
    }

    public static EmojiMessageAdapter getInstance() {
        return mLogic;
    }

    public void getHistoryMessages(Conversation.ConversationType conversationType, String targetId, int oldestMessageId, int count, final RongIMClient.ResultCallback<List<UIMessage>> callback) {

        RongIM.getInstance().getRongIMClient().getHistoryMessages(conversationType, targetId, oldestMessageId, count, new RongIMClient.ResultCallback<List<Message>>() {

            @Override
            public void onSuccess(List<Message> messages) {
                if (callback != null) {
                    callback.onSuccess(emojiMessageToUIMessage(messages));
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    public void getLatestMessages(Conversation.ConversationType conversationType, String targetId, int count, final RongIMClient.ResultCallback<List<UIMessage>> callback) {
        RongIM.getInstance().getRongIMClient().getLatestMessages(conversationType, targetId, count, new RongIMClient.ResultCallback<List<Message>>() {

            @Override
            public void onSuccess(List<Message> messages) {
                if (callback != null) {
                    if(messages != null && messages.size() > 0) {
                        Collections.reverse(messages);
                    }
                    callback.onSuccess(emojiMessageToUIMessage(messages));
                }
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    private final List<UIMessage> emojiMessageToUIMessage(List<Message> messages) {
        List<UIMessage> msgList = new ArrayList<>();
        if(messages==null || messages.size()==0){
            return msgList;
        }
        
        for (Message message : messages) {
            UIMessage uiMessage = UIMessage.obtain(message);
            if (message.getContent() instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message.getContent();
                if(textMessage.getContent() != null) {
                    SpannableStringBuilder spannable = new SpannableStringBuilder(textMessage.getContent());
                    AndroidEmoji.ensure(spannable);
                    uiMessage.setTextMessageContent(spannable);
                }
            }
            msgList.add(uiMessage);
        }
        return msgList;
    }
}