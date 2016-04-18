package io.rong.imkit.widget.provider;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;

/**
 * Created by zhjchen on 3/23/15.
 */

public class VoIPInputProvider extends InputProvider.ExtendProvider {

    HandlerThread mWorkThread;
    Handler mVoIPInputProviderHandler;
//    boolean isSynGetUserInfo = false;

    Handler handler;
    long mDeltaTime = 0;

    public VoIPInputProvider(RongContext context) {
        super(context);

        mWorkThread = new HandlerThread("VoIPInputProvider");
        mWorkThread.start();
        mVoIPInputProviderHandler = new Handler(mWorkThread.getLooper());

        handler = new Handler(Looper.getMainLooper());

        RLog.e(this, "VoIPInputProvider", "----------构造方法----------");

        RongContext.getInstance().getEventBus().register(this);
    }

    @Override
    public Drawable obtainPluginDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_phone);
    }


    @Override
    public CharSequence obtainPluginTitle(Context context) {
        return context.getString(R.string.rc_plugins_voip);
    }


    @Override
    public void onPluginClick(View view) {

        RongIMClient.ConnectionStatusListener.ConnectionStatus connectionStatus = RongIM.getInstance().getRongIMClient().getCurrentConnectionStatus();

        if (RongIMClient.ConnectionStatusListener.ConnectionStatus.CONNECTED != connectionStatus) {
            Log.e("VoIPInputProvider", "unable to access the server !");
            return;
        }

        String targetId = getCurrentConversation().getTargetId();

//        UserInfo targetUserInfo = RongContext.getInstance().getUserInfoFromCache(targetId);


//        if (targetUserInfo != null) {
            Intent intent = new Intent();

            String appKey = null;

            try {
                ApplicationInfo applicationInfo = RongContext.getInstance().getPackageManager().getApplicationInfo(RongContext.getInstance().getPackageName(), PackageManager.GET_META_DATA);
                appKey = RongContext.getInstance().getAppKey();
                if(TextUtils.isEmpty(appKey)) {
                    appKey = applicationInfo.metaData.getString("RONG_CLOUD_APP_KEY");
                }

            } catch (PackageManager.NameNotFoundException e) {
                RLog.e(this, "VoIPInputProvider", "appkey is null");
            }

            String token = RongContext.getInstance().getSharedPreferences("rc_token", Context.MODE_PRIVATE).getString("token_value", "");

            intent.putExtra("appId", appKey);
            intent.putExtra("token", token);
            intent.putExtra("mySelfId", RongIM.getInstance().getRongIMClient().getCurrentUserId());
//            intent.putExtra("myselfName", RongIM.getInstance().getRongIMClient().getCurrentUserInfo().getName());
            intent.putExtra("peerUId", targetId);
//            intent.putExtra("peerUserName", targetUserInfo.getName());
//            intent.putExtra("peerUserPhoteUri", targetUserInfo.getPortraitUri().toString());

            openVoIPActivity(intent);

//        }
//        else {
//            isSynGetUserInfo = true;
//        }

    }

    public void onEventBackgroundThread(final io.rong.imkit.model.Event.OnReceiveVoIPMessageEvent receiveMessageEvent) {

       final Message message = receiveMessageEvent.getMessage();


        if (message != null && message.getConversationType() != null && message.getMessageDirection() == Message.MessageDirection.RECEIVE) {
            io.rong.imkit.RLog.i(this, "onEvent-voip", ((Object) message.getContent()).getClass().getName());

            if (((Object) message.getContent()).getClass().getName().equals("io.rong.voipkit.message.VoIPCallMessage")) {

                //final CountDownLatch countDownLatch = new CountDownLatch(1);

                //TODO async
//                mDeltaTime = RongIM.getInstance().getRongIMClient().getDeltaTime(new EmojiMessageAdapter.LibCallback<Long>() {
//
//                    @Override
//                    public void onSuccess(Long time) {
//                        mDeltaTime = time;
//                        countDownLatch.countDown();
//                    }
//
//                    @Override
//                    public void onFailure(LibException e) {
//                        countDownLatch.countDown();
//                    }
//                });
//
//                try {
//                    countDownLatch.await();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

                mDeltaTime = RongIM.getInstance().getRongIMClient().getDeltaTime();

                handler.post(new Runnable() {

                    @Override
                    public void run() {

                        long normalTime = System.currentTimeMillis() - mDeltaTime;

                        if ((normalTime - message.getSentTime()) > 20 * 1000) {
                            return;
                        }

                        if (!android.os.Build.CPU_ABI.contains("arm")) {
                            return;
                        }


                        io.rong.imkit.RLog.i(this, "onEventBackgroundThread(message)", "----receive VoIP message-----VoIPCallMessage---"+message.getObjectName());

                        Intent intent = new Intent();

                        Uri uri = Uri.parse("rong://" + RongContext.getInstance().getApplicationInfo().packageName).buildUpon()
                                .appendPath("VoIPAccept").build();

                        String appKey = null;

                        try {
                            ApplicationInfo applicationInfo = RongContext.getInstance().getPackageManager().getApplicationInfo(RongContext.getInstance().getPackageName(), PackageManager.GET_META_DATA);
                            appKey = RongContext.getInstance().getAppKey();
                            if(TextUtils.isEmpty(appKey)) {
                                appKey = applicationInfo.metaData.getString("RONG_CLOUD_APP_KEY");
                            }

                        } catch (PackageManager.NameNotFoundException e) {
                            RLog.e(this, "VoIPInputProvider", "appkey is null");
                        }

                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(uri);
                        intent.putExtra("VoIPCallMessage", message.getContent());
                        intent.putExtra("appId", appKey);
                        intent.putExtra("peerUserPhoteUri", "");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        RongContext.getInstance().startActivity(intent);

                    }
                });
            }
        }

    }


