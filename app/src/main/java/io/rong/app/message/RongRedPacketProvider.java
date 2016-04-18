package io.rong.app.message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import io.rong.app.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 *
 * @author lsy
 * @date 2015-11-19
 */
public class RongRedPacketProvider extends InputProvider.ExtendProvider {

    HandlerThread mWorkThread;
    Handler mUploadHandler;
    private int REQUEST_CONTACT = 20;

    public RongRedPacketProvider(RongContext context) {
        super(context);
        mWorkThread = new HandlerThread("RongDemo");
        mWorkThread.start();
        mUploadHandler = new Handler(mWorkThread.getLooper());
    }

    /**
     * 设置展示的图标
     * @param context
     * @return
     */
    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.de_contacts);
    }

    /**
     * 设置图标下的title
     * @param context
     * @return
     */
    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.red_packet);
    }

    /**
     * click 事件
     * @param view
     */
    @Override
    public void onPluginClick(View view) {

        RongRedPacketMessage rongRedPacketMessage = RongRedPacketMessage.obtain("26596","hehe");
        if(RongIM.getInstance()!=null && RongIM.getInstance().getRongIMClient()!=null){
            RongIM.getInstance().getRongIMClient().sendMessage(Conversation.ConversationType.PRIVATE, "26596", rongRedPacketMessage, null, null, new RongIMClient.SendMessageCallback() {
                @Override
                public void onError(Integer messageId, RongIMClient.ErrorCode e) {
                    Log.e("RongRedPacketProvider","------onError---"+e);
                }

                @Override
                public void onSuccess(Integer integer) {
                    Log.e("RongRedPacketProvider","------onSuccess---"+integer);
                }
            });
        }
//        Intent intent = new Intent();
//        intent.setAction(Intent.ACTION_PICK);
//        intent.setData(ContactsContract.Contacts.CONTENT_URI);
//        startActivityForResult(intent, REQUEST_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK)
            return;

        if (data.getData() != null && "content".equals(data.getData().getScheme())) {
            mUploadHandler.post(new MyRunnable(data.getData()));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    class MyRunnable implements Runnable {

        Uri mUri;

        public MyRunnable(Uri uri) {
            mUri = uri;
        }

        @Override
        public void run() {

        }
    }

}
