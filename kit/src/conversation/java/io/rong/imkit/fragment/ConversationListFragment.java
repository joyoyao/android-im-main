package io.rong.imkit.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongNotificationManager;
import io.rong.imkit.model.Draft;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.util.ConversationListUtils;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imkit.widget.adapter.ConversationListAdapter;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Conversation.ConversationType;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.VoiceMessage;

public class ConversationListFragment extends UriFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static String TAG = "ConvListFrag";
    private ConversationListAdapter mAdapter;
    private ListView mList;
    private TextView mNotificationBar;
    private boolean isShowWithoutConnected = false;  //进会话列表时是否已经connect
    private ArrayList<ConversationType> mSupportConversationList = new ArrayList<>();
    private ArrayList<Message> mMessageCache = new ArrayList<>();

    public static ConversationListFragment getInstance() {
        return new ConversationListFragment();
    }

    private RongIMClient.ResultCallback<List<Conversation>> mCallback = new RongIMClient.ResultCallback<List<Conversation>>() {
        @Override
        public void onSuccess(List<Conversation> conversations) {
            RLog.d(this, "ConversationListFragment", "initFragment onSuccess callback : list = " +
                    (conversations != null ? conversations.size() : "null"));

            if (mAdapter != null && mAdapter.getCount() != 0) {
                mAdapter.clear();
            }

            if (conversations == null || conversations.size() == 0) {
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();
                return;
            }

            makeUiConversationList(conversations);

            if (mList != null && mList.getAdapter() != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(RongIMClient.ErrorCode e) {
            RLog.d(this, "ConversationListFragment", "initFragment onError callback, e=" + e);
            if (e.equals(RongIMClient.ErrorCode.IPC_DISCONNECT)) {
                isShowWithoutConnected = true;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        RLog.d(this, "ConversationListFragment", "onCreate");
        super.onCreate(savedInstanceState);

        mSupportConversationList.clear();

        RongContext.getInstance().getEventBus().register(this);
    }

    @Override
    public void onAttach(Activity activity) {
        RLog.d(this, "ConversationListFragment", "onAttach");
        super.onAttach(activity);
    }

    /**
     * function: parse uri send from app
     */
    @Override
    protected void initFragment(Uri uri) {
        ConversationType[] conversationType = {ConversationType.PRIVATE, ConversationType.GROUP,
                ConversationType.DISCUSSION, ConversationType.SYSTEM,
                ConversationType.CUSTOMER_SERVICE, ConversationType.CHATROOM,
                ConversationType.PUBLIC_SERVICE, ConversationType.APP_PUBLIC_SERVICE};

        RLog.d(this, "ConversationListFragment", "initFragment");

        if (uri == null) {
            RongIM.getInstance().getRongIMClient().getConversationList(mCallback);
            return;
        }

        for (ConversationType type : conversationType) {
            if (uri.getQueryParameter(type.getName()) != null) {
                mSupportConversationList.add(type);

                if ("true".equals(uri.getQueryParameter(type.getName()))) {
                    RongContext.getInstance().setConversationGatherState(type.getName(), true);
                } else if ("false".equals(uri.getQueryParameter(type.getName()))) {
                    RongContext.getInstance().setConversationGatherState(type.getName(), false);
                }
            }
        }

        if (RongIM.getInstance() == null || RongIM.getInstance().getRongIMClient() == null) {
            Log.d("ConversationListFr", "RongCloud haven't been connected yet, so the conversation list display blank !!!");
            isShowWithoutConnected = true;
            return;
        }

        if (mSupportConversationList.size() > 0)
            RongIM.getInstance().getRongIMClient().getConversationList(mCallback, mSupportConversationList.toArray(new ConversationType[mSupportConversationList.size()]));
        else
            RongIM.getInstance().getRongIMClient().getConversationList(mCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RLog.d(this, "ConversationListFragment", "onCreateView");
        View view = inflater.inflate(R.layout.rc_fr_conversationlist, container, false);
        mNotificationBar = findViewById(view, R.id.rc_status_bar);
        mNotificationBar.setVisibility(View.GONE);
        mList = findViewById(view, R.id.rc_list);

        TextView mEmptyView = findViewById(view, android.R.id.empty);
        if (RongIM.getInstance() == null || RongIM.getInstance().getRongIMClient() == null) {
            mEmptyView.setText(RongContext.getInstance().getResources().getString(R.string.rc_conversation_list_not_connected));
        } else {
            mEmptyView.setText(RongContext.getInstance().getResources().getString(R.string.rc_conversation_list_empty_prompt));
        }

        mList.setEmptyView(mEmptyView);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (mAdapter == null)
            mAdapter = new ConversationListAdapter(RongContext.getInstance());
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (RongIM.getInstance() == null || RongIM.getInstance().getRongIMClient() == null) {
            Log.d("ConversationListFr", "RongCloud haven't been connected yet, so the conversation list display blank !!!");
            isShowWithoutConnected = true;
            return;
        }
        RLog.d(ConversationListFragment.this, "onResume", "current connect status is:" + RongIM.getInstance().getRongIMClient().getCurrentConnectionStatus());
        RongNotificationManager.getInstance().onRemoveNotification();

        RongIMClient.ConnectionStatusListener.ConnectionStatus status = RongIM.getInstance().getRongIMClient().getCurrentConnectionStatus();

        Drawable drawable = getActivity().getResources().getDrawable(R.drawable.rc_notification_network_available);
        int width = (int) getActivity().getResources().getDimension(R.dimen.rc_message_send_status_image_size);
        drawable.setBounds(0, 0, width, width);
        mNotificationBar.setCompoundDrawablePadding(16);
        mNotificationBar.setCompoundDrawables(drawable, null, null, null);

        if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE)) {
            mNotificationBar.setVisibility(View.VISIBLE);
            mNotificationBar.setText(getResources().getString(R.string.rc_notice_network_unavailable));
            RongIM.getInstance().getRongIMClient().reconnect(null);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT)) {
            mNotificationBar.setVisibility(View.VISIBLE);
            mNotificationBar.setText(getResources().getString(R.string.rc_notice_tick));
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
            mNotificationBar.setVisibility(View.GONE);
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.DISCONNECTED)) {
            mNotificationBar.setVisibility(View.VISIBLE);
            mNotificationBar.setText(getResources().getString(R.string.rc_notice_network_unavailable));
        } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING)) {
            mNotificationBar.setVisibility(View.VISIBLE);
            mNotificationBar.setText(getResources().getString(R.string.rc_notice_connecting));
        }
    }

    @Override
    public void onDestroy() {
        RLog.d(this, "ConversationListFragment", "onDestroy");
        RongContext.getInstance().getEventBus().unregister(this);
        getHandler().removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        RLog.d(this, "ConversationListFragment", "onPause");
        super.onPause();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    public void setAdapter(ConversationListAdapter adapter) {
        if (mAdapter != null)
            mAdapter.clear();
        mAdapter = adapter;
        if (mList != null && getUri() != null) {
            mList.setAdapter(adapter);
            initFragment(getUri());
        }
    }

    public ConversationListAdapter getAdapter() {
        return mAdapter;
    }

    public void onEventMainThread(Event.ConnectEvent event) {
        RLog.d(this, "onEventMainThread", "Event.ConnectEvent: isListRetrieved = " + isShowWithoutConnected);

        if (isShowWithoutConnected) {
            if (mSupportConversationList.size() > 0)
                RongIM.getInstance().getRongIMClient().getConversationList(mCallback, mSupportConversationList.toArray(new ConversationType[mSupportConversationList.size()]));
            else
                RongIM.getInstance().getRongIMClient().getConversationList(mCallback);

            TextView mEmptyView = (TextView) mList.getEmptyView();
            mEmptyView.setText(RongContext.getInstance().getResources().getString(R.string.rc_conversation_list_empty_prompt));
        } else {
            return;
        }
        isShowWithoutConnected = false;
    }

    public void onEventMainThread(final Event.ReadReceiptEvent event) {
        if (mAdapter == null) {
            Log.d(TAG, "the conversation list adapter is null.");
            return;
        }

        final int originalIndex = mAdapter.findPosition(event.getMessage().getConversationType(), event.getMessage().getTargetId());
        boolean gatherState = RongContext.getInstance().getConversationGatherState(event.getMessage().getConversationType().getName());

        if (!gatherState) {
            if (originalIndex >= 0) {
                UIConversation conversation = mAdapter.getItem(originalIndex);
                ReadReceiptMessage content = (ReadReceiptMessage) event.getMessage().getContent();
                if (content.getLastMessageSendTime() >= conversation.getUIConversationTime()
                        && conversation.getConversationSenderId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                    conversation.setSentStatus(Message.SentStatus.READ);
                    mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition()), mList);
                }
            }
        }
    }

    //收到新消息。
    public void onEventMainThread(final Event.OnReceiveMessageEvent event) {
        Log.d(TAG, "Receive MessageEvent: id=" + event.getMessage().getTargetId() +
                ", type=" + event.getMessage().getConversationType());

        if ((mSupportConversationList.size() != 0 && !mSupportConversationList.contains(event.getMessage().getConversationType()))
                || (mSupportConversationList.size() == 0 && (event.getMessage().getConversationType() == ConversationType.CHATROOM
                || event.getMessage().getConversationType() == ConversationType.CUSTOMER_SERVICE))) {
            Log.e(TAG, "Not included in conversation list. Return directly!");
            return;
        }

        if (mAdapter == null) {
            Log.d(TAG, "the conversation list adapter is null. Cache the received message firstly!!!");
            mMessageCache.add(event.getMessage());
            return;
        }

        int originalIndex = mAdapter.findPosition(event.getMessage().getConversationType(), event.getMessage().getTargetId());
        UIConversation uiConversation = makeUiConversation(event.getMessage(), originalIndex);
        final int newPosition = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
        if (originalIndex < 0) {
            mAdapter.add(uiConversation, newPosition);
        } else if (originalIndex != newPosition) {
            mAdapter.remove(originalIndex);
            mAdapter.add(uiConversation, newPosition);
        }
        mAdapter.notifyDataSetChanged();

        //异步刷新未读消息数
        MessageTag msgTag = event.getMessage().getContent().getClass().getAnnotation(MessageTag.class);
        if (msgTag != null && (msgTag.flag() & MessageTag.ISCOUNTED) == MessageTag.ISCOUNTED) {
            refreshUnreadCount(event.getMessage().getConversationType(), event.getMessage().getTargetId());
        }
        // 如果是聚合状态，外层会话的草稿信息需要更新为最新那条子会话的信息。
        if (RongContext.getInstance().getConversationGatherState(event.getMessage().getConversationType().getName())) {
            RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                @Override
                public void onSuccess(List<Conversation> conversations) {
                    for (Conversation conv : conversations) {
                        if (conversations == null || conversations.size() == 0)
                            return;
                        if (conv.getConversationType().equals(event.getMessage().getConversationType()) && conv.getTargetId().equals(event.getMessage().getTargetId())) {
                            int pos = mAdapter.findPosition(conv.getConversationType(), conv.getTargetId());
                            if (pos >= 0) {
                                mAdapter.getItem(pos).setDraft(conv.getDraft());
                                if (TextUtils.isEmpty(conv.getDraft()))
                                    mAdapter.getItem(pos).setSentStatus(null);
                                else
                                    mAdapter.getItem(pos).setSentStatus(conv.getSentStatus());
                                mAdapter.getView(pos, mList.getChildAt(pos - mList.getFirstVisiblePosition()), mList);
                            }
                            break;
                        }
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            }, event.getMessage().getConversationType());
        }
    }

    public void onEventMainThread(Message message) {
        RLog.d(this, "onEventMainThread", "Receive Message: name=" + message.getObjectName() +
                ", type=" + message.getConversationType());

        if ((mSupportConversationList.size() != 0 && !mSupportConversationList.contains(message.getConversationType()))
                || (mSupportConversationList.size() == 0 && (message.getConversationType() == ConversationType.CHATROOM
                || message.getConversationType() == ConversationType.CUSTOMER_SERVICE))) {
            RLog.d(this, "onEventBackgroundThread", "Not included in conversation list. Return directly!");
            return;
        }

        if (mAdapter == null) {
            RLog.d(this, "onEventMainThread(Message)", "the conversation list adapter is null. Cache the received message firstly!!!");
            mMessageCache.add(message);
            return;
        }

        int originalIndex = mAdapter.findPosition(message.getConversationType(), message.getTargetId());

        UIConversation uiConversation = makeUiConversation(message, originalIndex);

        int newPosition = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);

        if (originalIndex >= 0) {
            if (newPosition == originalIndex) {
                mAdapter.getView(newPosition, mList.getChildAt(newPosition - mList.getFirstVisiblePosition()), mList);
            } else {
                mAdapter.remove(originalIndex);
                mAdapter.add(uiConversation, newPosition);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            mAdapter.add(uiConversation, newPosition);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(MessageContent content) {
        RLog.d(ConversationListFragment.this, "onEventMainThread:", "MessageContent");

        for (int index = 0; index < mAdapter.getCount(); index++) {
            UIConversation tempUIConversation = mAdapter.getItem(index);

            if (content != null && tempUIConversation.getMessageContent() != null && tempUIConversation.getMessageContent() == content) {

                tempUIConversation.setMessageContent(content);
                tempUIConversation.setConversationContent(tempUIConversation.buildConversationContent(tempUIConversation));

                if (index >= mList.getFirstVisiblePosition())
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition()), mList);
            } else {
                RLog.e(this, "onEventMainThread", "MessageContent is null");
            }
        }
    }

    public void onEventMainThread(final RongIMClient.ConnectionStatusListener.ConnectionStatus status) {

        RLog.d(this, "ConnectionStatus", status.toString());
        if (isResumed()) {
            Drawable drawable = getActivity().getResources().getDrawable(R.drawable.rc_notification_network_available);
            int width = (int) getActivity().getResources().getDimension(R.dimen.rc_message_send_status_image_size);
            drawable.setBounds(0, 0, width, width);
            mNotificationBar.setCompoundDrawablePadding(16);
            mNotificationBar.setCompoundDrawables(drawable, null, null, null);

            if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.NETWORK_UNAVAILABLE)) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.setText(getResources().getString(R.string.rc_notice_network_unavailable));
                RongIM.getInstance().getRongIMClient().reconnect(null);
            } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT)) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.setText(getResources().getString(R.string.rc_notice_tick));
            } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED)) {
                mNotificationBar.setVisibility(View.GONE);
            } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.DISCONNECTED)) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.setText(getResources().getString(R.string.rc_notice_network_unavailable));
            } else if (status.equals(RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTING)) {
                mNotificationBar.setVisibility(View.VISIBLE);
                mNotificationBar.setText(getResources().getString(R.string.rc_notice_connecting));
            }
        }
    }


    public void onEventMainThread(Event.CreateDiscussionEvent createDiscussionEvent) {
        RLog.d(ConversationListFragment.this, "onEventBackgroundThread:", "createDiscussionEvent");
        UIConversation conversation = new UIConversation();
        conversation.setConversationType(ConversationType.DISCUSSION);
        if (createDiscussionEvent.getDiscussionName() != null)
            conversation.setUIConversationTitle(createDiscussionEvent.getDiscussionName());
        else
            conversation.setUIConversationTitle("");

        conversation.setConversationTargetId(createDiscussionEvent.getDiscussionId());
        conversation.setUIConversationTime(System.currentTimeMillis());

        boolean isGather = RongContext.getInstance().getConversationGatherState(ConversationType.DISCUSSION.getName());
        conversation.setConversationGatherState(isGather);

        //如果是聚合显示，更新为聚合显示时的标题和内容
        if (isGather) {
            String name = RongContext.getInstance().getGatheredConversationTitle(conversation.getConversationType());
            conversation.setUIConversationTitle(name);
        }

        int gatherPosition = mAdapter.findGatherPosition(ConversationType.DISCUSSION);

        if (gatherPosition == -1) {
            mAdapter.add(conversation, ConversationListUtils.findPositionForNewConversation(conversation, mAdapter));
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Draft draft) {
        ConversationType curType = ConversationType.setValue(draft.getType());
        if (curType == null) {
            throw new IllegalArgumentException("the type of the draft is unknown!");
        }
        RLog.i(ConversationListFragment.this, "onEventMainThread(draft)", curType.getName());
        int position = mAdapter.findPosition(curType, draft.getId());
        if (position >= 0) {
            UIConversation conversation = mAdapter.getItem(position);
            if (conversation.getConversationTargetId().equals(draft.getId())) {
                conversation.setDraft(draft.getContent());
                if (!TextUtils.isEmpty(draft.getContent()))
                    conversation.setSentStatus(null);
                mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    /* function: 异步获取到群组信息后，在此进行相应刷新*/
    public void onEventMainThread(Group groupInfo) {
        int count = mAdapter.getCount();
        RLog.d(ConversationListFragment.this, "onEventMainThread", "Group: name=" + groupInfo.getName() + ", id=" + groupInfo.getId());
        if (groupInfo.getName() == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            UIConversation item = mAdapter.getItem(i);
            if (item != null && item.getConversationType().equals(ConversationType.GROUP) &&
                    item.getConversationTargetId().equals(groupInfo.getId())) {
                boolean gatherState = RongContext.getInstance().getConversationGatherState(item.getConversationType().getName());
                if (gatherState) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    Spannable messageData = RongContext.getInstance()
                            .getMessageTemplate(item.getMessageContent().getClass())
                            .getContentSummary(item.getMessageContent());
                    if (item.getMessageContent() instanceof VoiceMessage) {
                        boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(item.getConversationType(), item.getConversationTargetId())
                                .getReceivedStatus().isListened();
                        if (isListened) {
                            messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                    builder.append(groupInfo.getName()).append(" : ").append(messageData);
                    item.setConversationContent(builder);
                    if (groupInfo.getPortraitUri() != null)
                        item.setIconUrl(groupInfo.getPortraitUri());
                } else {
                    item.setUIConversationTitle(groupInfo.getName());
                    if (groupInfo.getPortraitUri() != null)
                        item.setIconUrl(groupInfo.getPortraitUri());
                }
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    public void onEventMainThread(Discussion discussion) {
        int count = mAdapter.getCount();
        RLog.d(ConversationListFragment.this, "onEventMainThread", "Discussion: name=" + discussion.getName() + ", id=" + discussion.getId());

        for (int i = 0; i < count; i++) {
            UIConversation item = mAdapter.getItem(i);
            if (item != null && item.getConversationType().equals(ConversationType.DISCUSSION) &&
                    item.getConversationTargetId().equals(discussion.getId())) {
                boolean gatherState = RongContext.getInstance().getConversationGatherState(item.getConversationType().getName());
                if (gatherState) {
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    Spannable messageData = RongContext.getInstance()
                            .getMessageTemplate(item.getMessageContent().getClass())
                            .getContentSummary(item.getMessageContent());

                    if (messageData != null) {
                        if (item.getMessageContent() instanceof VoiceMessage) {
                            boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(item.getConversationType(), item.getConversationTargetId())
                                    .getReceivedStatus().isListened();
                            if (isListened) {
                                messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else {
                                messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                        builder.append(discussion.getName()).append(" : ").append(messageData);
                    } else {
                        builder.append(discussion.getName());
                    }

                    item.setConversationContent(builder);
                } else {
                    item.setUIConversationTitle(discussion.getName());
                }
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    public void onEventMainThread(Event.GroupUserInfoEvent event) {
        int count = mAdapter.getCount();
        boolean isShowName;
        GroupUserInfo userInfo = event.getUserInfo();
        Log.d("qinxiao", "GroupUserInfoEvent: " + userInfo.getUserId());
        if (userInfo.getNickname() == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            ConversationType type = uiConversation.getConversationType();
            boolean gatherState = RongContext.getInstance().getConversationGatherState(uiConversation.getConversationType().getName());

            if (uiConversation.getMessageContent() == null) {
                isShowName = false;
            } else {
                isShowName = RongContext.getInstance().getMessageProviderTag(uiConversation.getMessageContent().getClass()).showSummaryWithName();
            }
            //群组或讨论组非聚合显示情况，需要比较该会话的senderId.因为此时的targetId为群组或讨论组名字。
            if (!gatherState && isShowName && (type.equals(ConversationType.GROUP))
                    && (uiConversation.getConversationSenderId().equals(userInfo.getUserId()))) {
                Spannable messageData = RongContext.getInstance().getMessageTemplate(uiConversation.getMessageContent().getClass()).getContentSummary(uiConversation.getMessageContent());
                SpannableStringBuilder builder = new SpannableStringBuilder();
                if (uiConversation.getMessageContent() instanceof VoiceMessage) {
                    boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(uiConversation.getConversationType(), uiConversation.getConversationTargetId())
                            .getReceivedStatus().isListened();
                    if (isListened) {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                uiConversation.addNickname(userInfo.getUserId());
                builder.append(userInfo.getNickname()).append(" : ").append(messageData);
                uiConversation.setConversationContent(builder);
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    /* function: 异步获取到用户信息后，在此进行相应刷新*/
    public void onEventMainThread(UserInfo userInfo) {
        int count = mAdapter.getCount();
        boolean isShowName;

        if (userInfo.getName() == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            UIConversation temp = mAdapter.getItem(i);
            String type = temp.getConversationType().getName();
            boolean gatherState = RongContext.getInstance().getConversationGatherState(temp.getConversationType().getName());

            if (temp.hasNickname(userInfo.getUserId()))
                continue;

            if (temp.getMessageContent() == null) {
                isShowName = false;
            } else {
                isShowName = RongContext.getInstance().getMessageProviderTag(temp.getMessageContent().getClass()).showSummaryWithName();
            }
            //群组或讨论组非聚合显示情况，需要比较该会话的senderId.因为此时的targetId为群组或讨论组名字。
            if (!gatherState && isShowName && (type.equals("group") || type.equals("discussion"))
                    && (temp.getConversationSenderId().equals(userInfo.getUserId()))) {
                Spannable messageData = RongContext.getInstance().getMessageTemplate(temp.getMessageContent().getClass()).getContentSummary(temp.getMessageContent());
                SpannableStringBuilder builder = new SpannableStringBuilder();
                if (temp.getMessageContent() instanceof VoiceMessage) {
                    boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(temp.getConversationType(), temp.getConversationTargetId())
                            .getReceivedStatus().isListened();
                    if (isListened) {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                builder.append(userInfo.getName()).append(" : ").append(messageData);
                temp.setConversationContent(builder);
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            } else if (temp.getConversationTargetId().equals(userInfo.getUserId())) {
                if (!gatherState && (type == "private" || type == "system")) {
                    temp.setUIConversationTitle(userInfo.getName());
                    temp.setIconUrl(userInfo.getPortraitUri());
                } else if (isShowName) {
                    Spannable messageData = RongContext.getInstance().getMessageTemplate(temp.getMessageContent().getClass()).getContentSummary(temp.getMessageContent());
                    SpannableStringBuilder builder = new SpannableStringBuilder();
                    if (messageData != null) {
                        if (temp.getMessageContent() instanceof VoiceMessage) {
                            boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(temp.getConversationType(), temp.getConversationTargetId())
                                    .getReceivedStatus().isListened();
                            if (isListened) {
                                messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else {
                                messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }

                        builder.append(userInfo.getName()).append(" : ").append(messageData);
                    } else {
                        builder.append(userInfo.getName());
                    }
                    temp.setConversationContent(builder);
                    temp.setIconUrl(userInfo.getPortraitUri());
                }
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    public void onEventMainThread(PublicServiceProfile accountInfo) {
        int count = mAdapter.getCount();
        boolean gatherState = RongContext.getInstance().getConversationGatherState(accountInfo.getConversationType().getName());
        for (int i = 0; i < count; i++) {
            if (mAdapter.getItem(i).getConversationType().equals(accountInfo.getConversationType())
                    && mAdapter.getItem(i).getConversationTargetId().equals(accountInfo.getTargetId())
                    && !gatherState) {
                mAdapter.getItem(i).setUIConversationTitle(accountInfo.getName());
                mAdapter.getItem(i).setIconUrl(accountInfo.getPortraitUri());
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
                break;
            }
        }

    }

    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
        if (event != null) {
            if (event.isFollow() == false) {
                int originalIndex = mAdapter.findPosition(event.getConversationType(), event.getTargetId());
                if (originalIndex >= 0) {
                    mAdapter.remove(originalIndex);
                    mAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    public void onEventMainThread(final Event.ConversationUnreadEvent unreadEvent) {
        final int targetIndex = mAdapter.findPosition(unreadEvent.getType(), unreadEvent.getTargetId());
        RLog.d(ConversationListFragment.this, "onEventMainThread", "ConversationUnreadEvent: name=");
        if (targetIndex >= 0) {
            final UIConversation temp = mAdapter.getItem(targetIndex);
            boolean gatherState = temp.getConversationGatherState();
            if (gatherState) {
                RongIM.getInstance().getRongIMClient().getUnreadCount(new RongIMClient.ResultCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        int pos = mAdapter.findPosition(unreadEvent.getType(), unreadEvent.getTargetId());
                        if (pos >= 0) {
                            mAdapter.getItem(pos).setUnReadMessageCount(count);
                            mAdapter.getView(pos, mList.getChildAt(pos - mList.getFirstVisiblePosition()), mList);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        System.err.print("Throw exception when get unread message count from ipc remote side!");
                    }
                }, unreadEvent.getType());
            } else {
                temp.setUnReadMessageCount(0);
                RLog.d(ConversationListFragment.this, "onEventMainThread", "ConversationUnreadEvent: set unRead count to be 0");
                mAdapter.getView(targetIndex, mList.getChildAt(targetIndex - mList.getFirstVisiblePosition()), mList);
            }
        }

    }

    public void onEventMainThread(final Event.ConversationTopEvent setTopEvent) throws IllegalAccessException {
        final int originalIndex = mAdapter.findPosition(setTopEvent.getConversationType(), setTopEvent.getTargetId());
        if (originalIndex >= 0) {
            UIConversation temp = mAdapter.getItem(originalIndex);
            boolean originalValue = temp.isTop();
            if (originalValue == setTopEvent.isTop())
                return;

            if (temp.getConversationGatherState()) {
                RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (conversations == null || conversations.size() == 0)
                            return;
                        UIConversation newConversation = makeUIConversationFromList(conversations);
                        int pos = mAdapter.findPosition(setTopEvent.getConversationType(), setTopEvent.getTargetId());
                        if (pos >= 0)
                            mAdapter.remove(pos);
                        int newIndex = ConversationListUtils.findPositionForNewConversation(newConversation, mAdapter);
                        mAdapter.add(newConversation, newIndex);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                }, temp.getConversationType());
            } else {
                int newIndex;
                if (originalValue == true) {
                    temp.setTop(false);
                    newIndex = ConversationListUtils.findPositionForCancleTop(originalIndex, mAdapter);
                } else {
                    temp.setTop(true);
                    newIndex = ConversationListUtils.findPositionForSetTop(temp, mAdapter);
                }

                if (originalIndex == newIndex)
                    mAdapter.getView(newIndex, mList.getChildAt(newIndex - mList.getFirstVisiblePosition()), mList);
                else {
                    mAdapter.remove(originalIndex);
                    mAdapter.add(temp, newIndex);
                    mAdapter.notifyDataSetChanged();
                }
            }
        } else {
            throw new IllegalAccessException("the item has already been deleted!");
        }
    }


    public void onEventMainThread(final Event.ConversationRemoveEvent removeEvent) {
        final int removedIndex = mAdapter.findPosition(removeEvent.getType(), removeEvent.getTargetId());

        boolean gatherState = RongContext.getInstance().getConversationGatherState(removeEvent.getType().getName());

        if (!gatherState) {
            if (removedIndex >= 0) {
                mAdapter.remove(removedIndex);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            if (removedIndex >= 0) {
                RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversationList) {
                        int oldPos = mAdapter.findPosition(removeEvent.getType(), removeEvent.getTargetId());
                        if (conversationList == null || conversationList.size() == 0) {
                            if (oldPos >= 0)
                                mAdapter.remove(oldPos);
                            mAdapter.notifyDataSetChanged();
                            return;
                        }

                        UIConversation newConversation = makeUIConversationFromList(conversationList);
                        if (oldPos >= 0) {
                            mAdapter.remove(oldPos);
                        }
                        int newIndex = ConversationListUtils.findPositionForNewConversation(newConversation, mAdapter);
                        mAdapter.add(newConversation, newIndex);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                }, removeEvent.getType());
            }
        }
    }

    public void onEventMainThread(Event.MessageDeleteEvent event) {
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (event.getMessageIds().contains(mAdapter.getItem(i).getLatestMessageId())) {
                final boolean gatherState = mAdapter.getItem(i).getConversationGatherState();
                final int index = i;
                if (gatherState) {
                    RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                        @Override
                        public void onSuccess(List<Conversation> conversationList) {
                            if (conversationList == null || conversationList.size() == 0)
                                return;
                            UIConversation uiConversation = makeUIConversationFromList(conversationList);
                            int oldPos = mAdapter.findPosition(uiConversation.getConversationType(), uiConversation.getConversationTargetId());
                            if (oldPos >= 0) {
                                mAdapter.remove(oldPos);
                            }
                            int newIndex = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
                            mAdapter.add(uiConversation, newIndex);
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    }, mAdapter.getItem(index).getConversationType());

                } else {
                    RongIM.getInstance().getRongIMClient().getConversation(mAdapter.getItem(index).getConversationType(), mAdapter.getItem(index).getConversationTargetId(),
                            new RongIMClient.ResultCallback<Conversation>() {
                                @Override
                                public void onSuccess(Conversation conversation) {
                                    if (conversation == null) {
                                        RLog.d(this, "onEventMainThread", "getConversation : onSuccess, conversation = null");
                                        return;
                                    }
                                    UIConversation temp = UIConversation.obtain(conversation, false);

                                    int pos = mAdapter.findPosition(conversation.getConversationType(), conversation.getTargetId());
                                    if (pos >= 0) {
                                        mAdapter.remove(pos);
                                    }
                                    int newPosition = ConversationListUtils.findPositionForNewConversation(temp, mAdapter);
                                    mAdapter.add(temp, newPosition);
                                    mAdapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {
                                }
                            });
                }
                break;
            }
        }
    }

    public void onEventMainThread(Event.ConversationNotificationEvent notificationEvent) {
        final int originalIndex = mAdapter.findPosition(notificationEvent.getConversationType(), notificationEvent.getTargetId());

        if (originalIndex >= 0) {
            mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition()), mList);
        }
    }


    public void onEventMainThread(Event.MessagesClearEvent clearMessagesEvent) {
        final int originalIndex = mAdapter.findPosition(clearMessagesEvent.getType(), clearMessagesEvent.getTargetId());

        if (originalIndex >= 0) {
            boolean gatherState = RongContext.getInstance().getConversationGatherState(clearMessagesEvent.getType().getName());
            if (gatherState) {
                RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversationList) {
                        if (conversationList == null || conversationList.size() == 0)
                            return;
                        UIConversation uiConversation = makeUIConversationFromList(conversationList);
                        int pos = mAdapter.findPosition(uiConversation.getConversationType(), uiConversation.getConversationTargetId());
                        if (pos >= 0) {
                            mAdapter.remove(pos);
                        }
                        int newIndex = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
                        mAdapter.add(uiConversation, newIndex);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                }, ConversationType.GROUP);
            } else {
                RongIMClient.getInstance().getConversation(clearMessagesEvent.getType(), clearMessagesEvent.getTargetId(), new RongIMClient.ResultCallback<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        UIConversation uiConversation = UIConversation.obtain(conversation, false);
                        mAdapter.remove(originalIndex);
                        int newPos = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
                        mAdapter.add(uiConversation, newPos);
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            }
        }
    }

    public void onEventMainThread(Event.OnMessageSendErrorEvent sendErrorEvent) {
        int index = mAdapter.findPosition(sendErrorEvent.getMessage().getConversationType(), sendErrorEvent.getMessage().getTargetId());

        if (index >= 0) {
            UIConversation temp = mAdapter.getItem(index);
            temp.setUIConversationTime(sendErrorEvent.getMessage().getSentTime());
            temp.setMessageContent(sendErrorEvent.getMessage().getContent());
            temp.setConversationContent(temp.buildConversationContent(temp));
            temp.setSentStatus(Message.SentStatus.FAILED);
            mAdapter.remove(index);
            int newPosition = ConversationListUtils.findPositionForNewConversation(temp, mAdapter);
            mAdapter.add(temp, newPosition);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.QuitDiscussionEvent event) {
        int index = mAdapter.findPosition(ConversationType.DISCUSSION, event.getDiscussionId());

        if (index >= 0) {
            mAdapter.remove(index);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.QuitGroupEvent event) {
        final int index = mAdapter.findPosition(ConversationType.GROUP, event.getGroupId());
        boolean gatherState = RongContext.getInstance().getConversationGatherState(ConversationType.GROUP.getName());

        if (index >= 0 && gatherState) {
            RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                @Override
                public void onSuccess(List<Conversation> conversationList) {
                    if (conversationList == null || conversationList.size() == 0){
                        if (index >= 0)
                            mAdapter.remove(index);
                        mAdapter.notifyDataSetChanged();
                        return;
                    }
                    UIConversation uiConversation = makeUIConversationFromList(conversationList);
                    int pos = mAdapter.findPosition(uiConversation.getConversationType(), uiConversation.getConversationTargetId());
                    if (pos >= 0) {
                        mAdapter.remove(pos);
                    }
                    int newIndex = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
                    mAdapter.add(uiConversation, newIndex);
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            }, ConversationType.GROUP);
        } else {
            if (index >= 0) {
                mAdapter.remove(index);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void onEventMainThread(Event.MessageListenedEvent event) {
        final int originalIndex = mAdapter.findPosition(event.getConversationType(), event.getTargetId());

        if (originalIndex >= 0) {
            UIConversation temp = mAdapter.getItem(originalIndex);
            if (temp.getLatestMessageId() == event.getLatestMessageId()) {
                temp.setConversationContent(temp.buildConversationContent(temp));
            }
            mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition()), mList);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        UIConversation uiconversation = mAdapter.getItem(position);
        ConversationType type = uiconversation.getConversationType();
        if (RongContext.getInstance().getConversationGatherState(type.getName())) {
            RongIM.getInstance().startSubConversationList(getActivity(), type);
        } else {
            if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
                boolean isDefault = RongContext.getInstance().getConversationListBehaviorListener().onConversationClick(getActivity(), view, uiconversation);
                if (isDefault == true)
                    return;
            }
            uiconversation.setUnReadMessageCount(0);
            RongIM.getInstance().startConversation(getActivity(), type, uiconversation.getConversationTargetId(), uiconversation.getUIConversationTitle());
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        UIConversation uiConversation = mAdapter.getItem(position);
        String type = uiConversation.getConversationType().getName();

        if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
            boolean isDealt = RongContext.getInstance().getConversationListBehaviorListener().onConversationLongClick(getActivity(), view, uiConversation);
            if (isDealt)
                return true;
        }
        if (RongContext.getInstance().getConversationGatherState(type) == false) {
            buildMultiDialog(uiConversation);
            return true;
        } else {
            buildSingleDialog(uiConversation);
            return true;
        }
    }

    private void buildMultiDialog(final UIConversation uiConversation) {

        String[] items = new String[2];

        if (uiConversation.isTop())
            items[0] = RongContext.getInstance().getString(R.string.rc_conversation_list_dialog_cancel_top);
        else
            items[0] = RongContext.getInstance().getString(R.string.rc_conversation_list_dialog_set_top);

        items[1] = RongContext.getInstance().getString(R.string.rc_conversation_list_dialog_remove);

        ArraysDialogFragment.newInstance(uiConversation.getUIConversationTitle(), items).setArraysDialogItemListener(new ArraysDialogFragment.OnArraysDialogItemListener() {
            @Override
            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    RongIM.getInstance().getRongIMClient().
                            setConversationToTop(uiConversation.getConversationType()
                                    , uiConversation.getConversationTargetId(), !uiConversation.isTop(), new RongIMClient.ResultCallback<Boolean>() {
                                @Override
                                public void onSuccess(Boolean aBoolean) {
                                    if (uiConversation.isTop() == true) {
                                        Toast.makeText(RongContext.getInstance(), getString(R.string.rc_conversation_list_popup_cancel_top), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(RongContext.getInstance(), getString(R.string.rc_conversation_list_dialog_set_top), Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(RongIMClient.ErrorCode e) {

                                }
                            });
                } else if (which == 1) {
                    RongIM.getInstance().getRongIMClient().removeConversation(uiConversation.getConversationType()
                            , uiConversation.getConversationTargetId());

                }
            }
        }).show(getFragmentManager());

    }

    private void buildSingleDialog(final UIConversation uiConversation) {

        String[] items = new String[1];
        items[0] = RongContext.getInstance().getString(R.string.rc_conversation_list_dialog_remove);

        ArraysDialogFragment.newInstance(uiConversation.getUIConversationTitle(), items).setArraysDialogItemListener(new ArraysDialogFragment.OnArraysDialogItemListener() {

            @Override
            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {

                RongIM.getInstance().getRongIMClient().getConversationList(new RongIMClient.ResultCallback<List<Conversation>>() {
                    @Override
                    public void onSuccess(List<Conversation> conversations) {
                        if (conversations == null || conversations.size() == 0)
                            return;
                        for (Conversation conversation : conversations) {
                            RongIM.getInstance().getRongIMClient().removeConversation(conversation.getConversationType(), conversation.getTargetId());
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode errorCode) {
                    }

                }, uiConversation.getConversationType());

            }

        }).show(getFragmentManager());

    }

    /**
     * 根据从本地数据库拉取到的conversation列表，构造UIConversation列表
     */
    private void makeUiConversationList(List<Conversation> conversationList) {
        UIConversation uiCon;

        //adapter 没准备好之前收到的消息，缓存在mMessageCache里，这里需要把它们从缓存里读出来放到adapter里。
        if (mMessageCache.size() != 0) {
            for (int i = 0; i < mMessageCache.size(); i++) {
                Message message = mMessageCache.get(i);
                int originalIndex = mAdapter.findPosition(message.getConversationType(), message.getTargetId());
                UIConversation uiConversation = makeUiConversation(message, originalIndex);

                int newPosition = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);

                if (originalIndex >= 0) {
                    mAdapter.remove(originalIndex);
                }
                mAdapter.add(uiConversation, newPosition);
            }
            mMessageCache.clear();
        }
        //获取到的conversationList排序规律：首先是top会话，按时间顺序排列。然后非top会话也是按时间排列。
        for (Conversation conversation : conversationList) {
            ConversationType conversationType = conversation.getConversationType();
            boolean gatherState = RongContext.getInstance().getConversationGatherState(conversationType.getName());
            int originalIndex = mAdapter.findPosition(conversationType, conversation.getTargetId());//判断该条会话是否已经在会话列表里建立。

            uiCon = UIConversation.obtain(conversation, gatherState);
            if (originalIndex < 0) {
                mAdapter.add(uiCon);
            }
            refreshUnreadCount(uiCon.getConversationType(), uiCon.getConversationTargetId());
        }
    }

    private UIConversation makeUiConversation(Message message, int pos) {
        UIConversation uiConversation = null;

        //如果找到对应记录，则更新该条记录的未读消息数，并判断记录位置以及需要更新的item,进行局部刷新。
        if (pos >= 0) {
            uiConversation = mAdapter.getItem(pos);
            if (uiConversation != null) {
                uiConversation.setMessageContent(message.getContent());
                if (message.getMessageDirection() == Message.MessageDirection.SEND) {
                    uiConversation.setUIConversationTime(message.getSentTime());
                    if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null)
                        uiConversation.setConversationSenderId(RongIM.getInstance().getRongIMClient().getCurrentUserId());
                } else {
                    uiConversation.setUIConversationTime(message.getSentTime());
                    uiConversation.setConversationSenderId(message.getSenderUserId());
                }
                uiConversation.setConversationTargetId(message.getTargetId());
                uiConversation.setConversationContent(uiConversation.buildConversationContent(uiConversation));
                uiConversation.setSentStatus(message.getSentStatus());
                uiConversation.setLatestMessageId(message.getMessageId());
            }
        } else {
            //没有对应记录，则新建一条记录插入列表。
            uiConversation = UIConversation.obtain(message, RongContext.getInstance().getConversationGatherState(message.getConversationType().getName()));
        }
        return uiConversation;
    }

    /**
     * 根据conversations列表，构建新的会话。如：聚合情况下，删掉某条子会话时，根据剩余会话构建新的UI会话。
     */
    private UIConversation makeUIConversationFromList(List<Conversation> conversations) {
        int unreadCount = 0;
        boolean topFlag = false;
        Conversation newest = conversations.get(0);

        for (Conversation conversation : conversations) {
            if (newest.isTop()) {
                if (conversation.isTop() && conversation.getSentTime() > newest.getSentTime()) {
                    newest = conversation;
                }
            } else {
                if (conversation.isTop() || conversation.getSentTime() > newest.getSentTime()) {
                    newest = conversation;
                }
            }
            if (conversation.isTop())
                topFlag = true;
            unreadCount = unreadCount + conversation.getUnreadMessageCount();
        }

        UIConversation uiConversation = UIConversation.obtain(newest, RongContext.getInstance().getConversationGatherState(newest.getConversationType().getName()));
        uiConversation.setUnReadMessageCount(unreadCount);
        uiConversation.setTop(topFlag);
        return uiConversation;
    }

    private void refreshUnreadCount(final ConversationType type, final String targetId) {
        if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
            if (RongContext.getInstance().getConversationGatherState(type.getName())) {
                RongIM.getInstance().getRongIMClient().getUnreadCount(new RongIMClient.ResultCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        int curPos = mAdapter.findPosition(type, targetId);
                        if (curPos >= 0) {
                            mAdapter.getItem(curPos).setUnReadMessageCount(count);
                            mAdapter.getView(curPos, mList.getChildAt(curPos - mList.getFirstVisiblePosition()), mList);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        System.err.print("Throw exception when get unread message count from ipc remote side!");
                    }
                }, type);
            } else {
                RongIM.getInstance().getRongIMClient().getUnreadCount(type, targetId, new RongIMClient.ResultCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        int curPos = mAdapter.findPosition(type, targetId);
                        if (curPos >= 0) {
                            mAdapter.getItem(curPos).setUnReadMessageCount(integer);
                            mAdapter.getView(curPos, mList.getChildAt(curPos - mList.getFirstVisiblePosition()), mList);
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {

                    }
                });
            }
        }
    }
}
