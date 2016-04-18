package io.rong.voipkit.activity;

//import io.rong.imkit.model.TextMessage;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ultrapower.mcs.engine.ITransportListener;
import com.ultrapower.mcs.engine.TransportType;
import com.ultrapower.mcs.engine.UMCS;
import com.ultrapower.mcs.engine.UmcsConfig;
import com.ultrapower.mcs.engine.internal.UMCSInternal;

import java.io.IOException;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.Event;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.MessageContent;
import io.rong.message.InformationNotificationMessage;
import io.rong.voipkit.message.VoIPAcceptMessage;
import io.rong.voipkit.message.VoIPCallMessage;
import io.rong.voipkit.message.VoIPFinishMessage;
import io.rong.voipkit.model.VOIPCallBackObject;
import io.rong.voiplib.RongIMVoIP;
import io.rong.voiplib.utils.VoIPUtil;
import io.rong.imkit.widget.AsyncImageView ;

public abstract class BaseActivity extends Activity implements SensorEventListener {

//	public static final String RECIVE_MSG_BROADCAST_ACTION = "com.ccrc.avtest.action.reciveMsg";
//	protected ReciveMsgBroadCastReciver rmb;

    protected UMCS session = null;
    protected UMCSInternal uUMCSInternal;
    protected AudioManager mAudioManager = null;
    private WakeLock wakeLock = null;

    protected String remoteIp = "";
    protected int localPort, remotePort;

    protected String peerid;
    protected String appId, mySelfId, sessionId, peerUserPhoteUri;


    protected boolean isVoIPSuccess = false;//startVoIP或acceptVoIP是否成功

    protected TextView calling_state;
    protected AsyncImageView user_photo;
    protected MediaPlayer mMediaPlayer;

    protected int initAudioMode;//进入本应用时，系统初始的audiaMdoe

    private Sensor mSensor;
    private SensorManager mSensorManager;
    private boolean isSupportCPU = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

//		rmb = new ReciveMsgBroadCastReciver();
//		IntentFilter intentFilter = new IntentFilter(this.getPackageName()+RECIVE_MSG_BROADCAST_ACTION);
//		this.registerReceiver(rmb, intentFilter);

        RongContext.getInstance().getEventBus().register(this);

