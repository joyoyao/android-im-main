package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.util.AndroidEmoji;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.HandshakeMessage;

@ProviderTag(messageContent = HandshakeMessage.class , showPortrait = false, centerInHorizontal = true, hide = true)
public class HandshakeMessageItemProvider extends IContainerItemProvider.MessageProvider<HandshakeMessage> {


    @Override
    public View newView(Context context, ViewGroup group) {
        return null;
    }

    @Override
    public Spannable getContentSummary(HandshakeMessage data) {
        if (data != null && data.getContent() != null)
            return new SpannableString(AndroidEmoji.ensure(data.getContent()));
        return null;
    }

    @Override
    public void onItemClick(View view, int position, HandshakeMessage content, UIMessage message) {

    }

    @Override
    public void onItemLongClick(View view, int position, HandshakeMessage content, final UIMessage message) {
        String name = null;
        if (message.getConversationType().getName().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE.getName()) ||
                message.getConversationType().getName().equals(Conversation.ConversationType.PUBLIC_SERVICE.getName())) {
            ConversationKey key = ConversationKey.obtain(message.getTargetId(), message.getConversationType());
            PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoCache().get(key.getKey());
            if (info != null)
                name = info.getName();
        } else {
            UserInfo userInfo = RongContext.getInstance().getUserInfoCache().get(message.getSenderUserId());
            if (userInfo != null)
                name = userInfo.getName();
        }
        String[] items;

        items = new String[]{view.getContext().getResources().getString(R.string.rc_dialog_item_message_delete)};

        ArraysDialogFragment.newInstance(name, items).setArraysDialogItemListener(new ArraysDialogFragment.OnArraysDialogItemListener() {
            @Override
            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {
                if (which == 0)
                    RongIM.getInstance().getRongIMClient().deleteMessages(new int[]{message.getMessageId()}, null);

            }
        }).show(((FragmentActivity) view.getContext()).getSupportFragmentManager());
    }

    @Override
    public void bindView(View v, int position, HandshakeMessage content, UIMessage data) {

    }
}
