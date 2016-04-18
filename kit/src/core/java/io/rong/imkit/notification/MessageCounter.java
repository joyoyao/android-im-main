package io.rong.imkit.notification;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.model.ConversationTypeFilter;
import io.rong.imkit.model.Event;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

/**
 * Created by DragonJ on 15/3/23.
 */
public class MessageCounter {

    RongContext mContext;

    List<Counter> mCounters;

    Handler mHandler;

    public MessageCounter(RongContext context) {
        mContext = context;
        mCounters = new ArrayList<>();

        mHandler = new Handler(Looper.getMainLooper());
        context.getEventBus().register(this);
    }

    public static class Counter {
        ConversationTypeFilter mFilter;
        int mCount;

        public Counter(ConversationTypeFilter filter) {
            mFilter = filter;
        }

        void onIncreased() {
            onMessageIncreased(++mCount);
        }

        public void onMessageIncreased(int count) {

        }

        public ConversationTypeFilter getFilter() {
            return mFilter;
        }

        boolean isCount(Message message) {
            return mFilter.hasFilter(message);
        }
    }


    public void registerMessageCounter(final Counter counter) {
        mCounters.add(counter);
        if (counter.getFilter().getLevel().equals(ConversationTypeFilter.Level.ALL)) {
            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                RongIM.getInstance().getRongIMClient().getTotalUnreadCount(new RongIMClient.ResultCallback<Integer>() {
                    int currentConversationMsgCount = 0;

                    @Override
                    public void onSuccess(Integer msgCount) {

                        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();

                        for (ConversationInfo conversationInfo : list) {
                            currentConversationMsgCount = currentConversationMsgCount + RongIM.getInstance().getRongIMClient().getUnreadCount(conversationInfo.getConversationType(), conversationInfo.getTargetId());
                        }

                        int totalCount = msgCount - currentConversationMsgCount;
                        counter.mCount = totalCount;
                        counter.onMessageIncreased(totalCount);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            } else {
                RLog.e(this, "registerMessageCounter", "RongIM.getInstance() is null");
            }
        } else if (counter.getFilter().getLevel().equals(ConversationTypeFilter.Level.CONVERSATION_TYPE)) {
            Conversation.ConversationType[] types;
            types = counter.getFilter().getConversationTypeList().toArray(new Conversation.ConversationType[counter.getFilter().getConversationTypeList().size()]);
            RLog.d(this, "registerMessageCounter", "RongIM.getInstance() :" + types.length);

            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                RongIM.getInstance().getRongIMClient().getUnreadCount(types, new RongIMClient.ResultCallback<Integer>() {

                    @Override
                    public void onSuccess(Integer msgCount) {
                        int currentConversationMsgCount = 0;
                        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();
                        for (ConversationInfo conversationInfo : list) {
                            currentConversationMsgCount = currentConversationMsgCount + RongIM.getInstance().getRongIMClient().getUnreadCount(conversationInfo.getConversationType(), conversationInfo.getTargetId());
                        }

                        int totalCount = msgCount - currentConversationMsgCount;
                        counter.mCount = totalCount;
                        counter.onMessageIncreased(totalCount);
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            } else {
                RLog.e(this, "registerMessageCounter", "RongIM.getInstance() is null");
            }
        }
    }

    public void unregisterMessageCounter(MessageCounter counter) {
        mCounters.remove(counter);
    }

    public void clearCache() {
        for (Counter messageCounter : mCounters) {
            messageCounter.mCount=0;
            messageCounter.onMessageIncreased(0);
        }
    }


    public void onEventBackgroundThread(Event.OnReceiveMessageEvent receiveMessageEvent) {
        Message message = receiveMessageEvent.getMessage();
        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();
        for (ConversationInfo conversationInfo : list) {
            if (message.getConversationType() == conversationInfo.getConversationType() && conversationInfo.getTargetId() != null && conversationInfo.getTargetId().equals(message.getTargetId())) {
                return;
            }
        }

        if (message.getContent() != null) {
            final MessageTag msgTag = ((Object) message.getContent()).getClass().getAnnotation(MessageTag.class);
            if (msgTag != null && (msgTag.flag() & MessageTag.ISCOUNTED) == MessageTag.ISCOUNTED) {
                for (final Counter counter : mCounters) {
                    if (counter.isCount(message)) {
                        if(receiveMessageEvent.getLeft() != 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    counter.onIncreased();
                                }
                            });
                        } else {
                            Conversation.ConversationType[] types;
                            types = counter.getFilter().getConversationTypeList().toArray(new Conversation.ConversationType[counter.getFilter().getConversationTypeList().size()]);
                            if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                                RongIM.getInstance().getRongIMClient().getUnreadCount(types, new RongIMClient.ResultCallback<Integer>() {

                                    @Override
                                    public void onSuccess(Integer msgCount) {
                                        int currentConversationMsgCount = 0;
                                        List<ConversationInfo> list = RongContext.getInstance().getCurrentConversationList();
                                        for (ConversationInfo conversationInfo : list) {
                                            currentConversationMsgCount = currentConversationMsgCount + RongIM.getInstance().getRongIMClient().getUnreadCount(conversationInfo.getConversationType(), conversationInfo.getTargetId());
                                        }

                                        int totalCount = msgCount - currentConversationMsgCount;
                                        counter.mCount = totalCount;
                                        counter.onMessageIncreased(totalCount);
                                    }

                                    @Override
                                    public void onError(RongIMClient.ErrorCode e) {

                                    }
                                });
                            } else {
                                RLog.e(this, "registerMessageCounter", "RongIM.getInstance() is null");
                            }
                        }
                    }
                }
            }
        }
    }

    public void onEvent(Event.ConversationRemoveEvent event) {
        mContext.getEventBus().post(new Event.ConversationUnreadEvent(event.getType(), event.getTargetId()));
    }

    public void onEvent(Event.ConversationUnreadEvent event) {
        for (final Counter counter : mCounters) {
            if (counter.getFilter().getLevel().equals(ConversationTypeFilter.Level.ALL)) {
                if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                    RongIM.getInstance().getRongIMClient().getUnreadCount(new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            counter.mCount = integer;
                            counter.onMessageIncreased(integer);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                } else {
                    RLog.e(this, "onEvent(Event.ConversationUnreadEvent)", "RongIM.getInstance() is null");
                }
            } else if (counter.getFilter().getLevel().equals(ConversationTypeFilter.Level.CONVERSATION_TYPE)) {
                Conversation.ConversationType[] types;
                types = counter.getFilter().getConversationTypeList().toArray(new Conversation.ConversationType[counter.getFilter().getConversationTypeList().size()]);
                if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                    RongIM.getInstance().getRongIMClient().getUnreadCount(types, new RongIMClient.ResultCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer integer) {
                            counter.mCount = integer;
                            counter.onMessageIncreased(integer);
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                } else {
                    RLog.e(this, "onEvent(Event.ConversationUnreadEvent)", "RongIM.getInstance() is null");
                }
            }
        }
    }
}
