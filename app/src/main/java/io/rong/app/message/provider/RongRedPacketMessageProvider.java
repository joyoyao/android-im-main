package io.rong.app.message.provider;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.rong.app.R;
import io.rong.app.message.RongRedPacketMessage;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imkit.widget.provider.IContainerItemProvider;


/**
 * 自定义融云IM消息提供者
 * 
 * @author lsy
 * 
 */
@ProviderTag(messageContent = RongRedPacketMessage.class, showPortrait = true, showProgress = true, centerInHorizontal = false)
// 会话界面自定义UI注解
public class RongRedPacketMessageProvider extends IContainerItemProvider.MessageProvider<RongRedPacketMessage> {

	/**
	 * 初始化View
	 */
	@Override
	public View newView(Context context, ViewGroup group) {
		View view = LayoutInflater.from(context).inflate(
				R.layout.de_customize_message_red_packet, null);
		ViewHolder holder = new ViewHolder();
		holder.message = (TextView) view.findViewById(R.id.textView1);
		holder.view = (View) view.findViewById(R.id.rc_img);
		view.setTag(holder);
		return view;
	}

	@Override
	public void bindView(View v, int position, RongRedPacketMessage content, UIMessage message) {
		ViewHolder holder = (ViewHolder) v.getTag();

		// 更改气泡样式
		if (message.getMessageDirection() == UIMessage.MessageDirection.SEND) {
			// 消息方向，自己发送的
			holder.view.setBackgroundResource(R.drawable.rc_ic_bubble_right);
		} else {
			// 消息方向，别人发送的
			holder.view.setBackgroundResource(R.drawable.rc_ic_bubble_left);
		}
		holder.message.setText(content.getMessage()); // 设置消息内容
	}

	@Override
	public Spannable getContentSummary(RongRedPacketMessage data) {
		if (data != null && !TextUtils.isEmpty(data.getMessage()))
			return new SpannableString(data.getMessage());
		return null;
	}

	@Override
	public void onItemClick(View view, int position, RongRedPacketMessage content, UIMessage message) {

	}

	@Override
	public void onItemLongClick(View view, int position, RongRedPacketMessage content, UIMessage message) {

	}

	class ViewHolder {
		TextView message;
		View view;
	}

}
