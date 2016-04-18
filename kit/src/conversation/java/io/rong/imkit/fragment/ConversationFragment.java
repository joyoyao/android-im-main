package io.rong.imkit.fragment;


import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongNotificationManager;
import io.rong.imkit.model.ConversationInfo;
import io.rong.imkit.widget.InputView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.message.PublicServiceCommandMessage;
import io.rong.message.SuspendMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
public class ConversationFragment extends DispatchResultFragment {


    UriFragment mListFragment, mInputFragment;

    Conversation.ConversationType mConversationType;
    String mTargetId;
    ConversationInfo mCurrentConversationInfo;
    MessageInputFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_conversation, container, false);
        return view;
    }


    @Override
    public void onResume() {
        RongNotificationManager.getInstance().onRemoveNotification();

        super.onResume();
    }

    private InputView.OnInfoButtonClick onInfoButtonClick;

    public void setOnInfoButtonClick(InputView.OnInfoButtonClick onInfoButtonClick) {
        this.onInfoButtonClick = onInfoButtonClick;

        if (fragment != null)
            fragment.setOnInfoButtonClick(onInfoButtonClick);
    }

    @Override
    protected void initFragment(final Uri uri) {

        if (uri == null)
            return;

        List<String> paths = uri.getPathSegments();

        String typeStr = uri.getLastPathSegment().toUpperCase();
        mConversationType = Conversation.ConversationType.valueOf(typeStr);
        mTargetId = uri.getQueryParameter("targetId");

        mCurrentConversationInfo = ConversationInfo.obtain(mConversationType, mTargetId);
        RongContext.getInstance().registerConversationInfo(mCurrentConversationInfo);


        mListFragment = (UriFragment) getChildFragmentManager().findFragmentById(android.R.id.list);
        mInputFragment = (UriFragment) getChildFragmentManager().findFragmentById(android.R.id.toggle);

        if (mListFragment == null)
            mListFragment = new MessageListFragment();

        if (mInputFragment == null)
            mInputFragment = new MessageInputFragment();

        if (mListFragment != null && (mListFragment.getUri() == null || !mListFragment.getUri().equals(uri)))
            mListFragment.setUri(uri);

        if (mInputFragment != null && (mInputFragment.getUri() == null || !mInputFragment.getUri().equals(uri)))
            mInputFragment.setUri(uri);

        if (paths.get(1).toLowerCase().equals("discussion") && !TextUtils.isEmpty(uri.getQueryParameter("targetIds"))) {
            String[] userIds = uri.getQueryParameter("targetIds").split(uri.getQueryParameter("delimiter"));


            if (userIds != null && userIds.length > 0) {
                final List<String> list = new ArrayList<String>();
                for (String item : userIds)
                    list.add(item);

                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        RongIM.getInstance().getRongIMClient().createDiscussion(uri.getQueryParameter("title"), list, new RongIMClient.CreateDiscussionCallback() {
                            @Override
                            public void onSuccess(String discussionId) {
                                Uri uri = Uri.parse("rong://" + getActivity().getApplicationInfo().processName).buildUpon()
                                        .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase())
                                        .appendQueryParameter("targetId", discussionId).build();

                                RLog.i(this, "createDiscussion", discussionId);

                                setUri(uri);

                                if (mListFragment != null)
                                    mListFragment.setUri(uri);

                                if (mInputFragment != null)
                                    mInputFragment.setUri(uri);
                            }

                            @Override
                            public void onError(RongIMClient.ErrorCode errorCode) {
                            }
                        });
                    }
                });
            }
        } else if (paths.get(1).toLowerCase().equals("chatroom")) {
            final String targetId = uri.getQueryParameter("targetId");

            if (TextUtils.isEmpty(targetId))
                return;

            getHandler().post(new Runnable() {

                @Override
                public void run() {
                    int pullCount = getResources().getInteger(R.integer.rc_chatroom_first_pull_message_count);

                    RongIM.getInstance().getRongIMClient().joinChatRoom(targetId, pullCount, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                        }
                    });
                }
            });
        }

        if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE ||
                mConversationType == Conversation.ConversationType.PUBLIC_SERVICE) {
            RongContext.getInstance().executorBackground(new Runnable() {
                @Override
                public void run() {
                    PublicServiceCommandMessage msg = new PublicServiceCommandMessage();
                    msg.setCommand(PublicServiceMenu.PublicServiceMenuItemType.Entry.getMessage());
                    if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null)
                        RongIM.getInstance().getRongIMClient().sendMessage(mConversationType, mTargetId, msg, null, null, null);
                }
            });
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment = (MessageInputFragment) getChildFragmentManager().findFragmentById(android.R.id.toggle);
        if(fragment != null)
            fragment.setOnInfoButtonClick(this.onInfoButtonClick);
    }

    @Override
    public void onDestroyView() {
        RongContext.getInstance().unregisterConversationInfo(mCurrentConversationInfo);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        if (mConversationType == Conversation.ConversationType.CHATROOM)
            RongContext.getInstance().executorBackground(new Runnable() {
                @Override
                public void run() {
                    RongIM.getInstance().getRongIMClient().quitChatRoom(mTargetId, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                        }
                    });
                }
            });
        if (mConversationType == Conversation.ConversationType.CUSTOMER_SERVICE) {
            RongContext.getInstance().executorBackground(new Runnable() {
                @Override
                public void run() {
                    SuspendMessage msg = new SuspendMessage();
                    RongIM.getInstance().getRongIMClient().sendMessage(mConversationType, mTargetId, msg, null, null, null);
                }
            });
        }

        super.onDestroy();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
}
