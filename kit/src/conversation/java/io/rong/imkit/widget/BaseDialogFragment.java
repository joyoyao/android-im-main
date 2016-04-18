package io.rong.imkit.widget;

import android.support.v4.app.DialogFragment;
import android.view.View;

/**
 * Created by zhjchen on 3/30/15.
 */

public class BaseDialogFragment extends DialogFragment {

    protected <T extends View> T getView(View view, int id) {
        return (T) view.findViewById(id);
    }
}
