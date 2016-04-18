package io.rong.imkit.fragment;

import android.os.Message;
import android.text.TextUtils;
import android.view.View;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 * Created by zhjchen on 3/20/15.
 */
public class SetConversationToTopFragment extends BaseSettingFragment {

    private static String TAG = SetConversationToTopFragment.class.getSimpleName();


    @Override
    protected void initData() {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().getEventBus().register(this);

        if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
            RongIM.getInstance().getRongIMClient().getConversation(getConversationType(), getTargetId(), new RongIMClient.ResultCallback<Conversation>() {

                @Override
                public void onSuccess(final Conversation conversation) {
                    if (conversation != null)
                        setSwitchBtnStatus(conversation.isTop());
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    @Override
    protected boolean setSwitchButtonEnabled() {
        return true;
    }

    @Override
    protected String setTitle() {
        return getString(R.string.rc_setting_set_top);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    protected void onSettingItemClick(View v) {
        RLog.i(this, "onSettingItemClick", v.toString());
    }

    @Override
    protected int setSwitchBtnVisibility() {
        return View.VISIBLE;
    }

    @Override
    protected void toggleSwitch(boolean toggle) {

        if (getConversationType() != null && !TextUtils.isEmpty(getTargetId())) {
            RongIM.getInstance().getRongIMClient().setConversationToTop(getConversationType(), getTargetId(), toggle,null);
        } else {
            RLog.e(this, TAG, "toggleSwitch() args is null");
        }

    }

    public void onEventMainThread(Event.ConversationTopEvent conversationTopEvent) {
        if (conversationTopEvent != null && conversationTopEvent.getTargetId().equals(getTargetId()) && conversationTopEvent.getConversationType().getValue() == getConversationType().getValue()) {
            setSwitchBtnStatus(conversationTopEvent.isTop());
        }
    }

    @Override
    public void onDestroy() {

        if (RongContext.getInstance() != null)
            RongContext.getInstance().getEventBus().unregister(this);

        super.onDestroy();

    }
}
