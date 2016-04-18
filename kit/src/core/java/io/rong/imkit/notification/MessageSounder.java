package io.rong.imkit.notification;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

/**
 * Created by zhjchen on 4/11/15.
 */

public class MessageSounder {

    private static Context mContext;
    static MessageSounder mRoundMgr;
    Handler mHandler;
    NewMessageReminderRunnable mLastReminderRunnable;

    public static void init(Context context) {
        mContext = context;
        mRoundMgr = new MessageSounder();
    }

    MessageSounder() {
        mHandler = new Handler();
    }

    public static MessageSounder getInstance() {
        return mRoundMgr;
    }

    public void messageReminder() {
        if (mLastReminderRunnable == null) {
            mLastReminderRunnable = new NewMessageReminderRunnable();
            mHandler.post(mLastReminderRunnable);
        } else {
            mHandler.removeCallbacks(mLastReminderRunnable);
            mHandler.postDelayed(mLastReminderRunnable, 500);
        }
    }

    class NewMessageReminderRunnable implements Runnable {

        @Override
        public void run() {

            final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

            switch (audioManager.getRingerMode()) {
                case AudioManager.RINGER_MODE_SILENT:
                    break;
                case AudioManager.RINGER_MODE_VIBRATE:
                    Vibrator vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(200);
                    break;
                case AudioManager.RINGER_MODE_NORMAL:
                    Uri alert = null;

//                    if (RongContext.getInstance() != null && RongContext.getInstance().getNewMessageSoundProvider() != null) {
//                        alert = RongContext.getInstance().getNewMessageSoundProvider().getSoundSource();
//                    } else {
                        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                    }

                    if (alert != null)
                        playSound(alert);

                    break;
            }

        }
    }

    private int getMobileRingerMode() {
        final AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getRingerMode();
    }


    private void playSound(Uri uri) {

        try {
            MediaPlayer mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });

            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d("RongIMService", "playNewMessageSound---onCompletion");
                    mp.reset();
                    mp.release();
                    mp = null;
                }
            });

            mMediaPlayer.setDataSource(mContext, uri);
            mMediaPlayer.prepare();


        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

}
