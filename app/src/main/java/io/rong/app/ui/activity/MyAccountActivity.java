package io.rong.app.ui.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sea_monster.resource.Resource;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.utils.Constants;
import io.rong.imkit.RongIM;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.model.UserInfo;

/**
 * Created by Administrator on 2015/3/2.
 */
public class MyAccountActivity extends BaseActionBarActivity  {

    private static final int RESULTCODE = 10;
    /**
     * 头像
     */
    private RelativeLayout mMyPortrait;
    /**
     * 昵称
     */
    private RelativeLayout mMyUsername;

    private TextView mTVUsername;
    private AsyncImageView mImgMyPortrait;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_myaccount);

        getSupportActionBar().setTitle(R.string.de_actionbar_myacc);

        mImgMyPortrait = (AsyncImageView) findViewById(R.id.img_my_portrait);
        mMyPortrait = (RelativeLayout) findViewById(R.id.rl_my_portrait);
        mMyUsername = (RelativeLayout) findViewById(R.id.rl_my_username);
        mTVUsername = (TextView) findViewById(R.id.tv_my_username);

        if (DemoContext.getInstance().getSharedPreferences() != null) {
            mUserName = DemoContext.getInstance().getSharedPreferences().getString(Constants.APP_USER_NAME, null);
            String userPortrait = DemoContext.getInstance().getSharedPreferences().getString("DEMO_USER_PORTRAIT", null);
            mImgMyPortrait.setResource(new Resource(Uri.parse(userPortrait)));
            mTVUsername.setText(mUserName.toString());
        }

        mMyUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MyAccountActivity.this, UpdateNameActivity.class);
                intent.putExtra("USERNAME", mUserName);
                startActivityForResult(intent, RESULTCODE);
            }
        });

        mMyPortrait.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String  id = DemoContext.getInstance().getSharedPreferences().getString(Constants.APP_USER_ID,Constants.DEFAULT);
                String  name = DemoContext.getInstance().getSharedPreferences().getString(Constants.APP_USER_NAME,Constants.DEFAULT);
                String uri = "http://jdd.kefu.rongcloud.cn/image/service_80x80.png";
                String uritest = "https://www.chembeango.com/assets/images/niko.png";

                UserInfo userInfo = new UserInfo(id,name,Uri.parse(uritest));
                RongIM.getInstance().refreshUserInfoCache(userInfo);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (resultCode) {
            case Constants.FIX_USERNAME_REQUESTCODE:
                if (data != null) {
                    mTVUsername.setText(data.getStringExtra("UPDATA_RESULT"));
                    mUserName = data.getStringExtra("UPDATA_RESULT");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}
