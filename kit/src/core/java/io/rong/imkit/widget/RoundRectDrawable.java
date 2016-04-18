//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package io.rong.imkit.widget;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

public class RoundRectDrawable extends Drawable {

    private final float cornerRadius;
    private final int margin;
    private final RectF mRect = new RectF();
    private RectF mBitmapRect;
    private final BitmapShader bitmapShader;
    private final Paint paint;

    public RoundRectDrawable(Bitmap bitmap, int cornerRadius) {
        this(bitmap, cornerRadius, 0);
    }

    public RoundRectDrawable(Bitmap bitmap, int cornerRadius, int margin) {

        this.cornerRadius = cornerRadius;
        this.margin = margin;

        bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mBitmapRect = new RectF(margin, margin, bitmap.getWidth() - margin, bitmap.getHeight() - margin);

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(bitmapShader);
        paint.setFilterBitmap(true);
        paint.setDither(true);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        mRect.set(margin, margin, bounds.width() - margin, bounds.height() - margin);

        Matrix shaderMatrix = new Matrix();
        shaderMatrix.setRectToRect(mBitmapRect, mRect, Matrix.ScaleToFit.FILL);
        bitmapShader.setLocalMatrix(shaderMatrix);

    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, paint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        paint.setColorFilter(cf);
    }
}