        try {
            initAudioManager();
            isSupportCPU = true;
        } catch (Throwable e){
            finish();
        }
//		
        initAudioMode = mAudioManager.getMode();
    }


    private void initAudioManager() {
        PowerManager pm = (PowerManager) this
                .getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "avtest");

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        wakeLock.acquire(); // screen stay on during the call

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);//proximity 亲近/临近

        if (null == session) {
            // session = new AVSession(this);
            session = new UMCS(this);
        }
        if (null == uUMCSInternal) {
            uUMCSInternal = new UMCSInternal();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    float mLastEventValue = Float.MIN_VALUE;

    @Override
    public void onSensorChanged(SensorEvent event) {
//    	 float range = event.values[0];
//    	 
//         if (range == mSensor.getMaximumRange()) {
//             Toast.makeText(this, "正常模式", Toast.LENGTH_LONG).show();
//             Log.i("afff","===================正常模式============================");
//             showMaskLayout(false);
//         } else {
//             Toast.makeText(this, "听筒模式", Toast.LENGTH_LONG).show();
//             Log.i("afff","====================听筒模式===========================");
//             showMaskLayout(true);
//         }


        float value = event.values[0];
        if (mLastEventValue == Float.MIN_VALUE) {
            mLastEventValue = value;
        }

        if (value > mLastEventValue) {
            mLastEventValue = value;
        }

        if (value >= mLastEventValue) {
            Toast.makeText(this, "正常模式", Toast.LENGTH_SHORT).show();
            Log.i("afff", "===================正常模式============================");
            showMaskLayout(false);
        } else {
            Toast.makeText(this, "听筒模式", Toast.LENGTH_SHORT).show();
            Log.i("afff", "====================听筒模式===========================");
            showMaskLayout(true);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    protected abstract void showMaskLayout(boolean isShow);

    protected void initCall() {
        UmcsConfig uConfig = new UmcsConfig();
        uConfig.setMultiMode(false);
        uConfig.setTraceFilter(2);
        uConfig.setTransportType(TransportType.kUdpGernal);
        int ret = session.Init(uConfig);
        // init
        if (-1 == ret) {
            return;
        }
    }

    protected void createVoIP() {
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        initCall();
        createLocaleAudio();// create audio
        createRemoteAudio();
        StartChat();
    }

//	private void initSpeaker() {
//		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
//	}

    protected void createLocaleAudio() {
//		initSpeaker();
        int ret;
        ret = uUMCSInternal.SetECEnable(1, 4);// ((ecEnable == true) ? 1 : 0, 3); // 3
        // speakerphone, 4 loud
        // speakerphone
        if (-1 == ret) {
            return;
        }
        ret = uUMCSInternal.SetNSEnable(1, 6);// ((nsEnable == true) ? 1 : 0, 6); // 4
        // moderate, 5 high, 6 very high
        if (-1 == ret) {
            return;
        }
        //ret = session.SetMicVolumeAutoGain(1, 4);// ((gainEnable == true) ? 1 :
        // 0, 4); // 4 FixedDigital
        if (-1 == ret) {
            return;
        }
        ret = uUMCSInternal.SetHighPassFilterEnable(1);// ((hfEnable == true) ? 1 :
        // 0);
        if (-1 == ret) {
            return;
        }

        ret = session.CreateLocalAudio(1);

        if (-1 == ret) {
            return;
        }
    }

    protected void createRemoteAudio() {
        // create remote audio
        int ret = session.CreateRemoteAudio(1);
        if (-1 == ret) {
            return;
        }
    }

    protected void StartChat() {
        //createRemote();
        int ret = 0;// 3, session, 1,1)
        String localIp = VoIPUtil.getLocalIpAddress(this);
        ret = session.StartTransport(localIp, localPort, remoteIp, remotePort, null, new ITransportListener() {
            @Override
            public void OnTransportFailed() {

            }
        });
        ret = session.StartSendLocalAudio(1);
        if (-1 == ret) {
            return;
        }

        ret = session.StartRecvRemoteAudio(1);
        if (-1 == ret) {
            return;
        }
    }


    protected void sendMsg(MessageContent messageContent) {
        String targetId = null;
        final MessageTag msgTag = ((Object) messageContent).getClass().getAnnotation(MessageTag.class);
        final String objectName = msgTag.value();

        if (messageContent instanceof VoIPCallMessage) {
            VoIPCallMessage voIPCallMessage = (VoIPCallMessage) messageContent;
            targetId = voIPCallMessage.getToId();
        } else if (messageContent instanceof VoIPAcceptMessage) {
            VoIPAcceptMessage voIPAcceptMessage = (VoIPAcceptMessage) messageContent;
            targetId = voIPAcceptMessage.getToId();
        } else if (messageContent instanceof VoIPFinishMessage) {
            VoIPFinishMessage voIPFinishMessage = (VoIPFinishMessage) messageContent;
            targetId = voIPFinishMessage.getToId();
        }

        RongIMClient.getInstance().sendMessage(Conversation.ConversationType.PRIVATE, targetId, messageContent, null, null, new RongIMClient.SendMessageCallback() {


            @Override
            public void onSuccess(Integer messageId) {
                RLog.i(this, "startVoIp", "---sendMessage--onSuccess----");
            }

            @Override
            public void onError(Integer messageId, RongIMClient.ErrorCode errorCode) {
                RLog.i(this, "startVoIp", "---sendMessage--onFailure----");
                VOIPCallBackObject voipCallBackObject = new VOIPCallBackObject();
                voipCallBackObject.setSuccess(false);
                voipCallBackObject.setObjectName(objectName);
                voipCallBackObject.setObject(errorCode);

                RongContext.getInstance().getEventBus().post(voipCallBackObject);

            }

        }, null);
//
//		Intent intent = new Intent();
//		intent.setAction(this.getPackageName()+".io.rong.imkitvoip.broadcast.SENDMESSAGE");//向 imkit中的io.rong.imkit.broadcast。SendMessageBroadcast这个广播类发消息
//		intent.putExtra("messageContent", mc);
//		sendBroadcast(intent);// 发送广播
    }

    public void onEventBackgroundThread(RongIMClient.ConnectionStatusListener.ConnectionStatus connectionStatus) {

        if (connectionStatus == RongIMClient.ConnectionStatusListener.ConnectionStatus.KICKED_OFFLINE_BY_OTHER_CLIENT) {

            VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
            vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
            sendMsg(vfm);

            BaseActivity.this.finish();
        }
    }


    public void onEventBackgroundThread(Event.OnReceiveVoIPMessageEvent receiveMessageEvent) {
        final io.rong.imlib.model.Message message = receiveMessageEvent.getMessage();

        if (message.getMessageDirection() == io.rong.imlib.model.Message.MessageDirection.SEND)
            return;


        final MessageContent messageContent = message.getContent();

        Log.d("voip", "==========voip=====接收到一个com.ccrc.avtest.action.reciveMsg的广播====" + messageContent);

        if (messageContent instanceof VoIPAcceptMessage) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((CallSideActivity) BaseActivity.this).CalledAcceptCall();// 通知主叫方，被叫方接受了呼叫请求
                }
            });

        } else if (messageContent instanceof VoIPFinishMessage) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    InformationNotificationMessage informationMessage = null;
                    VoIPFinishMessage vfm = (VoIPFinishMessage) messageContent;
                    if (vfm.getFinish_state() == VoIPFinishMessage.FINISH_NORMAL) {
                        informationMessage = InformationNotificationMessage.obtain("对方已经挂机");
                        RongIM.getInstance().getRongIMClient().insertMessage(message.getConversationType(), message.getTargetId(), "rong", informationMessage, null);
                        stopCountTime();
                    } else if (vfm.getFinish_state() == VoIPFinishMessage.FINISH_REFUSE) {
                        informationMessage = InformationNotificationMessage.obtain("对方拒绝通话");
                        RongIM.getInstance().getRongIMClient().insertMessage(message.getConversationType(), message.getTargetId(), "rong", informationMessage, null);
                    }
                    BaseActivity.this.finish();
                }
            });


        }
    }



