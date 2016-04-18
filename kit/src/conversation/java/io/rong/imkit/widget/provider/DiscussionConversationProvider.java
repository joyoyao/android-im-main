package io.rong.imkit.widget.provider;

import android.net.Uri;
import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.model.ConversationProviderTag;

@ConversationProviderTag(conversationType = "discussion", portraitPosition = 1)
public class DiscussionConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation> {
    @Override
    public String getTitle(String id) {
        String name;
        if (RongContext.getInstance().getDiscussionInfoFromCache(id) == null) {
            name = RongContext.getInstance().getResources().getString(R.string.rc_conversation_list_default_discussion_name);
        } else {
            name = RongContext.getInstance().getDiscussionInfoFromCache(id).getName();
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        return null;
    }
}
