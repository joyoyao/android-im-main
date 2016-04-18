package io.rong.imkit.widget.provider;

import android.content.Context;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;

import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIMessage;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;

/**
 * Created by DragonJ on 15/6/8.
 */

@ProviderTag(messageContent = MessageContent.class, hide = true, showWarning = false, centerInHorizontal = true, showSummaryWithName = false)
public class DefaultMessageItemProvider extends IContainerItemProvider.MessageProvider<MessageContent> {

    @Override
    public void bindView(View v, int position, MessageContent content, UIMessage message) {

    }

    @Override
    public Spannable getContentSummary(MessageContent data) {
        return null;
    }

    @Override
    public void onItemClick(View view, int position, MessageContent
            content, UIMessage message) {
    }

    @Override
    public void onItemLongClick(View view, int position, MessageContent content, final UIMessage message) {

    }

    @Override
    public View newView(Context context, ViewGroup group) {
        return null;
    }

}