package io.rong.imkit.widget.provider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.model.ProviderTag;
import io.rong.imkit.model.UIConversation;
import io.rong.imkit.model.ConversationProviderTag;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.util.RongDateUtils;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.PublicServiceProfile;

@ConversationProviderTag(conversationType = "public_service", portraitPosition = 1)
public class PublicServiceConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation>{

    private ConversationKey mKey;

    @Override
    public String getTitle(String id) {
        String name;
        mKey = ConversationKey.obtain(id, Conversation.ConversationType.PUBLIC_SERVICE);
        PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());

        if (info != null) {
            name = info.getName();
        } else {
            name = "";
        }
        return name;
    }

    @Override
    public Uri getPortraitUri(String id) {
        Uri uri;
        mKey = ConversationKey.obtain(id, Conversation.ConversationType.PUBLIC_SERVICE);
        PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(mKey.getKey());

        if (info != null) {
            uri = info.getPortraitUri();
        } else {
            uri = null;
        }
        return uri;
    }

    @Override
    public void bindView(View view, int position, UIConversation data) {
        super.bindView(view, position, data);
    }
}
