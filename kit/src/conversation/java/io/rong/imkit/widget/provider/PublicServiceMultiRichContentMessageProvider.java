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
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.tools.RongWebviewActivity;
import io.rong.imkit.widget.ArraysDialogFragment;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.PublicServiceMultiRichContentMessage;
import io.rong.message.RichContentItem;


/**
 * Created by weiqinxiao on 15/4/13.
 */
@ProviderTag(messageContent = PublicServiceMultiRichContentMessage.class, showPortrait = false, centerInHorizontal = true)
public class PublicServiceMultiRichContentMessageProvider extends IContainerItemProvider.MessageProvider<PublicServiceMultiRichContentMessage> {

    private PublicAccountMsgAdapter mAdapter;
    private Context mContext;

    @Override
    public void bindView(View v, int position, PublicServiceMultiRichContentMessage content, UIMessage message) {
        ViewHolder vh = (ViewHolder) v.getTag();
        final ArrayList<RichContentItem> msgList = content.getMessages();

        if(msgList.size() > 0) {
            vh.tv.setText(msgList.get(0).getTitle());
            vh.iv.setResource(new Resource(msgList.get(0).getImageUrl()));
        }

        int height = 0;
        ViewGroup.LayoutParams params = v.getLayoutParams();

        mAdapter = new PublicAccountMsgAdapter(mContext, msgList);
        vh.lv.setAdapter(mAdapter);

        vh.lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                RichContentItem item = mAdapter.getItem(position);
                RichContentItem item = msgList.get(position + 1);
                String url = item.getUrl();
                Intent intent = new Intent(mContext, RongWebviewActivity.class);
                intent.putExtra("url", url);
                mContext.startActivity(intent);
            }
        });

        height = getListViewHeight(vh.lv) + vh.height;
        params.height = height;

        v.setLayoutParams(params);
        v.requestLayout();
    }

    private int getListViewHeight(ListView list) {
        int totalHeight = 0;
        View item = null;

        ListAdapter adapter = list.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            item = adapter.getView(i, null, list);
            item.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            totalHeight = totalHeight + item.getMeasuredHeight() + 2;
        }

        return totalHeight;
    }

    @Override
    public Spannable getContentSummary(PublicServiceMultiRichContentMessage data) {
        List<RichContentItem> list = data.getMessages();
        if(list.size() > 0)
            return new SpannableString(data.getMessages().get(0).getTitle());
        else
            return null;
    }

    @Override
    public void onItemClick(View view, int position, PublicServiceMultiRichContentMessage content, UIMessage message) {

        if(content.getMessages().size() == 0)
            return;

        String url = content.getMessages().get(0).getUrl();
        Intent intent = new Intent(mContext, RongWebviewActivity.class);
        intent.putExtra("url", url);
        mContext.startActivity(intent);
    }

    @Override
    public void onItemLongClick(View view, int position, PublicServiceMultiRichContentMessage content, final UIMessage message) {
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
        int height;
        TextView tv;
        AsyncImageView iv;
        ListView lv;
    }

    @Override
    public View newView(Context context, ViewGroup group) {
        mContext = context;
        ViewHolder holder = new ViewHolder();

        View view = LayoutInflater.from(context).inflate(R.layout.rc_item_public_service_multi_rich_content_message, null);
        holder.lv = (ListView) view.findViewById(R.id.rc_list);
        holder.iv = (AsyncImageView) view.findViewById(R.id.rc_img);
        holder.tv = (TextView) view.findViewById(R.id.rc_txt);
        view.measure(0, 0);
        holder.height = view.getMeasuredHeight();
        view.setTag(holder);

        return view;
    }

    private class PublicAccountMsgAdapter extends android.widget.BaseAdapter {

        LayoutInflater inflater;
        ArrayList<RichContentItem> itemList;
        int itemCount;

        public PublicAccountMsgAdapter(Context context, ArrayList<RichContentItem> msgList) {
            inflater = LayoutInflater.from(context);
            itemList = new ArrayList<RichContentItem>();
            itemList.addAll(msgList);
            itemCount = msgList.size() - 1;
        }

        @Override
        public int getCount() {
            return itemCount;
        }

        @Override
        public RichContentItem getItem(int position) {
            if(itemList.size() == 0)
                return null;

            return itemList.get(position + 1);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            convertView = inflater.inflate(R.layout.rc_item_public_service_message, null);

            AsyncImageView iv = (AsyncImageView) convertView.findViewById(R.id.rc_img);
            TextView tv = (TextView) convertView.findViewById(R.id.rc_txt);

            if(itemList.size() == 0)
                return null;

            String title = itemList.get(position + 1).getTitle();
            if(title != null)
                tv.setText(title);

            iv.setResource(new Resource(itemList.get(position + 1).getImageUrl()));

            return convertView;
        }
    }
}
