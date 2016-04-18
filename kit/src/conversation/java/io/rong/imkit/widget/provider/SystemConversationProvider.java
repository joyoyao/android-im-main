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

@ConversationProviderTag(conversationType = "system", portraitPosition = 1)
public class SystemConversationProvider extends PrivateConversationProvider implements IContainerItemProvider.ConversationProvider<UIConversation>{

    @Override
    public String getTitle(String id) {
        String name;
        if (RongContext.getInstance().getUserInfoFromCache(id) == null) {
            name = id;
        } else {
            name = RongContext.getInstance().getUserInfoFromCache(id).getName();
        }
        return name;
    }



}
