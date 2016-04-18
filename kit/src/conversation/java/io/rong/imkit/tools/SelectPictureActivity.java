package io.rong.imkit.tools;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;
import android.widget.Toast;

import com.sea_monster.resource.Resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.widget.AsyncImageView;

/**
 * Created by AMing on 2015/3/23.
 */
public class SelectPictureActivity extends Activity implements View.OnClickListener, Handler.Callback {

    GridView mGridView;

    PictureAdapter mAdapter;

    private Button mButOK;

    private Button mButPreview;

    /**
     * 最多选择图片的个数
     */
    private static int MAX_NUM = 6;
    private static final int TAKE_PICTURE = 520; //开启系统相册的

    public static final String INTENT_MAX_NUM = "intent_max_num";

    public static final String INTENT_SELECTED_PICTURE = "intent_selected_picture";

    /**
     * 已选择的图片
     */
    private ArrayList<Uri> selectedPicture = new ArrayList<Uri>();

    private List<Uri> pictures;

    private static final int EVENT_PREVIEW_SEND = 0;
    private static final int EVENT_PREVIEW_SELECT = 1;

    private Handler mHandler = new Handler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rc_ac_albums);
        MAX_NUM = getIntent().getIntExtra(INTENT_MAX_NUM, 6);
        pictures = getThumb();
        Collections.reverse(pictures);
        initView();
    }

    private void initView() {
        mGridView = (GridView) findViewById(android.R.id.list);
        mButOK = (Button) findViewById(android.R.id.button1);
        mButPreview = (Button) findViewById(android.R.id.button2);

        String formatString = getResources().getString(R.string.rc_send_format);
        String content = String.format(formatString, 0);
        mButOK.setText(content);

        mAdapter = new PictureAdapter();
        mGridView.setAdapter(mAdapter);

        mButOK.setOnClickListener(this);
        mButPreview.setOnClickListener(this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        ArrayList<Uri> list = null;
        switch (msg.what) {
            case EVENT_PREVIEW_SEND:
                list = (ArrayList<Uri>)msg.obj;
                if (list != null && list.size() > 0) {
                    Intent preData = new Intent();
                    if (list.size() == 1) {
                        preData.setData(list.get(0));
                    } else {
                        preData.putExtra(Intent.EXTRA_RETURN_RESULT, list);
                    }
                    setResult(RESULT_OK, preData);
                    SelectPictureActivity.this.finish();
                }
                break;
            case EVENT_PREVIEW_SELECT:
                list = (ArrayList<Uri>)msg.obj;
                if(list.size() != selectedPicture.size()) {
                    selectedPicture.clear();
                    selectedPicture.addAll(list);
                    mAdapter.notifyDataSetChanged();
                }
                break;
        }
        return true;
    }


    class PictureAdapter extends BaseAdapter {
        HashMap<Integer, Uri> state = new HashMap<Integer, Uri>();

        @Override
        public int getCount() {
            return pictures.size();
        }

        @Override
        public Object getItem(int position) {
            return pictures.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;
            if (convertView == null || convertView.getTag() == null) {
                convertView = View.inflate(SelectPictureActivity.this, R.layout.rc_item_albums, null);
                holder = new ViewHolder();
                holder.icon = (AsyncImageView) convertView.findViewById(R.id.rc_icon);
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.rc_checkbox);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked) {
                        state.put(position, pictures.get(position));
                        if (!selectedPicture.contains(pictures.get(position))) {
                            selectedPicture.add(pictures.get(position));
                        }
                    } else {
                        state.remove(position);
                        selectedPicture.remove(pictures.get(position));
                    }

                    String formatString = getResources().getString(R.string.rc_send_format);
                    String content = String.format(formatString, selectedPicture.size());
                    mButOK.setText(content);
                }
            });
            boolean found = false;
            if(state.get(position) != null) {
                for(Uri uri : selectedPicture) {
                    if(uri.equals(state.get(position))) {
                        holder.checkBox.setChecked(true);
                        found = true;
                        break;
                    }
                }
                if(found == false) {
                    state.remove(position);
                    holder.checkBox.setChecked(false);
                }
            } else {
                holder.checkBox.setChecked(false);
            }
            holder.icon.setResource(new Resource(pictures.get(position)));

            return convertView;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null)
            return;

        if(data.getSerializableExtra("BACK_RESULT") != null) {
            Message msg = Message.obtain();
            msg.what = EVENT_PREVIEW_SELECT;
            msg.obj = data.getSerializableExtra("BACK_RESULT");
            mHandler.sendMessage(msg);
        } else {
            super.onActivityResult(requestCode, resultCode, data);

            Message msg = Message.obtain();
            msg.what = EVENT_PREVIEW_SEND;
            msg.obj = data.getSerializableExtra("PREVIEW_RESULT");
            mHandler.sendMessage(msg);
        }
    }

    class ViewHolder {
        AsyncImageView icon;
        CheckBox checkBox;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case android.R.id.button1:
                if (selectedPicture.size() > 0) {
                    Intent data = new Intent();
                    if (selectedPicture.size() == 1) {
                        data.setData(selectedPicture.get(0));
                    } else {
                        data.putExtra(Intent.EXTRA_RETURN_RESULT, selectedPicture);
                    }

                    setResult(RESULT_OK, data);
                    this.finish();
                } else {
                    Toast.makeText(this,
                            getResources().getString(R.string.rc_notice_select_one_picture_at_last),
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case android.R.id.button2://开启预览界面
                if (selectedPicture.size() > 0) {
                    Intent preview = new Intent(SelectPictureActivity.this, PreviewPictureActivity.class);
                    preview.putExtra("ArrayList", (Serializable) selectedPicture);
                    startActivityForResult(preview, 2);
                } else {
                    Toast.makeText(this,
                            getResources().getString(R.string.rc_notice_select_one_picture_at_last),
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * 查询数据库
     *
     * @return 图片的Uri
     */
    private List<Uri> getThumb() {

        List<Uri> uris = new ArrayList<>();

        Cursor cursor = null;

        try {
            cursor = MediaStore.Images.Media.query(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Images.Thumbnails._ID});
        } catch (SecurityException e) {
            Log.e("SelectPictureActivity", "SecurityException!!!");
        }
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                //如果用户手机一张相册也没有 会报错 崩溃  下面加了判断也没用
                do {
                    uris.add(Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getString(0)));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return uris;
    }

    /**
     * 左上角返回 退出当前页面
     */
    public void back(View v) {
        onBackPressed();
    }


}

