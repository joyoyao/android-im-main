package io.rong.imkit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.util.List;

import io.rong.common.Build;
import io.rong.imkit.model.ConversationTypeFilter;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.notification.MessageCounter;
import io.rong.imkit.utils.StringUtils;
import io.rong.imkit.utils.SystemUtils;
import io.rong.imkit.widget.provider.DiscussionNotificationMessageItemProvider;
import io.rong.imkit.widget.provider.HandshakeMessageItemProvider;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imkit.widget.provider.ImageMessageItemProvider;
import io.rong.imkit.widget.provider.InfoNotificationMsgItemProvider;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imkit.widget.provider.LocationMessageItemProvider;
import io.rong.imkit.widget.provider.PublicServiceMultiRichContentMessageProvider;
import io.rong.imkit.widget.provider.PublicServiceRichContentMessageProvider;
import io.rong.imkit.widget.provider.RichContentMessageItemProvider;
import io.rong.imkit.widget.provider.TextMessageItemProvider;
import io.rong.imkit.widget.provider.UnknownMessageItemProvider;
import io.rong.imkit.widget.provider.VoiceMessageItemProvider;
import io.rong.imlib.AnnotationNotFoundException;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Discussion;
import io.rong.imlib.model.Group;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HandshakeMessage;
import io.rong.message.LocationMessage;

/**
 * IM 界面组件核心类。
 * <p/>
 * 所有 IM 相关界面、功能都由此调用和设置。
 */
public class RongIM {

    private static final String TAG = RongIM.class.getSimpleName();

    private static RongIM sRongIM;
    private static Context mContext;
    private static String mCurrentProcessName;
    private static String mMainProcessName;
    private RongIMClientWrapper mClientWrapper;


    private RongIM() {

    }

