package io.rong.imkit.fragment;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.rong.common.SystemUtils;
import io.rong.imkit.ConversationConst;
import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.EmojiMessageAdapter;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.InputView;
import io.rong.imkit.widget.adapter.MessageListAdapter;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ImageMessage;
import io.rong.message.ReadReceiptMessage;
import io.rong.message.VoiceMessage;

/**
 * Created by DragonJ on 14/10/23.
 */
public class MessageListFragment extends UriFragment implements AbsListView.OnScrollListener {


    MessageListAdapter mAdapter;
    GestureDetector mGestureDetector;
    ListView mList;

    Conversation mConversation;
    /*上面的未读消息*/
    int mUnreadCount;

    /*下方的新消息提醒*/
    int mNewMessageCount;

    /*记录最后可见位置*/
    int mLastVisiblePosition;

    Button mUnreadBtn;
    /*蓝色小气泡*/
    ImageButton mNewMessageBtn;
    /*未读消息数*/
    TextView mNewMessageTextView;
    /*显示历史消息是否展示的状态*/
    boolean isShowUnreadMessageState;
    /*显示未读新消息是否展示的状态*/
    boolean isShowNewMessageState;
    /*未处理的消息数量*/
    int mMessageleft = -1;

    static final int REQ_LIST = 1;
    static final int RENDER_LIST = 2;
    static final int RENDER_HISTORY = 6;
    static final int NOTIFY_LIST = 9;
    static final int DELETE_MESSAGE = 11;

    static final int REFRESH_LIST = 3;
    static final int RESET_LIST_STACK = 10;
    static final int REFRESH_ITEM = 4;
    static final int REQ_HISTORY = 5;
    static final int REFRESH_ITEM_READ_RECEIPT = 7;
    private static final int REQ_UNREAD = 12;

    private static final int LISTVIEW_SHOW_COUNT = 5;

    View mHeaderView;
    private boolean isOnClickBtn;
    private boolean isShowWithoutConnected = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RongContext.getInstance().getEventBus().register(this);

        isShowUnreadMessageState = RongContext.getInstance().getUnreadMessageState();
        isShowNewMessageState = RongContext.getInstance().getNewMessageState();

        if (EmojiMessageAdapter.getInstance() == null)
            EmojiMessageAdapter.init(RongContext.getInstance());

        mAdapter = new MessageListAdapter(getActivity());
        mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                //判断是向下滑动&&新消息大于0&&新消息
                if (distanceY > 0 && mNewMessageCount >= 0) {
                    if (mList.getLastVisiblePosition() >= mList.getCount() - mNewMessageCount) {
                        mNewMessageTextView.setText(mList.getCount() - mList.getLastVisiblePosition() + "");
                        mNewMessageCount = mList.getCount() - mList.getLastVisiblePosition() - 1;
                        if (mNewMessageCount > 99) {
                            mNewMessageTextView.setText("99+");
                        } else {
                            mNewMessageTextView.setText(mNewMessageCount + "");
                        }
                    }
                }

                //如果未读消息等于0 说明已经到达 adapter 最底部
                if (mNewMessageCount == 0) {
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (velocityY < 0) {
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                }
                return super.onFling(e1, e2, velocityX, velocityY);
            }
        });

    }


    @Override
    protected void initFragment(Uri uri) {

        String typeStr = uri.getLastPathSegment().toUpperCase();
        Conversation.ConversationType type = Conversation.ConversationType.valueOf(typeStr);

        String targetId = uri.getQueryParameter("targetId");
        String title = uri.getQueryParameter("title");

        if (TextUtils.isEmpty(targetId) || type == null)
            return;

        mConversation = Conversation.obtain(type, targetId, title);

        if (mAdapter != null) {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                }
            });
        }

        mNewMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //设置点击新消息
                getHandler().postDelayed(new ScrollRunnable(), 500);
//                while (mList.getLastVisiblePosition()<mList.getCount()){
//                    mList.smoothScrollToPosition(mList.getCount()+5);
//                }

                mList.smoothScrollToPosition(mList.getCount() + 1);
