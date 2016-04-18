package io.rong.imkit.fragment;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;

public abstract class BaseFragment extends Fragment implements Handler.Callback {
    public static final String TOKEN = "RONG_TOKEN";
    public static final int UI_RESTORE = 1;
    private Handler mHandler;
    Thread mThread;

    private LayoutInflater mInflater;

    private static final Configuration CONFIGURATION_INFINITE = new Configuration.Builder()
            .setDuration(Configuration.DURATION_INFINITE)
            .build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String token = null;

        mHandler = new Handler(this);
        mThread = Thread.currentThread();

        if (savedInstanceState != null) {
            token = savedInstanceState.getString(TOKEN);
        }
        if (token != null && (RongIM.getInstance() == null || RongIM.getInstance().getRongIMClient() == null)) {
            RLog.i(this, "BaseFragment", "auto reconnect");
            RongIM.connect(token, new RongIMClient.ConnectCallback() {
                @Override
                public void onSuccess(String s) {
                    mHandler.sendEmptyMessage(UI_RESTORE);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {
                    RLog.e(this, "onError(...)", "ErrorCode:" + e);
                }

                @Override
                public void onTokenIncorrect() {
                    RLog.e(this, "onTokenIncorrect()", "onTokenIncorrect");
                }
            });
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mInflater = LayoutInflater.from(view.getContext());

        super.onViewCreated(view, savedInstanceState);
    }


    @SuppressWarnings("unchecked")
    protected <T extends View> T findViewById(View view, int id) {
        return (T) view.findViewById(id);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(TOKEN, RongContext.getInstance().getToken());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected Handler getHandler() {
        return mHandler;
    }

    public abstract boolean onBackPressed();

    public abstract void onRestoreUI();

    private View obtainView(LayoutInflater inflater, int color, Drawable drawable, final CharSequence notice) {
        View view = inflater.inflate(R.layout.rc_wi_notice, null);
        ((TextView) view.findViewById(android.R.id.message)).setText(notice);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageDrawable(drawable);
        if (color > 0)
            view.setBackgroundColor(color);

        return view;
    }

    private View obtainView(LayoutInflater inflater, int color, int res, final CharSequence notice) {
        View view = inflater.inflate(R.layout.rc_wi_notice, null);
        ((TextView) view.findViewById(android.R.id.message)).setText(notice);
        ((ImageView) view.findViewById(android.R.id.icon)).setImageResource(res);

        view.setBackgroundColor(color);
        return view;
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {

        switch (msg.what) {
            case UI_RESTORE:
                onRestoreUI();
                break;
            default:
                break;
        }
        return true;
    }
}