    private static void saveToken(String token) {
        SharedPreferences preferences = mContext.getSharedPreferences("rc_token", Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString("token_value", token);
        editor.commit();// 提交数据到背后的xml文件中
    }


    /**
     * 初始化 SDK。
     *
     * @param context 应用上下文。
     */
    public static void init(Context context) {
        mContext = context;

        if (sRongIM == null)
            sRongIM = new RongIM();

        RongContext.init(context);

        RongIMClientWrapper.init(context, RongContext.getInstance());

        if (mCurrentProcessName == null) {
            mCurrentProcessName = SystemUtils.getCurProcessName(context);
        }

        if (mMainProcessName == null)
            mMainProcessName = context.getPackageName();

        if (!mMainProcessName.equals(mCurrentProcessName))
            return;

        RLog.d(mContext, "init", "before init");

        if (Build.SDK_WITH_VOIP) {
            try {
                registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPAcceptMessage"));
                registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPCallMessage"));
                registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPFinishMessage"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        registerMessageTemplate(new TextMessageItemProvider());
        registerMessageTemplate(new ImageMessageItemProvider());
        registerMessageTemplate(new LocationMessageItemProvider());
        registerMessageTemplate(new VoiceMessageItemProvider(context));
        registerMessageTemplate(new DiscussionNotificationMessageItemProvider());
        registerMessageTemplate(new InfoNotificationMsgItemProvider());
        registerMessageTemplate(new RichContentMessageItemProvider());
        registerMessageTemplate(new PublicServiceMultiRichContentMessageProvider());
        registerMessageTemplate(new PublicServiceRichContentMessageProvider());
        registerMessageTemplate(new HandshakeMessageItemProvider());
        registerMessageTemplate(new UnknownMessageItemProvider());
    }

    /**
     * 初始化 SDK。
     *
     * @param appKey  应用的app key.
     * @param context 应用上下文。
     */
    public static void init(Context context, String appKey) {
        mContext = context;

        if (sRongIM == null)
            sRongIM = new RongIM();

        RongContext.init(context);

        RongIMClientWrapper.init(context, RongContext.getInstance(), appKey);

//        RongContext.getInstance().saveAppKey(appKey);

        if (mCurrentProcessName == null) {
            mCurrentProcessName = SystemUtils.getCurProcessName(context);
        }

        if (mMainProcessName == null)
            mMainProcessName = context.getPackageName();

        if (!mMainProcessName.equals(mCurrentProcessName))
            return;

        RLog.d(mContext, "init", "before init");
        if (Build.SDK_WITH_VOIP) {
        try {
            registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPAcceptMessage"));
            registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPCallMessage"));
            registerMessageType((Class<? extends MessageContent>) Class.forName("io.rong.voipkit.message.VoIPFinishMessage"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        }

        registerMessageTemplate(new TextMessageItemProvider());
        registerMessageTemplate(new ImageMessageItemProvider());
        registerMessageTemplate(new LocationMessageItemProvider());
        registerMessageTemplate(new VoiceMessageItemProvider(context));
        registerMessageTemplate(new DiscussionNotificationMessageItemProvider());
        registerMessageTemplate(new InfoNotificationMsgItemProvider());
        registerMessageTemplate(new RichContentMessageItemProvider());
        registerMessageTemplate(new PublicServiceMultiRichContentMessageProvider());
        registerMessageTemplate(new PublicServiceRichContentMessageProvider());
        registerMessageTemplate(new HandshakeMessageItemProvider());
        registerMessageTemplate(new UnknownMessageItemProvider());
    }

    /**
     * 注册消息类型，如果不对消息类型进行扩展，可以忽略此方法。
     *
     * @param messageContentClass 消息类型，必须要继承自 io.rong.imlib.model.MessageContent。
     */
    public static void registerMessageType(Class<? extends MessageContent> messageContentClass) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerMessageType", "RongCloud SDK not init");
            return;
        }

        try {
            RongIMClientWrapper.registerMessageType(messageContentClass);
        } catch (AnnotationNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 注册消息模板。
     *
     * @param provider 模板类型。
     */
    public static void registerMessageTemplate(IContainerItemProvider.MessageProvider provider) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerMessageTemplate", "RongCloud SDK not init");
            return;
        }

        RongContext.getInstance().registerMessageTemplate(provider);
    }

    /**
     * 设置当前用户信息，
     *
     * @param userInfo
     */
    public void setCurrentUserInfo(UserInfo userInfo) {

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerMessageTemplate", "RongCloud SDK not init");
            return;
        }

        RongContext.getInstance().setCurrentUserInfo(userInfo);
    }


    /**
     * IM 界面组件登录。
     *
     * @param token    从服务端获取的 <a
     *                 href="http://docs.rongcloud.cn/android#token">用户身份令牌（
     *                 Token）</a>。
     * @param callback 登录回调。
     * @return IM 界面组件。
     */
    public static RongIM connect(String token, final RongIMClient.ConnectCallback callback) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return null;
        }

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        saveToken(token);

        sRongIM.mClientWrapper = RongIMClientWrapper.connect(token, callback);

        return sRongIM;
    }

    /**
     * 设置接收消息的监听器。
     * <p/>
     * 所有接收到的消息、通知、状态都经由此处设置的监听器处理。包括私聊消息、讨论组消息、群组消息、聊天室消息以及各种状态。
     *
     * @param listener 接收消息的监听器。
     */
    public static void setOnReceiveMessageListener(RongIMClient.OnReceiveMessageListener listener) {
        RongIMClientWrapper.setOnReceiveMessageListener(listener);
    }

    /**
     * 获取 RongIMClientWrapper 实例，获取后可调用IM客户端核心类 IMLib 的基础功能。
     *
     * @return RongIMClientWrapper 实例。
     */
    public RongIMClientWrapper getRongIMClient() {

        return mClientWrapper;
    }

    /**
     * 断开连接或注销当前登录。
     *
     * @param isReceivePush 断开后是否接收 push。
     */
    @Deprecated
    public void disconnect(boolean isReceivePush) {

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        if (getRongIMClient() == null) {
            RLog.d(this, "disconnect", "RongIMClient does not init.");
            return;
        }

        getRongIMClient().disconnect(isReceivePush);
    }

    /**
     * 注销当前登录，执行该方法后不会再收到 push 消息。
     */
    public void logout() {

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        if (getRongIMClient() == null) {
            RLog.d(this, "disconnect", "RongIMClient does not init.");
            return;
        }
        getRongIMClient().disconnect(false);
    }

    /**
     * 位置信息的提供者，实现后获取用户位置信息。
     */
    public static interface LocationProvider {
        public void onStartLocation(Context context, LocationCallback callback);

        public interface LocationCallback {
            public void onSuccess(LocationMessage message);

            public void onFailure(String msg);
        }
    }

    /**
     * 设置位置信息的提供者。
     *
     * @param locationProvider 位置信息提供者。
     */
    public static void setLocationProvider(LocationProvider locationProvider) {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().setLocationProvider(locationProvider);
    }

    /**
     * 断开连接(默认断开后接收 Push 消息)。
     */
    public void disconnect() {
        disconnect(true);
    }

    /**
     * 获取界面组件的核心类单例。
     *
     * @return 界面组件的核心类单例。
     */
    public static RongIM getInstance() {
        return sRongIM;
    }

    /**
     * 启动会话列表界面。
     *
     * @param context 应用上下文。
     */
    public void startConversationList(Context context) {

        if (context == null)
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversationlist").build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * 启动聚合后的某类型的会话列表。<br> 例如：如果设置了单聊会话为聚合，则通过该方法可以打开包含所有的单聊会话的列表。
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     */
    public void startSubConversationList(Context context, Conversation.ConversationType conversationType) {

        if (context == null)
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("subconversationlist")
                .appendQueryParameter("type", conversationType.getName())
                .build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * 设置会话界面操作的监听器。
     *
     * @param listener 会话界面操作的监听器。
     */
    public static void setConversationBehaviorListener(ConversationBehaviorListener listener) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }
        if(RongContext.getInstance() == null){
            Log.e("RongIM", "RongCloud hasn't been init!!!!");
            return;
        }
        RongContext.getInstance().setConversationBehaviorListener(listener);
    }


    /**
     * 设置会话列表界面操作的监听器。
     *
     * @param listener 会话列表界面操作的监听器。
     */
    public static void setConversationListBehaviorListener(ConversationListBehaviorListener listener) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        RongContext.getInstance().setConversationListBehaviorListener(listener);
    }