//                mList.smoothScrollByOffset(50000);
//                mList.setSelection(mList.getCount() + 1);
                mNewMessageCount = 0;
                mNewMessageBtn.setVisibility(View.GONE);
                mNewMessageTextView.setVisibility(View.GONE);
            }
        });

        if(RongIM.getInstance()== null || RongIM.getInstance().getRongIMClient() == null){
            RLog.e(this, "initFragment", "Not connected yet.");
            isShowWithoutConnected = true;
            return;
        }
        getConversation();
        RongIM.getInstance().getRongIMClient().clearMessagesUnreadStatus(mConversation.getConversationType(), mConversation.getTargetId(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_messagelist, container, false);
        mUnreadBtn = findViewById(view, R.id.rc_unread_message_count);
        mNewMessageBtn = findViewById(view, R.id.rc_new_message_count);
        mNewMessageTextView = findViewById(view, R.id.rc_new_message_number);
        mList = findViewById(view, R.id.rc_list);
        mHeaderView = inflater.inflate(R.layout.rc_item_progress, null);
        mList.addHeaderView(mHeaderView);
        mList.setOnScrollListener(this);
        mList.setSelectionAfterHeaderView();

        mAdapter.setOnItemHandlerListener(new MessageListAdapter.OnItemHandlerListener() {

            @Override
            public void onWarningViewClick(int position, io.rong.imlib.model.Message data, final View v) {

                boolean isSendComplete = true;
                Object obj = v.getTag();

                if (obj != null) {
                    isSendComplete = (Boolean) obj;
                } else {
                    v.setTag(false);
                }

                if (!isSendComplete)
                    return;

                MessageContent content = data.getContent();
                io.rong.imlib.model.Message message = io.rong.imlib.model.Message.obtain(data.getTargetId(), data.getConversationType(), content);
                RongIM.getInstance().getRongIMClient().deleteMessages(new int[]{data.getMessageId()});

                if (content instanceof ImageMessage) {
                    ImageMessage msg = (ImageMessage) content;
                    content = ImageMessage.obtain(msg.getLocalUri(), msg.getLocalUri(),msg.isFull());
                    ((ImageMessage) content).setExtra(msg.getExtra());
                    message.setContent(content);

                    RongIM.getInstance().getRongIMClient().sendImageMessage(message, null, null, new RongIMClient.SendImageMessageCallback() {

                        @Override
                        public void onAttached(io.rong.imlib.model.Message message) {
                        }

                        @Override
                        public void onError(io.rong.imlib.model.Message message, RongIMClient.ErrorCode code) {
                            v.setTag(true);
                        }

                        @Override
                        public void onSuccess(io.rong.imlib.model.Message message) {
                            v.setTag(true);
                        }

                        @Override
                        public void onProgress(io.rong.imlib.model.Message message, int progress) {
                        }
                    });

                } else {

                    RongIM.getInstance().getRongIMClient().sendMessage(message, null, null, new RongIMClient.SendMessageCallback() {
                        @Override
                        public void onError(Integer messageId, RongIMClient.ErrorCode e) {
                            v.setTag(true);
                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            v.setTag(true);
                        }
                    });
                }
            }
        });

        return view;
    }


    boolean mIsLoading = true;
    boolean mHasMore = false;

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (firstVisibleItem == 0 && mAdapter.getCount() > 0 && !mIsLoading && mHasMore) {
            mIsLoading = true;
            getHandler().sendEmptyMessage(REQ_HISTORY);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (getActionBarHandler() != null) {
            getActionBarHandler().onTitleChanged(mConversation.getConversationTitle());
        }

        mList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_MOVE ||
                        event.getAction() == MotionEvent.ACTION_DOWN) {
                    EventBus.getDefault().post(Event.InputViewEvent.obtain(false));
                }
                mGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        mList.setAdapter(mAdapter);

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RongContext.getInstance().getPrimaryInputProvider().onInactive(view.getContext());
                RongContext.getInstance().getSecondaryInputProvider().onInactive(view.getContext());
            }
        });

        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private List<UIMessage> filterMessage(List<UIMessage> srcList) {
        List<UIMessage> destList = null;

        if (mAdapter.getCount() > 0) {
            destList = new ArrayList<UIMessage>();
            for (int i = 0; i < mAdapter.getCount(); i++) {
                for (UIMessage msg : srcList) {
                    if (destList.contains(msg))
                        continue;

                    if (msg.getMessageId() != mAdapter.getItem(i).getMessageId())
                        destList.add(msg);
                }
            }
        } else {
            destList = srcList;
        }

        return destList;
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case RENDER_LIST:

                if (msg.obj != null && msg.obj instanceof List<?>) {
                    final List<UIMessage> list = (List<UIMessage>) msg.obj;
                    if (list.size() < ConversationConst.CONFIG.DEF_LIST_COUNT) {
                        mHasMore = false;
                        mList.removeHeaderView(mHeaderView);
                        mHeaderView = null;
                    } else {
                        mHasMore = true;
                    }

                    mAdapter.clear();
                    mAdapter.addCollection(filterMessage(list));

                    if (list.size() <= LISTVIEW_SHOW_COUNT) {
                        mList.setStackFromBottom(false);
                        mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    } else {
                        mList.setStackFromBottom(true);
                        mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                    }

                    mAdapter.notifyDataSetChanged();

                    getHandler().sendEmptyMessage(RESET_LIST_STACK);

                    RLog.d(MessageListFragment.this, "RENDER_LIST", "count:" + list.size());

                } else {
                    mHasMore = false;
                    mList.removeHeaderView(mHeaderView);
                    mHeaderView = null;
                }
                mIsLoading = false;

                //设置动画
                if (mUnreadCount >= 10) {
                    TranslateAnimation animation = new TranslateAnimation(300, 0, 0, 0);
                    AlphaAnimation animation1 = new AlphaAnimation(0, 1);
                    animation.setDuration(1000);
                    animation1.setDuration(2000);
                    AnimationSet set = new AnimationSet(true);
                    set.addAnimation(animation);
                    set.addAnimation(animation1);
                    mUnreadBtn.setVisibility(View.VISIBLE);
                    mUnreadBtn.startAnimation(set);
                    set.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            getHandler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!isOnClickBtn) {
                                        TranslateAnimation animation = new TranslateAnimation(0, 700, 0, 0);
                                        animation.setDuration(700);
                                        animation.setFillAfter(true);
                                        mUnreadBtn.startAnimation(animation);
//                                        mUnreadBtn.setClickable(false);
//                                        getHandler().postDelayed(new R,700);

                                    }
                                }
                            }, 4000);//进去6s 没做任何操作 未读条目淡出
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                }


                break;
            case RENDER_HISTORY:
                RLog.d(this, "MessageListFragment", "RENDER_HISTORY");

                if (msg.obj instanceof List<?>) {
                    final List<UIMessage> list = (List<UIMessage>) msg.obj;
                    if (list.size() < ConversationConst.CONFIG.DEF_LIST_COUNT) {
                        mHasMore = false;
                        mList.removeHeaderView(mHeaderView);
                        mHeaderView = null;
                    } else {
                        mHasMore = true;
                    }
//                    Collections.reverse(list);

                    for (UIMessage item : list) {
                        mAdapter.add(item, 0);
                    }

                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_DISABLED);
                    mList.setStackFromBottom(false);

                    int index = mList.getFirstVisiblePosition();
                    mAdapter.notifyDataSetChanged();

                    if (index == 0) {
                        mList.setSelection(list.size());
                    }

                } else {
                    mHasMore = false;
                    mList.removeHeaderView(mHeaderView);
                    mHeaderView = null;
                }
                mIsLoading = false;
                break;
            case REQ_LIST:
                mAdapter.clear();
                mAdapter.notifyDataSetChanged();

                EmojiMessageAdapter.getInstance().getLatestMessages(mConversation.getConversationType(), mConversation.getTargetId(), ConversationConst.CONFIG.DEF_LIST_COUNT, new RongIMClient.ResultCallback<List<UIMessage>>() {

                    @Override
                    public void onSuccess(List<UIMessage> messages) {
                        getHandler().obtainMessage(RENDER_LIST, messages).sendToTarget();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        RLog.e(MessageListFragment.this, "IPC:getConversationMessage", e.toString());
                        getHandler().obtainMessage(RENDER_LIST, null).sendToTarget();
                    }
                });
                break;
            case REFRESH_LIST:
                UIMessage model = (UIMessage) msg.obj;
                //判断最后一条是否可见
                if (isShowNewMessageState && mList.getLastVisiblePosition() < (mList.getCount() - 1)
                        && io.rong.imlib.model.Message.MessageDirection.SEND != model.getMessageDirection()
                        && SystemUtils.isAppRunningOnTop(RongContext.getInstance(), RongContext.getInstance().getPackageName())) {

                    if (model.getConversationType() != Conversation.ConversationType.CHATROOM
                            && model.getConversationType() != Conversation.ConversationType.CUSTOMER_SERVICE
                            && model.getConversationType() != Conversation.ConversationType.APP_PUBLIC_SERVICE
                            && model.getConversationType() != Conversation.ConversationType.PUBLIC_SERVICE) {

                        mNewMessageCount++;
                        if (mNewMessageCount > 0) {
                            mNewMessageBtn.setVisibility(View.VISIBLE);
                            mNewMessageTextView.setVisibility(View.VISIBLE);
                        }
                        if (mNewMessageCount > 99) {
                            mNewMessageTextView.setText("99+");
                        } else {
                            mNewMessageTextView.setText(mNewMessageCount + "");
                        }
                    }
                }

                int last = mList.getLastVisiblePosition();
                int count = mList.getCount();
                if(last == count - 1) {
                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                } else if(last < mList.getCount() - 1) {
                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
                }

                mAdapter.notifyDataSetChanged();
                if (last == count - 1) {
                    mNewMessageBtn.setVisibility(View.GONE);
                    mNewMessageTextView.setVisibility(View.GONE);
                }
                break;
            case RESET_LIST_STACK:
                resetListViewStack();
                mAdapter.notifyDataSetChanged();

                break;
            case REFRESH_ITEM:
                int position = (Integer) msg.obj;

                if (position >= mList.getFirstVisiblePosition() && position <= mList.getLastVisiblePosition()) {
                    RLog.d(MessageListFragment.this, "REFRESH_ITEM", "Index:" + position);
                    mAdapter.getView(position, mList.getChildAt(position - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }

                mList.setSelection(mAdapter.getCount() - 1);

                break;
            case REFRESH_ITEM_READ_RECEIPT:
                int pos = (Integer) msg.obj;

                if (pos >= mList.getFirstVisiblePosition() && pos <= mList.getLastVisiblePosition()) {
                    RLog.d(MessageListFragment.this, "REFRESH_ITEM", "Index:" + pos);
                    mAdapter.getView(pos, mList.getChildAt(pos - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }

                break;
            case REQ_HISTORY:
                RLog.d(this, "MessageListFragment", "REQ_HISTORY");

                UIMessage message = mAdapter.getItem(0);

                EmojiMessageAdapter.getInstance().getHistoryMessages(mConversation.getConversationType(), mConversation.getTargetId(), message.getMessageId(), ConversationConst.CONFIG.DEF_LIST_COUNT, new RongIMClient.ResultCallback<List<UIMessage>>() {
                    @Override
                    public void onSuccess(List<UIMessage> messages) {
                        getHandler().obtainMessage(RENDER_HISTORY, messages).sendToTarget();
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        RLog.e(MessageListFragment.this, "IPC:getConversationMessage", e.toString());
                    }
                });
                break;

            //滚动到未读消息
            case REQ_UNREAD:
                RLog.d(this, "MessageListFragment", "REQ_HISTORY");
                //获取到当前adapter的最顶上一条
                message = mAdapter.getItem(0);
                // 关键参数 4 : 继续向上拉取多少条
                EmojiMessageAdapter.getInstance().getHistoryMessages(mConversation.getConversationType(), mConversation.getTargetId(), message.getMessageId(), mUnreadCount - 29, new RongIMClient.ResultCallback<List<UIMessage>>() {
                    @Override
                    public void onSuccess(List<UIMessage> messages) {

                        final List<UIMessage> list = messages;
                        //如果数据库未读消息小于 30
                        if (list.size() < ConversationConst.CONFIG.DEF_LIST_COUNT) {
                            mHasMore = false;
                            mList.removeHeaderView(mHeaderView);
                            mHeaderView = null;
                        } else {
                            mHasMore = true;
                        }
                        //倒序 历史消息记录集合
                        //remove message reverse
                        //Collections.reverse(list);
                        //添加到 UI 上
                        for (UIMessage item : list) {
                            mAdapter.add(item, 0);
                        }
                        mAdapter.notifyDataSetChanged();

                        mList.setStackFromBottom(false);

                        //移动到最顶端
                        mList.smoothScrollToPosition(0);
                        //刷新 UI
                        mIsLoading = false;
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        RLog.e(MessageListFragment.this, "IPC:getConversationMessage", e.toString());
                    }
                });

                break;

            case NOTIFY_LIST:
                if (mAdapter != null)
                    mAdapter.notifyDataSetChanged();

                break;
            case DELETE_MESSAGE:
                mAdapter.notifyDataSetChanged();

                getHandler().post(new Runnable() {
                    @Override
                    public void run() {

                        if (mList.getCount() > 0) {

                            View firstView = mList.getChildAt(mList.getFirstVisiblePosition());
                            View lastView = mList.getChildAt(mList.getLastVisiblePosition());

                            if (firstView != null && lastView != null) {
                                int listViewPadding = mList.getListPaddingBottom() + mList.getListPaddingTop();
                                int childViewsHeight = lastView.getBottom() - (firstView.getTop() == -1 ? 0 : firstView.getTop());
                                int listViewHeight = mList.getBottom() - listViewPadding;

                                RLog.e(MessageListFragment.this, "handle-DELETE_MESSAGE", "firstView-top-height:" + firstView.getTop());
                                RLog.e(MessageListFragment.this, "handle-DELETE_MESSAGE", "lastView-bottom-height:" + lastView.getBottom());
                                RLog.e(MessageListFragment.this, "handle-DELETE_MESSAGE", "childViews-height:" + childViewsHeight);
                                RLog.e(MessageListFragment.this, "handle-DELETE_MESSAGE", "listView-height:" + listViewHeight);

                                if (childViewsHeight < listViewHeight) {
                                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                                    mList.setStackFromBottom(false);
                                } else {
                                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
                                }

                                mAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

                break;
        }
        return false;
    }


    private void resetListViewStack() {
        int count = mList.getChildCount();
        View firstView = mList.getChildAt(0);
        View lastView = mList.getChildAt(count - 1);

        if(firstView != null && lastView != null) {
            int listViewPadding = mList.getListPaddingBottom() + mList.getListPaddingTop();
            int childViewsHeight = lastView.getBottom() - (firstView.getTop() == -1 ? 0 : firstView.getTop());
            int listViewHeight = mList.getBottom() - listViewPadding;

            if (childViewsHeight < listViewHeight) {
                mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
                mList.setStackFromBottom(false);
            } else {
                mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            }
        }
    }

    public void onEventMainThread(final Event.ReadReceiptEvent event) {
        if (mConversation != null && mConversation.getTargetId().equals(event.getMessage().getTargetId()) && mConversation.getConversationType() == event.getMessage().getConversationType()) {
            if(event.getMessage().getConversationType() != Conversation.ConversationType.PRIVATE)
                return;

            ReadReceiptMessage content = (ReadReceiptMessage) event.getMessage().getContent();
            long ntfTime = content.getLastMessageSendTime();
            for (int i = mAdapter.getCount() - 1; i >= 0; i--) {
                if (mAdapter.getItem(i).getSentStatus() == io.rong.imlib.model.Message.SentStatus.READ) {
                    break;
                } else if (mAdapter.getItem(i).getSentStatus() == io.rong.imlib.model.Message.SentStatus.SENT) {
                    if ((mAdapter.getItem(i).getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.SEND))
                            && (ntfTime >= mAdapter.getItem(i).getSentTime())) {
                        mAdapter.getItem(i).setSentStatus(io.rong.imlib.model.Message.SentStatus.READ);
                        getHandler().obtainMessage(REFRESH_ITEM_READ_RECEIPT, i).sendToTarget();
                    }
                }
            }
        }
    }

    public void onEventMainThread(io.rong.imlib.model.Message msg) {
        UIMessage message = UIMessage.obtain(msg);
        boolean readRec = RongIMClient.getInstance().getReadReceipt();

        RLog.d(this, "onEventMainThread", "message : " + message.getMessageId() + " " + message.getSentStatus());

        if (mConversation != null && mConversation.getTargetId().equals(message.getTargetId()) && mConversation.getConversationType() == message.getConversationType()) {
            int position = mAdapter.findPosition(message.getMessageId());
            if(message.getMessageId() > 0) {
                io.rong.imlib.model.Message.ReceivedStatus status = message.getReceivedStatus();
                status.setRead();
                message.setReceivedStatus(status);
                RongIMClient.getInstance().setMessageReceivedStatus(msg.getMessageId(), status, null);
            }
            if (position == -1) {
                if(mMessageleft <= 0) {
                    if (readRec) {
                        //发送已读回执
                        if (message.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE) && message.getConversationType() == Conversation.ConversationType.PRIVATE) {
                            if (SystemUtils.isAppRunningOnTop(RongContext.getInstance(), RongContext.getInstance().getPackageName())) {
                                RongIMClient.getInstance().sendReadReceiptMessage(message.getConversationType(), message.getTargetId(), message.getSentTime());
                            }
                        }
                    }
                }
                mConversation.setSentTime(message.getSentTime());
                mConversation.setSenderUserId(message.getSenderUserId());

                mAdapter.add(message);
                mAdapter.notifyDataSetChanged();
                getHandler().obtainMessage(REFRESH_LIST, message).sendToTarget();
            } else {
                mAdapter.getItem(position).setSentStatus(message.getSentStatus());
                mAdapter.getItem(position).setExtra(message.getExtra());
                mAdapter.getItem(position).setSentTime(message.getSentTime());
                getHandler().obtainMessage(REFRESH_ITEM, position).sendToTarget();
            }
        }
    }

    public void onEventMainThread(Event.GroupUserInfoEvent event) {
        GroupUserInfo userInfo = event.getUserInfo();
        if (mList != null && isResumed()) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();
            int index = first - 1;

            while (++index <= last && index >= 0 && index < mAdapter.getCount()) {
                io.rong.imlib.model.Message message = mAdapter.getItem(index);
                if (message != null && (TextUtils.isEmpty(message.getSenderUserId()) || userInfo.getUserId().equals(message.getSenderUserId()))) {
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }
            }
        }
    }

     public void onEventMainThread(Event.OnMessageSendErrorEvent event) {
        io.rong.imlib.model.Message msg = event.getMessage();
        onEventMainThread(msg);
    }

    public void onEventMainThread(Event.OnReceiveMessageEvent event) {
        mMessageleft = event.getLeft();
        onEventMainThread(event.getMessage());
    }

    public void onEventMainThread(MessageContent messageContent) {

        if (mList != null && isResumed()) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();

            int index = first - 1;

            while (++index <= last && index >= 0 && index < mAdapter.getCount()) {
                if (mAdapter.getItem(index).getContent().equals(messageContent)) {
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                    break;
                }
            }
        }
    }

    public void onEventMainThread(Event.PlayAudioEvent event) {
        MessageContent messageContent = event.getContent();
        if (mList != null && isResumed()) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();
            int index = first;
            boolean continuously = false;
            UIMessage uiMessage;

            while (index <= last && index >= 0 && index < mAdapter.getCount()) {
                uiMessage = mAdapter.getItem(index);
                if (uiMessage.getContent().equals(messageContent)) {
                    mAdapter.getView(index, mList.getChildAt(index - first), mList);
                    if (uiMessage.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE) &&
                            !event.isListened()) {
                        continuously = true;
                    }
                }

                if(continuously) {
                    try {
                        continuously = RongContext.getInstance().getResources().getBoolean(R.bool.rc_play_audio_continuous);
                    } catch (Resources.NotFoundException e) {
                        RLog.e(this, "PlayAudioEvent", "rc_play_audio_continuous not configure in rc_config.xml");
                        e.printStackTrace();
                    }
                }

                index++;
                if(continuously && event.isFinished() && index <= last && index < mAdapter.getCount()) {
                    uiMessage = mAdapter.getItem(index);
                    if (uiMessage.getContent() instanceof VoiceMessage) {
                        if(uiMessage.getMessageDirection().equals(io.rong.imlib.model.Message.MessageDirection.RECEIVE) &&
                                !uiMessage.getReceivedStatus().isListened()) {
                            View view = mList.getChildAt(index - first);
                            if (view != null) {
                                mAdapter.getView(index, view, mList);
                                mAdapter.playNextAudioIfNeed(uiMessage, index);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void onEventMainThread(Event.OnReceiveMessageProgressEvent event) {

        if (mList != null && isResumed()) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();

            int index = first - 1;

            while (++index <= last && index >= 0 && index < mAdapter.getCount()) {
                UIMessage uiMessage = mAdapter.getItem(index);
                if (uiMessage.getMessageId() == event.getMessage().getMessageId()) {
                    uiMessage.setProgress(event.getProgress());
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);

                    if(event.getProgress()==1){
                        getHandler().sendEmptyMessage(RESET_LIST_STACK);
                    }
                    break;
                }
            }
        }
    }

    public void onEventMainThread(InputView.Event event) {
        if (mAdapter == null)
            return;

        if (event == InputView.Event.ACTION) {
            getHandler().sendEmptyMessage(RESET_LIST_STACK);
        }
    }

    public void onEventMainThread(UserInfo userInfo) {

        if (mList != null) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();

            int index = first - 1;

            while (++index <= last && index >= 0 && index < mAdapter.getCount()) {

                UIMessage uiMessage  = mAdapter.getItem(index);

                if (uiMessage != null && (TextUtils.isEmpty(uiMessage.getSenderUserId()) || userInfo.getUserId().equals(uiMessage.getSenderUserId()))) {
                    uiMessage.setUserInfo(userInfo);
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }
            }
        }

    }

    public void onEventMainThread(PublicServiceProfile publicServiceProfile) {

        if (mList != null && isResumed() && mAdapter != null) {
            int first = mList.getFirstVisiblePosition() - mList.getHeaderViewsCount();
            int last = mList.getLastVisiblePosition() - mList.getHeaderViewsCount();

            int index = first - 1;

            while (++index <= last && index >= 0 && index < mAdapter.getCount()) {

                io.rong.imlib.model.Message message = mAdapter.getItem(index);

                if (message != null && (TextUtils.isEmpty(message.getTargetId()) || publicServiceProfile.getTargetId().equals(message.getTargetId()))) {
                    mAdapter.getView(index, mList.getChildAt(index - mList.getFirstVisiblePosition() + mList.getHeaderViewsCount()), mList);
                }
            }
        }
    }

    private void getConversation() {
        RongIM.getInstance().getRongIMClient().getConversation(mConversation.getConversationType(), mConversation.getTargetId(), new RongIMClient.ResultCallback<Conversation>() {
            @Override
            public void onSuccess(Conversation conversation) {
                if (conversation != null) {
                    if (!TextUtils.isEmpty(mConversation.getConversationTitle()))
                        conversation.setConversationTitle(mConversation.getConversationTitle());
                    //拿到lib层有数据的 conversation
                    mConversation = conversation;
                    //获取当前 会话的 未读数

                    if (isShowUnreadMessageState
                            && conversation.getConversationType() != Conversation.ConversationType.APP_PUBLIC_SERVICE
                            && conversation.getConversationType() != Conversation.ConversationType.PUBLIC_SERVICE
                            && conversation.getConversationType() != Conversation.ConversationType.CUSTOMER_SERVICE
                            && conversation.getConversationType() != Conversation.ConversationType.CHATROOM) {
                        mUnreadCount = mConversation.getUnreadMessageCount();
                    }
                    if (mUnreadCount > 150) {
                        mUnreadBtn.setText("150+条新消息");
                    } else {
                        mUnreadBtn.setText(mUnreadCount + "条新消息");
                    }
                    //发送已读回执
                    //readRec 是否显示已读回执
                    boolean readRec = RongIMClient.getInstance().getReadReceipt();
                    if (readRec == true) {
                        if (conversation.getConversationType() == Conversation.ConversationType.PRIVATE) {
                            if(!(conversation.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())) || mUnreadCount > 0) {
                                RongIMClient.getInstance().sendReadReceiptMessage(conversation.getConversationType(), conversation.getTargetId(), conversation.getSentTime());
                            }
                        }
                    }
                    // 设置未读消息提示按钮的点击事件
                    mUnreadBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            isOnClickBtn = true;
                            mUnreadBtn.setClickable(false);
                            TranslateAnimation animation = new TranslateAnimation(0, 500, 0, 0);
                            animation.setDuration(500);
//                            mUnreadBtn.setVisibility(View.VISIBLE);
                            mUnreadBtn.startAnimation(animation);
                            animation.setFillAfter(true);
                            animation.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    mUnreadBtn.setVisibility(View.GONE);
                                    if (mUnreadCount <= 30) {
                                        //未读消息小于等于 30条
                                        mList.smoothScrollToPosition(31 - mUnreadCount);
                                    } else if (mUnreadCount >= 30) {
                                        mUnreadCount = 150;
                                        //拉取条目
                                        getHandler().sendEmptyMessage(REQ_UNREAD);
                                    }
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                        }
                    });
                }
                getHandler().sendEmptyMessage(REQ_LIST);
            }

            @Override
            public void onError(RongIMClient.ErrorCode e) {
                RLog.e(MessageListFragment.this, "fail", e.toString());
            }
        });
    }

    public void onEventMainThread(Event.ConnectEvent event){
        RLog.d(this, "onEventMainThread", "Event.ConnectEvent: isListRetrieved = " + isShowWithoutConnected);
        if(isShowWithoutConnected) {
            getConversation();
            RongIM.getInstance().getRongIMClient().clearMessagesUnreadStatus(mConversation.getConversationType(), mConversation.getTargetId(), null);
        } else {
            return;
        }
        isShowWithoutConnected = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        RongContext.getInstance().getEventBus().post(InputView.Event.DESTROY);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (RongIM.getInstance() == null || RongIM.getInstance().getRongIMClient() == null) {
            isShowWithoutConnected = true;
            RLog.e(this, "onResume", "Not connected yet.");
            return;
        }

        if (mList.getLastVisiblePosition() == mList.getCount() - 1) {
            mNewMessageCount = 0;
            mNewMessageTextView.setVisibility(View.GONE);
            mNewMessageBtn.setVisibility(View.GONE);
        }

        //发送已读回执
        //readRec 是否显示已读回执
        if (mConversation != null && mConversation.getSenderUserId() != null) {
            boolean readRec = RongIMClient.getInstance().getReadReceipt();
            if (readRec == true) {
                if (mConversation.getConversationType() == Conversation.ConversationType.PRIVATE
                        && !mConversation.getSenderUserId().equals(RongIMClient.getInstance().getCurrentUserId())) {
                    RongIMClient.getInstance().sendReadReceiptMessage(mConversation.getConversationType(), mConversation.getTargetId(), mConversation.getSentTime());
                }
            }
        }
    }

    public void onEventMainThread(Event.MessageDeleteEvent deleteEvent) {
        if (deleteEvent.getMessageIds() != null) {
            boolean hasChanged = false;
            int position = 0;

            for (long item : deleteEvent.getMessageIds()) {
                position = mAdapter.findPosition(item);
                if (position >= 0) {
                    mAdapter.remove(position);
                    hasChanged = true;
                }
            }

            if (hasChanged) {
//                getHandler().post(new Runnable() {
//                    @Override
//                    public void run() {
                mAdapter.notifyDataSetChanged();
                getHandler().obtainMessage(DELETE_MESSAGE).sendToTarget();
//
//                    }
//                });
            }
        }
    }

    public void onEventMainThread(Event.PublicServiceFollowableEvent event) {
        if (event != null && !event.isFollow()) {
            getActivity().finish();
        }
    }

    public void onEventMainThread(Event.MessagesClearEvent clearEvent) {
        if (clearEvent.getTargetId().equals(mConversation.getTargetId()) && clearEvent.getType().equals(mConversation.getConversationType())) {
            mAdapter.removeAll();
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    mList.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
                    mList.setStackFromBottom(false);
                    mAdapter.notifyDataSetChanged();
                }
            });
            mAdapter.notifyDataSetChanged();
        }
    }

    public class ScrollRunnable implements Runnable {

        @Override
        public void run() {
            if (mList.getLastVisiblePosition() < mList.getCount() - 1) {
//                mList.smoothScrollToPosition(mList.getCount()+1);
                mList.setSelection(mList.getLastVisiblePosition() + 10);
                getHandler().postDelayed(new ScrollRunnable(), 100);

            }
        }
    }


    public static class Builder {
        private Conversation.ConversationType conversationType;
        private String targetId;
        private Uri uri;

        public Conversation.ConversationType getConversationType() {
            return conversationType;
        }

        public void setConversationType(Conversation.ConversationType conversationType) {
            this.conversationType = conversationType;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }

    }

    @Override
    public void onDestroy() {
        if (mConversation != null &&
                RongIM.getInstance() != null &&
                RongIM.getInstance().getRongIMClient() != null) {
            RongIM.getInstance().getRongIMClient().clearMessagesUnreadStatus(mConversation.getConversationType(), mConversation.getTargetId(), null);
        }
        RongContext.getInstance().getEventBus().unregister(this);
        super.onDestroy();
    }

    public void setAdapter(MessageListAdapter adapter) {
        if(mAdapter != null)
            mAdapter.clear();
        mAdapter = adapter;
        if(mList != null && getUri() != null) {
            mList.setAdapter(adapter);
            initFragment(getUri());
        }
    }

    public MessageListAdapter getAdapter() {
        return mAdapter;
    }
}
