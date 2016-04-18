package io.rong.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import io.rong.app.R;
import io.rong.app.ui.adapter.SubConversationListAdapterEx;
import io.rong.imkit.RongContext;
import io.rong.imkit.fragment.SubConversationListFragment;

/**
 * Created by Bob on 15/11/3.
 * 聚合会话列表
 */
public class SubConversationListActivity extends BaseActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rong_activity);
        SubConversationListFragment fragment = new SubConversationListFragment();
        fragment.setAdapter(new SubConversationListAdapterEx(RongContext.getInstance()));
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.rong_content, fragment);
        transaction.commit();

        Intent intent = getIntent();
        //聚合会话参数
        String type = intent.getData().getQueryParameter("type");

        if(type == null )
            return;

        if (type.equals("group")) {
            getSupportActionBar().setTitle(R.string.de_actionbar_sub_group);
        } else if (type.equals("private")) {
            getSupportActionBar().setTitle(R.string.de_actionbar_sub_private);
        } else if (type.equals("discussion")) {
            getSupportActionBar().setTitle(R.string.de_actionbar_sub_discussion);
        } else if (type.equals("system")) {
            getSupportActionBar().setTitle(R.string.de_actionbar_sub_system);
        } else {
            getSupportActionBar().setTitle(R.string.de_actionbar_sub_defult);
        }
    }

}
