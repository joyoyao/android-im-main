package io.rong.imkit.widget.provider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.message.LocationMessage;

/**
 * Created by zhjchen on 3/17/15.
 */

public class LocationInputProvider extends InputProvider.ExtendProvider {

    Message mCurrentMessage;

    public LocationInputProvider(RongContext context) {
        super(context);
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_location);
    }

    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.rc_plugins_location);
    }

    @Override
    public void onPluginClick(View view) {
        if (RongContext.getInstance() != null && RongContext.getInstance().getLocationProvider() != null) {
            RongContext.getInstance().getLocationProvider().onStartLocation(getContext(), new RongIM.LocationProvider.LocationCallback() {

                @Override
                public void onSuccess(final LocationMessage locationMessage) {

                    RongIM.getInstance().getRongIMClient().insertMessage(mCurrentConversation.getConversationType(), mCurrentConversation.getTargetId(), RongIM.getInstance().getRongIMClient().getCurrentUserId(), locationMessage, new RongIMClient.ResultCallback<Message>() {
                        @Override
                        public void onSuccess(Message message) {
                            if (locationMessage.getImgUri() != null) {
                                if (locationMessage.getImgUri().getScheme().equals("http")) {
                                    message.setContent(locationMessage);
                                    getContext().executorBackground(new DownloadRunnable(message, locationMessage.getImgUri()));
                                } else if(locationMessage.getImgUri().getScheme().equals("file")){
                                    RongIM.getInstance().getRongIMClient().sendMessage(message, null, null, null);
                                } else {
                                    RLog.e(this, "onPluginClick", locationMessage.getImgUri().getScheme() + " scheme does not support!");
                                }
                            } else {
                                RLog.e(this, "onPluginClick", "File does not exist!");
                            }
                        }
                        @Override
                        public void onError(RongIMClient.ErrorCode e) {

                        }
                    });
                }
                @Override
                public void onFailure(String msg) {

                }
            });
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();
    }

    public Message getCurrentMessage() {
        return mCurrentMessage;
    }

    class DownloadRunnable implements Runnable {
        private Message message;
        private Uri uri;

        public DownloadRunnable(Message message, Uri uri) {
            this.message = message;
            this.uri = uri;
        }

        @Override
        public void run() {
            mCurrentMessage = message;
            HttpUriRequest request = new HttpGet(uri.toString());
            DefaultHttpClient client = new DefaultHttpClient();
            final Event.OnReceiveMessageProgressEvent event = new Event.OnReceiveMessageProgressEvent();
            LocationMessage locationMessage = (LocationMessage) message.getContent();
            event.setMessage(message);
            event.setProgress(100);
            getContext().getEventBus().post(event);
            try {
                HttpResponse response = client.execute(request);
                StatusLine statusLine = response.getStatusLine();
                int code = statusLine.getStatusCode();
                if(code >= 200 && code < 300) {
                    HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    Uri path = obtainImageUri(getContext());
                    final File file = new File(path.getPath() + File.separator + message.getMessageId());
                    OutputStream os = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int read = -1;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.close();
                    is.close();
                    locationMessage.setImgUri(Uri.fromFile(file));
                    RongIM.getInstance().getRongIMClient().sendMessage(message, null, null, new RongIMClient.SendMessageCallback() {
                        @Override
                        public void onError(Integer messageId, RongIMClient.ErrorCode e) {
                            if(file.exists()) {
                                file.delete();
                            }
                            message.setSentStatus(Message.SentStatus.FAILED);
                            getContext().getEventBus().post(event);
                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            if(file.exists()) {
                                file.delete();
                            }
                            message.setSentStatus(Message.SentStatus.SENT);
                            event.setProgress(100);
                            getContext().getEventBus().post(event);
                        }
                    });
                } else {
                    message.setSentStatus(Message.SentStatus.FAILED);
                    getContext().getEventBus().post(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Uri obtainImageUri(Context context) {
        File file = context.getFilesDir();
        String path = file.getAbsolutePath();
        Uri uri = Uri.parse(path);
        return uri;
    }
}
