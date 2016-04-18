package io.rong.imkit;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;

import com.sea_monster.exception.BaseException;
import com.sea_monster.network.AbstractHttpRequest;
import com.sea_monster.network.StoreStatusCallback;
import com.sea_monster.resource.ResCallback;
import com.sea_monster.resource.Resource;
import com.sea_monster.resource.ResourceHandler;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.notification.MessageNotificationManager;
import io.rong.imkit.utils.CommonUtils;
import io.rong.imkit.utils.SystemUtils;
import io.rong.imlib.AnnotationNotFoundException;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.PublicServiceProfileList;
import io.rong.imlib.model.UserData;
import io.rong.imlib.model.UserInfo;
import io.rong.message.InformationNotificationMessage;

/**
 * IM 客户端核心类。
 * <p/>
 * 所有 IM 相关方法、监听器都由此调用和设置。
 */
public class RongIMClientWrapper extends RongIMClient {

    RongIMClient mClient;
    RongContext mContext;

    static ConnectionStatusListener sConnectionStatusListener;
    static OnReceiveMessageListener sMessageListener;
    static RongIM.OnSelectMemberListener sMemberSelectListener;
    static RongIMClientWrapper sS;
    List<String> mRegCache;


    private RongIMClientWrapper() {
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param context 应用上下文。
     */
    public static void init(Context context, RongContext rongContext) {
        RongIMClient.init(context);

        String processName = SystemUtils.getCurProcessName(context);
        String mainProcessName = context.getPackageName();

        if (!mainProcessName.equals(processName))
            return;

        sS = new RongIMClientWrapper();
        sS.mContext = rongContext;
        sS.mRegCache = new ArrayList<>();
    }

    /**
     * 初始化 SDK，在整个应用程序全局，只需要调用一次。
     *
     * @param appKey  应用的app key。
     * @param context 应用上下文。
     */
    public static void init(Context context, RongContext rongContext, String appKey) {
        RongIMClient.init(context, appKey);

        String processName = SystemUtils.getCurProcessName(context);
        String mainProcessName = context.getPackageName();

        if (!mainProcessName.equals(processName))
            return;

        sS = new RongIMClientWrapper();
        sS.mContext = rongContext;
        sS.mRegCache = new ArrayList<>();

        rongContext.saveAppKey(appKey);
    }

    /**
     * 连接服务器，在整个应用程序全局，只需要调用一次。
     *
     * @param token    从服务端获取的用户身份令牌（Token）。
     * @param callback 连接回调。
     * @return RongIMClientWrapper 实例。
     */
    public static RongIMClientWrapper connect(String token, final ConnectCallback callback) {

        RongIMClient client = RongIMClient.connect(token, new ConnectCallback() {
            @Override
            public void onSuccess(String userId) {
                if (callback != null) {
                    callback.onSuccess(userId);
                }
                RongContext.getInstance().getEventBus().post(Event.ConnectEvent.obtain(true));
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null) {
                    callback.onError(e);
                }

                RongContext.getInstance().getEventBus().post(Event.ConnectEvent.obtain(false));
            }

            @Override
            public void onTokenIncorrect() {
                if (callback != null)
                    callback.onTokenIncorrect();
            }
        });

        if (client != null) {
            initConnectedData(client);
        }
        sS.mClient = client;
        RLog.i(client, "RongIMClientWrapper", "client:" + client.toString());

        return sS;
    }

    private static final void initConnectedData(RongIMClient client) {
        RLog.i(client, "RongIMClientWrapper", "initConnectedData");

        RongIMClient.setOnReceiveMessageListener(new OnReceiveMessageListener() {
            @Override
            public boolean onReceived(final Message message, final int left) {
                boolean isProcess = false;

                if (sMessageListener != null)
                    isProcess = sMessageListener.onReceived(message, left); //首先透传给用户处理。

                final MessageTag msgTag = message.getContent().getClass().getAnnotation(MessageTag.class);
                //如果该条消息是计数的或者存到历史记录的，则post到相应界面显示或响铃，否则直接返回（VoIP消息除外）。
                if (msgTag != null && (msgTag.flag() == MessageTag.ISCOUNTED || msgTag.flag() == MessageTag.ISPERSISTED)) {
                    sS.mContext.getEventBus().post(new Event.OnReceiveMessageEvent(message, left));

                    //如果消息中附带了用户信息，则通知界面刷新此用户信息。
                    if (message.getContent() != null && message.getContent().getUserInfo() != null) {
                        CommonUtils.refreshUserInfoIfNeed(sS.mContext, message.getContent().getUserInfo());
                    }

                    //如果用户自己处理铃声和后台通知，或者是web端自己发送的消息，则直接返回。
                    if (isProcess || message.getSenderUserId().equals(RongIM.getInstance().getRongIMClient().getCurrentUserId())) {
                        return true;
                    }

                    MessageNotificationManager.getInstance().notifyIfNeed(sS.mContext, message, left);
                } else {
                    if (message.getConversationType() == Conversation.ConversationType.PRIVATE) {
                        String className = ((Object) message.getContent()).getClass().getName();
                        if (className.equals("io.rong.voipkit.message.VoIPCallMessage") || className.equals("io.rong.voipkit.message.VoIPAcceptMessage") || className.equals("io.rong.voipkit.message.VoIPFinishMessage")) {
                            sS.mContext.getEventBus().post(new Event.OnReceiveVoIPMessageEvent(message, left));
                        }
                    }
                }

                return false;
            }
        });

        //消息回执监听
        if(RongIMClient.getInstance().getReadReceipt()) {
            RongIMClient.setReadReceiptListener(new ReadReceiptListener() {
                @Override
                public void onReadReceiptReceived(Message message) {
                    sS.mContext.getEventBus().post(new Event.ReadReceiptEvent(message));
                }
            });
        }

        RongIMClient.setConnectionStatusListener(mConnectionStatusListener);
    }


    private static ConnectionStatusListener mConnectionStatusListener = new ConnectionStatusListener() {

        @Override
        public void onChanged(ConnectionStatus status) {
            if(status == null) {
                Log.i("RongIMClientWrapper","onChanged. The status is null, return directly!");
                return;
            }
            RLog.d(this, "RongIMClientWrapper : ConnectStatus", status.toString());

            sS.mContext.getEventBus().post(status);

            if (sConnectionStatusListener != null)
                sConnectionStatusListener.onChanged(status);
        }
    };

    /**
     * 设置连接状态变化的监听器。
     *
     * @param listener 连接状态变化的监听器。
     */
    public static void setConnectionStatusListener(final ConnectionStatusListener listener) {
        sConnectionStatusListener = listener;
    }

    /**
     * 注册消息类型，如果对消息类型进行扩展，可以忽略此方法。
     *
     * @param type 消息类型，必须要继承自 {@link MessageContent}
     * @throws AnnotationNotFoundException 如果没有找到注解时抛出。
     */
    public static void registerMessageType(Class<? extends MessageContent> type) throws AnnotationNotFoundException {
        RongIMClient.registerMessageType(type);
    }

    /**
     * 获取连接状态。
     *
     * @return 连接状态枚举。
     */
    @Override
    public ConnectionStatusListener.ConnectionStatus getCurrentConnectionStatus() {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getCurrentConnectionStatus();
    }

