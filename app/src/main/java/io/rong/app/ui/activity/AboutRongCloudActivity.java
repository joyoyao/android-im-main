package io.rong.app.ui.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import io.rong.app.R;

/**
 * Created by Administrator on 2015/3/3.
 */
public class AboutRongCloudActivity extends BaseActionBarActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_about_rongcloud);

        getSupportActionBar().setTitle(R.string.set_rongcloud);
        RelativeLayout mUpdateLog = (RelativeLayout) findViewById(R.id.rl_update_log);
        RelativeLayout mFunctionIntroduce = (RelativeLayout) findViewById(R.id.rl_function_introduce);
        RelativeLayout mDVDocument = (RelativeLayout) findViewById(R.id.rl_dv_document);
        RelativeLayout mRongCloudWeb = (RelativeLayout) findViewById(R.id.rl_rongcloud_web);
        TextView mCurrentVersion = (TextView) findViewById(R.id.version_new);

        mUpdateLog.setOnClickListener(this);
        mFunctionIntroduce.setOnClickListener(this);
        mDVDocument.setOnClickListener(this);
        mRongCloudWeb.setOnClickListener(this);

        String[] versionInfo = getVersionInfo();
        mCurrentVersion.setText(versionInfo[1]);
    }

    private String[] getVersionInfo() {
        String[] version = new String[2];

        PackageManager packageManager = getPackageManager();

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            version[0] = String.valueOf(packageInfo.versionCode);
            version[1] = packageInfo.versionName;
            return version;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_update_log://更新日志
//                test1();
                startActivity(new Intent(AboutRongCloudActivity.this, UpdateLogActivity.class));
                break;
            case R.id.rl_function_introduce://功能介绍
//                test2();
                startActivity(new Intent(AboutRongCloudActivity.this, FunctionIntroducedActivity.class));
                break;
            case R.id.rl_dv_document://开发者文档
//                test3();
                startActivity(new Intent(AboutRongCloudActivity.this, DocumentActivity.class));
                break;
            case R.id.rl_rongcloud_web://官方网站
//                test4();
                startActivity(new Intent(AboutRongCloudActivity.this, RongWebActivity.class));
                break;
        }
    }


}
