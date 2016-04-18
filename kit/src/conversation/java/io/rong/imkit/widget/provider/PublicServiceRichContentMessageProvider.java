package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.tools.RongWebviewActivity;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.PublicServiceRichContentMessage;
import com.sea_monster.resource.Resource;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.rong.imkit.widget.AsyncImageView ;

/**
 * Created by weiqinxiao on 15/4/18.
 */
@ProviderTag(messageContent = PublicServiceRichContentMessage.class, showPortrait = false, centerInHorizontal = true)
public class PublicServiceRichContentMessageProvider extends IContainerItemProvider.MessageProvider<PublicServiceRichContentMessage> {
    private Context mContext;
    private int width, height;

    @Override
    public View newView(Context context, ViewGroup group) {
        mContext = context;
        ViewHolder holder = new ViewHolder();
        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_public_service_rich_content_message, null);

        holder.title = (TextView) view.findViewById(R.id.rc_title);
        holder.time = (TextView) view.findViewById(R.id.rc_time);
        holder.description = (TextView) view.findViewById(R.id.rc_content);
        holder.imageView = (AsyncImageView) view.findViewById(R.id.rc_img);

        WindowManager m = (WindowManager)view.getContext().getSystemService(Context.WINDOW_SERVICE);
        int w = m.getDefaultDisplay().getWidth() - 35;
        view.setLayoutParams(new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT));
        width = w - 100;
        height = 800;
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View v, int position, PublicServiceRichContentMessage content, UIMessage message) {
        ViewHolder holder = (ViewHolder) v.getTag();

        PublicServiceRichContentMessage msg = (PublicServiceRichContentMessage) message.getContent();

        holder.title.setText(msg.getMessage().getTitle());
        holder.description.setText(msg.getMessage().getDigest());

        int w = width;
        int h = height;

        holder.imageView.setResource(new Resource(msg.getMessage().getImageUrl()));
        String time = formatDate(message.getReceivedTime(), "MM月dd日 HH:mm");
        holder.time.setText(time);
    }

    private String formatDate(long timeMillis, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(timeMillis));
    }

    @Override
    public Spannable getContentSummary(PublicServiceRichContentMessage data) {
        return new SpannableString(data.getMessage().getTitle());
    }

    @Override
    public void onItemClick(View view, int position, PublicServiceRichContentMessage content, UIMessage message) {
        String url = content.getMessage().getUrl();
        Intent intent = new Intent(mContext, RongWebviewActivity.class);
        intent.putExtra("url", url);
        mContext.startActivity(intent);
    }

    @Override
    public void onItemLongClick(View view, int position, PublicServiceRichContentMessage content, final UIMessage message) {
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

    private class ViewHolder {
        TextView title;
        AsyncImageView imageView;
        TextView time;
        TextView description;
    }

}
