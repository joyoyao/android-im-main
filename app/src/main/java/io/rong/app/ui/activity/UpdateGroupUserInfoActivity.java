package io.rong.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.ui.widget.WinToast;
import io.rong.app.utils.Constants;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.GroupUserInfo;

/**
 * Created by Bob on 15/11/13.
 */
public class UpdateGroupUserInfoActivity extends BaseActionBarActivity {

    private EditText mNewName;
    String mGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_update_group_info);

        getSupportActionBar().setTitle(R.string.de_actionbar_update_group);
        mNewName = (EditText) findViewById(R.id.et_new_name);

        Intent intent = getIntent();
        mGroupId = intent.getStringExtra("DEMO_GROUP_ID");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.de_fix_username, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.icon:
                if (RongIM.getInstance() == null || DemoContext.getInstance() == null )

                    return true;

                    RongIM.setGroupUserInfoProvider(new RongIM.GroupUserInfoProvider() {
                        @Override
                        public GroupUserInfo getGroupUserInfo(String groupId, String userId) {

                            String currentUserId = DemoContext.getInstance().getSharedPreferences().getString(Constants.APP_USER_ID, Constants.DEFAULT);

                            if (userId.equals(currentUserId)) {

                                WinToast.toast(UpdateGroupUserInfoActivity.this, "修改成功");

                                Intent intent = new Intent();
                                intent.putExtra("UPDATA_GROPU_INFO", mNewName.getText().toString());
                                setResult(Constants.FIX_GROUP_INFO, intent);
                                finish();
                                return new GroupUserInfo(mGroupId, userId, mNewName.getText().toString());

                            } else {
                                return null;
                            }
                        }
                    },true);

                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