//	/**
//	 * 通知消息已经发送到指定的Activity后，在该activity中发出的 一个广播，并被该接收器接收。 发送广播 Intent intent =
//	 * new Intent(); intent.setAction(NOTIFICATION_SENDED_BROADCAST_ACTION);
//	 * sendBroadcast(intent);//发送广播
//	 */
//	public class ReciveMsgBroadCastReciver extends BroadcastReceiver {
//
//		public void onReceive(Context contex, Intent intent) {
//			io.rong.imlib.model.Message.MessageContent mc = (io.rong.imlib.model.Message.MessageContent) intent
//					.getParcelableExtra("messageContent");
//			Log.d("voip","==========voip=====接收到一个com.ccrc.avtest.action.reciveMsg的广播===="+ mc);
//			if(mc == null && intent.getBooleanExtra("kicked_message",false)){//踢人
//				VoIPFinishMessage vfm = new VoIPFinishMessage(peerid);
//				vfm.setFinish_state(VoIPFinishMessage.FINISH_NORMAL);
//				sendMsg(vfm);
//
//				BaseActivity.this.finish();
//			}else if (mc instanceof VoIPAcceptMessage) {
//
//				((CallSideActivity) BaseActivity.this).CalledAcceptCall();// 通知主叫方，被叫方接受了呼叫请求
//
//			} else if (mc instanceof VoIPFinishMessage) {
//
//				VoIPFinishMessage vfm = (VoIPFinishMessage) mc;
//				if (vfm.getFinish_state() == VoIPFinishMessage.FINISH_NORMAL) {
//					Toast.makeText(BaseActivity.this, "对方已经挂机！！！",Toast.LENGTH_LONG).show();
//
//					stopCountTime();
//
////					sendNormalMsg("通话时长"+calling_state.getText().toString());
//
//				} else if (vfm.getFinish_state() == VoIPFinishMessage.FINISH_REFUSE) {
//					Toast.makeText(BaseActivity.this, "对方拒绝了你的请求！！！",
//							Toast.LENGTH_LONG).show();
//				}
//				BaseActivity.this.finish();
//			}
//		}
//	}


    private String showTimeCount(long time) {
        if (time >= 360000000) {
            return "00:00:00";
        }
        String timeCount = "";
        long hourc = time / 3600000;
        String hour = "0" + hourc;
        hour = hour.substring(hour.length() - 2, hour.length());

        long minuec = (time - hourc * 3600000) / (60000);
        String minue = "0" + minuec;
        minue = minue.substring(minue.length() - 2, minue.length());

        long secc = (time - hourc * 3600000 - minuec * 60000) / 1000;
        String sec = "0" + secc;
        sec = sec.substring(sec.length() - 2, sec.length());
        timeCount = hour + ":" + minue + ":" + sec;
        return timeCount;
    }


    private Handler stepTimeHandler;
    private Runnable mTicker;
    long startTime = 0;

    public void startCountTime() {

        // 清零 开始计时
        calling_state.setText("00:00:00");
        stepTimeHandler = new Handler();
        startTime = System.currentTimeMillis();
        mTicker = new Runnable() {
            public void run() {
                String content = showTimeCount(System.currentTimeMillis()
                        - startTime);
                calling_state.setText(content);

                long now = SystemClock.uptimeMillis();
                long next = now + (1000 - now % 1000);
                stepTimeHandler.postAtTime(mTicker, next);
            }
        };
        // 启动计时线程，定时更新
        mTicker.run();
    }

    public void stopCountTime() {
        if (stepTimeHandler != null)
            stepTimeHandler.removeCallbacks(mTicker);
    }


    public static final int RINGSTYLE_CALL = 88;
    public static final int RINGSTYLE_CALLED = 188;

    /**
     * 播放 听筒音/铃声
     */
    protected void playerRingtone(int audiaMode, int ringstyle) {
        Log.d("playerNotificationSound", "==================playerNotificationSound============");
        setVolumeControlStream(AudioManager.STREAM_ALARM);
        mAudioManager.setMode(audiaMode);
        try {
            String ringname = null;
            if (ringstyle == RINGSTYLE_CALL) {
                ringname = "voip_ring_call.mp3";
            } else {
                ringname = "voip_ring_called.mp3";
            }
            AssetFileDescriptor fileDescriptor = this.getAssets().openFd(ringname);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(fileDescriptor.getFileDescriptor(),
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getLength());
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void releaseRingtong() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        recoverAudiaMode();
    }

    private void recoverAudiaMode() {
        mAudioManager.setMode(initAudioMode);
    }


    /**
     * 使静音和免提按钮生效
     */
    protected void enableHandOffAndSoundOff(final ImageView sound_off, final ImageView hands_off) {

        sound_off.getDrawable().setLevel(2);

        sound_off.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("aff", "=================sound_off_id================");
                if (sound_off.getDrawable().getLevel() == 2) {//静音
                    session.StopSendLocalAudio(1);
                    sound_off.getDrawable().setLevel(3);
                } else if (sound_off.getDrawable().getLevel() == 3) {//恢复
                    session.StartSendLocalAudio(1);
                    sound_off.getDrawable().setLevel(2);
                }
            }
        });

        hands_off.getDrawable().setLevel(2);

        hands_off.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("aff", "=================hands_off================");
                if (hands_off.getDrawable().getLevel() == 2) {//免提
                    session.SetLoudSpeakerEnable(true);
                    hands_off.getDrawable().setLevel(3);
                } else if (hands_off.getDrawable().getLevel() == 3) {//恢复
                    session.SetLoudSpeakerEnable(false);
                    hands_off.getDrawable().setLevel(2);
                }
            }
        });
    }


    private Handler setPhotoHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bitmap bitmap = (Bitmap) msg.obj;
            if (bitmap != null)
                user_photo.setImageBitmap(bitmap);
        }
    };

