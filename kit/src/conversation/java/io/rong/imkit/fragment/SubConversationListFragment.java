package io.rong.imkit.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
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
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.model.Draft;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.util.ConversationListUtils;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imkit.widget.adapter.SubConversationListAdapter;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.VoiceMessage;

public class SubConversationListFragment extends UriFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private SubConversationListAdapter mAdapter;
    private Conversation.ConversationType currentType;
    private TextView mNotificationBar;
    private ListView mList;

    RongIMClient.ResultCallback<List<Conversation>> mCallback = new RongIMClient.ResultCallback<List<Conversation>>() {
        @Override
        public void onSuccess(List<Conversation> conversations) {
            RLog.d(this, "SubConversationListFragment", "initFragment onSuccess callback");
            if (conversations == null || conversations.size() == 0)
                return;

            List<UIConversation> uiConversationList = new ArrayList<UIConversation>();
            for (Conversation conversation : conversations) {
                if (mAdapter.getCount() > 0) {
                    int pos = mAdapter.findPosition(conversation.getConversationType(), conversation.getTargetId());
                    if (pos < 0) {
                        UIConversation uiConversation = UIConversation.obtain(conversation, false);
                        uiConversationList.add(uiConversation);
                    }
                } else {
                    UIConversation uiConversation = UIConversation.obtain(conversation, false);
                    uiConversationList.add(uiConversation);
                }
            }

            mAdapter.addCollection(uiConversationList);

            if (mList != null && mList.getAdapter() != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(RongIMClient.ErrorCode e) {
            //TODO show notice
            RLog.d(this, "SubConversationListFragment", "initFragment onError callback, e=" + e);
        }
    };

    public static ConversationListFragment getInstance() {
        return new ConversationListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RongContext.getInstance().getEventBus().register(this);

        if (getActivity().getIntent() == null || getActivity().getIntent().getData() == null)
            throw new IllegalArgumentException();
        if (mAdapter == null)
            mAdapter = new SubConversationListAdapter(getActivity());
    }


    public void initFragment(Uri uri) {
        String type = uri.getQueryParameter("type");
        Conversation.ConversationType value = null;

        RLog.d(this, "initFragment", "uri=" + uri);

        currentType = null;

        Conversation.ConversationType[] defaultTypes = {Conversation.ConversationType.PRIVATE, Conversation.ConversationType.DISCUSSION,
                Conversation.ConversationType.GROUP, Conversation.ConversationType.CHATROOM, Conversation.ConversationType.CUSTOMER_SERVICE,
                Conversation.ConversationType.SYSTEM, Conversation.ConversationType.PUBLIC_SERVICE, Conversation.ConversationType.APP_PUBLIC_SERVICE};
        for (Conversation.ConversationType conversationType : defaultTypes) {
            if (conversationType.getName().equals(type)) {
                currentType = conversationType;
                value = conversationType;
                break;
            }
        }
        if (value != null)
            RongIM.getInstance().getRongIMClient().getConversationList(mCallback, value);
        else
            throw new IllegalArgumentException("Unknown conversation type!!");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_conversationlist, null);
        mNotificationBar = findViewById(view, R.id.rc_status_bar);
        mList = findViewById(view, R.id.rc_list);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);
        mList.setOnItemLongClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        RLog.d(this, "SubConversationListFragment", "onResume");
        super.onResume();
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

    public void onEventMainThread(final Event.ReadReceiptEvent event) {
        if (mAdapter == null) {
            RLog.d(SubConversationListFragment.this, "onEventMainThread ReadReceiptEvent", "adapter is null");
            return;
        }

        final int originalIndex = mAdapter.findPosition(event.getMessage().getConversationType(), event.getMessage().getTargetId());

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

    public void onEventMainThread(Message message) {

        RLog.d(SubConversationListFragment.this, "onEventMainThread", "Message");

        int originalIndex = mAdapter.findPosition(message.getConversationType(), message.getTargetId());

        if (!(message.getConversationType().equals(currentType)))
            return;//如果该条消息类型和当前列表的消息类型不符合，则忽略不处理。

        UIConversation uiConversation = null;
        /*如果找到对应记录，则更新该条记录的未读消息数，并判断记录位置以及需要更新的item,进行局部刷新。*/
        if (originalIndex >= 0) {
            uiConversation = makeUiConversation(message, originalIndex);

            int newPosition = ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter);
            if (newPosition == originalIndex) {
                mAdapter.getView(newPosition, mList.getChildAt(newPosition - mList.getFirstVisiblePosition()), mList);
            } else {
                mAdapter.remove(originalIndex);
                mAdapter.add(uiConversation, newPosition);
                mAdapter.notifyDataSetChanged();
            }
        } else {
            //没有对应记录，则新建一条记录插入列表。
            uiConversation = UIConversation.obtain(message, false);
            mAdapter.add(uiConversation, ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter));
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.OnReceiveMessageEvent onReceiveMessageEvent) {
        onEventMainThread(onReceiveMessageEvent.getMessage());
    }

    public void onEventMainThread(MessageContent content) {

        RLog.d(SubConversationListFragment.this, "onEventMainThread::MessageContent", "MessageContent");

        for (int index = mList.getFirstVisiblePosition(); index < mList.getLastVisiblePosition(); index++) {
            UIConversation tempUIConversation = mAdapter.getItem(index);
            if (tempUIConversation.getMessageContent().equals(content)) {
                tempUIConversation.setMessageContent(content);

                Spannable messageData = RongContext.getInstance().getMessageTemplate(content.getClass()).getContentSummary(content);

                if (tempUIConversation.getMessageContent() instanceof VoiceMessage) {
                    boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(tempUIConversation.getConversationType(), tempUIConversation.getConversationTargetId())
                            .getReceivedStatus().isListened();
                    if (isListened) {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        messageData.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, messageData.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                tempUIConversation.setConversationContent(messageData);
                mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    public void onEventMainThread(Draft draft) {
        Conversation.ConversationType curType = Conversation.ConversationType.setValue(draft.getType());
        if (curType == null) {
            throw new IllegalArgumentException("the type of the draft is unknown!");
        }

        int position = mAdapter.findPosition(curType, draft.getId());

        if (position >= 0) {
            UIConversation conversation = mAdapter.getItem(position);
            if (draft.getContent() == null) {
                conversation.setDraft("");
            } else {
                conversation.setDraft(draft.getContent());
            }
            mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition()), mList);
        }

    }

    /* function: 异步获取到群组信息后，在此进行相应刷新*/
    public void onEventMainThread(Group groupInfo) {

        int count = mAdapter.getCount();

        if (groupInfo.getName() == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            UIConversation temp = mAdapter.getItem(i);
            if (temp.getConversationTargetId().equals(groupInfo.getId())) {
                temp.setUIConversationTitle(groupInfo.getName());
                if (groupInfo.getPortraitUri() != null)
                    temp.setIconUrl(groupInfo.getPortraitUri());
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
            }
        }
    }

    public void onEventMainThread(Event.GroupUserInfoEvent event) {
        RLog.d(this, "onEvent", "update GroupUserInfoEvent");
        GroupUserInfo userInfo = event.getUserInfo();
        if (userInfo == null || userInfo.getNickname() == null) {
            return;
        }

        RongContext context = RongContext.getInstance();
        if (context == null) {
            return;
        }

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            String type = uiConversation.getConversationType().getName();
            MessageContent messageContent = uiConversation.getMessageContent();
            if (messageContent == null) {
                continue;
            }
            IContainerItemProvider.MessageProvider provider = context.getMessageTemplate(messageContent.getClass());
            if (provider == null) {
                continue;
            }
            Spannable messageData = provider.getContentSummary(messageContent);
            if (messageData == null) {
                continue;
            }

            //群组或讨论组非聚合显示情况，需要比较该会话的senderId.因为此时的targetId为群组。
            if (type.equals(Conversation.ConversationType.GROUP.getName())
                    && (uiConversation.getConversationSenderId().equals(userInfo.getUserId()))) {
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
        RLog.d(this, "onEvent", "update userInfo");
        if (userInfo == null || userInfo.getName() == null) {
            return;
        }

        RongContext context = RongContext.getInstance();
        if (context == null) {
            return;
        }

        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            UIConversation uiConversation = mAdapter.getItem(i);
            String type = uiConversation.getConversationType().getName();
            MessageContent messageContent = uiConversation.getMessageContent();
            if (uiConversation.hasNickname(userInfo.getUserId())) {
                continue;
            }
            if (messageContent == null) {
                continue;
            }
            IContainerItemProvider.MessageProvider provider = context.getMessageTemplate(messageContent.getClass());
            if (provider == null) {
                continue;
            }
            Spannable messageData = provider.getContentSummary(messageContent);
            if (messageData == null) {
                continue;
            }

            //群组或讨论组非聚合显示情况，需要比较该会话的senderId.因为此时的targetId为群组或讨论组名字。
            if ((type.equals(Conversation.ConversationType.GROUP.getName()) ||
                    type.equals(Conversation.ConversationType.DISCUSSION.getName()))
                    && (uiConversation.getConversationSenderId().equals(userInfo.getUserId()))) {
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
                builder.append(userInfo.getName()).append(" : ").append(messageData);
                uiConversation.setConversationContent(builder);
            } else if (uiConversation.getConversationTargetId().equals(userInfo.getUserId())) {
                if (type.equals(Conversation.ConversationType.PRIVATE.getName())) {
                    uiConversation.setUIConversationTitle(userInfo.getName());
                } else {
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
                    builder.append(userInfo.getName()).append(" : ").append(messageData);
                    uiConversation.setConversationContent(builder);
                    uiConversation.setIconUrl(userInfo.getPortraitUri());
                }
            }
            mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
        }
    }

    public void onEventMainThread(final RongIMClient.ConnectionStatusListener.ConnectionStatus status) {

        RLog.d(SubConversationListFragment.this, "ConnectionStatus", status.toString());

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

    public void onEventMainThread(Discussion discussion) {
        int count = mAdapter.getCount();

        for (int i = 0; i < count; i++) {
            UIConversation temp = mAdapter.getItem(i);
            boolean gatherState = RongContext.getInstance().getConversationGatherState(temp.getConversationType().getName());
            if (temp.getConversationTargetId().equals(discussion.getId())) {
                temp.setUIConversationTitle(discussion.getName());
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
                break;
            }
        }
    }

    public void onEventMainThread(PublicServiceProfile accountInfo) {
        int count = mAdapter.getCount();

        for (int i = 0; i < count; i++) {
            if (mAdapter.getItem(i).getConversationTargetId().equals(accountInfo.getTargetId())) {
                mAdapter.getItem(i).setIconUrl(accountInfo.getPortraitUri());
                mAdapter.getItem(i).setUIConversationTitle(accountInfo.getName());
                mAdapter.getView(i, mList.getChildAt(i - mList.getFirstVisiblePosition()), mList);
                break;
            }
        }
    }

    public void onEventMainThread(Event.ConversationUnreadEvent unreadEvent) {
        final int targetIndex = mAdapter.findPosition(unreadEvent.getType(), unreadEvent.getTargetId());

        if (targetIndex >= 0) {
            final UIConversation temp = mAdapter.getItem(targetIndex);
            temp.setUnReadMessageCount(0);
            mAdapter.getView(targetIndex, mList.getChildAt(targetIndex - mList.getFirstVisiblePosition()), mList);
        }
    }

    public void onEventMainThread(Event.ConversationTopEvent setTopEvent) throws IllegalAccessException {
        final int originalIndex = mAdapter.findPosition(setTopEvent.getConversationType(), setTopEvent.getTargetId());

        if (originalIndex >= 0) {
            UIConversation temp = mAdapter.getItem(originalIndex);
            boolean originalValue = temp.isTop();
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
        } else {
            throw new IllegalAccessException("the item has already been deleted!");
        }
    }

    public void onEventMainThread(Event.ConversationRemoveEvent removeEvent) {
        int originalIndex = mAdapter.findPosition(removeEvent.getType(), removeEvent.getTargetId());
        if (originalIndex >= 0) {
            mAdapter.remove(originalIndex);
            mAdapter.notifyDataSetChanged();
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

        if (clearMessagesEvent != null && originalIndex >= 0) {
            Conversation temp = RongIMClient.getInstance().getConversation(clearMessagesEvent.getType(), clearMessagesEvent.getTargetId());
            UIConversation uiConversation = UIConversation.obtain(temp, false);
            mAdapter.remove(originalIndex);
            mAdapter.add(UIConversation.obtain(temp, false), ConversationListUtils.findPositionForNewConversation(uiConversation, mAdapter));
            mAdapter.notifyDataSetChanged();
        }

    }

    public void onEventMainThread(Event.MessageDeleteEvent event) {
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (event.getMessageIds().contains(mAdapter.getItem(i).getLatestMessageId())) {
                final int index = i;
                RongIM.getInstance().getRongIMClient().getConversation(mAdapter.getItem(index).getConversationType(), mAdapter.getItem(index).getConversationTargetId(),
                        new RongIMClient.ResultCallback<Conversation>() {
                            @Override
                            public void onSuccess(Conversation conversation) {
                                if (conversation == null) {
                                    RLog.d(this, "onEventMainThread", "getConversation : onSuccess, conversation = null");
                                    return;
                                }
                                UIConversation temp = UIConversation.obtain(conversation, false);
                                mAdapter.remove(index);
                                int newPosition = ConversationListUtils.findPositionForNewConversation(temp, mAdapter);
                                mAdapter.add(temp, newPosition);
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode e) {
                            }
                        });
                break;
            }
        }

    }

    public void onEventMainThread(Event.OnMessageSendErrorEvent sendErrorEvent) {
        int index = mAdapter.findPosition(sendErrorEvent.getMessage().getConversationType(), sendErrorEvent.getMessage().getTargetId());

        if (index >= 0) {
            mAdapter.getItem(index).setSentStatus(Message.SentStatus.FAILED);
            mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition()), mList);
        }
    }

    public void onEventMainThread(Event.QuitDiscussionEvent event) {
        int index = mAdapter.findPosition(Conversation.ConversationType.DISCUSSION, event.getDiscussionId());

        if (index >= 0) {
            mAdapter.remove(index);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.QuitGroupEvent event) {
        int index = mAdapter.findPosition(Conversation.ConversationType.GROUP, event.getGroupId());

        if (index >= 0) {
            mAdapter.remove(index);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Event.MessageListenedEvent event) {
        final int originalIndex = mAdapter.findPosition(event.getConversationType(), event.getTargetId());

        if (originalIndex >= 0) {
            UIConversation temp = mAdapter.getItem(originalIndex);
            if (temp.getLatestMessageId() == event.getLatestMessageId()) {
                Spannable content = RongContext.getInstance().getMessageTemplate(temp.getMessageContent().getClass()).getContentSummary(temp.getMessageContent());
                boolean isListened = RongIM.getInstance().getRongIMClient().getConversation(event.getConversationType(), event.getTargetId()).getReceivedStatus().isListened();
                if (isListened) {
                    content.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_text_color_secondary)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    content.setSpan(new ForegroundColorSpan(RongContext.getInstance().getResources().getColor(R.color.rc_voice_color)), 0, content.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                temp.setConversationContent(content);
            }
            mAdapter.getView(originalIndex, mList.getChildAt(originalIndex - mList.getFirstVisiblePosition()), mList);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        UIConversation uiconversation = mAdapter.getItem(position);

        if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
            boolean isDefault = RongContext.getInstance().getConversationListBehaviorListener().onConversationClick(getActivity(), view, uiconversation);
            if (isDefault == true)
                return;
        }

        Conversation.ConversationType type = uiconversation.getConversationType();
        uiconversation.setUnReadMessageCount(0);
        RongIM.getInstance().startConversation(getActivity(), type, uiconversation.getConversationTargetId(), uiconversation.getUIConversationTitle());
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        UIConversation uiConversation = mAdapter.getItem(position);
        String title = uiConversation.getUIConversationTitle();

        if (RongContext.getInstance().getConversationListBehaviorListener() != null) {
            boolean isDealt = RongContext.getInstance().getConversationListBehaviorListener().onConversationLongClick(getActivity(), view, uiConversation);
            if (isDealt)
                return true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        buildMultiDialog(uiConversation);
        return true;
    }

    private void buildMultiDialog(final UIConversation uiConversation) {
        String[] items = new String[2];
        if (uiConversation.isTop())
            items[0] = getActivity().getString(R.string.rc_conversation_list_dialog_cancel_top);
        else
            items[0] = getActivity().getString(R.string.rc_conversation_list_dialog_set_top);
        items[1] = getActivity().getString(R.string.rc_conversation_list_dialog_remove);

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

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onDestroy() {
        RLog.d(this, "SubConversationListFragment", "onDestroy");
        RongContext.getInstance().getEventBus().unregister(this);
        getHandler().removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onPause() {
        RLog.d(this, "SubConversationListFragment", "onPause");
        super.onPause();
    }

    public void setAdapter(SubConversationListAdapter adapter) {
        if (mAdapter != null)
            mAdapter.clear();
        mAdapter = adapter;
        if (mList != null && getUri() != null) {
            mList.setAdapter(adapter);
            initFragment(getUri());
        }
    }

    public SubConversationListAdapter getAdapter() {
        return mAdapter;
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

                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);
                if (message.getMessageDirection() == Message.MessageDirection.RECEIVE && (tag.flag() & MessageTag.ISCOUNTED) != 0) {
                    uiConversation.setUnReadMessageCount(uiConversation.getUnReadMessageCount() + 1);
                    List<ConversationInfo> infoList = RongContext.getInstance().getCurrentConversationList();
                    for (ConversationInfo info : infoList) {
                        if (info != null && info.getConversationType().equals(message.getConversationType()) && info.getTargetId().equals(message.getTargetId()))
                            uiConversation.setUnReadMessageCount(0);
                    }
                } else {
                    uiConversation.setUnReadMessageCount(0);
                }
            }
        }
        return uiConversation;
    }
}
