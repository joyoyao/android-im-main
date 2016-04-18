package io.rong.imkit.tools;


import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sea_monster.exception.BaseException;
import com.sea_monster.network.AbstractHttpRequest;
import com.sea_monster.network.StoreStatusCallback;
import com.sea_monster.resource.ResCallback;
import com.sea_monster.resource.Resource;
import com.sea_monster.resource.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.fragment.BaseFragment;
import io.rong.message.utils.BitmapUtil;
import uk.co.senab.photoview.PhotoView;

/**
* Created by DragonJ on 15/4/13.
*/
public class PhotoFragment extends BaseFragment {
    PhotoView mPhotoView;
    Uri mUri;
    Uri mThumbnail;
    ProgressBar mProgressBar;
    TextView mProgressText;
    PhotoDownloadListener mListener;

    final static int REQ_PHOTO = 0x1;
    final static int SHOW_PHOTO = 0x2;
    final static int DOWNLOAD_PROGRESS = 0x3;
    final static int REQ_FAILURE = 0x4;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_photo, container, true);
        mPhotoView = (PhotoView) view.findViewById(R.id.rc_icon);
        mProgressBar = (ProgressBar) view.findViewById(R.id.rc_progress);
        mProgressText = (TextView) view.findViewById(R.id.rc_txt);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public boolean handleMessage(Message msg) {

        switch (msg.what) {
            case REQ_PHOTO:
                RongContext.getInstance().executorBackground(new Runnable() {
                    final Uri uri = mUri;

                    @Override
                    public void run() {
                        try {
                            ResourceHandler.getInstance().requestResource(new Resource(uri), new ResCallback() {
                                @Override
                                public void onComplete(AbstractHttpRequest<File> abstractHttpRequest, File file) {
                                    RLog.i(PhotoFragment.this, "onComplete", file.getPath());
                                    getHandler().obtainMessage(SHOW_PHOTO, Uri.fromFile(file)).sendToTarget();
                                }

                                @Override
                                public void onFailure(AbstractHttpRequest<File> abstractHttpRequest, BaseException e) {
                                    RLog.e(PhotoFragment.this, "onFailure", e.toString(), e);
                                    File file = ResourceHandler.getInstance().getFile(new Resource(uri));
                                    if (file != null && file.exists()) {
                                        file.delete();
                                    }
                                    getHandler().obtainMessage(REQ_FAILURE).sendToTarget();
                                }
                            }, mRcvDataCallback);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressText.setVisibility(View.VISIBLE);
                mProgressText.setText(0+"%");
                break;
            case SHOW_PHOTO:
                Uri uri = (Uri)msg.obj;
                mProgressText.setVisibility(View.GONE);
                mProgressBar.setVisibility(View.GONE);
                if(mListener != null)
                    mListener.onDownloaded(uri);
                try {
                    Bitmap bitmap = BitmapUtil.getResizedBitmap(RongContext.getInstance(), uri, 960, 960);
                    mPhotoView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                break;
            case DOWNLOAD_PROGRESS:
                int total = msg.arg1;
                int received = msg.arg2;
                if(received < total){
                    mProgressText.setText((received * 100)/total+"%");
                }
                break;
            case REQ_FAILURE:
                mProgressBar.setVisibility(View.GONE);
                try {
                    Activity ac = getActivity();
                    String str = "Fail!";
                    if(ac != null) {
                         str = ac.getResources().getString(R.string.rc_notice_download_fail);
                    }
                    mProgressText.setText(str);
                }catch (IllegalStateException e){
                    e.printStackTrace();
                }
                if(mListener != null)
                    mListener.onDownloadError();
                break;
        }

        return true;
    }

    private StoreStatusCallback mRcvDataCallback = new StoreStatusCallback() {
        @Override
        public void statusCallback(StoreStatus storeStatus) {
            getHandler().obtainMessage(DOWNLOAD_PROGRESS, (int)storeStatus.getTotalSize(), (int)storeStatus.getReceivedSize())
                        .sendToTarget();
        }
    };

    public void initPhoto(final Uri uri, final Uri thumbnail, PhotoDownloadListener listener) {
        mUri = uri;
        mThumbnail = thumbnail;
        mListener= listener;

        if(mUri == null || mUri.getScheme() == null || mPhotoView == null){
            RLog.e(this, "initPhoto", "Scheme is null!");
            return;
        }

        if (mUri.getScheme().equals("http") || mUri.getScheme().equals("https")) {
            RongContext.getInstance().executorBackground(new Runnable() {
                @Override
                public void run() {
                    if (getHandler() == null)
                        return;

                    if (ResourceHandler.getInstance().containsInDiskCache(new Resource(mUri))) {
                        mUri = Uri.fromFile(ResourceHandler.getInstance().getFile(new Resource(mUri)));
                        getHandler().obtainMessage(SHOW_PHOTO, mUri).sendToTarget();
                    } else {
                        getHandler().obtainMessage(SHOW_PHOTO, thumbnail).sendToTarget();
                        getHandler().obtainMessage(REQ_PHOTO, mUri).sendToTarget();
                    }
                }
            });
        } else if (uri.getScheme().equals("content")) {
            Cursor cursor = getActivity().getContentResolver().query(uri, new String[]{android.provider.MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return;
            }

            Uri image = uri;
            if(cursor.moveToFirst()) {
                image = Uri.parse("file://" + cursor.getString(0));
            }
            cursor.close();
            getHandler().obtainMessage(SHOW_PHOTO, image).sendToTarget();
        } else if(uri.getScheme().equals("file")) {
            getHandler().obtainMessage(SHOW_PHOTO, uri).sendToTarget();
        }
    }

    @Override
    public void onRestoreUI(){

    }

    public interface PhotoDownloadListener {
        public void onDownloaded(Uri file);
        public void onDownloadError();
    }

    @Override
    public void onDestroy() {
        if(mUri != null)
            ResourceHandler.getInstance().cancel(new Resource(mUri));
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mUri != null)
            outState.putParcelable("imageUri", mUri);
        if(mThumbnail != null)
            outState.putParcelable("thumbnailUri", mThumbnail);
        super.onSaveInstanceState(outState);
    }
}