//    public void onEvent(UserInfo userInfoArg) {
//
//        if (isSynGetUserInfo) {
//            String targetId = getCurrentConversation().getTargetId();
//
//            if (userInfoArg.getUserId().equals(targetId)) {
//
//                UserInfo userInfo = RongContext.getInstance().getCurrentUserInfo();
//                UserInfo targetUserInfo = RongContext.getInstance().getUserInfoFromCache(targetId);
//
//                Intent intent = new Intent();
//                String appKey = null;
//
//                try {
//                    ApplicationInfo applicationInfo = RongContext.getInstance().getPackageManager().getApplicationInfo(RongContext.getInstance().getPackageName(), PackageManager.GET_META_DATA);
//                    appKey = applicationInfo.metaData.getString("RONG_CLOUD_APP_KEY");
//
//                } catch (PackageManager.NameNotFoundException e) {
//                    RLog.e(this, "VoIPInputProvider", "appkey is null");
//                }
//
//                String token = RongContext.getInstance().getSharedPreferences("rc_token", Context.MODE_PRIVATE).getString("token_value", "");
//                intent.putExtra("appId", appKey);
//                intent.putExtra("token", token);
//                intent.putExtra("mySelfId", userInfo.getUserId());
//                intent.putExtra("myselfName", userInfo.getName());
//                intent.putExtra("peerUId", targetUserInfo.getUserId());
//                intent.putExtra("peerUserName", targetUserInfo.getName());
//                intent.putExtra("peerUserPhoteUri", targetUserInfo.getPortraitUri().toString());
//
//                openVoIPActivity(intent);
//            }
//        }
//    }


    private final void openVoIPActivity(final Intent intent) {

        mVoIPInputProviderHandler.post(new Runnable() {

            @Override
            public void run() {

                Uri uri = Uri.parse("rong://" + mFragment.getActivity().getApplicationInfo().packageName).buildUpon()
                        .appendPath("VoIPCall").build();

                intent.setData(uri);
                intent.setAction(Intent.ACTION_VIEW);

                mFragment.getActivity().startActivity(intent);
            }
        });
    }
}
