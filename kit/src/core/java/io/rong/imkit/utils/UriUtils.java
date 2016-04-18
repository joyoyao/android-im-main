package io.rong.imkit.utils;

import android.content.Context;
import android.net.Uri;

import io.rong.imlib.model.Message;

/**
 * Created by DragonJ on 14-9-12.
 */
public class UriUtils {

    public static Uri obtainThumImageUri(Context context, Message message){
        Uri uri = Uri.parse("rong://"+context.getPackageName()).buildUpon().appendPath("image").appendPath("thum").appendPath(String.valueOf(message.getMessageId())).build();
        return uri;
    }

    public static Uri obtainVoiceUri(Context context, Message message){
        Uri uri = Uri.parse("rong://"+context.getPackageName()).buildUpon().appendPath("voice").appendPath(String.valueOf(message.getMessageId())).build();
        return uri;
    }
}
