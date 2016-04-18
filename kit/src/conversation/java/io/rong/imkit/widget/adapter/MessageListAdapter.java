package io.rong.imkit.widget.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import java.util.Date;

import de.greenrobot.event.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.model.GroupUserInfo;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.util.RongDateUtils;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imkit.widget.ProviderContainerView;
import io.rong.imkit.widget.provider.IContainerItemProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.PublicServiceProfile;
import io.rong.imlib.model.UserInfo;
import io.rong.message.InformationNotificationMessage;

//import io.rong.imkit.widget.provider.IContainerItemProvider;


/**
 * Created by DragonJ on 14-10-11.
 */
public class MessageListAdapter extends BaseAdapter<UIMessage> {
    LayoutInflater mInflater;
    Context mContext;
    Drawable mDefaultDrawable;
    OnItemHandlerListener mOnItemHandlerListener;
    View subView;
    private boolean timeGone = false;

    class ViewHolder {
        AsyncImageView leftIconView;
        AsyncImageView rightIconView;
        TextView nameView;
        ProviderContainerView contentView;
        ProgressBar progressBar;
        ImageView warning;
        ImageView readReceipt;
        ViewGroup layout;
        TextView time;
        TextView sentStatus;
    }

    public MessageListAdapter(Context context) {
        super();
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mDefaultDrawable = context.getResources().getDrawable(R.drawable.rc_ic_def_msg_portrait);
    }

    public void setOnItemHandlerListener(OnItemHandlerListener onItemHandlerListener) {
        this.mOnItemHandlerListener = onItemHandlerListener;
    }

    public interface OnItemHandlerListener {
        public void onWarningViewClick(int position, Message data, View v);
    }

    @Override
    public long getItemId(int position) {
        Message message = getItem(position);
        if (message == null)
            return -1;
        return message.getMessageId();
    }

    @Override
    protected View newView(final Context context, final int position, ViewGroup group) {
        View result = mInflater.inflate(R.layout.rc_item_message, null);

        final ViewHolder holder = new ViewHolder();
        holder.leftIconView = findViewById(result, R.id.rc_left);
        holder.rightIconView = findViewById(result, R.id.rc_right);
        holder.nameView = findViewById(result, R.id.rc_title);
        holder.contentView = findViewById(result, R.id.rc_content);
        holder.layout = findViewById(result, R.id.rc_layout);
        holder.progressBar = findViewById(result, R.id.rc_progress);
        holder.warning = findViewById(result, R.id.rc_warning);
        holder.readReceipt = findViewById(result, R.id.rc_read_receipt);
        holder.time = findViewById(result, R.id.rc_time);
        holder.sentStatus = findViewById(result, R.id.rc_sent_status);
        if (holder.time.getVisibility() == View.GONE) {
            timeGone = true;
        } else {
            timeGone = false;
        }

        result.setTag(holder);


        return result;
    }

    public void playNextAudioIfNeed(UIMessage data, int position) {
        IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
        if(provider != null && subView != null)
            provider.onItemClick(subView, position, data.getContent(), data);
    }

    @Override
    protected void bindView(View v, final int position, final UIMessage data) {

        ViewHolder holder = (ViewHolder) v.getTag();

        IContainerItemProvider provider = null;

        if (RongContext.getInstance() != null && data != null && data.getContent() != null) {
            provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
            if (provider == null) {
                RLog.e(this, "MessageListAdapter", "bindView provider is null !");
            }
        } else {
            RLog.e(this, "MessageListAdapter", "Message is null !");
        }

        View view = null;

        if (provider != null) {
            view = holder.contentView.inflate(provider);
            provider.bindView(view, position, data);
        } else {
            RLog.e(this, "MessageListAdapter", "bindView provider is null !!");
        }

        subView = view;
        if (data == null)
            return;

        ProviderTag tag = RongContext.getInstance().getMessageProviderTag(data.getContent().getClass());

        if (tag.hide()) {
            holder.contentView.setVisibility(View.GONE);
            holder.time.setVisibility(View.GONE);
            holder.nameView.setVisibility(View.GONE);
            holder.leftIconView.setVisibility(View.GONE);
            holder.rightIconView.setVisibility(View.GONE);
        } else {
            holder.contentView.setVisibility(View.VISIBLE);
        }

        if (data.getMessageDirection() == Message.MessageDirection.SEND) {

            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.VISIBLE);
                holder.leftIconView.setVisibility(View.GONE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.RIGHT);
                holder.contentView.containerViewRight();
                holder.nameView.setGravity(Gravity.RIGHT);
            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            //readRec 是否显示已读回执
            boolean readRec = RongIMClient.getInstance().getReadReceipt();

            if (data.getSentStatus() == Message.SentStatus.SENDING) {
                if (tag.showProgress())
                    holder.progressBar.setVisibility(View.VISIBLE);
                else
                    holder.progressBar.setVisibility(View.GONE);

                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.FAILED) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.VISIBLE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (data.getSentStatus() == Message.SentStatus.SENT) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            } else if (readRec == true && data.getSentStatus() == Message.SentStatus.READ) {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                MessageContent content = data.getMessage().getContent();
                if(!(content instanceof InformationNotificationMessage)) {
                    holder.readReceipt.setVisibility(View.VISIBLE);
                } else {
                    holder.readReceipt.setVisibility(View.GONE);
                }
            }else {
                holder.progressBar.setVisibility(View.GONE);
                holder.warning.setVisibility(View.GONE);
                holder.readReceipt.setVisibility(View.GONE);
            }

