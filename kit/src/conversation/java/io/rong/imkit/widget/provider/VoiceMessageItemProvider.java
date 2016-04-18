package io.rong.imkit.widget.provider;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.RongIMClientWrapper;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.util.IVoiceHandler;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.VoiceMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
@ProviderTag(messageContent = VoiceMessage.class)
public class VoiceMessageItemProvider extends IContainerItemProvider.MessageProvider<VoiceMessage> {

    class ViewHolder {
        ImageView img;
        TextView left;
        TextView right;
        ImageView unread;
    }

    IVoiceHandler mVoiceHandler;
    private int voiceInputOperationStatus = Event.VoiceInputOperationEvent.STATUS_DEFAULT;
    Message mCurrentMessage;
    AnimationDrawable animationDrawable;

    public VoiceMessageItemProvider(Context context) {
        mVoiceHandler = new IVoiceHandler.VoiceHandler(context);
        mVoiceHandler.setPlayListener(new PlayListener());
        RongContext.getInstance().getEventBus().register(this);
    }

    class PlayListener implements IVoiceHandler.OnPlayListener {
        View mParent;

        @Override
        public void onPlay(Context context) {
            if(animationDrawable != null) {
                animationDrawable.start();
            }
            if (context instanceof Activity) {
                mParent = ((Activity) context).getWindow().getDecorView();
            }
        }

        @Override
        public void onCover(boolean limited) {

        }

        @Override
        public void onStop() {
            if(animationDrawable != null) {
                animationDrawable.stop();
            }
            boolean isListened = mCurrentMessage.getReceivedStatus().isListened();
            mCurrentMessage.getReceivedStatus().setListened();
            RongIMClientWrapper.getInstance().setMessageReceivedStatus(mCurrentMessage.getMessageId(), mCurrentMessage.getReceivedStatus(), null);
            RongContext.getInstance().getEventBus().post(Event.PlayAudioEvent.obtain(mCurrentMessage.getContent(), false, isListened));
            RongContext.getInstance().getEventBus().post(new Event.MessageListenedEvent(mCurrentMessage.getConversationType(), mCurrentMessage.getTargetId(), mCurrentMessage.getMessageId()));
        }

