package io.rong.app.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sea_monster.exception.BaseException;
import com.sea_monster.network.AbstractHttpRequest;
import com.sea_monster.resource.Resource;

import java.util.List;

import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.database.UserInfos;
import io.rong.app.model.Status;
import io.rong.app.model.User;
import io.rong.app.ui.widget.LoadingDialog;
import io.rong.app.ui.widget.WinToast;
import io.rong.app.utils.Constants;
import io.rong.imkit.RongIM;
import io.rong.imkit.widget.AsyncImageView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by Bob on 2015/4/7.
 * <p/>
 * 个人详情
 */
public class PersonalDetailActivity extends BaseApiActivity {

    private AbstractHttpRequest<Status> mDeleteFriendRequest;
    private AbstractHttpRequest<User> mUserHttpRequest;
    private AbstractHttpRequest<User> getUserInfoByUserIdHttpRequest;

    private LoadingDialog mDialog;
    /**
     * 好友id list
     */
    private List friendList;
    /**
     * 当前页面用户的 UserInfo
     */
    private UserInfo userInfo;
    /**
     * 当前页面用户的 UserId
     */
    private String currentUserId;
    private boolean isSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.de_fr_personal_intro);

        getSupportActionBar().setTitle(R.string.de_actionbar_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.de_actionbar_back);

        mPersonalImg = (AsyncImageView) findViewById(R.id.personal_portrait);
        mPersonalName = (TextView) findViewById(R.id.personal_name);
        mPersonalId = (TextView) findViewById(R.id.personal_id);
        mSendMessage = (Button) findViewById(R.id.send_message);
        mAddFriend = (Button) findViewById(R.id.add_friend);

        mDialog = new LoadingDialog(this);

        mSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (RongIM.getInstance() != null && DemoContext.getInstance() != null) {
                    if (currentUserId != null)
                        RongIM.getInstance().startPrivateChat(PersonalDetailActivity.this, currentUserId,
                                DemoContext.getInstance().getUserInfoById(currentUserId).getName());
                }
            }
        });

        mAddFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currentUserId != null) {
                    if (mDialog != null && !mDialog.isShowing())
                        mDialog.show();

                    mUserHttpRequest = DemoContext.getInstance().getDemoApi().sendFriendInvite(currentUserId, "请添加我为好友 ", PersonalDetailActivity.this);
                }
            }
        });

        initData();
    }

    protected void initData() {

        if (DemoContext.getInstance() == null)
            return;

        friendList = DemoContext.getInstance().getFriendListId();

        if (getIntent().hasExtra("USER")) {
            userInfo = getIntent().getParcelableExtra("USER");

            currentUserId = userInfo.getUserId();
            if (userInfo != null && friendList != null) {

                if (friendList.contains(userInfo.getUserId())) {
                    mAddFriend.setVisibility(View.GONE);
                    mSendMessage.setVisibility(View.VISIBLE);
                    mPersonalId.setText("Id: " + userInfo.getUserId());
                } else {
                    mAddFriend.setVisibility(View.VISIBLE);
                    mSendMessage.setVisibility(View.GONE);
                }
                mPersonalImg.setResource(new Resource(userInfo.getPortraitUri()));
                mPersonalName.setText(userInfo.getName());
            }
        } else if (getIntent().hasExtra("CONTACTS_USER")) {

            currentUserId = getIntent().getStringExtra("CONTACTS_USER");

            userInfo = DemoContext.getInstance().getUserInfoById(currentUserId);
            mPersonalImg.setResource(new Resource(userInfo.getPortraitUri()));
            mPersonalName.setText(userInfo.getName());
            mPersonalId.setText("Id:" + userInfo.getUserId());
            mAddFriend.setVisibility(View.GONE);
            mSendMessage.setVisibility(View.VISIBLE);
        } else if (getIntent().hasExtra("USER_SEARCH")) {
            isSearch = getIntent().getBooleanExtra("USER_SEARCH", false);
            mAddFriend.setVisibility(View.VISIBLE);
            mSendMessage.setVisibility(View.GONE);
            mPersonalImg.setResource(new Resource(userInfo.getPortraitUri()));
            mPersonalName.setText(userInfo.getName());
            currentUserId = userInfo.getUserId();
        }

        if (currentUserId != null)
            getUserInfoByUserIdHttpRequest = DemoContext.getInstance().getDemoApi().getUserInfoByUserId(currentUserId, this);
    }

    @Override
    public void onCallApiSuccess(AbstractHttpRequest request, Object obj) {
        if (mDeleteFriendRequest != null && mDeleteFriendRequest.equals(request)) {
            if (mDialog != null)
                mDialog.dismiss();
            if (obj instanceof Status) {
                final Status status = (Status) obj;
                if (status.getCode() == 200) {
                    WinToast.toast(this, "删除好友成功");
                    if (DemoContext.getInstance() != null && currentUserId != null) {
                        //删除好友成功后，将这个好友的会话从会话列表删除
                        RongIM.getInstance().getRongIMClient().removeConversation(Conversation.ConversationType.PRIVATE, currentUserId);
                        DemoContext.getInstance().updateUserInfos(currentUserId, "2");

                        Intent intent = new Intent();
                        this.setResult(Constants.DELETE_USERNAME_REQUESTCODE, intent);

                    }
                } else if (status.getCode() == 306) {
                    WinToast.toast(this, status.getMessage());
                }
            }
        } else if (getUserInfoByUserIdHttpRequest != null && getUserInfoByUserIdHttpRequest.equals(request)) {
            if (obj instanceof User) {
                final User user = (User) obj;

                if (user.getCode() == 200) {

                    UserInfos addFriend = new UserInfos();
                    addFriend.setUsername(user.getResult().getUsername());
                    addFriend.setUserid(user.getResult().getId());
                    addFriend.setPortrait(user.getResult().getPortrait());
                    if (friendList.contains(user.getResult().getId())) {
                        addFriend.setStatus("1");
                    } else {
                        addFriend.setStatus("0");
                    }

                    if (DemoContext.getInstance() != null)
                        DemoContext.getInstance().insertOrReplaceUserInfos(addFriend);

                    mPersonalName.setText(user.getResult().getUsername());

                    RongIM.getInstance().refreshUserInfoCache(new UserInfo(user.getResult().getId(), user.getResult().getUsername(), Uri.parse(user.getResult().getPortrait())));
                }
            }
        }else if(mUserHttpRequest!=null && mUserHttpRequest.equals(request)){
            if (mDialog != null)
                mDialog.dismiss();

            WinToast.toast(this, "好友请求发送成功");
        }
    }

    @Override
    public void onCallApiFailure(AbstractHttpRequest request, BaseException e) {
        if (mDialog != null)
            mDialog.dismiss();
        Log.e("PersonalDetailActivity","-----onCallApiFailure------"+e);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.per_menu, menu);

        if (userInfo != null && friendList != null) {
            if (!friendList.contains(userInfo.getUserId())) {
                menu.getItem(0).setVisible(false);
            }
        } else if (isSearch) {
            menu.getItem(0).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.per_item1://加入黑名单
                if (DemoContext.getInstance() != null && RongIM.getInstance().getRongIMClient() != null && currentUserId != null) {
                    RongIM.getInstance().getRongIMClient().addToBlacklist(currentUserId, new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            WinToast.toast(PersonalDetailActivity.this, "加入黑名单成功");
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {

                        }
                    });
                }

                break;
            case R.id.per_item2://删除好友
                final AlertDialog.Builder alterDialog = new AlertDialog.Builder(this);
                alterDialog.setMessage("是否删除好友？");
                alterDialog.setCancelable(true);

                alterDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (DemoContext.getInstance() != null && currentUserId != null) {

                            if (mDialog != null && !mDialog.isShowing())
                                mDialog.show();

                            mDeleteFriendRequest = DemoContext.getInstance().getDemoApi().deletefriends(currentUserId, PersonalDetailActivity.this);
                        }
                    }
                });
                alterDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alterDialog.show();

                break;
            case android.R.id.home:
                finish();
                break;

        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * 头像
     */
    private AsyncImageView mPersonalImg;
    /**
     * 昵称
     */
    private TextView mPersonalName;
    /**
     * 用户 id
     */
    private TextView mPersonalId;
    /**
     * 发送消息
     */
    private Button mSendMessage;
    /**
     * 添加到通讯录
     */
    private Button mAddFriend;


}
