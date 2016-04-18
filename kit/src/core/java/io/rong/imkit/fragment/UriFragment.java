package io.rong.imkit.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import io.rong.imkit.RLog;
import io.rong.imkit.RongIM;

public abstract class UriFragment extends BaseFragment {

    private Uri mUri;

    public static final String RONG_URI = "RONG_URI";

    IActionBarHandler mBarHandler;

    protected Bundle obtainUriBundle(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(RONG_URI, uri);
        return args;
    }


    protected interface IActionBarHandler {
        void onTitleChanged(CharSequence title);

        void onUnreadCountChanged(int count);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mUri == null) {
            if (savedInstanceState == null) {
                mUri = getActivity().getIntent().getData();
            } else {
                mUri = savedInstanceState.getParcelable(RONG_URI);
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null)
            if (getUri() != null)
                initFragment(getUri());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        RLog.d(this, "UriFragment", "onViewStateRestored");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(RONG_URI, getUri());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreUI() {
        if (getUri() != null)
            initFragment(getUri());
    }

    public void setActionBarHandler(IActionBarHandler mBarHandler) {
        this.mBarHandler = mBarHandler;
    }

    protected IActionBarHandler getActionBarHandler() {
        return mBarHandler;
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;

        if (mUri != null && isInLayout())
            initFragment(mUri);
    }

    protected abstract void initFragment(Uri uri);

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
