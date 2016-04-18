package io.rong.imkit.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import io.rong.imkit.RongContext;
import io.rong.imkit.widget.InputView;

/**
 * Created by DragonJ on 14-7-19.
 */
public interface IVoiceHandler {
    public class VoiceException extends RuntimeException {
        public VoiceException(Throwable e) {
            super(e);
        }
    }

    public interface OnPlayListener {
        public void onPlay(Context context);

        public void onCover(boolean limited);

        public void onStop();

        public void onFinish();
    }

    public void setPlayListener(OnPlayListener listener);

    public void play(Context context, Uri uri) throws VoiceException;

    public void stop();

    public Uri getCurrentPlayUri();


    public class VoiceHandler implements IVoiceHandler, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, SensorEventListener {

        private AudioManager mAudioManager;
        private SensorManager mSensorManager;
        private PowerManager mPowerManager;

        private MediaPlayer mMediaPlayer;
        private Sensor mSensor;
        private PowerManager.WakeLock mLock;

        private OnPlayListener mPlayListener;

        float mLastEventValue = Float.MIN_VALUE;

        private Uri mCurrentUri;

        public VoiceHandler(Context context) {

            try{
                mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                mLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "VoiceHandler");
                RongContext.getInstance().getEventBus().register(this);
            }catch (NullPointerException e){
                e.printStackTrace();
            }

        }

        public void setPlayListener(OnPlayListener listener) {
            mPlayListener = listener;
        }


        public void play(final Context context, Uri uri) throws VoiceException {
            stop();

            if (uri == null)
                return;

            mCurrentUri = uri;

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mLock.acquire(mp.getDuration());

                    if (mPlayListener != null)
                        mPlayListener.onPlay(context);
                    if (mSensor != null) {
                        mSensorManager.registerListener(VoiceHandler.this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        mAudioManager.setMode(AudioManager.MODE_NORMAL);
                    }
                }
            });

            File file = new File(mCurrentUri.getPath());
            if (!file.exists())
                return ;

            try {
                FileInputStream fis = new FileInputStream(file);
                mMediaPlayer.setDataSource(fis.getFD());
//                mMediaPlayer.setDataSource(context, uri);
                mMediaPlayer.prepare();
            } catch (IllegalArgumentException e) {
                throw new VoiceException(e);
            } catch (SecurityException e) {
                throw new VoiceException(e);
            } catch (IllegalStateException e) {
                throw new VoiceException(e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stop() {

            mCurrentUri = null;

            if (mMediaPlayer == null)
                return;

            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            if (mPlayListener != null) {
                mPlayListener.onStop();
            }

            mSensorManager.unregisterListener(VoiceHandler.this);
        }


        public Uri getCurrentPlayUri() {
            return mCurrentUri;
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            mCurrentUri = null;
            if (mMediaPlayer == null)
                return;

            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayListener.onFinish();
            mSensorManager.unregisterListener(VoiceHandler.this);
        }

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mMediaPlayer.reset();
            return false;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[0];

            if (mLastEventValue == Float.MIN_VALUE) {
                mLastEventValue = value;
            }

            if (value > mLastEventValue) {
                mLastEventValue = value;
            }

            if (value >= mLastEventValue) {
                if (mAudioManager.getMode() != AudioManager.MODE_NORMAL)
                    mAudioManager.setMode(AudioManager.MODE_NORMAL);

                if (mPlayListener != null) {
                    mPlayListener.onCover(false);
                }

            } else {
                if (mAudioManager.getMode() != AudioManager.MODE_IN_CALL) {
                    mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                }

                if (mPlayListener != null) {
                    mPlayListener.onCover(true);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        public void onEvent(InputView.Event event) {
            if (mCurrentUri != null && event == InputView.Event.DESTROY) {
                stop();
            }
        }

    }
}
