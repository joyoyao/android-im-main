package io.rong.imkit.widget;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

/**
 * Created by zhjchen on 4/8/15.
 */

public class ArraysDialogFragment extends BaseDialogFragment {

    private static final String ARGS_TITLE = "args_title";
    private static final String ARGS_ARRAYS = "args_arrays";

    private OnArraysDialogItemListener mItemListener;
    private int count;

    public static ArraysDialogFragment newInstance(String title, String[] arrays) {
        ArraysDialogFragment dialogFragment = new ArraysDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARGS_TITLE, title);
        bundle.putStringArray(ARGS_ARRAYS, arrays);
        dialogFragment.setArguments(bundle);
        return dialogFragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(ARGS_TITLE);
        String[] arrays = getArguments().getStringArray(ARGS_ARRAYS);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        if(arrays.length > 0 && arrays[0] != null) {
            setCount(arrays.length);

            builder.setItems(arrays, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mItemListener != null) {
                        mItemListener.OnArraysDialogItemClick(dialog, which);
                    }
                }
            });
        }

        return builder.create();
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ArraysDialogFragment setArraysDialogItemListener(OnArraysDialogItemListener mItemListener) {
        this.mItemListener = mItemListener;
        return this;
    }

    public interface OnArraysDialogItemListener {
        public void OnArraysDialogItemClick(DialogInterface dialog, int which);
    }

    public void show(FragmentManager manager) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.add(this, "ArraysDialogFragment");
        ft.commitAllowingStateLoss();
    }
}