    /**
     * 重新连接服务器。
     *
     * @param callback 连接回调。
     */
    @Override
    public void reconnect(ConnectCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.reconnect(callback);
    }

    /**
     * 断开连接(默认断开后接收Push消息)。
     */
    @Override
    public void disconnect() {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        if (mClient != null)
            mClient.disconnect();


    }

    /**
     * 断开连接(默认断开后接收Push消息)。
     *
     * @param isReceivePush 断开后是否接收push。
     */
    @Deprecated
    @Override
    public void disconnect(boolean isReceivePush) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.disconnect(isReceivePush);
    }

    /**
     * 注销登录(不再接收 Push 消息)。
     */
    public void logout() {

        disconnect(false);

        if (RongContext.getInstance() != null && RongContext.getInstance().getMessageCounterLogic() != null)
            RongContext.getInstance().getMessageCounterLogic().clearCache();
    }

    /**
     * 设置接收消息的监听器。
     * <p/>
     * 所有接收到的消息、通知、状态都经由此处设置的监听器处理。包括私聊消息、讨论组消息、群组消息、聊天室消息以及各种状态。
     *
     * @param listener 接收消息的监听器。
     */
    public static void setOnReceiveMessageListener(OnReceiveMessageListener listener) {
        RLog.i(listener, "RongIMClientWrapper", "setOnReceiveMessageListener");
        sMessageListener = listener;
    }

    /**
     * 设置push通知的监听函数。
     *
     * @param listener, push通知的监听函数。
     */
    public static void setOnReceivePushMessageListener(OnReceivePushMessageListener listener) {
        RongIMClient.setOnReceivePushMessageListener(listener);
    }

    @Override
    public void getConversationList(ResultCallback<List<Conversation>> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getConversationList(callback);
    }

    /**
     * 获取对应对会话列表。
     *
     * @return 会话列表。
     * @see Conversation。
     */
    @Override
    public List<Conversation> getConversationList() {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getConversationList();
    }

    /**
     * 根据会话类型，回调方式获取会话列表。
     *
     * @param callback 获取会话列表的回调。
     * @param types    会话类型。
     */
    @Override
    public void getConversationList(ResultCallback<List<Conversation>> callback, Conversation.ConversationType... types) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getConversationList(callback, types);
    }


    /**
     * 根据会话类型，获取会话列表。
     *
     * @param types 会话类型。
     * @return 返回会话列表。
     */
    @Override
    public List<Conversation> getConversationList(Conversation.ConversationType... types) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getConversationList(types);
    }

    /**
     * 根据不同会话类型的目标Id，回调方式获取某一会话信息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 获取会话信息的回调。
     */
    @Override
    public void getConversation(Conversation.ConversationType type, String targetId, ResultCallback<Conversation> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getConversation(type, targetId, callback);
    }

    /**
     * 获取某一会话信息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @return 会话信息。
     */
    @Override
    public Conversation getConversation(Conversation.ConversationType type, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getConversation(type, targetId);
    }

    /**
     * 从会话列表中移除某一会话，但是不删除会话内的消息。
     * <p/>
     * 如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback 移除会话是否成功的回调。
     */
    @Override
    public void removeConversation(final Conversation.ConversationType type, final String targetId, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.removeConversation(type, targetId, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {

                if (callback != null)
                    callback.onSuccess(bool);

                if (bool)
                    mContext.getEventBus().post(new Event.ConversationRemoveEvent(type, targetId));
            }

            @Override
            public void onError(ErrorCode e) {

                if (callback != null)
                    callback.onFail(e);
            }
        });
    }

    /**
     * 从会话列表中移除某一会话，但是不删除会话内的消息。
     * <p/>
     * 如果此会话中有新的消息，该会话将重新在会话列表中显示，并显示最近的历史消息。
     *
     * @param type     会话类型。
     * @param targetId 目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @return 是否移除成功。
     */
    @Override
    public boolean removeConversation(Conversation.ConversationType type, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        boolean result = mClient.removeConversation(type, targetId);

        if (result)
            mContext.getEventBus().post(new Event.ConversationRemoveEvent(type, targetId));

        return result;
    }

    /**
     * 设置某一会话为置顶或者取消置顶，回调方式获取设置是否成功。
     *
     * @param type     会话类型。
     * @param id       目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop    是否置顶。
     * @param callback 设置置顶或取消置顶是否成功的回调。
     */
    @Override
    public void setConversationToTop(final Conversation.ConversationType type, final String id, final boolean isTop, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setConversationToTop(type, id, isTop, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool)
                    mContext.getEventBus().post(new Event.ConversationTopEvent(type, id, isTop));
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onFail(e);
            }
        });
    }

    /**
     * 设置某一会话为置顶或者取消置顶。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param isTop            是否置顶。
     * @return 是否设置成功。
     */
    @Override
    public boolean setConversationToTop(Conversation.ConversationType conversationType, String targetId, boolean isTop) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        boolean result = mClient.setConversationToTop(conversationType, targetId, isTop);

        if (result) {
            mContext.getEventBus().post(new Event.ConversationTopEvent(conversationType, targetId, isTop));
        }

        return result;
    }

    /**
     * 通过回调方式，获取所有未读消息数。
     *
     * @param callback 消息数的回调。
     */
    @Override
    public void getTotalUnreadCount(final ResultCallback<Integer> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getTotalUnreadCount(new ResultCallback<Integer>() {
            @Override
            public void onSuccess(Integer integer) {
                if (callback != null)
                    callback.onSuccess(integer);
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onFail(e);
            }
        });
    }

    /**
     * 获取所有未读消息数。
     *
     * @return 未读消息数。
     */
    @Override
    public int getTotalUnreadCount() {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getTotalUnreadCount();
    }

    /**
     * 根据会话类型的目标 Id,回调方式获取来自某用户（某会话）的未读消息数。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         未读消息数的回调
     */
    @Override
    public void getUnreadCount(Conversation.ConversationType conversationType, String targetId, ResultCallback<Integer> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getUnreadCount(conversationType, targetId, callback);
    }

    /**
     * 获取来自某用户（某会话）的未读消息数。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @return 未读消息数。
     */
    @Override
    public int getUnreadCount(Conversation.ConversationType conversationType, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getUnreadCount(conversationType, targetId);
    }


    /**
     * 回调方式获取某会话类型的未读消息数。
     *
     * @param callback          未读消息数的回调。
     * @param conversationTypes 会话类型。
     */
    @Override
    public void getUnreadCount(ResultCallback<Integer> callback, Conversation.ConversationType... conversationTypes) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getUnreadCount(callback, conversationTypes);
    }

    /**
     * 根据会话类型数组，回调方式获取某会话类型的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @return 未读消息数的回调。
     */
    @Override
    public int getUnreadCount(Conversation.ConversationType... conversationTypes) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getUnreadCount(conversationTypes);
    }

    /**
     * 根据会话类型数组，回调方式获取某会话类型的未读消息数。
     *
     * @param conversationTypes 会话类型。
     * @param callback          未读消息数的回调。
     */
    @Override
    public void getUnreadCount(Conversation.ConversationType[] conversationTypes, ResultCallback<Integer> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getUnreadCount(conversationTypes, callback);
    }

    /**
     * 获取最新消息记录。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。
     * @param count            要获取的消息数量。
     * @return 最新消息记录，按照时间顺序从新到旧排列。
     */
    @Override
    public List<Message> getLatestMessages(Conversation.ConversationType conversationType, String targetId, int count) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getLatestMessages(conversationType, targetId, count);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取最新的 N 条消息记录。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param count            要获取的消息数量。
     * @param callback         获取最新消息记录的回调，按照时间顺序从新到旧排列。
     */
    @Override
    public void getLatestMessages(Conversation.ConversationType conversationType, String targetId, int count, ResultCallback<List<Message>> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getLatestMessages(conversationType, targetId, count, callback);
    }

    /**
     * 获取历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息，没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @return 历史消息记录，按照时间顺序从新到旧排列。
     */
    @Override
    public List<Message> getHistoryMessages(Conversation.ConversationType conversationType, String targetId, int oldestMessageId, int count) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getHistoryMessages(conversationType, targetId, oldestMessageId, count);
    }

    /**
     * 获取历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param objectName       消息类型标识。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @return 历史消息记录，按照时间顺序从新到旧排列。
     */
    @Override
    public List<Message> getHistoryMessages(Conversation.ConversationType conversationType, String targetId, String objectName, int oldestMessageId, int count) {
        return mClient.getHistoryMessages(conversationType, targetId, objectName, oldestMessageId, count);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取某消息类型标识的N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param objectName       消息类型标识。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息,没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    @Override
    public void getHistoryMessages(Conversation.ConversationType conversationType, String targetId, String objectName, int oldestMessageId, int count, ResultCallback<List<Message>> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getHistoryMessages(conversationType, targetId, objectName, oldestMessageId, count, callback);
    }

    /**
     * 根据会话类型的目标 Id，回调方式获取N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param oldestMessageId  最后一条消息的 Id，获取此消息之前的 count 条消息，没有消息第一次调用应设置为:-1。
     * @param count            要获取的消息数量。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    @Override
    public void getHistoryMessages(Conversation.ConversationType conversationType, String targetId, int oldestMessageId, int count, ResultCallback<List<Message>> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getHistoryMessages(conversationType, targetId, oldestMessageId, count, callback);
    }


    /**
     * 根据会话类型的目标 Id，回调方式获取N条历史消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param dataTime         从该时间点开始获取消息。即：消息中的 sendTime；第一次可传 0，获取最新 count 条。
     * @param count            要获取的消息数量，最多 20 条。
     * @param callback         获取历史消息记录的回调，按照时间顺序从新到旧排列。
     */
    @Override
    public void getRemoteHistoryMessages(Conversation.ConversationType conversationType, String targetId, long dataTime, int count, ResultCallback<List<Message>> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getRemoteHistoryMessages(conversationType, targetId, dataTime, count, callback);
    }

    /**
     * 删除指定的一条或者一组消息。
     *
     * @param messageIds 要删除的消息 Id 数组。
     * @return 是否删除成功。
     */
    @Override
    public boolean deleteMessages(final int[] messageIds) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        Boolean bool = mClient.deleteMessages(messageIds);

        if (bool)
            mContext.getEventBus().post(new Event.MessageDeleteEvent(messageIds));

        return bool;
    }

    /**
     * 删除指定的一条或者一组消息，回调方式获取是否删除成功。
     *
     * @param messageIds 要删除的消息 Id 数组。
     * @param callback   是否删除成功的回调。
     */
    @Override
    public void deleteMessages(final int[] messageIds, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.deleteMessages(messageIds, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool)
                    mContext.getEventBus().post(new Event.MessageDeleteEvent(messageIds));

                if (callback != null)
                    callback.onCallback(bool);
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 清空某一会话的所有聊天消息记录。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @return 是否清空成功。
     */
    @Override
    public boolean clearMessages(Conversation.ConversationType conversationType, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        boolean bool = mClient.clearMessages(conversationType, targetId);

        if (bool)
            mContext.getEventBus().post(new Event.MessagesClearEvent(conversationType, targetId));

        return bool;
    }

    /**
     * 根据会话类型，清空某一会话的所有聊天消息记录,回调方式获取清空是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清空是否成功的回调。
     */
    @Override
    public void clearMessages(final Conversation.ConversationType conversationType, final String targetId, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.clearMessages(conversationType, targetId, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (bool)
                    mContext.getEventBus().post(new Event.MessagesClearEvent(conversationType, targetId));

                if (callback != null)
                    callback.onCallback(bool);
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 清除消息未读状态。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @return 是否清空成功。
     */
    @Override
    public boolean clearMessagesUnreadStatus(Conversation.ConversationType conversationType, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        boolean result = mClient.clearMessagesUnreadStatus(conversationType, targetId);
        RLog.d(this, "clearMessagesUnreadStatus", "result :" + result);
        if (result) {
            mContext.getEventBus().post(new Event.ConversationUnreadEvent(conversationType, targetId));
        }

        return result;
    }

    /**
     * 根据会话类型，清除目标 Id 的消息未读状态，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。不支持传入 ConversationType.CHATROOM。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         清除是否成功的回调。
     */
    @Override
    public void clearMessagesUnreadStatus(final Conversation.ConversationType conversationType, final String targetId, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        RLog.d(this, "clearMessagesUnreadStatus", "result :");

        mClient.clearMessagesUnreadStatus(conversationType, targetId, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);
                mContext.getEventBus().post(new Event.ConversationUnreadEvent(conversationType, targetId));
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onError(e);
            }
        });
    }

    /**
     * 设置消息的附加信息，此信息只保存在本地。
     *
     * @param messageId 消息 Id。
     * @param value     消息附加信息，最大 1024 字节。
     * @return 是否设置成功。
     */
    @Override
    public boolean setMessageExtra(int messageId, String value) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.setMessageExtra(messageId, value);
    }

    /**
     * 设置消息的附加信息，此信息只保存在本地，回调方式获取设置是否成功。
     *
     * @param messageId 消息 Id。
     * @param value     消息附加信息，最大 1024 字节。
     * @param callback  是否设置成功的回调。
     */
    @Override
    public void setMessageExtra(int messageId, String value, ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setMessageExtra(messageId, value, callback);
    }

    /**
     * 设置接收到的消息状态。
     *
     * @param messageId      消息 Id。
     * @param receivedStatus 接收到的消息状态。
     * @return 是否设置成功。
     */
    @Override
    public boolean setMessageReceivedStatus(int messageId, Message.ReceivedStatus receivedStatus) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.setMessageReceivedStatus(messageId, receivedStatus);
    }

    /**
     * 根据消息 Id，设置接收到的消息状态，回调方式获取设置是否成功。
     *
     * @param messageId      消息 Id。
     * @param receivedStatus 接收到的消息状态。
     * @param callback       是否设置成功的回调。
     */
    @Override
    public void setMessageReceivedStatus(int messageId, Message.ReceivedStatus receivedStatus, ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setMessageReceivedStatus(messageId, receivedStatus, callback);
    }

    /**
     * 设置发送的消息状态。
     *
     * @param messageId  消息 Id。
     * @param sentStatus 发送的消息状态。
     * @return 是否设置成功。
     */
    @Override
    public boolean setMessageSentStatus(int messageId, Message.SentStatus sentStatus) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        boolean result = mClient.setMessageSentStatus(messageId, sentStatus);

        if (result)
            mContext.getEventBus().post(new Event.MessageSentStatusEvent(messageId, sentStatus));

        return result;
    }

    /**
     * 根据消息 Id，设置发送的消息状态，回调方式获取设置是否成功。
     *
     * @param messageId  消息 Id。
     * @param sentStatus 发送的消息状态。
     * @param callback   是否设置成功的回调。
     */
    @Override
    public void setMessageSentStatus(final int messageId, final Message.SentStatus sentStatus, final ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setMessageSentStatus(messageId, sentStatus, new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean bool) {
                if (callback != null)
                    callback.onSuccess(bool);

                if (bool)
                    mContext.getEventBus().post(new Event.MessageSentStatusEvent(messageId, sentStatus));
            }

            @Override
            public void onError(ErrorCode e) {
                if (callback != null)
                    callback.onError(e);


            }
        });
    }

    /**
     * 获取某一会话的文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @return 草稿的文字内容。
     */
    @Override
    public String getTextMessageDraft(Conversation.ConversationType conversationType, String targetId) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getTextMessageDraft(conversationType, targetId);
    }

    /**
     * 保存文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content          草稿的文字内容。
     * @return 是否保存成功。
     */
    @Override
    public boolean saveTextMessageDraft(Conversation.ConversationType conversationType, String targetId, String content) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.saveTextMessageDraft(conversationType, targetId, content);
    }

    /**
     * 清除某一会话的文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @return 是否清除成功。
     */
    @Override
    public boolean clearTextMessageDraft(Conversation.ConversationType conversationType, String targetId) {
        return mClient.clearTextMessageDraft(conversationType, targetId);
    }

    /**
     * 根据会话类型，获取某一会话的文字消息草稿。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         获取草稿文字内容的回调。
     */
    @Override
    public void getTextMessageDraft(Conversation.ConversationType conversationType, String targetId, ResultCallback<String> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 保存文字消息草稿，回调方式获取保存是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content          草稿的文字内容。
     * @param callback         是否保存成功的回调。
     */
    @Override
    public void saveTextMessageDraft(Conversation.ConversationType conversationType, String targetId, String content, ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.saveTextMessageDraft(conversationType, targetId, content, callback);
    }

    /**
     * 清除某一会话的文字消息草稿，回调方式获取清除是否成功。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param callback         是否清除成功的回调。
     */
    @Override
    public void clearTextMessageDraft(Conversation.ConversationType conversationType, String targetId, ResultCallback<Boolean> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.clearTextMessageDraft(conversationType, targetId, callback);
    }

    /**
     * 获取讨论组信息和设置。
     *
     * @param discussionId 讨论组 Id。
     * @param callback     获取讨论组的回调。
     */
    @Override
    public void getDiscussion(String discussionId, ResultCallback<Discussion> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getDiscussion(discussionId, callback);
    }

    /**
     * 设置讨论组名称。
     *
     * @param discussionId 讨论组 Id。
     * @param name         讨论组名称。
     * @param callback     设置讨论组的回调。
     */
    @Override
    public void setDiscussionName(final String discussionId, final String name, final OperationCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setDiscussionName(discussionId, name, new OperationCallback() {

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }

            @Override
            public void onSuccess() {
                if (callback != null) {
                    mContext.getEventBus().post(new Discussion(discussionId, name));
                    callback.onSuccess();
                }
            }
        });
    }

    /**
     * 创建讨论组。
     *
     * @param name       讨论组名称，如：当前所有成员的名字的组合。
     * @param userIdList 讨论组成员 Id 列表。
     * @param callback   创建讨论组成功后的回调。
     */
    @Override
    public void createDiscussion(final String name, final List<String> userIdList, final CreateDiscussionCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.createDiscussion(name, userIdList, new CreateDiscussionCallback() {
            @Override
            public void onSuccess(String discussionId) {
                mContext.getEventBus().post(new Event.CreateDiscussionEvent(discussionId, name, userIdList));

                if (callback != null)
                    callback.onCallback(discussionId);
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 添加一名或者一组用户加入讨论组。
     *
     * @param discussionId 讨论组 Id。
     * @param userIdList   邀请的用户 Id 列表。
     * @param callback     执行操作的回调。
     */
    @Override
    public void addMemberToDiscussion(final String discussionId, final List<String> userIdList, final OperationCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.addMemberToDiscussion(discussionId, userIdList, new OperationCallback() {
            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.AddMemberToDiscussionEvent(discussionId, userIdList));

                if (callback != null)
                    callback.onSuccess();

            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 供创建者将某用户移出讨论组。
     * <p/>
     * 移出自己或者调用者非讨论组创建者将产生
     * {@link ErrorCode#UNKNOWN}
     * 错误。
     *
     * @param discussionId 讨论组 Id。
     * @param userId       用户 Id。
     * @param callback     执行操作的回调。
     */
    @Override
    public void removeMemberFromDiscussion(final String discussionId, final String userId, final OperationCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.removeMemberFromDiscussion(discussionId, userId, new OperationCallback() {
            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.RemoveMemberFromDiscussionEvent(discussionId, userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 退出当前用户所在的某讨论组。
     *
     * @param discussionId 讨论组 Id。
     * @param callback     执行操作的回调。
     */
    @Override
    public void quitDiscussion(final String discussionId, final OperationCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.quitDiscussion(discussionId, new OperationCallback() {
            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.QuitDiscussionEvent(discussionId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 模拟消息，向本地目标 Id 中插入一条消息
     *
     * @param type         会话类型。
     * @param targetId     目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param senderUserId 发送用户 Id。
     * @param content      消息内容。
     * @param callback     获得消息发送实体的回调。
     */
    @Override
    public void insertMessage(Conversation.ConversationType type, String targetId, String senderUserId, MessageContent content, final ResultCallback<Message> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");


        final MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {

            mClient.insertMessage(type, targetId, senderUserId, content, new ResultCallback<Message>() {
                @Override
                public void onSuccess(Message message) {

                    if (callback != null)
                        callback.onSuccess(message);

                    mContext.getEventBus().post(message);
                }

                @Override
                public void onError(ErrorCode e) {

                    if (callback != null)
                        callback.onError(e);

                    mContext.getEventBus().post(e);
                }
            });
        } else {
            RLog.e(this, "insertMessage", "Message is missing MessageTag.ISPERSISTED");
        }
    }

    /**
     * 模拟消息。
     *
     * @param type         会话类型。
     * @param targetId     目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param senderUserId 发送用户 Id。
     * @param content      消息内容。
     * @return
     */
    @Deprecated
    public Message insertMessage(Conversation.ConversationType type, String targetId, String senderUserId, MessageContent content) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");


        MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

        Message message = null;

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
            message = mClient.insertMessage(type, targetId, senderUserId, content);
        } else {
            message = Message.obtain(targetId, type, content);
            RLog.e(this, "insertMessage", "Message is missing MessageTag.ISPERSISTED");
        }

        mContext.getEventBus().post(message);

        return message;
    }

    /**
     * 发送消息。
     *
     * @param type        会话类型。
     * @param targetId    目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content     消息内容。
     * @param pushContent push 内容，为空时不 push 信息。
     * @param pushData    push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback    发送消息的回调。
     * @return
     */
    @Deprecated
    public Message sendMessage(Conversation.ConversationType type, String targetId, MessageContent content, String pushContent, String pushData, final SendMessageCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        final ResultCallback.Result<Message> result = new ResultCallback.Result<>();
        final Conversation.ConversationType conversationType = type;
        final String id = targetId;

        Message messageTemp = Message.obtain(targetId, type, content);

        Message temp = filterSendMessage(messageTemp);
        if(temp == null)
            return null;

        if (temp != messageTemp)
            messageTemp = temp;

        content = messageTemp.getContent();

        content = setMessageAttachedUserInfo(content);

        final Message message = mClient.sendMessage(type, targetId, content, pushContent, pushData, new SendMessageCallback() {
            @Override
            public void onSuccess(Integer messageId) {
                result.t.setSentStatus(Message.SentStatus.SENT);
                long tt = sS.mClient.getSendTimeByMessageId(messageId);
                if (tt != 0) {
                    result.t.setSentTime(tt);
                }
                filterSentMessage(result.t, null);
//                mContext.getEventBus().post(result.t);

                if (callback != null)
                    callback.onSuccess(messageId);
            }

            @Override
            public void onError(Integer messageId, ErrorCode errorCode) {
                result.t.setSentStatus(Message.SentStatus.FAILED);
                filterSentMessage(result.t, errorCode);
//                mContext.getEventBus().post(new Event.OnMessageSendErrorEvent(result.t, errorCode));

                if (callback != null)
                    callback.onError(messageId, errorCode);
            }
        });

        MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
            mContext.getEventBus().post(message);
        }
        result.t = message;

        return message;
    }


    /**
     * 根据会话类型，发送消息。
     *
     * @param type           会话类型。
     * @param targetId       目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content        消息内容。
     * @param pushContent    push 内容，为空时不 push 信息。
     * @param pushData       push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback       发送消息的回调。
     * @param resultCallback 获取发送消息实体的回调。
     */
    @Override
    public void sendMessage(Conversation.ConversationType type, String targetId, MessageContent
            content, String pushContent, final String pushData, final SendMessageCallback callback,
                            final ResultCallback<Message> resultCallback) {
        final ResultCallback.Result<Message> result = new ResultCallback.Result<>();
        final Conversation.ConversationType conversationType = type;
        final String id = targetId;

        Message message = Message.obtain(targetId, type, content);

        Message temp = filterSendMessage(message);
        if(temp == null)
            return;

        if (temp != message)
            message = temp;

        content = message.getContent();

        content = setMessageAttachedUserInfo(content);
        mClient.sendMessage(type, targetId, content, pushContent, pushData, new SendMessageCallback() {
            @Override
            public void onSuccess(Integer messageId) {
                if (result.t == null)
                    return;
                result.t.setSentStatus(Message.SentStatus.SENT);
                long tt = sS.mClient.getSendTimeByMessageId(messageId);
                if (tt != 0) {
                    result.t.setSentTime(tt);
                }
                filterSentMessage(result.t, null);

//                mContext.getEventBus().post(result.t);

                if (callback != null)
                    callback.onSuccess(messageId);
            }

            @Override
            public void onError(Integer messageId, ErrorCode errorCode) {
                result.t.setSentStatus(Message.SentStatus.FAILED);
                filterSentMessage(result.t, errorCode);

//                mContext.getEventBus().post(new Event.OnMessageSendErrorEvent(result.t, errorCode));

                if (callback != null)
                    callback.onError(messageId, errorCode);

            }
        }, new ResultCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);

                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    mContext.getEventBus().post(message);
                }

                result.t = message;

                if (resultCallback != null)
                    resultCallback.onSuccess(message);
            }

            @Override
            public void onError(ErrorCode e) {
                mContext.getEventBus().post(e);

                if (resultCallback != null)
                    resultCallback.onError(e);
            }
        });
    }

    /**
     * 发送消息。
     *
     * @param message        发送消息的实体。
     * @param pushContent    push 内容，为空时不 push 信息。
     * @param pushData       push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback       发送消息的回调。
     * @param resultCallback 获取发送消息实体的回调。
     */
    @Override
    public void sendMessage(Message message, String pushContent, final String pushData,
                            final SendMessageCallback callback, final ResultCallback<Message> resultCallback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        final ResultCallback.Result<Message> result = new ResultCallback.Result<>();

        final Message temp = filterSendMessage(message);
        if(temp == null)
            return;

        if (temp != message)
            message = temp;

        message.setContent(setMessageAttachedUserInfo(message.getContent()));

        mClient.sendMessage(message, pushContent, pushData, new SendMessageCallback() {
            @Override
            public void onSuccess(Integer messageId) {
                if (result.t == null)
                    return;
                result.t.setSentStatus(Message.SentStatus.SENT);
                long tt = sS.mClient.getSendTimeByMessageId(messageId);
                if (tt != 0) {
                    result.t.setSentTime(tt);
                }
                filterSentMessage(result.t, null);
//                mContext.getEventBus().post(result.t);
                if (callback != null)
                    callback.onSuccess(messageId);
            }

            @Override
            public void onError(Integer messageId, ErrorCode errorCode) {

                if (result.t == null)
                    return;

                result.t.setSentStatus(Message.SentStatus.FAILED);
                filterSentMessage(result.t, errorCode);

//                mContext.getEventBus().post(result.t);
//                mContext.getEventBus().post(new Event.OnMessageSendErrorEvent(result.t, errorCode));
                if (callback != null)
                    callback.onError(messageId, errorCode);
            }
        }, new ResultCallback<Message>() {
            @Override
            public void onSuccess(Message message) {
                result.t = message;
                MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);

                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    mContext.getEventBus().post(message);
                }

                if (resultCallback != null)
                    resultCallback.onSuccess(message);
            }

            @Override
            public void onError(ErrorCode e) {
                mContext.getEventBus().post(e);
                if (resultCallback != null)
                    resultCallback.onError(e);
            }
        });
    }

    /**
     * 发送消息，返回发送的消息实体。
     *
     * @param message     发送消息的实体。
     * @param pushContent push 内容，为空时不 push 信息。
     * @param pushData    push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback    发送消息的回调。
     * @return 发送的消息实体。
     */
    @Override
    public Message sendMessage(Message message, String pushContent, final String pushData,
                               final SendMessageCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        final ResultCallback.Result<Message> result = new ResultCallback.Result<>();

        final Message temp = filterSendMessage(message);

        if(temp == null)
            return null;

        if (temp != message)
            message = temp;

        message.setContent(setMessageAttachedUserInfo(message.getContent()));

        Message msg = mClient.sendMessage(message, pushContent, pushData, new SendMessageCallback() {
            @Override
            public void onSuccess(Integer messageId) {
                if (result.t == null)
                    return;
                result.t.setSentStatus(Message.SentStatus.SENT);
                long tt = sS.mClient.getSendTimeByMessageId(messageId);
                if (tt != 0) {
                    result.t.setSentTime(tt);
                }
                filterSentMessage(result.t, null);
//                mContext.getEventBus().post(result.t);
                if (callback != null)
                    callback.onSuccess(messageId);
            }

            @Override
            public void onError(Integer messageId, ErrorCode errorCode) {

                result.t.setSentStatus(Message.SentStatus.FAILED);
                filterSentMessage(result.t, errorCode);

//                mContext.getEventBus().post(new Event.OnMessageSendErrorEvent(result.t, errorCode));

                if (callback != null)
                    callback.onError(messageId, errorCode);
            }
        });

        MessageTag tag = message.getContent().getClass().getAnnotation(MessageTag.class);

        if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
            EventBus.getDefault().post(msg);
        }
        result.t = msg;

        return msg;
    }

    /**
     * 发送图片消息。
     *
     * @param type        会话类型。
     * @param targetId    目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param content     消息内容。
     * @param pushContent push 内容，为空时不 push 信息。
     * @param pushData    push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback    发送消息的回调。
     */
    @Override
    public void sendImageMessage(Conversation.ConversationType type, String
            targetId, MessageContent content, String pushContent, String pushData,
                                 final SendImageMessageCallback callback) {

        Message message = Message.obtain(targetId, type, content);

        Message temp = filterSendMessage(message);
        if(temp == null)
            return;

        if (temp != message)
            message = temp;

        content = message.getContent();

        content = setMessageAttachedUserInfo(content);

        final ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        SendImageMessageCallback sendMessageCallback = new SendImageMessageCallback() {

            @Override
            public void onAttached(Message message) {

                mContext.getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onProgress(Message message, int progress) {

                result.t.setMessage(message);
                result.t.setProgress(progress);
                mContext.getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        mClient.sendImageMessage(type, targetId, content, pushContent, pushData, sendMessageCallback);
    }

    /**
     * 发送图片消息。
     *
     * @param message     发送消息的实体。
     * @param pushContent push 内容，为空时不 push 信息。
     * @param pushData    push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback    发送消息的回调。
     */
    @Override
    public void sendImageMessage(Message message, String pushContent,
                                 final String pushData, final SendImageMessageCallback callback) {

        Message temp = filterSendMessage(message);

        if(temp == null)
            return;

        if (temp != message)
            message = temp;

        setMessageAttachedUserInfo(message.getContent());

        final ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();

        SendImageMessageCallback sendMessageCallback = new SendImageMessageCallback() {

            @Override
            public void onAttached(Message message) {
                mContext.getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message);
            }

            @Override
            public void onProgress(Message message, int progress) {

                result.t.setMessage(message);
                result.t.setProgress(progress);
                mContext.getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        mClient.sendImageMessage(message, pushContent, pushData, sendMessageCallback);
    }

    /**
     * 发送图片消息，可以使用该方法将图片上传到自己的服务器发送，同时更新图片状态。
     * 该方法适用于使用者自己上传图片，并通过 listener 将上传进度更新在UI上显示。
     *
     * @param message     发送消息的实体。
     * @param pushContent push 内容，为空时不 push 信息。
     * @param pushData    push 附加信息，开发者根据自已的需要设置 pushData。
     * @param callback    发送消息的回调，该回调携带 listener 对象，使用者可以调用其方法，更新图片上传进度。
     */
    @Override
    public void sendImageMessage(Message message, String pushContent,
                                 final String pushData,
                                 final SendImageMessageWithUploadListenerCallback callback) {

        Message temp = filterSendMessage(message);

        if(temp == null)
            return;

        if (temp != message)
            message = temp;

        final ResultCallback.Result<Event.OnReceiveMessageProgressEvent> result = new ResultCallback.Result<>();
        result.t = new Event.OnReceiveMessageProgressEvent();
        
        SendImageMessageWithUploadListenerCallback sendMessageCallback = new SendImageMessageWithUploadListenerCallback() {

            @Override
            public void onAttached(Message message, uploadImageStatusListener listener) {
                mContext.getEventBus().post(message);

                if (callback != null)
                    callback.onAttached(message, listener);
            }

            @Override
            public void onProgress(Message message, int progress) {

                result.t.setMessage(message);
                result.t.setProgress(progress);
                mContext.getEventBus().post(result.t);

                if (callback != null)
                    callback.onProgress(message, progress);
            }

            @Override
            public void onError(Message message, ErrorCode errorCode) {

                filterSentMessage(message, errorCode);

                if (callback != null)
                    callback.onError(message, errorCode);
            }

            @Override
            public void onSuccess(Message message) {

                filterSentMessage(message, null);

                if (callback != null)
                    callback.onSuccess(message);
            }
        };

        mClient.sendImageMessage(message, pushContent, pushData, sendMessageCallback);
    }

    private MessageContent setMessageAttachedUserInfo(MessageContent content) {

        if (RongContext.getInstance().getUserInfoAttachedState()) {

            if (content.getUserInfo() == null) {
                String userId = RongIM.getInstance().getRongIMClient().getCurrentUserId();

                UserInfo info = RongContext.getInstance().getCurrentUserInfo();

                if (info == null)
                    info = RongContext.getInstance().getUserInfoFromCache(userId);

                if (info != null)
                    content.setUserInfo(info);
            }
        }

        return content;
    }

    /**
     * 对 UI 已发送消息进行过虑。
     *
     * @param message
     * @return
     */
    private Message filterSendMessage(Message message) {

        if (RongContext.getInstance().getOnSendMessageListener() != null) {
            message = RongContext.getInstance().getOnSendMessageListener().onSend(message);
        }

        return message;
    }

    private void filterSentMessage(Message message, ErrorCode errorCode) {

        RongIM.SentMessageErrorCode sentMessageErrorCode = null;
        boolean isExecute = false;

        if (RongContext.getInstance().getOnSendMessageListener() != null) {

            if (errorCode != null) {
                sentMessageErrorCode = RongIM.SentMessageErrorCode.setValue(errorCode.getValue());
            }

            isExecute = RongContext.getInstance().getOnSendMessageListener().onSent(message, sentMessageErrorCode);
        }

        if (errorCode != null && !isExecute) {

            if (errorCode.equals(ErrorCode.NOT_IN_DISCUSSION) || errorCode.equals(ErrorCode.NOT_IN_GROUP)
                    || errorCode.equals(ErrorCode.NOT_IN_CHATROOM) || errorCode.equals(ErrorCode.REJECTED_BY_BLACKLIST)||errorCode.equals(ErrorCode.FORBIDDEN_IN_GROUP)
                    || errorCode.equals(ErrorCode.FORBIDDEN_IN_CHATROOM) || errorCode.equals(ErrorCode.KICKED_FROM_CHATROOM)) {

                InformationNotificationMessage informationMessage = null;

                if (errorCode.equals(ErrorCode.NOT_IN_DISCUSSION)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_discussion));
                } else if (errorCode.equals(ErrorCode.NOT_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_group));
                } else if (errorCode.equals(ErrorCode.NOT_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_not_in_chatroom));
                } else if (errorCode.equals(ErrorCode.REJECTED_BY_BLACKLIST)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_rejected_by_blacklist_prompt));
                } else if (errorCode.equals(ErrorCode.FORBIDDEN_IN_GROUP)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_info_forbidden_to_talk));
                } else if (errorCode.equals(ErrorCode.FORBIDDEN_IN_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_forbidden_in_chatroom));
                } else if (errorCode.equals(ErrorCode.KICKED_FROM_CHATROOM)) {
                    informationMessage = InformationNotificationMessage.obtain(mContext.getString(R.string.rc_kicked_from_chatroom));
                }

                insertMessage(message.getConversationType(), message.getTargetId(), "rong", informationMessage, new ResultCallback<Message>() {
                    @Override
                    public void onSuccess(Message message) {
                        mContext.getEventBus().post(message);
                    }

                    @Override
                    public void onError(ErrorCode e) {

                    }
                });
            }

            MessageContent content = message.getContent();
            MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

            if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                mContext.getEventBus().post(new Event.OnMessageSendErrorEvent(message, errorCode));
            }

        } else {//发消息成功 onSuccess()或onProgress()
            if (message != null) {
                MessageContent content = message.getContent();

                MessageTag tag = content.getClass().getAnnotation(MessageTag.class);

                if (tag != null && (tag.flag() & MessageTag.ISPERSISTED) == MessageTag.ISPERSISTED) {
                    mContext.getEventBus().post(message);
                }
            }
        }
    }

    /**
     * 对UI已发送消息进行过虑。
     *
     * @param conversationType
     * @param targetId
     * @param messageContent
     * @return
     */
    private Message filterSendMessage(Conversation.ConversationType conversationType, String targetId, MessageContent messageContent) {
        Message message = new Message();
        message.setConversationType(conversationType);
        message.setTargetId(targetId);
        message.setContent(messageContent);

        if (RongContext.getInstance().getOnSendMessageListener() != null) {
            message = RongContext.getInstance().getOnSendMessageListener().onSend(message);
        }

        return message;
    }

    /**
     * 上传媒体文件。
     * <p/>
     * 请使用 public void uploadMedia(final Conversation.ConversationType conversationType, final String targetId, Uri uri, final UploadMediaCallback callback) 代替。
     * 上传文件。
     * 用来实现自定义消息时，上传消息中的文件内容到服务器。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param stream           文件的流。
     * @param callback         上传文件的回调。
     */
    @Override
    public void uploadMedia(Conversation.ConversationType conversationType, String targetId, InputStream stream, UploadMediaCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.uploadMedia(conversationType, targetId, stream, callback);
    }

    /**
     * 下载文件。
     * <p/>
     * 用来获取媒体原文件时调用。如果本地缓存中包含此文件，则从本地缓存中直接获取，否则将从服务器端下载。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param mediaType        文件类型。
     * @param imageUrl         文件的 URL 地址。
     * @param callback         下载文件的回调。
     */
    @Override
    public void downloadMedia(Conversation.ConversationType conversationType, String targetId, MediaType mediaType, String imageUrl, final DownloadMediaCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.downloadMedia(conversationType, targetId, mediaType, imageUrl, callback);
    }

    /**
     * 下载文件。
     *
     * @param imageUrl 文件的 URL 地址。
     * @param callback 下载文件的回调。
     */
    public void downloadMedia(String imageUrl, final DownloadMediaCallback callback) {

        try {
            ResourceHandler.getInstance().requestResource(new Resource(imageUrl), new ResCallback() {

                @Override
                public void onComplete(AbstractHttpRequest<File> abstractHttpRequest, File file) {
                    if (callback != null) {
                        callback.onSuccess(Uri.fromFile(file).toString());
                    }
                }

                @Override
                public void onFailure(AbstractHttpRequest<File> abstractHttpRequest, BaseException e) {
                    io.rong.common.RLog.e(RongIMClientWrapper.this, "downloadMedia", e.toString());
                }

            }, new StoreStatusCallback() {

                @Override
                public void statusCallback(StoreStatus storeStatus) {
                    if (callback != null) {
                        callback.onProgress((int) storeStatus.getPercent());
                    }
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取会话消息提醒状态。
     *
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param callback         获取状态的回调。
     */
    @Override
    public void getConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final ResultCallback<Conversation.ConversationNotificationStatus> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getConversationNotificationStatus(conversationType, targetId, new ResultCallback<Conversation.ConversationNotificationStatus>() {
            @Override
            public void onSuccess(Conversation.ConversationNotificationStatus status) {

                RongContext.getInstance().setConversationNotifyStatusToCache(ConversationKey.obtain(targetId, conversationType), status);

                if (callback != null) {
                    callback.onSuccess(status);
                }
            }

            @Override
            public void onError(ErrorCode e) {

                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 设置会话消息提醒状态。
     *
     * @param conversationType   会话类型。
     * @param targetId           目标 Id。根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id。
     * @param notificationStatus 是否屏蔽。
     * @param callback           设置状态的回调。
     */
    @Override
    public void setConversationNotificationStatus(final Conversation.ConversationType conversationType, final String targetId, final Conversation.ConversationNotificationStatus notificationStatus, final ResultCallback<Conversation.ConversationNotificationStatus> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setConversationNotificationStatus(conversationType, targetId, notificationStatus, new ResultCallback<Conversation.ConversationNotificationStatus>() {
            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }

            @Override
            public void onSuccess(Conversation.ConversationNotificationStatus status) {
                mContext.getEventBus().post(new Event.ConversationNotificationEvent(targetId, conversationType, notificationStatus));
                RongContext.getInstance().setConversationNotifyStatusToCache(ConversationKey.obtain(targetId, conversationType), status);

                if (callback != null)
                    callback.onSuccess(status);
            }
        });
    }

    /**
     * 设置讨论组成员邀请权限。
     *
     * @param discussionId 讨论组 id。
     * @param status       邀请状态，默认为开放。
     * @param callback     设置权限的回调。
     */
    @Override

    public void setDiscussionInviteStatus(final String discussionId, final DiscussionInviteStatus status, final OperationCallback callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setDiscussionInviteStatus(discussionId, status, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.DiscussionInviteStatusEvent(discussionId, status));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 同步当前用户的群组信息。
     * Warning: 已废弃，请勿使用。
     * 此方法已废弃，建议您通过您的App Server进行群组操作。 群组操作的流程，可以参考：http://support.rongcloud.cn/kb/MzY5
     *
     * @param groups   需要同步的群组实体。
     * @param callback 同步状态的回调。
     */
    @Deprecated
    @Override
    public void syncGroup(final List<Group> groups, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.syncGroup(groups, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.SyncGroupEvent(groups));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 加入群组。
     * Warning: 已废弃，请勿使用。
     * 此方法已废弃，建议您通过您的App Server进行群组操作。 群组操作的流程，可以参考：http://support.rongcloud.cn/kb/MzY5
     *
     * @param groupId   群组 Id。
     * @param groupName 群组名称。
     * @param callback  加入群组状态的回调。
     */
    @Deprecated
    @Override
    public void joinGroup(final String groupId, final String groupName, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.joinGroup(groupId, groupName, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.JoinGroupEvent(groupId, groupName));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 退出群组。
     * Warning: 已废弃，请勿使用。
     * 此方法已废弃，建议您通过您的App Server进行群组操作。 群组操作的流程，可以参考：http://support.rongcloud.cn/kb/MzY5
     *
     * @param groupId  群组 Id。
     * @param callback 退出群组状态的回调。
     */
    @Deprecated
    @Override
    public void quitGroup(final String groupId, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.quitGroup(groupId, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.QuitGroupEvent(groupId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 获取当前连接用户的信息。
     *
     * @return 当前连接用户的信息。
     */
    @Override
    public String getCurrentUserId() {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getCurrentUserId();
    }

    /**
     * 获取本地时间与服务器时间的差值。
     *
     * @return 本地时间与服务器时间的差值。
     */
    @Override
    public long getDeltaTime() {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.getDeltaTime();
    }

    /**
     * 加入聊天室。
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，为 -1 时不拉取任何消息，默认拉取 10 条消息。
     * @param callback        状态回调。
     */
    @Override
    public void joinChatRoom(final String chatroomId, final int defMessageCount, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.joinChatRoom(chatroomId, defMessageCount, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.JoinChatRoomEvent(chatroomId, defMessageCount));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 加入已存在的聊天室。
     *
     * @param chatroomId      聊天室 Id。
     * @param defMessageCount 进入聊天室拉取消息数目，为 -1 时不拉取任何消息，默认拉取 10 条消息。
     * @param callback        状态回调。
     */
    @Override
    public void joinExistChatRoom(final String chatroomId, final int defMessageCount, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.joinExistChatRoom(chatroomId, defMessageCount, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.JoinChatRoomEvent(chatroomId, defMessageCount));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 退出聊天室。
     *
     * @param chatroomId 聊天室 Id。
     * @param callback   状态回调。
     */
    @Override
    public void quitChatRoom(final String chatroomId, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.quitChatRoom(chatroomId, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.QuitChatRoomEvent(chatroomId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 清空所有会话及会话消息，回调方式通知是否清空成功。
     *
     * @param callback          是否清空成功的回调。
     * @param conversationTypes 会话类型。
     */
    @Override
    public void clearConversations(ResultCallback callback, Conversation.ConversationType... conversationTypes) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.clearConversations(callback, conversationTypes);
    }

    /**
     * 清空所有会话及会话消息。
     *
     * @param conversationTypes 会话类型。
     * @return 是否清空成功。
     */
    @Override
    public boolean clearConversations(Conversation.ConversationType... conversationTypes) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        return mClient.clearConversations(conversationTypes);
    }

    /**
     * 将某个用户加到黑名单中。
     *
     * @param userId   用户 Id。
     * @param callback 加到黑名单回调。
     */
    @Override
    public void addToBlacklist(final String userId, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.addToBlacklist(userId, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.AddToBlacklistEvent(userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 将个某用户从黑名单中移出。
     *
     * @param userId   用户 Id。
     * @param callback 移除黑名单回调。
     */
    @Override
    public void removeFromBlacklist(final String userId, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");


        mClient.removeFromBlacklist(userId, new OperationCallback() {

            @Override
            public void onSuccess() {
                mContext.getEventBus().post(new Event.RemoveFromBlacklistEvent(userId));

                if (callback != null)
                    callback.onSuccess();
            }

            @Override
            public void onError(ErrorCode errorCode) {

                if (callback != null)
                    callback.onError(errorCode);
            }
        });
    }

    /**
     * 获取某用户是否在黑名单中。
     *
     * @param userId   用户 Id。
     * @param callback 获取用户是否在黑名单回调。
     */
    @Override
    public void getBlacklistStatus(String userId, ResultCallback<BlacklistStatus> callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getBlacklistStatus(userId, callback);
    }

    /**
     * 获取当前用户的黑名单列表。
     *
     * @param callback 获取黑名单回调。
     */
    @Override
    public void getBlacklist(GetBlacklistCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getBlacklist(callback);
    }

    /**
     * 设置会话通知免打扰时间。
     *
     * @param startTime   起始时间 格式 HH:MM:SS。
     * @param spanMinutes 间隔分钟数 0 < spanMinutes < 1440。
     * @param callback    设置会话通知免打扰时间回调。
     */
    @Override
    public void setNotificationQuietHours(final String startTime, final int spanMinutes, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.setNotificationQuietHours(startTime, spanMinutes, new OperationCallback() {
            @Override
            public void onSuccess() {

                CommonUtils.saveNotificationQuietHours(mContext, startTime, spanMinutes);

                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 移除会话通知免打扰时间。
     *
     * @param callback 移除会话通知免打扰时间回调。
     */
    @Override
    public void removeNotificationQuietHours(final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.removeNotificationQuietHours(new OperationCallback() {
            @Override
            public void onSuccess() {
                CommonUtils.saveNotificationQuietHours(mContext, "-1", -1);

                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 获取会话通知免打扰时间。
     *
     * @param callback 获取会话通知免打扰时间回调。
     */
    @Override
    public void getNotificationQuietHours(final GetNotificationQuietHoursCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getNotificationQuietHours(new GetNotificationQuietHoursCallback() {
            @Override
            public void onSuccess(String startTime, int spanMinutes) {
                CommonUtils.saveNotificationQuietHours(mContext, startTime, spanMinutes);

                if (callback != null) {
                    callback.onSuccess(startTime, spanMinutes);
                }
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }

    /**
     * 获取公众服务信息。
     *
     * @param publicServiceType 会话类型，APP_PUBLIC_SERVICE 或者 PUBLIC_SERVICE。
     * @param publicServiceId   公众服务 Id。
     * @param callback          获取公众号信息回调。
     */
    @Override
    public void getPublicServiceProfile(Conversation.PublicServiceType publicServiceType, String publicServiceId, ResultCallback<PublicServiceProfile> callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getPublicServiceProfile(publicServiceType, publicServiceId, callback);
    }

    /**
     * 搜索公众服务。
     *
     * @param searchType 搜索类型枚举。
     * @param keywords   搜索关键字。
     * @param callback   搜索结果回调。
     */
    @Override
    public void searchPublicService(SearchType searchType, String keywords, ResultCallback<PublicServiceProfileList> callback) {

        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.searchPublicService(searchType, keywords, callback);
    }

    /**
     * 按公众服务类型搜索公众服务。
     *
     * @param publicServiceType 公众服务类型。
     * @param searchType        搜索类型枚举。
     * @param keywords          搜索关键字。
     * @param callback          搜索结果回调。
     */
    public void searchPublicServiceByType(Conversation.PublicServiceType publicServiceType, SearchType searchType, final String keywords, final ResultCallback<PublicServiceProfileList> callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.searchPublicServiceByType(publicServiceType, searchType, keywords, callback);
    }

    /**
     * 订阅公众号。
     *
     * @param publicServiceId   公共服务 Id。
     * @param publicServiceType 公众服务类型枚举。
     * @param callback          订阅公众号回调。
     */
    @Override
    public void subscribePublicService(Conversation.PublicServiceType publicServiceType, String publicServiceId, OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.subscribePublicService(publicServiceType, publicServiceId, callback);
    }

    /**
     * 取消订阅公众号。
     *
     * @param publicServiceId   公共服务 Id。
     * @param publicServiceType 公众服务类型枚举。
     * @param callback          取消订阅公众号回调。
     */
    @Override
    public void unsubscribePublicService(Conversation.PublicServiceType publicServiceType, String publicServiceId, OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.unsubscribePublicService(publicServiceType, publicServiceId, callback);
    }

    /**
     * 获取己关注公共账号列表。
     *
     * @param callback 获取己关注公共账号列表回调。
     */
    @Override
    public void getPublicServiceList(ResultCallback<PublicServiceProfileList> callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.getPublicServiceList(callback);
    }


    /**
     * 设置用户信息。
     *
     * @param userData 用户信息。
     * @param callback 设置用户信息回调。
     */
    public void syncUserData(final UserData userData, final OperationCallback callback) {
        if (mClient == null)
            throw new RuntimeException("服务尚未连接!");

        mClient.syncUserData(userData, new OperationCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onError(ErrorCode errorCode) {
                if (callback != null) {
                    callback.onError(errorCode);
                }
            }
        });
    }
}
