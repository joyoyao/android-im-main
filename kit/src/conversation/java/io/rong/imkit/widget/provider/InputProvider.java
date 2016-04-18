package io.rong.imkit.widget.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.fragment.MessageInputFragment;
import io.rong.imkit.widget.InputView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

/**
 * 输入区适配器。
 */
public abstract class InputProvider {


    RongContext mContext;
    MessageInputFragment mFragment;
    int index;
    InputView mCurrentView;


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    Conversation mCurrentConversation;

    /**
     * 获取当前融云IM上线文。
     *
     * @return  融云IM上下文。
     */
    public RongContext getContext() {
        return mContext;
    }

    /**
     * 实例化方法。
     *
     * @param context   融云IM上下文。（通过 RongContext.getInstance() 可以获取）
     */
    public InputProvider(RongContext context) {
        mContext = context;
    }


    /**
     * 设置当前活动会话。
     *
     * @param conversation  会话实例。
     */
    public void setCurrentConversation(Conversation conversation) {
        mCurrentConversation = conversation;
    }

    /**
     * 获取当前会话实例。
     *
     * @return  会话实例。
     */
    public Conversation getCurrentConversation() {
        return mCurrentConversation;
    }

    /**
     * Receive the result from a previous call to
     * {@link #startActivityForResult(Intent, int)}.  This follows the
     * related Activity API as described there in
     * {@link Activity#onActivityResult(int, int, Intent)}.
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }


    /**
     * Call {@link Activity#startActivityForResult(Intent, int)} on the fragment's
     * containing Activity.
     *
     * requestCode can only support 0~127.
     */
    public void startActivityForResult(Intent intent, int requestCode) {
        mFragment.startActivityFromProvider(this, intent, requestCode);
    }

    /**
     * 获取所属Fragment。
     *
     * @return  当前所属Fragment。
     */
    public MessageInputFragment getCurrentFragment() {
        return mFragment;
    }


    /**
     * 发送消息内容实例。
     *
     * @param content 消息内容实例。
     */
    public void publish(MessageContent content) {
        publish(content, null);
    }

    /**
     * 发送消息内容实例。
     *
     * @param content   消息内容实例。
     * @param callback  发送消息回调。
     */
    public void publish(MessageContent content, final RongIMClient.ResultCallback<Message> callback){
        if (content == null) {
            RLog.w(this, "publish", "content is null");
            return;
        }

        if (mCurrentConversation == null || TextUtils.isEmpty(mCurrentConversation.getTargetId()) || mCurrentConversation.getConversationType() == null) {
            Log.e("InputProvider", "the conversation hasn't been created yet!!!");
            return;
        }

        Message message = Message.obtain(mCurrentConversation.getTargetId(), mCurrentConversation.getConversationType(), content);
        if(RongIM.getInstance()!=null && RongIM.getInstance().getRongIMClient()!=null) {
            RongIM.getInstance().getRongIMClient().sendMessage(message, null, null, null, callback);
        }
    }

    /**
     * 发送输入状态消息。
     *
     */
    public void onTypingMessage(String objectName) {
        boolean isShow = RongIMClient.getInstance().getTypingStatus();
        if (isShow) {
            if (RongIMClient.getInstance() != null) {
                RongIMClient.getInstance().sendTypingStatus(mCurrentConversation.getConversationType(),
                        mCurrentConversation.getTargetId(), objectName);
            }
        }
    }

    /**
     * 在依附上页面其他控件时候调用。
     *
     * @param fragment      依附上的 Fragment。
     * @param inputView     依附上的 InputView。
     */
    public void onAttached(MessageInputFragment fragment, InputView inputView) {
        mFragment = fragment;
        mCurrentView = inputView;
    }

    /**
     * 脱离依附时调用。
     *
     */
    public void onDetached() {
        mFragment = null;
        mCurrentView = null;
    }

    /**
     * 获取依附的 InputView 。
     *
     * @return  依附的 InputView 。
     */
    public InputView getInputView() {
        return mCurrentView;
    }

    /**
     * 会话主输入区适配器。
     */
    public static abstract class MainInputProvider extends InputProvider {

        /**
         * 实例化适配器。
         *
         * @param context   融云IM上下文。（通过 RongContext.getInstance() 可以获取）
         */
        public MainInputProvider(RongContext context) {
            super(context);
        }

        /**
         * 生成切换是需要的图标。
         *
         * @param context   页面上下文。
         * @return  切换时候的图标。
         */
        public abstract Drawable obtainSwitchDrawable(Context context);


        /**
         * 生成主输入区 View。
         *
         * @param inflater  生成View所需 LayoutInflater 。
         * @param parent    生成页面父容器。
         * @param inputView 所属 InputView 。
         * @return  主输入区 View。
         */
        public abstract View onCreateView(LayoutInflater inflater, ViewGroup parent, InputView inputView);

        /**
         * 激活时调用。
         *
         * @param context   页面上下文。
         */
        public abstract void onActive(Context context);

        /**
         * 在失去激活时调用。
         *
         * @param context   页面上下文。
         */
        public abstract void onInactive(Context context);

        /**
         *
         *
         * @param context   页面上下文。
         */
        public abstract void onSwitch(Context context);
    }

    /**
     * 会话扩展区适配器。
     */
    public static abstract class ExtendProvider extends InputProvider{

        /**
         * 实例化适配器。
         *
         * @param context   融云IM上下文。（通过 RongContext.getInstance() 可以获取）
         */
        public ExtendProvider(RongContext context){super(context);}

        /**
         * 生成扩展区图片。
         *
         * @param context   页面上下文。
         * @return  扩展区图片。
         */
        public abstract Drawable obtainPluginDrawable(Context context);

        /**
         * 生成扩展区标题。
         *
         * @param context   页面上下文。
         * @return  扩展区标题。
         */
        public abstract CharSequence obtainPluginTitle(Context context);

        /**
         * 扩展区点击时候触发。
         *
         * @param view  点击发生的View。
         */
        public abstract void onPluginClick(View view);

    }
}