        @Override
        public void onFinish() {
            if(animationDrawable != null) {
                animationDrawable.stop();
            }
            boolean isListened = mCurrentMessage.getReceivedStatus().isListened();
            mCurrentMessage.getReceivedStatus().setListened();
            RongIMClientWrapper.getInstance().setMessageReceivedStatus(mCurrentMessage.getMessageId(), mCurrentMessage.getReceivedStatus(), null);
            RongContext.getInstance().getEventBus().post(Event.PlayAudioEvent.obtain(mCurrentMessage.getContent(), true, isListened));
            RongContext.getInstance().getEventBus().post(new Event.MessageListenedEvent(mCurrentMessage.getConversationType(), mCurrentMessage.getTargetId(), mCurrentMessage.getMessageId()));
        }
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_voice_message, null);
        ViewHolder holder = new ViewHolder();
        holder.left = (TextView) view.findViewById(R.id.rc_left);
        holder.right = (TextView) view.findViewById(R.id.rc_right);
        holder.img = (ImageView) view.findViewById(R.id.rc_img);
        holder.unread = (ImageView) view.findViewById(R.id.rc_voice_unread);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int position, VoiceMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();
        int length = (int) ((content.getDuration() * 2 + 60) * v.getResources().getDisplayMetrics().density);
        holder.img.getLayoutParams().width = length;
        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            holder.left.setText(content.getDuration() + "\"");
            holder.left.setVisibility(View.VISIBLE);
            holder.right.setVisibility(View.GONE);
            holder.unread.setVisibility(View.GONE);
            rightSet(holder, content);
        } else {
            holder.right.setText(content.getDuration() + "\"");
            holder.right.setVisibility(View.VISIBLE);
            holder.left.setVisibility(View.GONE);
            if (!message.getReceivedStatus().isListened())
                holder.unread.setVisibility(View.VISIBLE);
            else
                holder.unread.setVisibility(View.GONE);
            holder.img.setScaleType(ImageView.ScaleType.FIT_START);
            leftSet(holder, content);
        }
    }

    @Override
    public void onItemClick(View view, int position, VoiceMessage content, UIMessage message) {
        RLog.d(this, "Item", "index:" + position);
        ViewHolder holder = (ViewHolder) view.getTag();
        if (voiceInputOperationStatus == Event.VoiceInputOperationEvent.STATUS_INPUTING) {
            if (mVoiceHandler.getCurrentPlayUri() != null) {
                AnimationDrawable animationDrawable;
                if (message.getMessageDirection() == Message.MessageDirection.SEND)
                    animationDrawable = (AnimationDrawable) view.getContext().getResources().getDrawable(R.drawable.rc_an_voice_sent);
                else
                    animationDrawable = (AnimationDrawable) view.getContext().getResources().getDrawable(R.drawable.rc_an_voice_receive);
                holder.img.setImageDrawable(animationDrawable);
                mVoiceHandler.stop();
            }
            return;
        }

        if (mVoiceHandler.getCurrentPlayUri() != null) {
            if (mVoiceHandler.getCurrentPlayUri().equals(content.getUri())) {
                mVoiceHandler.stop();
                return;
            }
        }

        if (mVoiceHandler.getCurrentPlayUri() != null) {
            mVoiceHandler.stop();
        }

        if (message.getMessageDirection() == Message.MessageDirection.SEND)
            animationDrawable = (AnimationDrawable) view.getContext().getResources().getDrawable(R.drawable.rc_an_voice_sent);
        else
            animationDrawable = (AnimationDrawable) view.getContext().getResources().getDrawable(R.drawable.rc_an_voice_receive);
        holder.img.setImageDrawable(animationDrawable);
        holder.unread.setVisibility(View.GONE);
        mVoiceHandler.play(view.getContext(), content.getUri());
        mCurrentMessage = message.getMessage();
    }

    @Override
    public void onItemLongClick(View view, int position, VoiceMessage content, final UIMessage message) {
        String name = null;
        if (message.getConversationType().getName().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE.getName()) ||
                message.getConversationType().getName().equals(Conversation.ConversationType.PUBLIC_SERVICE.getName())) {
            ConversationKey key = ConversationKey.obtain(message.getTargetId(), message.getConversationType());
            PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoCache().get(key.getKey());
            if (info != null)
                name = info.getName();
        } else {
            UserInfo userInfo = message.getUserInfo();
            if(userInfo == null)
                userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());
            if (userInfo != null)
                name = userInfo.getName();
        }
        String[] items;
        items = new String[]{view.getContext().getResources().getString(R.string.rc_dialog_item_message_delete)};
        ArraysDialogFragment.newInstance(name, items).setArraysDialogItemListener(new ArraysDialogFragment.OnArraysDialogItemListener() {
            @Override
            public void OnArraysDialogItemClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    if (mVoiceHandler.getCurrentPlayUri() != null) {
                        mVoiceHandler.stop();
                    }
                    RongIM.getInstance().getRongIMClient().deleteMessages(new int[]{message.getMessageId()}, null);
                }
            }
        }).show(((FragmentActivity) view.getContext()).getSupportFragmentManager());
    }

    private void leftSet(ViewHolder holder, VoiceMessage voice) {
        if (mVoiceHandler.getCurrentPlayUri() == null || !mVoiceHandler.getCurrentPlayUri().equals(voice.getUri())) {
            holder.img.setImageDrawable(holder.img.getResources().getDrawable(R.drawable.rc_ic_voice_receive));
        } else {
            AnimationDrawable drawable = (AnimationDrawable) holder.img.getResources().getDrawable(R.drawable.rc_an_voice_receive);
            holder.img.setImageDrawable(drawable);
            drawable.start();
        }
        holder.img.setBackgroundResource(R.drawable.rc_ic_bubble_left);
    }

    private void rightSet(ViewHolder holder, VoiceMessage voice) {
        if (mVoiceHandler.getCurrentPlayUri() == null || !mVoiceHandler.getCurrentPlayUri().equals(voice.getUri())) {
            holder.img.setImageDrawable(holder.img.getResources().getDrawable(R.drawable.rc_ic_voice_sent));
        } else {
            AnimationDrawable drawable = (AnimationDrawable) holder.img.getResources().getDrawable(R.drawable.rc_an_voice_sent);
            holder.img.setImageDrawable(drawable);
            drawable.start();
        }
        holder.img.setScaleType(ImageView.ScaleType.FIT_END);
        holder.img.setBackgroundResource(R.drawable.rc_ic_bubble_right);
    }

    @Override
    public Spannable getContentSummary(VoiceMessage data) {
        SpannableString string = new SpannableString(RongContext.getInstance().getString(R.string.rc_message_content_voice));
        return string;
    }

    public void onEvent(Event.VoiceInputOperationEvent event) {
        voiceInputOperationStatus = event.getStatus();
        if (voiceInputOperationStatus == Event.VoiceInputOperationEvent.STATUS_INPUTING) {
            if (mVoiceHandler.getCurrentPlayUri() != null)
                mVoiceHandler.stop();
        } else {
            voiceInputOperationStatus = Event.VoiceInputOperationEvent.STATUS_DEFAULT;
        }
    }
}