            holder.nameView.setVisibility(View.GONE);

            holder.rightIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = data.getUserInfo();
                        if (userInfo == null || userInfo.getName() == null) {
                            if (!TextUtils.isEmpty(data.getSenderUserId())) {
                                userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                                userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                            }
                        }
                        RongContext.getInstance().getConversationBehaviorListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo);
                    }
                }
            });

            holder.rightIconView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {

                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = data.getUserInfo();
                        if (userInfo == null || userInfo.getName() == null) {
                            if (!TextUtils.isEmpty(data.getSenderUserId())) {
                                userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                                userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                            }
                        }
                        return RongContext.getInstance().getConversationBehaviorListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo);
                    }

                    return true;
                }
            });

            if (tag.showWarning() == false)
                holder.warning.setVisibility(View.GONE);

//            holder.sentStatus.setVisibility(View.VISIBLE);

        } else {
            if (tag.showPortrait()) {
                holder.rightIconView.setVisibility(View.GONE);
                holder.leftIconView.setVisibility(View.VISIBLE);
            } else {
                holder.leftIconView.setVisibility(View.GONE);
                holder.rightIconView.setVisibility(View.GONE);
            }

            if (!tag.centerInHorizontal()) {
                setGravity(holder.layout, Gravity.LEFT);
                holder.contentView.containerViewLeft();
                holder.nameView.setGravity(Gravity.LEFT);

            } else {
                setGravity(holder.layout, Gravity.CENTER);
                holder.contentView.containerViewCenter();
                holder.nameView.setGravity(Gravity.CENTER_HORIZONTAL);
                holder.contentView.setBackgroundColor(Color.TRANSPARENT);
            }

            holder.progressBar.setVisibility(View.GONE);
            holder.warning.setVisibility(View.GONE);
            holder.readReceipt.setVisibility(View.GONE);

            holder.nameView.setVisibility(View.VISIBLE);

            if (data.getConversationType() == Conversation.ConversationType.PRIVATE
                    || !tag.showPortrait()
                    || data.getConversationType() == Conversation.ConversationType.PUBLIC_SERVICE
                    || data.getConversationType() == Conversation.ConversationType.APP_PUBLIC_SERVICE) {

                holder.nameView.setVisibility(View.GONE);
            } else {
                UserInfo userInfo;
                if(data.getConversationType() == Conversation.ConversationType.GROUP) {
                    GroupUserInfo groupUserInfo = RongContext.getInstance().getGroupUserInfoFromCache(data.getTargetId(), data.getSenderUserId());
                    if(groupUserInfo != null) {
                        holder.nameView.setText(groupUserInfo.getNickname());
                    } else {
                        userInfo = data.getUserInfo();
                        if(userInfo == null || userInfo.getName() == null)
                            userInfo = RongContext.getInstance().getUserInfoCache().get(data.getSenderUserId());
                        if (userInfo == null)
                            holder.nameView.setText(data.getSenderUserId());
                        else
                            holder.nameView.setText(userInfo.getName());
                    }
                } else {
                    userInfo = data.getUserInfo();
                    if(userInfo == null || userInfo.getName() == null)
                        userInfo = RongContext.getInstance().getUserInfoCache().get(data.getSenderUserId());
                    if (userInfo == null)
                        holder.nameView.setText(data.getSenderUserId());
                    else
                        holder.nameView.setText(userInfo.getName());
                }
            }


            holder.leftIconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        UserInfo userInfo = data.getUserInfo();
                        if (userInfo == null || userInfo.getName() == null) {
                            if (!TextUtils.isEmpty(data.getSenderUserId())) {
                                userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                                userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                            }
                        }
                        RongContext.getInstance().getConversationBehaviorListener().onUserPortraitClick(mContext, data.getConversationType(), userInfo);
                    }
                    EventBus.getDefault().post(Event.InputViewEvent.obtain(false));
                }
            });
        }

        holder.leftIconView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {

                if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                    UserInfo userInfo = data.getUserInfo();
                    if (userInfo == null || userInfo.getName() == null) {
                        if (!TextUtils.isEmpty(data.getSenderUserId())) {
                            userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                            userInfo = userInfo == null ? (new UserInfo(data.getSenderUserId(), null, null)) : userInfo;
                        }
                    }
                    return RongContext.getInstance().getConversationBehaviorListener().onUserPortraitLongClick(mContext, data.getConversationType(), userInfo);
                }

                return false;
            }
        });


        if (holder.rightIconView.getVisibility() == View.VISIBLE) {

            if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && !TextUtils.isEmpty(data.getTargetId()) && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {

                UserInfo info = data.getUserInfo();
                PublicServiceProfile publicServiceProfile = null;
                Uri portrait = null;
                if(info == null) {
                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    portrait = publicServiceProfile.getPortraitUri();
                } else {
                    portrait = info.getPortraitUri();
                }

                if (portrait != null) {
                    Resource resource = new Resource(portrait);
                    holder.rightIconView.setResource(resource);
                } else {
                    holder.rightIconView.setResource(null);
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                UserInfo userInfo = data.getUserInfo();
                if(userInfo == null || userInfo.getName() == null) {
                    userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                }
                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    Resource resource = new Resource(userInfo.getPortraitUri());
                    holder.rightIconView.setResource(resource);
                } else {
                    holder.rightIconView.setResource(null);
                }
            } else {
                holder.rightIconView.setResource(null);
            }
        } else if (holder.leftIconView.getVisibility() == View.VISIBLE) {
            if ((data.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)
                    || data.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE))
                    && !TextUtils.isEmpty(data.getTargetId()) && data.getMessageDirection().equals(Message.MessageDirection.RECEIVE)) {

                UserInfo info = data.getUserInfo();
                PublicServiceProfile publicServiceProfile = null;
                Uri portrait = null;
                if(info == null) {
                    ConversationKey mKey = ConversationKey.obtain(data.getTargetId(), data.getConversationType());
                    publicServiceProfile = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());
                    portrait = publicServiceProfile.getPortraitUri();
                } else {
                    portrait = info.getPortraitUri();
                }
                if (portrait != null) {
                    Resource resource = new Resource(portrait);
                    holder.leftIconView.setResource(resource);
                } else {
                    holder.leftIconView.setResource(null);
                }
            } else if (!TextUtils.isEmpty(data.getSenderUserId())) {
                UserInfo userInfo = data.getUserInfo();
                if(userInfo == null || userInfo.getName() == null) {
                    userInfo = RongContext.getInstance().getUserInfoFromCache(data.getSenderUserId());
                }
                if (userInfo != null && userInfo.getPortraitUri() != null) {
                    Resource resource = new Resource(userInfo.getPortraitUri());
                    holder.leftIconView.setResource(resource);
                } else {
                    holder.leftIconView.setResource(null);
                }
            } else {
                holder.leftIconView.setResource(null);
            }
        }

        if (view != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (RongContext.getInstance().getConversationBehaviorListener() != null) {
                        if (RongContext.getInstance().getConversationBehaviorListener().onMessageClick(mContext, v, data)) {
                            return;
                        }
                    }
                    IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    provider.onItemClick(v, position, data.getContent(), data);
                }
            });

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (RongContext.getInstance().getConversationBehaviorListener() != null)
                        if (RongContext.getInstance().getConversationBehaviorListener().onMessageLongClick(mContext, v, data))
                            return true;

                    IContainerItemProvider.MessageProvider provider = RongContext.getInstance().getMessageTemplate(data.getContent().getClass());
                    provider.onItemLongClick(v, position, data.getContent(), data);
                    return true;
                }
            });
        }

        holder.warning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemHandlerListener != null)
                    mOnItemHandlerListener.onWarningViewClick(position, data, v);
            }
        });

        if (tag.hide()) {
            holder.time.setVisibility(View.GONE);
            return;
        }

        if (!timeGone){
            String time = RongDateUtils.getConversationFormatDate(new Date(data.getSentTime()));
            holder.time.setText(time);
            if(position == 0) {
                holder.time.setVisibility(View.VISIBLE);
            } else {
                Message pre = getItem(position - 1);

                if (data.getSentTime() - pre.getSentTime() > 60 * 1000) {
                    holder.time.setVisibility(View.VISIBLE);
                } else {
                    holder.time.setVisibility(View.GONE);
                }
            }
        }
    }

    private final void setGravity(View view, int gravity) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = gravity;
    }
}
