package io.rong.voipkit.activity;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import io.rong.imkit.RongContext;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.UserInfo;
import io.rong.voipkit.message.VoIPCallMessage;
import io.rong.voipkit.message.VoIPFinishMessage;
import io.rong.voipkit.model.VOIPCallBackObject;
import io.rong.voiplib.NativeObject;
import io.rong.voiplib.RongIMVoIP;
import io.rong.imkit.R;
import io.rong.voiplib.utils.VoIPUtil;
import com.sea_monster.resource.Resource;
import io.rong.imkit.widget.AsyncImageView ;

public class CallSideActivity extends BaseActivity {

    private String peerUserName;
    private TextView userName;
    private ImageView sound_off, hands_off;
    private View maskView;
    private String myselfName, token;

    public static final int STARTVOIP_SUCCESS = 0;
    public static final int STARTVOIP_ERROR = 1;
    private static boolean isActiviyShow = false;

    Handler connectedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STARTVOIP_SUCCESS) {
                VoIPCallMessage vcm = (VoIPCallMessage) msg.obj;

                sendMsg(vcm);

                playerRingtone(AudioManager.MODE_IN_CALL, BaseActivity.RINGSTYLE_CALL);
            } else if (msg.what == STARTVOIP_ERROR) {
                String info = getResources().getString(R.string.rc_network_is_busy);
                if (msg.arg1 == 404) {
                    info = getResources().getString(R.string.rc_called_is_calling);
                }
                if (isActiviyShow) {
                    new AlertDialog.Builder(CallSideActivity.this)
                            .setTitle(info)
                            .setMessage(getResources().getString(R.string.rc_exit_calling))
                            .setPositiveButton(getResources().getText(R.string.rc_yes), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CallSideActivity.this.finish();
                        }
                    }).show();
                }
            }
        }
    };


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_callside_layout);

        userName = (TextView) findViewById(R.id.user_name);
        sound_off = (ImageView) findViewById(R.id.sound_off_id);
        hands_off = (ImageView) findViewById(R.id.hands_off_id);
        user_photo = (AsyncImageView) findViewById(R.id.user_photo);

        calling_state = (TextView) findViewById(R.id.calling_state);
        maskView = findViewById(R.id.mask_layout);

        reciveIntentData();

        if (RongContext.getInstance() != null && RongContext.getInstance().getUserInfoCache() != null && !TextUtils.isEmpty(peerid)) {
            UserInfo userInfo = RongContext.getInstance().getUserInfoCache().get(peerid);
            if (userInfo != null) {
                userName.setText(userInfo.getName());
                user_photo.setResource(new Resource(userInfo.getPortraitUri()));
            }
        }

//        userName.setText(peerUserName);
//        setPhoto(peerUserPhoteUri);

        delayTimesFinish();
        Log.i("fff", "===================called====================mode==" + mAudioManager.getMode() + "===" + mySelfId + "===" + appId + "===" + sessionId);

