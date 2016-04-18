package io.rong.imkit.util;


import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ReplacementSpan;
import android.util.DisplayMetrics;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.rong.imkit.R;
import io.rong.imkit.model.Emoji;

public class AndroidEmoji {


    private static Resources sResources;
    private static float density;

    public static void init(Context context) {
        sEmojiMap = new HashMap<>();
        sEmojiList = new ArrayList<>();
        sResources = context.getResources();

        int[] codes = sResources.getIntArray(R.array.rc_emoji_code);
        TypedArray array = sResources.obtainTypedArray(R.array.rc_emoji_res);

        if (codes.length != array.length()) {
            throw new RuntimeException("Emoji resource init fail.");
        }

        int i = -1;
        while (++i < codes.length) {
            Emoji emoji = new Emoji(codes[i], array.getResourceId(i, -1));

            sEmojiMap.put(codes[i], emoji);
            sEmojiList.add(emoji);
        }

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        density = dm.density;

        Log.d("SystemUtils", "density:" + density);

    }

    public static List<Emoji> getEmojiList() {
        return sEmojiList;
    }

    private static Map<Integer, Emoji> sEmojiMap;
    private static List<Emoji> sEmojiList;

    public static class EmojiImageSpan extends ReplacementSpan {
        Drawable mDrawable;

        private EmojiImageSpan(Resources resources, int codePoint) {

            if (sEmojiMap.containsKey(codePoint)) {
                mDrawable = resources.getDrawable(sEmojiMap.get(codePoint).getRes());

                int width = mDrawable.getIntrinsicWidth() - (int) (4 * density);
                int height = mDrawable.getIntrinsicHeight() - (int) (4 * density);
                mDrawable.setBounds(0, 0, width > 0 ? width : 0, height > 0 ? height : 0);

            }
        }

        private static final String TAG = "DynamicDrawableSpan";

        /**
         * A constant indicating that the bottom of this span should be aligned
         * with the bottom of the surrounding text, i.e., at the same level as the
         * lowest descender in the text.
         */
        public static final int ALIGN_BOTTOM = 0;


        /**
         * Your subclass must implement this method to provide the bitmap
         * to be drawn.  The dimensions of the bitmap must be the same
         * from each call to the next.
         */
        public Drawable getDrawable() {
            return mDrawable;
        }

        @Override
        public int getSize(Paint paint, CharSequence text,
                           int start, int end,
                           Paint.FontMetricsInt fm) {
            Drawable d = getCachedDrawable();
            Rect rect = d.getBounds();

            if (fm != null) {
                fm.ascent = -rect.bottom;
                fm.descent = 0;

                fm.top = fm.ascent;
                fm.bottom = 0;
            }

            return rect.right;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, Paint paint) {
            Drawable b = getCachedDrawable();
            canvas.save();

            int transY = bottom - b.getBounds().bottom;

            transY -=  density;


            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }

        private Drawable getCachedDrawable() {
            WeakReference<Drawable> wr = mDrawableRef;
            Drawable d = null;

            if (wr != null)
                d = wr.get();

            if (d == null) {
                d = getDrawable();
                mDrawableRef = new WeakReference<Drawable>(d);
            }

            return d;
        }

        private WeakReference<Drawable> mDrawableRef;
    }

    public static int getEmojiCount(String input) {
        if (input == null) {
            return 0;
        }

        int count = 0;

        // extract the single chars that will be operated on
        final char[] chars = input.toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters
        final SpannableStringBuilder ssb = new SpannableStringBuilder(input);

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (sEmojiMap.containsKey(codePoint)) {
                count++;
            }
        }
        return count;
    }

    public static CharSequence ensure(String input) {

        if (input == null) {
            return input;
        }

        // extract the single chars that will be operated on
        final char[] chars = input.toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters
        final SpannableStringBuilder ssb = new SpannableStringBuilder(input);

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (sEmojiMap.containsKey(codePoint)) {
                ssb.setSpan(new EmojiImageSpan(sResources, codePoint), isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }

        return ssb;
    }


    public static boolean isEmoji(String input) {

        if (input == null) {
            return false;
        }

        final char[] chars = input.toCharArray();

        int codePoint = 0;
        int length = chars.length;

        for (int i = 0; i < length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
            }

            if (sEmojiMap.containsKey(codePoint)) {
                return true;
            }
        }

//        if (length >= 2) {
//
//            if (Character.isLowSurrogate(chars[length - 1])) {
//
//                if (Character.isSurrogatePair(chars[length - 2], chars[length - 1])) {
//                    codePoint = Character.toCodePoint(chars[length - 2], chars[length - 1]);
//                } else {
//                    codePoint = (int) chars[length - 1];
//                }
//            }
//
//            if (sEmojiMap.containsKey(codePoint)) {
//                return true;
//            }
//        }

        return false;
    }

    public static void ensure(Spannable spannable) {

        // extract the single chars that will be operated on
        final char[] chars = spannable.toString().toCharArray();
        // create a SpannableStringBuilder instance where the font ranges will be set for emoji characters

        int codePoint;
        boolean isSurrogatePair;
        for (int i = 0; i < chars.length; i++) {
            if (Character.isHighSurrogate(chars[i])) {
                continue;
            } else if (Character.isLowSurrogate(chars[i])) {
                if (i > 0 && Character.isSurrogatePair(chars[i - 1], chars[i])) {
                    codePoint = Character.toCodePoint(chars[i - 1], chars[i]);
                    isSurrogatePair = true;
                } else {
                    continue;
                }
            } else {
                codePoint = (int) chars[i];
                isSurrogatePair = false;
            }

            if (sEmojiMap.containsKey(codePoint)) {
                spannable.setSpan(new EmojiImageSpan(sResources, codePoint), isSurrogatePair ? i - 1 : i, i + 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
            }
        }
    }


}
