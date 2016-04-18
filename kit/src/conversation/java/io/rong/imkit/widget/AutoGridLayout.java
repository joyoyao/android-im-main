package io.rong.imkit.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Created by DragonJ on 15/2/11.
 */
public class AutoGridLayout extends ViewGroup {

    int mLastOrientation;

    public AutoGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation != mLastOrientation) {
            mLastOrientation = newConfig.orientation;
            getViewTreeObserver().addOnGlobalLayoutListener(orientationChangeListener);
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener orientationChangeListener = new ViewTreeObserver.OnGlobalLayoutListener() {

        @Override
        public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                getViewTreeObserver().removeOnGlobalLayoutListener(orientationChangeListener);
            else
                getViewTreeObserver().removeGlobalOnLayoutListener(orientationChangeListener);
            invalidate();
        }
    };

    int mCurrentItemWidth = 0;


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        int currentWidth = 0;
        int currentHeight = 0;

        int vWidth = MeasureSpec.getSize(widthMeasureSpec);

        if (count > 0) {
            View view = getChildAt(0);
            measureChildWithMargins(view, widthMeasureSpec, 0, heightMeasureSpec, 0);

            final LayoutParams lp = (LayoutParams) view.getLayoutParams();

            mCurrentItemWidth = Math.max(0, view.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            int itemCount = 0;

            if (vWidth == 0) {
                setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));
                return;
            }

            while (vWidth / ++itemCount > mCurrentItemWidth) {

            }

            mCurrentItemWidth = vWidth / --itemCount;

        }

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(mCurrentItemWidth, child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight, child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);

                int itemHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

                currentWidth += maxWidth;

                if (currentWidth > vWidth) {
                    currentWidth = maxWidth;
                    currentHeight += itemHeight;
                }
            }
        }

        maxWidth += getPaddingLeft() + getPaddingRight();

        maxHeight = Math.max(maxHeight, currentHeight + maxHeight);

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec), resolveSize(maxHeight, heightMeasureSpec));

        if (count > 0) {
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);

                final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
                int childWidthMeasureSpec;
                int childHeightMeasureSpec;

                if (lp.width == LayoutParams.MATCH_PARENT) {
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - lp.leftMargin
                            - lp.rightMargin, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                if (lp.height == LayoutParams.MATCH_PARENT) {
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - lp.topMargin
                            - lp.bottomMargin, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int count = getChildCount();
        final int parentLeft = getPaddingLeft();
        final int parentRight = right - left - getPaddingRight();

        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        int currentLeft = parentLeft;
        int currentTop = parentTop;
        int currentBottom = parentTop;

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                childLeft = currentLeft + (mCurrentItemWidth - width) / 2;
                childTop = currentTop + lp.topMargin;

                if (childLeft + width > parentRight) {
                    childLeft = parentLeft + (mCurrentItemWidth - width) / 2;
                    currentLeft = parentLeft;
                    childTop = currentBottom + lp.topMargin;
                    currentTop = currentBottom;
                }
                Log.d(VIEW_LOG_TAG, String.format("left:%1$d top:%2$d right:%3$d bottom:%4$d", childLeft, childTop, childLeft + width, childTop + height));
                child.layout(childLeft, childTop, childLeft + width, childTop + height);
                currentLeft += mCurrentItemWidth;
                currentBottom = childTop + height + lp.bottomMargin;

            }
        }

    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}