//        RongContext.getInstance().getEventBus().register(this);
    }

    public void onEvent(final UserInfo userInfoArg) {
        if(userInfoArg!=null&&!TextUtils.isEmpty(peerid)&&peerid.equals(userInfoArg.getUserId())){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userName.setText(userInfoArg.getName());
                    user_photo.setResource(new Resource(userInfoArg.getPortraitUri()));
                }
            });
        }
    }

    public void onEventBackgroundThread(VOIPCallBackObject voipCallBackObject) {

        if (voipCallBackObject != null && !voipCallBackObject.isSuccess()) {

            if (voipCallBackObject.getObject() != null && voipCallBackObject.getObject() instanceof RongIMClient.ErrorCode) {
                RongIMClient.ErrorCode errorCode = (RongIMClient.ErrorCode) voipCallBackObject.getObject();

//                if (errorCode.getValue() == RongIMClient.SendMessageCallback.ErrorCode.REJECTED_BY_BLACKLIST.getValue()) {
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(CallSideActivity.this, getResources().getString(R.string.rc_rejected_by_blacklist_voice_prompt));
//                            CallSideActivity.this.finish();
//                        }
//                    });
//                }
            }
        }

    }


    protected void showMaskLayout(boolean isShow) {
        if (isShow) {
            maskView.setVisibility(View.VISIBLE);
        } else {
            maskView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActiviyShow = true;
        connectVoIPServer_call();
    }

    private void reciveIntentData() {
        Bundle bundle = getIntent().getExtras();

        appId = bundle.getString("appId");
        token = bundle.getString("token");
        mySelfId = bundle.getString("mySelfId");
//        myselfName = bundle.getString("myselfName");

        peerid = bundle.getString("peerUId");
//        peerUserName = bundle.getString("peerUserName");
        peerUserPhoteUri = bundle.getString("peerUserPhoteUri");
    }

    private void connectVoIPServer_call() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                localPort = VoIPUtil.getPort();
                RongIMVoIP.startVoIP(appId, token, mySelfId, peerid, localPort, new NativeObject.StartVoIPCallback() {

                    @Override
                    public void OnSuccess(String sessionId, String ip,
                                          String remoteTransferPort, String remoteControlPort) {
                        Log.i("voip",
                                "===============java enter OnSuccess===============" + sessionId + "====" + ip + "===" + remoteTransferPort);
                        isVoIPSuccess = true;
                        CallSideActivity.this.sessionId = sessionId;
                        CallSideActivity.this.remoteIp = ip;
                        CallSideActivity.this.remotePort = Integer.parseInt(remoteTransferPort);
                        Message msg = connectedHandler.obtainMessage();

                        VoIPCallMessage vcm = new VoIPCallMessage(sessionId, ip, Integer.parseInt(remoteTransferPort),
                                Integer.parseInt(remoteControlPort), peerid, peerUserName, mySelfId, myselfName);
                        msg.obj = vcm;
                        msg.what = STARTVOIP_SUCCESS;
                        connectedHandler.sendMessage(msg);
                    }

                    @Override
                    public void OnError(int errorcode, String description) {
                        Log.i("void", "============java enter OnError=======================" + errorcode + "===" + description);
                        isVoIPSuccess = false;

                        Message msg = connectedHandler.obtainMessage();
                        msg.what = STARTVOIP_ERROR;
                        msg.arg1 = errorcode;
                        connectedHandler.sendMessage(msg);
                    }
                });
            }
        }).start();
    }

    /**
     * 挂机
     */
    public void doFinishChat(View view) {
        Log.i("aff", "======================finishChat==================");
        VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
        vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
        sendMsg(vfm);

        this.finish();
    }

    @Override
    public void onBackPressed() {
        VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
        vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
        sendMsg(vfm);
        super.onBackPressed();
    }

    Handler delayHandler;
    Runnable finishActivity;

    private void delayTimesFinish() {
        delayHandler = new Handler();
        finishActivity = new Runnable() {

            @Override
            public void run() {
                Toast.makeText(CallSideActivity.this, getResources().getString(R.string.rc_called_not_accept), Toast.LENGTH_SHORT).show();

                VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
                vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
                sendMsg(vfm);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CallSideActivity.this.finish();
            }
        };
        delayHandler.postDelayed(finishActivity, 60 * 1000);
    }

    /**
     * 被叫方接受了呼叫
     */
    public void CalledAcceptCall() {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {

                Log.i("effaaa", "=========================被叫方接受了呼叫请求==============================");
                Toast.makeText(CallSideActivity.this, getResources().getString(R.string.rc_called_accept), Toast.LENGTH_SHORT).show();
                delayHandler.removeCallbacks(finishActivity);
                startCountTime();
                releaseRingtong();

                createVoIP();

                enableHandOffAndSoundOff(sound_off, hands_off);

            }
        });
    }

    @Override
    protected void onDestroy() {
//        RongContext.getInstance().getEventBus().unregister(this);
        isActiviyShow = false;
        super.onDestroy();
        delayHandler.removeCallbacks(finishActivity);
    }
}