//    protected void setPhoto(final String uri) {
//
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                Log.i("aff", "======================setPhoto==================" + uri);
//                BitmapFactory.Options sDefaultOptions = new BitmapFactory.Options();
//                sDefaultOptions.inDither = true;
//                sDefaultOptions.inScaled = true;
//                sDefaultOptions.inDensity = DisplayMetrics.DENSITY_MEDIUM;
//                sDefaultOptions.inTargetDensity = getResources().getDisplayMetrics().densityDpi;
//
//                InputStream inputStream;
//                Bitmap bitmap = null;
////                try {
////                    inputStream = new URL(uri).openStream();
////                    bitmap = BitmapFactory.decodeStream(inputStream, null, sDefaultOptions);
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//                Message msg = setPhotoHandler.obtainMessage();
//                msg.obj = bitmap;
////                setPhotoHandler.sendMessage(msg);
//            }
//
//        }).start();
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(!isSupportCPU){
            Toast.makeText(this, R.string.rc_voip_cpu_error, Toast.LENGTH_SHORT).show();
        }else {
            RongContext.getInstance().getEventBus().unregister(this);

            session.StartSendLocalAudio(1);
            session.SetLoudSpeakerEnable(false);
            session.Terminate();

            Log.d("fff", "==============onDestroy=====================" + isVoIPSuccess);
            if (isVoIPSuccess)
                RongIMVoIP.endVoIP(appId, sessionId, mySelfId);

//        this.unregisterReceiver(rmb);
            releaseRingtong();
            wakeLock.release();
        }
    }
}
