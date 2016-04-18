package io.rong.app.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.sea_monster.exception.BaseException;
import com.sea_monster.network.AbstractHttpRequest;

import java.util.ArrayList;
import java.util.List;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.ui.adapter.SearchFriendAdapter;
import io.rong.app.model.ApiResult;
import io.rong.app.model.Friends;
import io.rong.app.ui.widget.LoadingDialog;
import io.rong.app.utils.Constants;
import io.rong.imlib.model.UserInfo;

/**
 * Created by Bob on 2015/3/26.
 */
public class SearchFriendActivity extends BaseApiActivity {

    private EditText mEtSearch;
    private ListView mListSearch;
    private AbstractHttpRequest<Friends> searchHttpRequest;
    private List<ApiResult> mResultList;
    private SearchFriendAdapter adapter;
    private LoadingDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_ac_search);
        getSupportActionBar().setTitle(R.string.public_account_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);
        mEtSearch = (EditText) findViewById(R.id.de_ui_search);
        Button mBtSearch = (Button) findViewById(R.id.de_search);
        mListSearch = (ListView) findViewById(R.id.de_search_list);
        mResultList = new ArrayList<ApiResult>();
        mDialog = new LoadingDialog(this);

        mBtSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = mEtSearch.getText().toString();
                if (mDialog != null && !mDialog.isShowing())
                    mDialog.show();

                if (DemoContext.getInstance() != null) {
                    searchHttpRequest = DemoContext.getInstance().getDemoApi().searchUserByUserName(userName, SearchFriendActivity.this);
                }
            }
        });

        mListSearch.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent in = new Intent(SearchFriendActivity.this, PersonalDetailActivity.class);
                UserInfo userInfo = new UserInfo(mResultList.get(position).getId(), mResultList.get(position).getUsername(), Uri.parse(mResultList.get(position).getPortrait()));
                in.putExtra("USER", userInfo);
                in.putExtra("USER_SEARCH", true);
                startActivityForResult(in, Constants.SEARCH_REQUESTCODE);
            }
        });
    }

    @Override
    public void onCallApiSuccess(AbstractHttpRequest request, Object obj) {
        if (searchHttpRequest !=null && searchHttpRequest.equals(request)) {
            if (mDialog != null)
                mDialog.dismiss();
            if (mResultList.size() > 0)
                mResultList.clear();
            if (obj instanceof Friends) {
                final Friends friends = (Friends) obj;

                if (friends.getCode() == 200) {
                    if (friends.getResult().size() > 0) {
                        for (int i = 0; i < friends.getResult().size(); i++) {
                            mResultList.add(friends.getResult().get(i));
                            Log.i("", "------onCallApiSuccess-user.getCode() == 200)-----" + friends.getResult().get(0).getId().toString());
                        }
                        adapter = new SearchFriendAdapter(mResultList, SearchFriendActivity.this);
                        mListSearch.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        }
    }

    @Override
    public void onCallApiFailure(AbstractHttpRequest request, BaseException e) {
        if (mDialog != null)
            mDialog.dismiss();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Constants.PERSONAL_REQUESTCODE) {
            Intent intent = new Intent();
            this.setResult(Constants.SEARCH_REQUESTCODE, intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }
}
