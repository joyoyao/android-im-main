package io.rong.imkit.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

/**
 * Created by weiqinxiao on 15/7/14.
 */
public class LinkTextView extends TextView {

    private static OnLinkClickListener listener;

    public LinkTextView(Context context) {
        super(context);
    }

    public LinkTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

    public void setOnLinkClickListener(OnLinkClickListener listener) {
        this.listener = listener;
    }

    public interface OnLinkClickListener {
        boolean onLinkClick(String link);
    }

    public static class LinkTextViewMovementMethod extends LinkMovementMethod {

        private static LinkTextViewMovementMethod sInstance;
        private long mLastActionDownTime;
        public LinkTextViewMovementMethod() {}

        public static LinkTextViewMovementMethod getInstance() {
            if (sInstance == null) {
                sInstance = new LinkTextViewMovementMethod();
            }
            return sInstance;
        }

        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        long actionUpTime = System.currentTimeMillis();
                        if (actionUpTime - mLastActionDownTime > ViewConfiguration.getLongPressTimeout()) {
                            return true;
                        }
                        String url = null;
                        if(link[0] instanceof URLSpan)
                            url = ((URLSpan) link[0]).getURL();
                        if(listener != null && listener.onLinkClick(url))
                            return true;
                        else
                            link[0].onClick(widget);
                    } else if(action == MotionEvent.ACTION_DOWN) {
                        mLastActionDownTime = System.currentTimeMillis();
                    }
                    return true;
                } else {
                    Touch.onTouchEvent(widget, buffer, event);
                    return false;
                }
            }
            return Touch.onTouchEvent(widget, buffer, event);
        }
    }
}
