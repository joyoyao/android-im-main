package io.rong.app.message.provider;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imlib.model.Conversation;
//import io.rong.message.FileMessage;

/**
 * Created by AMing on 15/12/24.
 * Company RongCloud
 */
public class SendFileProvider extends InputProvider.ExtendProvider {

    private static final String TAG = SendFileProvider.class.getSimpleName();
    private Context context;

    /**
     * 实例化适配器。
     *
     * @param context 融云IM上下文。（通过 RongContext.getInstance() 可以获取）
     */
    public SendFileProvider(RongContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(io.rong.imkit.R.drawable.rc_ic_picture);
    }

    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return "文件";
    }

    @Override
    public void onPluginClick(View view) {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
//        startActivityForResult(intent, 1);
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("file/*");
        startActivityForResult(i, 1);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            if (data.getData().getScheme().equals("file")) {
                String s = data.getData().getPath();
                Uri uri = data.getData();
                File file = new File(s);
                if (file.exists()) {
                    Log.e("file", "f是文件且存在");
                   Conversation conversation = getCurrentConversation();
                   sendFile(conversation.getConversationType(),conversation.getTargetId(),file,uri);
                } else {
                    Toast.makeText(context,"文件不存在",Toast.LENGTH_SHORT);
                }
            }

        }
    }

    private void sendFile(Conversation.ConversationType conversationType, String id, File file,Uri uri) {
        if (RongIM.getInstance()!= null && RongIM.getInstance().getRongIMClient() != null) {
            //TODO 文件消息

            Log.e("tag","");

            Uri themUri = Uri.parse("file:///sdcard/bob/bob.zip" );

//            Uri localUri = Uri.parse("file:///sdcard/bob/bob.zip");
//            FileMessage fileMessage = FileMessage.obtain(uri,uri);
//            ImageMessage fileMessage = ImageMessage.obtain(themUri,localUri);

//            RongIM.getInstance().getRongIMClient().sendImageMessage(Conversation.ConversationType.PRIVATE, id, fileMessage, null, null, new RongIMClient.SendImageMessageCallback() {
//                @Override
//                public void onAttached(Message message) {
//
//                    Log.e(TAG, "-------------onAttached--------");
//                }
//
//                @Override
//                public void onError(Message message, RongIMClient.ErrorCode code) {
//                    Log.e(TAG, "----------------onError-----" + code);
//                }
//
//                @Override
//                public void onSuccess(Message message) {
//                    Log.e(TAG, "------------------onSuccess---");
//                }
//
//                @Override
//                public void onProgress(Message message, int progress) {
//                    Log.e(TAG, "-----------------onProgress----" + progress);
//
//                }
//            });
        }
    }
}
