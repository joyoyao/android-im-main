package io.rong.app.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import io.rong.app.R;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;

/**
 * Created by Bob_ge on 15/11/24.
 */
public class TextActivityA extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.a_layout_a);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String targetID = intent.getStringExtra("ddsfasf");

        if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
            RongIM.getInstance().getRongIMClient().clearMessagesUnreadStatus(Conversation.ConversationType.PRIVATE, targetID, new RongIMClient.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {

                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });


        }

    }
}
