package io.rong.imkit.tools;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;

import io.rong.imkit.R;

/**
 * Created by AMing on 2015/3/24.
 */
public class PreviewPictureActivity extends FragmentActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {

    private Button mButtonBack;

    private Button mButtonSend;

    private TextView mCurrentPage;

    private CheckBox mCheckBox;

    private ArrayList<Uri> previewedPictures;

    private PreviewAdapter mAdapter;

    private ArrayList<SaveSelectState> mSelectedPics;

    private int mCurrentPicIndex = 0;

    @Override
    public void onBackPressed() {
        ArrayList<Uri> sendUriList = new ArrayList<>();
        for (SaveSelectState state : mSelectedPics) {
            if (state.isSelect() == true)
                sendUriList.add(state.getUri());
        }
        previewedPictures.clear();

        Intent mIntent = new Intent(PreviewPictureActivity.this, SelectPictureActivity.class);
        mIntent.putExtra("BACK_RESULT", (Serializable) sendUriList);
        PreviewPictureActivity.this.setResult(1, mIntent);
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.rc_ac_preview);
        mSelectedPics = new ArrayList<SaveSelectState>();

        previewedPictures = (ArrayList<Uri>) getIntent().getSerializableExtra("ArrayList");
        if(previewedPictures != null)
        for (int i = 0; i < previewedPictures.size(); i++) {
            mSelectedPics.add(new SaveSelectState(previewedPictures.get(i), true));
        }
        initView();
    }


    private void initView() {
        ViewPager viewPager = (ViewPager) findViewById(R.id.rc_pager);
        mButtonBack = (Button) findViewById(R.id.rc_back);
        mButtonSend = (Button) findViewById(R.id.rc_send);
        mCurrentPage = (TextView) findViewById(R.id.rc_msg);
        mCurrentPage.setText(1 + "/" + previewedPictures.size());
        mCheckBox = (CheckBox) findViewById(R.id.rc_checkbox);

        mCheckBox.setChecked(true);
        mButtonSend.setClickable(true);
        mButtonSend.setTextColor(Color.WHITE);
        mButtonSend.setText(getResources().getString(R.string.rc_input_send) + "(" + previewedPictures.size() + ")");

        mButtonBack.setOnClickListener(this);
        mButtonSend.setOnClickListener(this);

        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectedPics.get(mCurrentPicIndex).setIsSelect(true);
                } else {
                    mSelectedPics.get(mCurrentPicIndex).setIsSelect(false);
                }
                int selected = 0;
                for (SaveSelectState state : mSelectedPics) {
                    if (state.isSelect)
                        selected++;
                }
                String formatString = getResources().getString(R.string.rc_send_format);
                String content = String.format(formatString, selected);
                mButtonSend.setText(content);

                if (selected < 1) {
                    mButtonSend.setTextColor(Color.GRAY);
                    mButtonSend.setClickable(false);
                } else {
                    mButtonSend.setTextColor(Color.WHITE);
                    mButtonSend.setClickable(true);
                }
            }
        });

        if (previewedPictures != null) {
            mAdapter = new PreviewAdapter(getSupportFragmentManager(), previewedPictures);
            viewPager.setAdapter(mAdapter);
            viewPager.setOnPageChangeListener(this);
        }

    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int i) {

        mCurrentPage.setText((i + 1) + "/" + previewedPictures.size());

        mCurrentPicIndex = i;

        if (mSelectedPics.get(i).isSelect() == true) {
            mCheckBox.setChecked(true);
        } else {
            mCheckBox.setChecked(false);
        }
    }


    @Override
    public void onPageScrollStateChanged(int i) {

    }

    class PreviewAdapter extends FragmentPagerAdapter {

        private ArrayList<Uri> selectedPicture;

        public PreviewAdapter(FragmentManager fm, ArrayList<Uri> selectedPicture) {
            super(fm);
            this.selectedPicture = selectedPicture;
        }

        @Override
        public Fragment getItem(int position) {
            return new PictureFragment(PreviewPictureActivity.this,selectedPicture.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PictureFragment mPictureFragment = (PictureFragment) super.instantiateItem(container, position);
            if (mPictureFragment != null) {
                Uri info = selectedPicture.get(position);

                mPictureFragment.initPhoto(info);
            }
            return mPictureFragment;
        }

        @Override
        public int getCount() {
            return selectedPicture.size();
        }
    }

    class SaveSelectState {

        private Uri mUri;

        private Boolean isSelect;

        SaveSelectState(Uri mUri, Boolean isSelect) {
            this.mUri = mUri;
            this.isSelect = isSelect;
        }

        public Uri getUri() {
            return mUri;
        }

        public void setUri(Uri mUri) {
            this.mUri = mUri;
        }

        public Boolean isSelect() {
            return isSelect;
        }

        public void setIsSelect(Boolean isSelect) {
            this.isSelect = isSelect;
        }
    }

    @Override
    public void onClick(View v) {
        ArrayList<Uri> sendUriList = new ArrayList<>();
        for (SaveSelectState state : mSelectedPics) {
            if (state.isSelect() == true)
                sendUriList.add(state.getUri());
        }

        if (v.getId() == R.id.rc_back) {
            Intent mIntent = new Intent(PreviewPictureActivity.this, SelectPictureActivity.class);
            mIntent.putExtra("BACK_RESULT", (Serializable) sendUriList);
            PreviewPictureActivity.this.setResult(1, mIntent);
            finish();
        } else if (v.getId() == R.id.rc_send) {
            Intent mIntent = new Intent(PreviewPictureActivity.this, SelectPictureActivity.class);
            mIntent.putExtra("PREVIEW_RESULT", (Serializable) sendUriList);
            PreviewPictureActivity.this.setResult(1, mIntent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewedPictures != null) {
            previewedPictures.clear();
        }
    }
}
