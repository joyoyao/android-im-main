package io.rong.imkit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.rong.imkit.widget.provider.IContainerItemProvider;

/**
 * Created by DragonJ on 14-10-17.
 */
public class ProviderContainerView extends FrameLayout {
    Map<Class<? extends IContainerItemProvider>, AtomicInteger> mViewCounterMap;
    Map<Class<? extends IContainerItemProvider>, View> mContentViewMap;
    View mInflateView;
    int mMaxContainSize = 3;

    public ProviderContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode())
            init(attrs);
    }


    private void init(AttributeSet attrs) {
        mViewCounterMap = new HashMap();
        mContentViewMap = new HashMap();
    }


    public <T extends IContainerItemProvider> View inflate(T t) {
        View result = null;

        if (mInflateView != null)
            mInflateView.setVisibility(View.GONE);

        if (mContentViewMap.containsKey(t.getClass())) {
            result = mContentViewMap.get(t.getClass());
            mInflateView = result;
            mViewCounterMap.get(t.getClass()).incrementAndGet();
        }

        if (result != null) {
            if (result.getVisibility() == View.GONE)
                result.setVisibility(View.VISIBLE);

            return result;
        }

        recycle();

        result = t.newView(getContext(), this);

        if (result != null) {
            super.addView(result);
            mContentViewMap.put(t.getClass(), result);
            mViewCounterMap.put(t.getClass(), new AtomicInteger());
        }


        mInflateView = result;


        return result;
    }

    public View getCurrentInflateView() {
        return mInflateView;
    }

    public void containerViewLeft() {
        if (mInflateView == null)
            return;
        LayoutParams params = (LayoutParams) mInflateView.getLayoutParams();
        params.gravity = Gravity.LEFT|Gravity.CENTER_VERTICAL;
    }

    public void containerViewRight() {
        if (mInflateView == null)
            return;
        LayoutParams params = (LayoutParams) mInflateView.getLayoutParams();
        params.gravity = Gravity.RIGHT|Gravity.CENTER_VERTICAL;
    }

    public void containerViewCenter() {
        if (mInflateView == null)
            return;

        LayoutParams params = (LayoutParams) mInflateView.getLayoutParams();
        params.gravity = Gravity.CENTER;
    }

    private void recycle() {
        if (mInflateView == null)
            return;

        int count = getChildCount();
        if (count >= mMaxContainSize) {
            Map.Entry<Class<? extends IContainerItemProvider>, AtomicInteger> min = null;

            for (Map.Entry<Class<? extends IContainerItemProvider>, AtomicInteger> item : mViewCounterMap.entrySet()) {
                if (min == null)
                    min = item;

                min = min.getValue().get() > item.getValue().get() ? item : min;
            }

            mViewCounterMap.remove(min.getKey());
            View view = mContentViewMap.remove(min.getKey());
            removeView(view);
        }
    }
}
