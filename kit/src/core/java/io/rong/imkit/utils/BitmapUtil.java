package io.rong.imkit.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.rong.imkit.RLog;

public class BitmapUtil {
	private static final String TAG = "Util";

	public static String getBase64FromBitmap(Bitmap bitmap) {

		String base64Str = null;
		ByteArrayOutputStream baos = null;

		try {
			if (bitmap != null) {

				baos = new ByteArrayOutputStream();
				bitmap.compress(CompressFormat.JPEG, 60, baos);

				byte[] bitmapBytes = baos.toByteArray();
				base64Str = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);

				baos.flush();
				baos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (baos != null) {
					baos.flush();
					baos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return base64Str;

	}

	public static Bitmap getBitmapFromBase64(String base64Str) {

		if (TextUtils.isEmpty(base64Str)) {
			return null;
		}

		byte[] bytes = Base64.decode(base64Str, Base64.NO_WRAP);
		return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
	}

	public static Bitmap getResizedBitmap(Context context, Uri uri, int widthLimit, int heightLimit) throws IOException{

        String path = null;
        Bitmap result = null;

        if(uri.getScheme().equals("file")){
            path = uri.getPath();
        }else if(uri.getScheme().equals("content")){
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA},null,null,null);
            cursor.moveToFirst();
            path = cursor.getString(0);
            cursor.close();
        }else {
            return null;
        }

        ExifInterface exifInterface = new ExifInterface(path);

        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);

        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90
        || orientation == ExifInterface.ORIENTATION_ROTATE_270
                || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = widthLimit;
            widthLimit = heightLimit;
            heightLimit = tmp;
        }

        int width = options.outWidth;
        int height = options.outHeight;
        int sampleW = 1, sampleH = 1;
        while (width / 2 > widthLimit) {
            width /= 2;
            sampleW <<= 1;

        }

        while (height / 2 > heightLimit) {
            height /= 2;
            sampleH <<= 1;
        }
        int sampleSize = 1;

        options = new Options();
        if (widthLimit == Integer.MAX_VALUE || heightLimit == Integer.MAX_VALUE) {
            sampleSize = Math.max(sampleW, sampleH);
        } else {
            sampleSize = Math.max(sampleW, sampleH);
        }
        options.inSampleSize = sampleSize;

        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            options.inSampleSize = options.inSampleSize << 1;
            bitmap = BitmapFactory.decodeFile(path, options);
        }

        Matrix matrix = new Matrix();
        if (bitmap == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90
                || orientation == ExifInterface.ORIENTATION_ROTATE_270
                || orientation == ExifInterface.ORIENTATION_TRANSPOSE
                || orientation == ExifInterface.ORIENTATION_TRANSVERSE) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270, w / 2f, h / 2f);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270, w / 2f, h / 2f);
                matrix.preScale(1, -1);
                break;
        }
        float xS = (float) widthLimit/ bitmap.getWidth();
        float yS = (float) heightLimit / bitmap.getHeight();

        matrix.postScale(Math.min(xS, yS), Math.min(xS, yS));
        try {
            result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            Log.e("ResourceCompressHandler", "OOM" + "Height:" + bitmap.getHeight() + "Width:" + bitmap.getHeight() + "matrix:" + xS + " " + yS);
            return null;
        }
        return result;
	}

	public static Bitmap getRotateBitmap(float degrees, Bitmap bm) {
		int bmpW = bm.getWidth();
		int bmpH = bm.getHeight();

		Matrix mt = new Matrix();
		// 设置旋转角度
		// 如果是设置为0则表示不旋转
		// 设置的数是负数则向左转
		// 设置的数是正数则向右转
		mt.setRotate(degrees);
		return Bitmap.createBitmap(bm, 0, 0, bmpW, bmpH, mt, true);
	}

	private static Options decodeBitmapOptionsInfo(Context context, Uri uri) {
		InputStream input = null;
		Options opt = new Options();
		try {
            if(uri.getScheme().equals("content")) {
                input = context.getContentResolver().openInputStream(uri);
            }else if(uri.getScheme().equals("file")){
                input = new FileInputStream(uri.getPath());
            }
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, null, opt);
			return opt;
		} catch (FileNotFoundException e) {

			if (input == null) {
				input = FileUtil.getFileInputStream(uri.getPath());
			}
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(input, null, opt);
			return opt;

		} finally {
			if (null != input) {
				try {
					input.close();
				} catch (IOException e) {
					// Ignore
					// Log.e(DevConfig.MESSAGING,
					// "IOException caught while closing stream", e);
				}
			}
		}
	}

	/**
	 * 用于压缩时旋转图片
	 *
	 * @param srcFilePath
	 * @param bitmap
	 * @return
	 * @throws IOException
	 * @throws OutOfMemoryError
	 */
	private static Bitmap rotateBitMap(String srcFilePath, Bitmap bitmap) {
		ExifInterface exif = null;

		try {
			exif = new ExifInterface(srcFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		float degree = 0F;

		if (exif != null) {
			switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_UNDEFINED)) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90F;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180F;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270F;
				break;
			default:
				break;
			}
		}

		if (degree != 0F) {
			Matrix matrix = new Matrix();
			matrix.setRotate(degree, bitmap.getWidth(), bitmap.getHeight());
			Bitmap b2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
					bitmap.getHeight(), matrix, true);
			if (b2 != null && bitmap != b2) {
				bitmap.recycle();
				bitmap = b2;
			}
		}

		return bitmap;
	}
}
