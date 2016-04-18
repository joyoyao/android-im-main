package io.rong.imkit.widget.provider;

import android.net.Uri;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.model.ConversationProviderTag;

@ConversationProviderTag(conversationType = "group", portraitPosition = 1)
public class GroupConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation> {
    @Override
    public String getTitle(String groupId) {
        String name;
        if (RongContext.getInstance().getGroupInfoFromCache(groupId) == null) {
            name = "";
        } else {
            name = RongContext.getInstance().getGroupInfoFromCache(groupId).getName();
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        Uri uri;
        if (RongContext.getInstance().getGroupInfoFromCache(id) == null) {
            uri = null;
        } else {
            uri = RongContext.getInstance().getGroupInfoFromCache(id).getPortraitUri();
        }
        return uri;
    }


}
