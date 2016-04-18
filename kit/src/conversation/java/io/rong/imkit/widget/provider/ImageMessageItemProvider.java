package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.UserInfo;
import io.rong.message.ImageMessage;

/**
 * Created by DragonJ on 14-8-2.
 */
@ProviderTag(messageContent = ImageMessage.class, showProgress = false)
public class ImageMessageItemProvider extends IContainerItemProvider.MessageProvider<ImageMessage> {

    class ViewHolder {
        AsyncImageView img;
        TextView message;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_image_message, null);

        ViewHolder holder = new ViewHolder();

        holder.message = (TextView) view.findViewById(R.id.rc_msg);
        holder.img = (AsyncImageView) view.findViewById(R.id.rc_img);

        view.setTag(holder);
        return view;
    }

    @Override
    public void onItemClick(View view, int position, ImageMessage content, UIMessage message) {
        if (content == null) {

        }
    }

    @Override
    public void onItemLongClick(View view, int position, ImageMessage content, final UIMessage message) {
        String name = null;
        String[] items;

        if (message != null && message.getSentStatus().equals(Message.SentStatus.SENDING))
            return;

        if (message.getSenderUserId() != null) {
            UserInfo userInfo = message.getUserInfo();
            if(userInfo == null)
                userInfo = RongContext.getInstance().getUserInfoFromCache(message.getSenderUserId());
            if (userInfo != null)
                name = userInfo.getName();
        }
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
    public void bindView(View v, int position, ImageMessage content, UIMessage message) {
        final ViewHolder holder = (ViewHolder) v.getTag();

        if (message.getMessageDirection() == Message.MessageDirection.SEND) {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_no_right);
        } else {
            v.setBackgroundResource(R.drawable.rc_ic_bubble_no_left);
        }

        holder.img.setResource(content.getThumUri() == null ? null : new Resource(content.getThumUri()));

        int progress = message.getProgress();

        Message.SentStatus status = message.getSentStatus();

        if (status.equals(Message.SentStatus.SENDING) && progress < 100) {

            if (progress == 0)
                holder.message.setText(RongContext.getInstance().getResources().getString(R.string.rc_waiting));
            else
                holder.message.setText(progress + "%");

            holder.message.setVisibility(View.VISIBLE);
        } else {
            holder.message.setVisibility(View.GONE);
        }
    }

    @Override
    public Spannable getContentSummary(ImageMessage data) {
        return new SpannableString(RongContext.getInstance()
                .getResources()
                .getString(R.string.rc_message_content_image));
    }
}
