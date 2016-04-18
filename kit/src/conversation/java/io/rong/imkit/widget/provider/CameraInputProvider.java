package io.rong.imkit.widget.provider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import java.util.ArrayList;

import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.ImageMessage;

import io.rong.imkit.R;

/**
 * Created by DragonJ on 15/4/15.
 */
public class CameraInputProvider extends InputProvider.ExtendProvider {

    public CameraInputProvider(RongContext context) {
        super(context);
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_camera);
    }

    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.rc_plugins_camera);
    }

    @Override
    public void onPluginClick(View view) {
        Intent intent = new Intent();
        intent.setClass(view.getContext(), TakingPicturesActivity.class);
        startActivityForResult(intent, 24);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK || data == null)
            return;

        if (data.getData() != null && "content".equals(data.getData().getScheme())) {
            getContext().executorBackground(new PublishRunnable(data.getData()));
        } else if (data.getData() != null && "file".equals(data.getData().getScheme())) {
            getContext().executorBackground(new PublicLocationRunnable(data.getData()));
        } else if (data.hasExtra(Intent.EXTRA_RETURN_RESULT)) {

            ArrayList<Uri> uris = data.getParcelableArrayListExtra(Intent.EXTRA_RETURN_RESULT);

            for (Uri item : uris) {
                getContext().executorBackground(new PublishRunnable(item));
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendImage(Uri uri) {
        if(mCurrentConversation == null)
            return;

        final ImageMessage content = ImageMessage.obtain(uri, uri);
        final Message message = Message.obtain(mCurrentConversation.getTargetId(), mCurrentConversation.getConversationType(), content);

        RongIM.getInstance().getRongIMClient().sendImageMessage(message, null, null, new RongIMClient.SendImageMessageCallback() {
            @Override
            public void onAttached(Message message) {

            }

            @Override
            public void onError(Message message, RongIMClient.ErrorCode code) {

            }

            @Override
            public void onSuccess(Message message) {

            }

            @Override
            public void onProgress(Message message, int progress) {

            }
        });
    }

    class PublishRunnable implements Runnable {

        Uri mUri;

        public PublishRunnable(Uri uri) {
            mUri = uri;
        }

        @Override
        public void run() {
            Cursor cursor = getContext().getContentResolver().query(mUri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);

            if (cursor == null || cursor.getCount() == 0) {
                cursor.close();
                return;
            }

            cursor.moveToFirst();
            Uri uri = Uri.parse("file://" + cursor.getString(0));
            cursor.close();

            sendImage(uri);
        }
    }

    class PublicLocationRunnable implements Runnable {
        Uri mUri;

        public PublicLocationRunnable(Uri uri) {
            mUri = uri;
        }

        @Override
        public void run() {
            sendImage(mUri);
        }
    }
}