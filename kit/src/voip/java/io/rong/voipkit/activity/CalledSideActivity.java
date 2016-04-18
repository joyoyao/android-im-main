package io.rong.voipkit.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sea_monster.resource.Resource;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;
import io.rong.imlib.model.MessageContent;
import io.rong.imlib.model.UserInfo;
import io.rong.voipkit.message.VoIPAcceptMessage;
import io.rong.voipkit.message.VoIPCallMessage;
import io.rong.voipkit.message.VoIPFinishMessage;
import io.rong.voipkit.model.VOIPCallBackObject;
import io.rong.voiplib.NativeObject.AcceptVoIPCallback;
import io.rong.voiplib.RongIMVoIP;
import io.rong.voiplib.utils.VoIPUtil;

public class CalledSideActivity extends BaseActivity {

    private Button call_finish;
    private LinearLayout vioce_control_container;
    private LinearLayout voipCalledLayout;
    private TextView user_name;
    private View maskView;
    private ImageView sound_off, hands_off;

    private VoIPCallMessage voipCallMessage;

    private RongIMClient mRongIMClient = null;

    private boolean isPushJumpHere = false;
    private String fromUserNameByPush = "";

    Handler acceptVoIPHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            if (!isPushJumpHere)
                sendMsg(new VoIPAcceptMessage(peerid));
            else
                sendMessageByPushJump(new VoIPAcceptMessage(peerid));
            switchView();
            startCountTime();
            createVoIP();
        }
    };


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.rc_calledside_layout);

        call_finish = (Button) findViewById(R.id.call_finish);
        vioce_control_container = (LinearLayout) findViewById(R.id.vioce_control_container);
        voipCalledLayout = (LinearLayout) findViewById(R.id.rc_voip_called_layout);
        user_name = (TextView) findViewById(R.id.user_name);
        user_photo = (AsyncImageView) findViewById(R.id.user_photo);
        calling_state = (TextView) findViewById(R.id.calling_state);

        sound_off = (ImageView) findViewById(R.id.sound_off_id);
        hands_off = (ImageView) findViewById(R.id.hands_off_id);
        maskView = findViewById(R.id.mask_layout);

        Bundle bundle = getIntent().getExtras();

        if (bundle.getString("appId") != null && bundle.getBoolean("push", false)) {//push 过来
            appId = bundle.getString("appId");
//            String token = getIntent().getStringExtra("token");
            String token = this.getSharedPreferences("RONG_SDK", this.MODE_PRIVATE).getString("token_value", "");
            peerUserPhoteUri = bundle.getString("fromUserPhoteUri");
            fromUserNameByPush = bundle.getString("fromUserName");
            Log.i("fff", "===================doPush=====================ak=" + appId + "=token=" + token + "==fromUserName=" + fromUserNameByPush);
            isPushJumpHere = true;
            doPush(appId, token);
        } else {
            isPushJumpHere = false;
            appId = bundle.getString("appId");
            voipCallMessage = (VoIPCallMessage) bundle.getParcelable("VoIPCallMessage");
            peerUserPhoteUri = bundle.getString("peerUserPhoteUri");
            if (voipCallMessage != null) {
                initParam();
            }
        }


        if (mAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)
            playerRingtone(AudioManager.MODE_NORMAL, BaseActivity.RINGSTYLE_CALLED);

        if (RongContext.getInstance() != null && RongContext.getInstance().getUserInfoCache() != null && !TextUtils.isEmpty(peerid)) {
            UserInfo userInfo = RongContext.getInstance().getUserInfoCache().get(peerid);
            if (userInfo != null) {
                user_name.setText(userInfo.getName());
                user_photo.setResource(new Resource(userInfo.getPortraitUri()));
            }
        }
    }

    public void onEvent(final UserInfo userInfoArg) {
        if (userInfoArg != null && !TextUtils.isEmpty(peerid) && peerid.equals(userInfoArg.getUserId())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    user_name.setText(userInfoArg.getName());
                    user_photo.setResource(new Resource(userInfoArg.getPortraitUri()));
                }
            });
        }
    }

    protected void showMaskLayout(boolean isShow) {
        if (isShow) {
            maskView.setVisibility(View.VISIBLE);
        } else {
            maskView.setVisibility(View.GONE);
        }
    }

    private void initParam() {
        Log.i("aff", "=================initParam==============up===" + voipCallMessage.getToId());
        mySelfId = voipCallMessage.getToId();
        peerid = voipCallMessage.getFromId();
//        user_name.setText(voipCallMessage.getFromUserName());
//        setPhoto(peerUserPhoteUri);

        remoteIp = voipCallMessage.getIp();
        remotePort = voipCallMessage.getRemoteTransferPort();
        sessionId = voipCallMessage.getSessionId();

        Log.i("fff", "============================mySelfId=" + mySelfId + "==peerid=" + peerid + "==un="
                + user_name + "===remoteIp=" + remoteIp + "==remotePort=" + remotePort + "==sid=" + sessionId + "=--=" + voipCallMessage.getFromUserName());
    }

    Handler connectedHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {

            if (!CalledSideActivity.this.isFinishing()) {
                String info = getResources().getString(R.string.rc_network_error);
                new AlertDialog.Builder(CalledSideActivity.this).setTitle(info)
                        .setMessage(getResources().getString(R.string.rc_exit_calling))
                        .setPositiveButton(getResources().getString(R.string.rc_yes), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        CalledSideActivity.this.finish();
                    }
                }).show();
            }
        }
    };

    private void connectVoIPServer_accept() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                localPort = VoIPUtil.getPort();
                RongIMVoIP.acceptVoIP(appId, sessionId, mySelfId, remoteIp,
                        remotePort, localPort,
                        voipCallMessage.getRemoteControlPort(), new AcceptVoIPCallback() {
                            public void OnSuccess() {
                                Log.i("voip", "==============acceptVoIP=======java enter OnSuccess===================");
                                isVoIPSuccess = true;
                                android.os.Message msg = acceptVoIPHandler.obtainMessage();
                                acceptVoIPHandler.sendMessage(msg);
                            }

                            public void OnError(int errorcode, String description) {
                                isVoIPSuccess = false;
                                Log.e("voip", "===========acceptVoIP=====================java enter OnError=====");
                                connectedHandler.sendEmptyMessage(0);
                            }
                        });
            }
        }).start();
    }

    /**
     * 应答
     */
    public void doYes(View view) {

        if (voipCallMessage == null) {
            Toast.makeText(CalledSideActivity.this, getResources().getString(R.string.rc_network_exception), Toast.LENGTH_SHORT).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finish();
            return;
        }
        Log.i("fff", "====================doYes=======================");

        releaseRingtong();

        connectVoIPServer_accept();
    }

    /**
     * 拒绝
     */
    public void doRefuse(View view) {

        if (voipCallMessage == null) {
            Toast.makeText(CalledSideActivity.this, getResources().getString(R.string.rc_network_exception), Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finish();
            return;
        }
        VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
        vfm.setFinish_state(VoIPFinishMessage.FINISH_REFUSE);
        if (!isPushJumpHere) {
            sendFinishMessage(vfm);
        }
        else {
            sendMessageByPushJump(vfm);
            this.finish();
        }
    }

    protected void sendFinishMessage(MessageContent messageContent) {
        String targetId = null;
        final MessageTag msgTag = ((Object) messageContent).getClass().getAnnotation(MessageTag.class);
        final String objectName = msgTag.value();
        VoIPFinishMessage voIPFinishMessage = (VoIPFinishMessage) messageContent;
        targetId = voIPFinishMessage.getToId();

        RongIMClient.getInstance().sendMessage(Conversation.ConversationType.PRIVATE, targetId, messageContent, null, null, new RongIMClient.SendMessageCallback() {
            @Override
            public void onSuccess(Integer messageId) {
                RLog.i(this, "startVoIp", "---sendMessage--onSuccess----");
                CalledSideActivity.this.finish();
            }

            @Override
            public void onError(Integer messageId, RongIMClient.ErrorCode errorCode) {
                RLog.i(this, "startVoIp", "---sendMessage--onFailure----");
                VOIPCallBackObject voipCallBackObject = new VOIPCallBackObject();
                voipCallBackObject.setSuccess(false);
                voipCallBackObject.setObjectName(objectName);
                voipCallBackObject.setObject(errorCode);

                RongContext.getInstance().getEventBus().post(voipCallBackObject);
                CalledSideActivity.this.finish();
            }

        }, null);
    }

    @Override
    public void onBackPressed() {

        if (voipCallMessage == null) {
            Toast.makeText(CalledSideActivity.this, getResources().getString(R.string.rc_network_exception), Toast.LENGTH_LONG).show();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finish();
            return;
        }
        VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
        vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
        if (!isPushJumpHere)
            sendMsg(vfm);
        else
            sendMessageByPushJump(vfm);
        super.onBackPressed();
    }

    /**
     * 挂机
     */
    public void doFinishChat(View view) {
        VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
        vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
        if (!isPushJumpHere) {
            sendFinishMessage(vfm);
        } else {
            sendMessageByPushJump(vfm);
            this.finish();
        }
    }

    private void switchView() {
        call_finish.setVisibility(View.VISIBLE);
        vioce_control_container.setVisibility(View.VISIBLE);
        voipCalledLayout.setVisibility(View.GONE);

        enableHandOffAndSoundOff(sound_off, hands_off);
    }


    public void doPush(String appKey, String token) {
//		if(!PushUtil.processesIsAlive(this,PushUtil.getAppName(this))){
        RongIMClient.init(this);
//			Log.e("aff","=========RongIMClient===init==================");
//		}
        try {
            mRongIMClient = RongIMClient.connect(token, new RongIMClient.ConnectCallback() {

                @Override
                public void onSuccess(String userId) {
                    Log.i("fff", "=============voip.connect======onSuccess======================");
                    registerReceiveMessage();
                }

                @Override
                public void onError(RongIMClient.ErrorCode errorCode) {
                    Log.e("fff", "=============voip.connect======onError======================");
                }

                @Override
                public void onTokenIncorrect() {
                    Log.e("fff", "=============voip.connect======onTokenIncorrect======================");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final int MESSAGE_VOIPCALL = 1020;
    private static final int MESSAGE_VOIPFINISH = 1021;

    Handler doPushRecevieHandler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MESSAGE_VOIPCALL:
                    if (voipCallMessage == null) {
                        Toast.makeText(CalledSideActivity.this, getResources().getString(R.string.rc_network_exception), Toast.LENGTH_LONG).show();
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        finish();
                        return;
                    }
                    initParam();
                    break;
                case MESSAGE_VOIPFINISH:
                    Toast.makeText(CalledSideActivity.this, getResources().getString(R.string.rc_called_on_hook), Toast.LENGTH_LONG).show();
                    stopCountTime();
                    finish();
                    break;
                default:
                    break;
            }

        }
    };

    public void registerReceiveMessage() {
        final long startTime = System.currentTimeMillis();

        mRongIMClient.setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageListener() {
            @Override
            public boolean onReceived(Message message, int left) {
                if (message.getConversationType() == Conversation.ConversationType.PRIVATE) {
                    Log.i("afff", "==============================push=====message====type=======" + message.getContent());
                    if (message.getContent() instanceof VoIPCallMessage) {
                        voipCallMessage = (VoIPCallMessage) message.getContent();
                        Log.i("fff", "=========================push=================voipcall===============" + voipCallMessage.getFromId());
                        if (voipCallMessage != null)
                            voipCallMessage.setFromUserName(fromUserNameByPush);
                        doPushRecevieHandler.sendEmptyMessage(MESSAGE_VOIPCALL);

                    } else if (message.getContent() instanceof VoIPFinishMessage) {
                        Log.i("fff", "=========================push==================voipfinish==============");
                        long endTime = System.currentTimeMillis();
                        if ((endTime - startTime) > 1000) {
                            doPushRecevieHandler.sendEmptyMessage(MESSAGE_VOIPFINISH);
                        }
                    }
                }
                return false;
            }
        });

        mRongIMClient.setConnectionStatusListener(new RongIMClient.ConnectionStatusListener() {
            @Override
            public void onChanged(ConnectionStatus status) {
                if (ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT == status) {
                    Log.i("aff", "================================你被踢了===============================");
                    doPushRecevieHandler.sendEmptyMessage(MESSAGE_VOIPFINISH);
                }
            }
        });
    }


    public void sendMessageByPushJump(MessageContent mc) {
        Log.d("aaa", "============voip===sendmessage==========");
        if (mc instanceof VoIPCallMessage) {
            VoIPCallMessage vcm = (VoIPCallMessage) mc;
            mRongIMClient.sendMessage(Conversation.ConversationType.PRIVATE, vcm.getToId(), vcm, null, null, new MySendMessageCallback());
        } else if (mc instanceof VoIPAcceptMessage) {
            VoIPAcceptMessage vam = (VoIPAcceptMessage) mc;
            mRongIMClient.sendMessage(Conversation.ConversationType.PRIVATE, vam.getToId(), vam, null, null, new MySendMessageCallback());
        } else if (mc instanceof VoIPFinishMessage) {
            VoIPFinishMessage vfm = (VoIPFinishMessage) mc;
            mRongIMClient.sendMessage(Conversation.ConversationType.PRIVATE, vfm.getToId(), vfm, null, null, new MySendMessageCallback());
        }
    }

    class MySendMessageCallback extends RongIMClient.SendMessageCallback {

        @Override
        public void onError(Integer messageId, RongIMClient.ErrorCode errorCode) {
            Log.i("SendMessageBroadcast", "================onError==============" + errorCode);
        }

        @Override
        public void onSuccess(Integer integer) {
            Log.i("SendMessageBroadcast", "=================onSucces====================");

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPushJumpHere)
            android.os.Process.killProcess(android.os.Process.myPid());
    }
}