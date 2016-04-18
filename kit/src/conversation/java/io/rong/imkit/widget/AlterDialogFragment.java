package io.rong.imkit.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

/**
 * Created by zhjchen on 3/30/15.
 */

public class AlterDialogFragment extends BaseDialogFragment {

    private static final String ARGS_TITLE = "args_title";
    private static final String ARGS_MESSAGE = "args_message";
    private static final String ARGS_CANCEL_BTN_TXT = "args_cancel_button_text";
    private static final String ARGS_OK_BTN_TXT = "args_ok_button_text";

    private AlterDialogBtnListener mAlterDialogBtnListener;


    public static AlterDialogFragment newInstance(String title, String message, String cancelBtnText, String okBtnText) {

        AlterDialogFragment dialogFragment = new AlterDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARGS_TITLE, title);
        args.putString(ARGS_MESSAGE, message);
        args.putString(ARGS_CANCEL_BTN_TXT, cancelBtnText);
        args.putString(ARGS_OK_BTN_TXT, okBtnText);
        dialogFragment.setArguments(args);

        return dialogFragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString(ARGS_TITLE);
        String message = getArguments().getString(ARGS_MESSAGE);
        String cancelBtnText = getArguments().getString(ARGS_CANCEL_BTN_TXT);
        String okBtnText = getArguments().getString(ARGS_OK_BTN_TXT);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (!TextUtils.isEmpty(title)) {
            builder.setTitle(title);
        }

        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }

        if (!TextUtils.isEmpty(okBtnText)) {
            builder.setPositiveButton(okBtnText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (mAlterDialogBtnListener != null) {
                        mAlterDialogBtnListener.onDialogPositiveClick(AlterDialogFragment.this);
                    }
                }
            });
        }

        if (!TextUtils.isEmpty(cancelBtnText)) {
            builder.setNegativeButton(cancelBtnText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (mAlterDialogBtnListener != null) {
                        mAlterDialogBtnListener.onDialogNegativeClick(AlterDialogFragment.this);
                    }
                }
            });
        }
        // Create the AlertDialog object and return it
        return builder.create();
    }


    public interface AlterDialogBtnListener {
        public void onDialogPositiveClick(AlterDialogFragment dialog);

        public void onDialogNegativeClick(AlterDialogFragment dialog);
    }

    public void show(FragmentManager manager) {
        show(manager, "AlterDialogFragment");
    }


    public void setOnAlterDialogBtnListener(AlterDialogBtnListener alterDialogListener) {
        mAlterDialogBtnListener = alterDialogListener;
    }
}
