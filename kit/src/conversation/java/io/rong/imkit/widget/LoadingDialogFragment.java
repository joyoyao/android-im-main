package io.rong.imkit.widget;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

/**
 * Created by zhjchen on 4/20/15.
 */

public class LoadingDialogFragment extends BaseDialogFragment {


    private static final String ARGS_TITLE = "args_title";
    private static final String ARGS_MESSAGE = "args_message";


    public static LoadingDialogFragment newInstance(String title, String message) {

        LoadingDialogFragment dialogFragment = new LoadingDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARGS_TITLE, title);
        args.putString(ARGS_MESSAGE, message);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        ProgressDialog dialog = new ProgressDialog(getActivity());
        String title = getArguments().getString(ARGS_TITLE);
        String message = getArguments().getString(ARGS_MESSAGE);

        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        if (!TextUtils.isEmpty(title))
            dialog.setTitle(title);

        if (!TextUtils.isEmpty(message))
            dialog.setMessage(message);

        return dialog;
    }

    public void show(FragmentManager manager) {
        show(manager, "LoadingDialogFragment");
    }
}