    /**
     * 设置公众号界面操作的监听器。
     *
     * @param listener 会话公众号界面操作的监听器。
     */
    public static void setPublicServiceBehaviorListener(PublicServiceBehaviorListener listener) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        RongContext.getInstance().setPublicServiceBehaviorListener(listener);
    }

    /**
     * 公众号界面操作的监听器
     */
    public static interface PublicServiceBehaviorListener {
        /**
         * 当点击关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击取消关注后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onUnFollowClick(Context context, PublicServiceProfile info);

        /**
         * 当点击进入进入会话后执行。
         *
         * @param context 上下文。
         * @param info    公众号信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onEnterConversationClick(Context context, PublicServiceProfile info);
    }


    /**
     * 启动单聊界面。
     *
     * @param context      应用上下文。
     * @param targetUserId 要与之聊天的用户 Id。
     * @param title        聊天的标题，如果传入空值，则默认显示与之聊天的用户名称。
     */
    public void startPrivateChat(Context context, String targetUserId, String title) {

        if (context == null || TextUtils.isEmpty(targetUserId))
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.PRIVATE.getName().toLowerCase())
                .appendQueryParameter("targetId", targetUserId).appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * 启动会话界面。
     *
     * @param context          应用上下文。
     * @param conversationType 开启会话类型。
     * @param targetId         目标 Id；根据不同的 conversationType，可能是用户 Id、讨论组 Id、群组 Id 或聊天室 Id。
     * @param title            聊天的标题，如果传入空值，则默认显示会话的名称。
     */
    public void startConversation(Context context, Conversation.ConversationType conversationType, String targetId, String title) {

        if (context == null || TextUtils.isEmpty(targetId) || conversationType == null)
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().processName).buildUpon()
                .appendPath("conversation").appendPath(conversationType.getName().toLowerCase())
                .appendQueryParameter("targetId", targetId).appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }


    /**
     * 创建讨论组会话并进入会话界面。
     *
     * @param context       应用上下文。
     * @param targetUserIds 要与之聊天的讨论组用户 Id 列表。
     * @param title         聊天的标题，如果传入空值，则默认显示与之聊天的用户名称。
     */
    public void createDiscussionChat(Context context, List<String> targetUserIds, String title) {

        if (context == null || targetUserIds == null || targetUserIds.size() == 0)
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase())
                .appendQueryParameter("targetIds", TextUtils.join(",", targetUserIds)).appendQueryParameter("delimiter", ",")
                .appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * 启动讨论组聊天界面。
     *
     * @param context            应用上下文。
     * @param targetDiscussionId 要聊天的讨论组 Id。
     * @param title              聊天的标题，如果传入空值，则默认显示讨论组名称。
     */
    public void startDiscussionChat(Context context, String targetDiscussionId, String title) {

        if (context == null || TextUtils.isEmpty(targetDiscussionId)) {
            throw new IllegalArgumentException();
        }

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.DISCUSSION.getName().toLowerCase())
                .appendQueryParameter("targetId", targetDiscussionId).appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /**
     * 启动群组聊天界面。
     *
     * @param context       应用上下文。
     * @param targetGroupId 要聊天的群组 Id。
     * @param title         聊天的标题，如果传入空值，则默认显示群组名称。
     */
    public void startGroupChat(Context context, String targetGroupId, String title) {

        if (context == null || TextUtils.isEmpty(targetGroupId))
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.GROUP.getName().toLowerCase())
                .appendQueryParameter("targetId", targetGroupId).appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }


    /**
     * 启动客户服聊天界面。
     *
     * @param context               应用上下文。
     * @param customerServiceUserId 要与之聊天的客服 Id。
     * @param title                 聊天的标题，如果传入空值，则默认显示与之聊天的客服名称。
     */
    public void startCustomerServiceChat(Context context, String customerServiceUserId, String title) {

        if (context == null || TextUtils.isEmpty(customerServiceUserId))
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().packageName).buildUpon()
                .appendPath("conversation").appendPath(Conversation.ConversationType.CUSTOMER_SERVICE.getName().toLowerCase())
                .appendQueryParameter("targetId", customerServiceUserId).appendQueryParameter("title", title).build();

        context.startActivity(new Intent(Intent.ACTION_VIEW, uri));

        getRongIMClient().sendMessage(Conversation.ConversationType.CUSTOMER_SERVICE, customerServiceUserId, new HandshakeMessage(), null, null, null);
    }


    /**
     * 设置用户信息的提供者，供 RongIM 调用获取用户名称和头像信息。
     *
     * @param userInfoProvider 用户信息提供者。
     * @param isCacheUserInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                         如果 App 提供的 UserInfoProvider。
     *                         每次都需要通过网络请求用户数据，而不是将用户数据缓存到本地，会影响用户信息的加载速度；<br>
     *                         此时最好将本参数设置为 true，由 IMKit 来缓存用户信息。
     * @see UserInfoProvider
     */
    public static void setUserInfoProvider(UserInfoProvider userInfoProvider, boolean isCacheUserInfo) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        RongContext.getInstance().setGetUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    public static void setGroupUserInfoProvider(GroupUserInfoProvider userInfoProvider, boolean isCacheUserInfo) {
        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        RongContext.getInstance().setGroupUserInfoProvider(userInfoProvider, isCacheUserInfo);
    }

    /**
     * 设置群组信息的提供者，供 RongIM 调用获取群组名称和头像信息。
     *
     * @param groupInfoProvider 群组信息的提供者。
     * @param isCacheGroupInfo  设置是否由 IMKit 来缓存用户信息。<br>
     *                          如果 App 提供的 GroupInfoProvider。
     *                          每次都需要通过网络请求群组数据，而不是将群组数据缓存到本地，会影响群组信息的加载速度；<br>
     *                          此时最好将本参数设置为 true，由 IMKit 来缓存群组信息。
     * @see GroupInfoProvider
     */
    public static void setGroupInfoProvider(GroupInfoProvider groupInfoProvider, boolean isCacheGroupInfo) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        RongContext.getInstance().setGetGroupInfoProvider(groupInfoProvider, isCacheGroupInfo);
    }

    /**
     * 刷新讨论组缓存数据，可用于讨论组修改名称后刷新讨论组内其他人员的缓存数据。
     *
     * @param discussion 需要更新的讨论组缓存数据。
     */
    public void refreshDiscussionCache(Discussion discussion) {

        if (discussion == null)
            return;

        RongContext.getInstance().getDiscussionCache().put(discussion.getId(), discussion);
        RongContext.getInstance().getEventBus().post(discussion);
    }

    /**
     * 刷新用户缓存数据。
     *
     * @param userInfo 需要更新的用户缓存数据。
     */
    public void refreshUserInfoCache(UserInfo userInfo) {

        if (userInfo == null)
            return;

        RongContext.getInstance().getUserInfoCache().put(userInfo.getUserId(), userInfo);
        RongContext.getInstance().getEventBus().post(userInfo);
    }

    /**
     * 刷新、更改群组用户缓存数据。
     *
     * @param groupUserInfo 需要更新的群组用户缓存数据。
     */
    public void refreshGroupUserInfoCache(GroupUserInfo groupUserInfo) {

        if (groupUserInfo == null)
            return;

        String key = StringUtils.getKey(groupUserInfo.getGroupId(), groupUserInfo.getUserId());
        RongContext.getInstance().getGroupUserInfoCache().put(key, groupUserInfo);
        RongContext.getInstance().getEventBus().post(Event.GroupUserInfoEvent.obtain(groupUserInfo));
    }
    /**
     * 刷新群组缓存数据。
     *
     * @param group 需要更新的群组缓存数据。
     */
    public void refreshGroupInfoCache(Group group) {
        if (group == null)
            return;

        RongContext.getInstance().getGroupInfoCache().put(group.getId(), group);
        RongContext.getInstance().getEventBus().post(group);
    }

    /**
     * 设置发送消息的监听。
     *
     * @param listener 发送消息的监听。
     */
    public void setSendMessageListener(OnSendMessageListener listener) {

        if (listener == null)
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        RongContext.getInstance().setOnSendMessageListener(listener);
    }

    /**
     * 会话界面操作的监听器。
     */
    public static interface ConversationBehaviorListener {


        /**
         * 当点击用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onUserPortraitClick(Context context, Conversation.ConversationType conversationType, UserInfo user);

        /**
         * 当长按用户头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param user             被点击的用户的信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onUserPortraitLongClick(Context context, Conversation.ConversationType conversationType, UserInfo user);

        /**
         * 当点击消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被点击的消息的实体信息。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        public boolean onMessageClick(Context context, View view, Message message);

        /**
         * 当点击链接消息时执行。
         *
         * @param context 上下文。
         * @param link    被点击的链接。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true， 否则返回 false, false 走融云默认处理方式。
         */
        public boolean onMessageLinkClick(Context context, String link);

        /**
         * 当长按消息时执行。
         *
         * @param context 上下文。
         * @param view    触发点击的 View。
         * @param message 被长按的消息的实体信息。
         * @return 如果用户自己处理了长按后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onMessageLongClick(Context context, View view, Message message);

    }


    /**
     * 会话列表界面操作的监听器。
     */
    public static interface ConversationListBehaviorListener {
        /**
         * 当点击会话头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param targetId         被点击的用户id。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onConversationPortraitClick(Context context, Conversation.ConversationType conversationType, String targetId);

        /**
         * 当长按会话头像后执行。
         *
         * @param context          上下文。
         * @param conversationType 会话类型。
         * @param targetId         被点击的用户id。
         * @return 如果用户自己处理了点击后的逻辑处理，则返回 true，否则返回 false，false 走融云默认处理方式。
         */
        public boolean onConversationPortraitLongClick(Context context, Conversation.ConversationType conversationType, String targetId);

        /**
         * 长按会话列表中的 item 时执行。
         *
         * @param context      上下文。
         * @param view         触发点击的 View。
         * @param conversation 长按时的会话条目。
         * @return 如果用户自己处理了长按会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
         */
        public boolean onConversationLongClick(Context context, View view, UIConversation conversation);

        /**
         * 点击会话列表中的 item 时执行。
         *
         * @param context      上下文。
         * @param view         触发点击的 View。
         * @param conversation 会话条目。
         * @return 如果用户自己处理了点击会话后的逻辑处理，则返回 true， 否则返回 false，false 走融云默认处理方式。
         */
        public boolean onConversationClick(Context context, View view, UIConversation conversation);
    }

    /**
     * 用户信息的提供者。
     * <p/>
     * 如果在聊天中遇到的聊天对象是没有登录过的用户（即没有通过融云服务器鉴权过的），RongIM 是不知道用户信息的，RongIM 将调用此
     * Provider 获取用户信息。
     */
    public static interface UserInfoProvider {
        /**
         * 获取用户信息。
         *
         * @param userId 用户 Id。
         * @return 用户信息。
         */
        public UserInfo getUserInfo(String userId);
    }

    public static interface GroupUserInfoProvider {
        public GroupUserInfo getGroupUserInfo(String groupId, String userId);
    }


    /**
     * 群组信息的提供者。
     * <p/>
     * RongIM 本身不保存群组信息，如果在聊天中需要使用群组信息，RongIM 将调用此 Provider 获取群组信息。
     */
    public static interface GroupInfoProvider {
        /**
         * 获取群组信息。
         *
         * @param groupId 群组 Id.
         * @return 群组信息。
         */
        public Group getGroupInfo(String groupId);
    }

    /**
     * 启动好友选择界面的监听器
     */
    public static interface OnSelectMemberListener {
        /**
         * 启动好友选择界面的接口。
         *
         * @param context          上下文。
         * @param conversationType 会话类型：PRIVATE / DISCUSSION.
         * @param targetId         该会话对应的 Id，私聊时为发送者 Id，讨论组时为讨论组 Id。
         */

        public void startSelectMember(Context context, Conversation.ConversationType conversationType, String targetId);
    }

    /**
     * 获取自己发出的消息监听器。
     */
    public interface OnSendMessageListener {

        /**
         * 消息发送前监听器处理接口（是否发送成功可以从SentStatus属性获取）
         * 可以通过这个方法，过滤，修改发送出的消息。
         *
         * @param message 发送的消息实例。
         * @return        处理后的消息实例，注意：可以通过 return 的返回值，过滤消息
         *                当 return null 时，该消息不发送，界面也无显示
         *                也可以更改 message 内的消息内容，发送出的消息，就是更改后的。
         */
        public Message onSend(Message message);


        /**
         * 消息发送后回调接口。
         *
         * @param message              消息实例。
         * @param sentMessageErrorCode 发送消息失败的状态码，消息发送成功 SentMessageErrorCode 为 null。
         */
        public boolean onSent(Message message, SentMessageErrorCode sentMessageErrorCode);

    }

    /**
     * 发出的消息错误码。
     */
    public enum SentMessageErrorCode {

        /**
         * 未知错误。
         */
        UNKNOWN(-1, "Unknown error."),

        /**
         * 不在讨论组。
         */
        NOT_IN_DISCUSSION(21406, "not_in_discussion"),
        /**
         * 不在群组。
         */
        NOT_IN_GROUP(22406, "not_in_group"),
        /**
         * 群组禁言
         */
        FORBIDDEN_IN_GROUP(22408, "forbidden_in_group"),
        /**
         * 不在聊天室。
         */
        NOT_IN_CHATROOM(23406, "not_in_chatroom"),

        /**
         * 在黑名单中。
         */
        REJECTED_BY_BLACKLIST(405, "rejected by blacklist");


        private int code;
        private String msg;

        /**
         * 构造函数。
         *
         * @param code 错误代码。
         * @param msg  错误消息。
         */
        SentMessageErrorCode(int code, String msg) {
            this.code = code;
            this.msg = msg;
        }

        /**
         * 获取错误代码值。
         *
         * @return 错误代码值。
         */
        public int getValue() {
            return this.code;
        }

        /**
         * 获取错误消息。
         *
         * @return 错误消息。
         */
        public String getMessage() {
            return this.msg;
        }

        /**
         * 设置错误代码值。
         *
         * @param code 错误代码。
         * @return 错误代码枚举。
         */
        public static SentMessageErrorCode setValue(int code) {
            for (SentMessageErrorCode c : SentMessageErrorCode.values()) {
                if (code == c.getValue()) {
                    return c;
                }
            }

            Log.d("RongIMClient", "SentMessageErrorCode---ErrorCode---code:" + code);

            return UNKNOWN;
        }
    }

//    /**
//     * 新消息提示音的提供者。
//     */
//    public static interface GetNewMessageSoundProvider {
//        public Uri getSoundSource();
//    }
//
//    /**
//     * 设置收到消息时的提示音。
//     *
//     * @param provider 消息提示音提供者。
//     */
//    public void setGetNewMessageSoundProvider(GetNewMessageSoundProvider provider) {
//        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
//            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
//            return;
//        }
//
//        RongContext.getInstance().setNewMessageSoundProvider(provider);
//    }


    /**
     * 设置消息体内是否携带用户信息。
     *
     * @param state 是否携带用户信息，true 携带，false 不携带。
     */
    public void setMessageAttachedUserInfo(boolean state) {
        RongContext.getInstance().setUserInfoAttachedState(state);
    }

    /**
     * 接收未读消息的监听器。
     */
    public interface OnReceiveUnreadCountChangedListener {
        public void onMessageIncreased(int count);
    }

    /**
     * 设置接收未读消息的监听器。
     *
     * @param listener          接收未读消息消息的监听器。
     * @param conversationTypes 接收未读消息的会话类型。
     */
    public void setOnReceiveUnreadCountChangedListener(final OnReceiveUnreadCountChangedListener listener, Conversation.ConversationType... conversationTypes) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (listener != null && conversationTypes != null && conversationTypes.length > 0) {
            MessageCounter.Counter mCounter = new MessageCounter.Counter(ConversationTypeFilter.obtain(conversationTypes)) {
                @Override
                public void onMessageIncreased(int count) {
                    listener.onMessageIncreased(count);
                }
            };

            RongContext.getInstance().getMessageCounterLogic().registerMessageCounter(mCounter);
        }
    }

    /**
     * 启动公众号信息界面。
     *
     * @param context          应用上下文。
     * @param conversationType 会话类型。
     * @param targetId         目标 Id。
     */
    public void startPublicServiceProfile(Context context, Conversation.ConversationType conversationType, String targetId) {

        if (context == null || conversationType == null || TextUtils.isEmpty(targetId))
            throw new IllegalArgumentException();

        if (RongContext.getInstance() == null)
            throw new ExceptionInInitializerError("RongCloud SDK not init");

        Uri uri = Uri.parse("rong://" + context.getApplicationInfo().processName).buildUpon()
                .appendPath("publicServiceProfile").appendPath(conversationType.getName().toLowerCase()).appendQueryParameter("targetId", targetId).build();

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 设置接收 push 消息的监听器。
     *
     * @param listener 接收 push 消息的监听器。
     */
    public static void setOnReceivePushMessageListener(RongIMClient.OnReceivePushMessageListener listener) {
        RongIMClient.setOnReceivePushMessageListener(listener);
    }

    /**
     * 设置主要的输入项。
     *
     * @param provider 输入项 provider。
     */
    public static void setPrimaryInputProvider(InputProvider.MainInputProvider provider) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "setPrimaryInputProvider", "RongCloud SDK not init");
            return;
        }

        if (provider == null)
            throw new IllegalArgumentException();

        RongContext.getInstance().setPrimaryInputProvider(provider);
    }

    /**
     * 设置次要的输入项。
     *
     * @param provider 输入项 provider。
     */
    public static void setSecondaryInputProvider(InputProvider.MainInputProvider provider) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "setSecondaryInputProvider", "RongCloud SDK not init");
            return;
        }

        if (provider == null)
            throw new IllegalArgumentException();

        RongContext.getInstance().setSecondaryInputProvider(provider);
    }

    /**
     * 为会话类型设置自定义输入适配器。
     *
     * @param conversationType 会话类型。
     * @param providers        输入扩展适配器。
     */
    public static void resetInputExtensionProvider(Conversation.ConversationType conversationType, InputProvider.ExtendProvider[] providers) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerInputProvider", "RongCloud SDK not init");
            return;
        }

        if (providers == null)
            throw new IllegalArgumentException();

        RongContext.getInstance().resetInputExtentionProvider(conversationType, providers);
    }

    /**
     * 为会话类型添加自定义输入适配器。
     *
     * @param conversationType 会话类型。
     * @param providers        输入扩展适配器。
     */
    public static void addInputExtensionProvider(Conversation.ConversationType conversationType, InputProvider.ExtendProvider[] providers) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerInputProvider", "RongCloud SDK not init");
            return;
        }

        if (providers == null)
            throw new IllegalArgumentException();

        RongContext.getInstance().addInputExtentionProvider(conversationType, providers);
    }

    /**
     * 注册会话列表消息模板提供者。
     *
     * @param provider 会话列表模板提供者。
     */
    public void registerConversationTemplate(IContainerItemProvider.ConversationProvider provider) {

        if (mCurrentProcessName != null && !mCurrentProcessName.equals(mContext.getPackageName())) {
            RLog.e(mContext, "RONG_SDK", "Rong SDK should not be initialized at subprocess");
            return;
        }

        if (RongContext.getInstance() == null) {
            RLog.e(sRongIM, "registerConversationTemplate", "RongCloud SDK not init");
            return;
        }

        if (provider == null)
            throw new IllegalArgumentException();

        RongContext.getInstance().registerConversationTemplate(provider);

    }


    /**
     * 设置会话界面未读新消息是否展示 注:未读新消息大于1条即展示
     *
     * @param state true 展示，false 不展示。
     */
    public void enableNewComingMessageIcon(boolean state) {
        RongContext.getInstance().showNewMessageIcon(state);
    }

    /**
     * 设置会话界面历史消息是否展示 注:历史消息大于10条即展示
     *
     * @param state true 展示，false 不展示。
     */
    public void enableUnreadMessageIcon(boolean state) {
        RongContext.getInstance().showUnreadMessageIcon(state);
    }

    /**
     * 设置语音消息的最大长度
     *
     * @param sec 默认值是60s，有效值为不小于5秒，不大于60秒
     */
    public void setMaxVoiceDurationg(int sec) {
        RongContext.getInstance().getVoiceInputProvider().setMaxVoiceDuration(sec);
    }
}
