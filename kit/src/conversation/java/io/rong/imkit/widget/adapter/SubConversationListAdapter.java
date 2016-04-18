package io.rong.imkit.widget.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.ProviderContainerView;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.model.Conversation;

public class SubConversationListAdapter extends BaseAdapter<UIConversation> {

    LayoutInflater mInflater;
    Context mContext;

    @Override
    public long getItemId(int position) {
        UIConversation conversation = getItem(position);
        if (conversation == null)
            return 0;
        return conversation.hashCode();
    }

    class ViewHolder {
        View layout;
        View leftImageLayout;
        View rightImageLayout;
        AsyncImageView leftImageView;
        AsyncImageView rightImageView;
        ProviderContainerView contentView;
        TextView unReadMsgCount;
        TextView unReadMsgCountRight;
        ImageView unReadMsgCountRightIcon;
        ImageView unReadMsgCountIcon;
    }

    public SubConversationListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
    }

    public int findPosition(Conversation.ConversationType type, String targetId) {
        int index = getCount();
        int position = -1;

        while (index-- > 0) {
            if (getItem(index).getConversationType().equals(type)
                    && getItem(index).getConversationTargetId().equals(targetId)) {
                position = index;
                break;
            }
        }

        return position;
    }

    @Override
    protected View newView(Context context, int position, ViewGroup group) {
        View result = mInflater.inflate(R.layout.rc_item_conversation, group,false);

        ViewHolder holder = new ViewHolder();
        holder.layout = findViewById(result, R.id.rc_item_conversation);
        holder.leftImageLayout = findViewById(result, R.id.rc_item1);
        holder.rightImageLayout = findViewById(result, R.id.rc_item2);
        holder.leftImageView = findViewById(result, R.id.rc_left);
        holder.rightImageView = findViewById(result, R.id.rc_right);
        holder.contentView = findViewById(result, R.id.rc_content);
        holder.unReadMsgCount = findViewById(result, R.id.rc_unread_message);
        holder.unReadMsgCountRight = findViewById(result, R.id.rc_unread_message_right);
        holder.unReadMsgCountIcon = findViewById(result, R.id.rc_unread_message_icon);
        holder.unReadMsgCountRightIcon = findViewById(result, R.id.rc_unread_message_icon_right);

        result.setTag(holder);

        return result;
    }

    @Override
    protected void bindView(View v, int position, UIConversation data) {
        ViewHolder holder = (ViewHolder) v.getTag();

        /*通过会话类型，获得对应的会话provider.ex: PrivateConversationProvider*/
        IContainerItemProvider provider = RongContext.getInstance().getConversationTemplate(data.getConversationType().getName());

        View view = holder.contentView.inflate(provider);

        provider.bindView(view, position, data);

        //设置背景色
        if (data.isTop())
            holder.layout.setBackgroundColor(mContext.getResources().getColor(R.color.rc_conversation_top_bg));
        else
            holder.layout.setBackgroundColor(mContext.getResources().getColor(R.color.rc_text_color_primary_inverse));

        ConversationProviderTag tag = RongContext.getInstance().getConversationProviderTag(data.getConversationType().getName());

        // 1:图像靠左显示。2：图像靠右显示。3：不显示图像。
        if (tag.portraitPosition() == 1) {
            holder.leftImageLayout.setVisibility(View.VISIBLE);

            if (data.getConversationType()==Conversation.ConversationType.GROUP) {
                holder.leftImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_group_default_portrait));
            }else if(data.getConversationType()==Conversation.ConversationType.DISCUSSION) {
                holder.leftImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_default_discussion_portrait));
            }else{
                holder.leftImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_default_portrait));
            }

            if (data.getIconUrl() != null) {
                holder.leftImageView.setResource(new Resource(data.getIconUrl()));
            } else {
                holder.leftImageView.setResource(null);
            }

            if (data.getUnReadMessageCount() > 0) {
                holder.unReadMsgCountIcon.setVisibility(View.VISIBLE);
                if(data.getUnReadType().equals(UIConversation.UnreadRemindType.REMIND_WITH_COUNTING)) {
                    holder.unReadMsgCount.setVisibility(View.VISIBLE);
                    if (data.getUnReadMessageCount() > 99) {
                        holder.unReadMsgCount.setText(mContext.getResources().getString(R.string.rc_message_unread_count));
                    } else {
                        holder.unReadMsgCount.setText(Integer.toString(data.getUnReadMessageCount()));
                    }
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_count_bg);
                } else {
                    holder.unReadMsgCount.setVisibility(View.GONE);
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_remind_without_count);
                }
            } else {
                holder.unReadMsgCountIcon.setVisibility(View.GONE);
                holder.unReadMsgCount.setVisibility(View.GONE);
            }
            holder.rightImageLayout.setVisibility(View.GONE);
        } else if (tag.portraitPosition() == 2) {
            holder.rightImageLayout.setVisibility(View.VISIBLE);

            if (data.getConversationType()==Conversation.ConversationType.GROUP) {
                holder.rightImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_group_default_portrait));
            }else if(data.getConversationType()==Conversation.ConversationType.DISCUSSION) {
                holder.rightImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_default_discussion_portrait));
            }else{
                holder.rightImageView.setDefaultDrawable(v.getContext().getResources().getDrawable(R.drawable.rc_default_portrait));
            }

            if (data.getIconUrl() != null) {
                holder.rightImageView.setResource(new Resource(data.getIconUrl()));
            } else {
                holder.rightImageView.setResource(null);
            }

            if (data.getUnReadMessageCount() > 0) {
                holder.unReadMsgCountRight.setVisibility(View.VISIBLE);
                holder.unReadMsgCountIcon.setVisibility(View.VISIBLE);
                if(data.getUnReadType().equals(UIConversation.UnreadRemindType.REMIND_WITH_COUNTING)) {
                    if (data.getUnReadMessageCount() > 99) {
                        holder.unReadMsgCountRight.setText(mContext.getResources().getString(R.string.rc_message_unread_count));
                    } else {
                        holder.unReadMsgCountRight.setText(Integer.toString(data.getUnReadMessageCount()));
                    }
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_count_bg);
                } else {
                    holder.unReadMsgCountIcon.setImageResource(R.drawable.rc_unread_remind_without_count);
                }
            }

            holder.leftImageLayout.setVisibility(View.GONE);
        } else if (tag.portraitPosition() == 3) {
            holder.rightImageLayout.setVisibility(View.GONE);
            holder.leftImageLayout.setVisibility(View.GONE);
        } else {
            throw new IllegalArgumentException("the portrait position is wrong!");
        }
    }
}
