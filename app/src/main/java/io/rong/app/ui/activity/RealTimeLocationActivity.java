package io.rong.app.ui.activity;

import android.os.Bundle;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.LatLng;
import com.sea_monster.exception.BaseException;

import java.util.List;

import de.greenrobot.event.EventBus;
import io.rong.app.DemoContext;
import io.rong.app.R;
import io.rong.app.DemoApi;
import io.rong.app.model.RongEvent;
import io.rong.app.ui.widget.RealTimeLocationHorizontalScrollView;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.UserInfo;

/**
 * Created by zhjchen on 8/12/15.
 */
public class RealTimeLocationActivity extends LocationMapActivity implements View.OnClickListener {

    private RealTimeLocationHorizontalScrollView horizontalScrollView;
    private ImageView mExitImageView;
    private ImageView mCloseImageView;
    private TextView mParticipantTextView;

    private RelativeLayout mLayout;


    @Override
    protected int getContentView() {
        return R.layout.activity_share_location;
    }

    @Override
    protected MapView initView(Bundle savedInstanceState) {

        MapView mapView = (MapView) findViewById(R.id.map);
        horizontalScrollView = (RealTimeLocationHorizontalScrollView) findViewById(R.id.scroll_view);

        mExitImageView = (ImageView) findViewById(android.R.id.icon);
        mCloseImageView = (ImageView) findViewById(android.R.id.icon1);

        mExitImageView.setOnClickListener(this);
        mCloseImageView.setOnClickListener(this);

        mParticipantTextView = (TextView) findViewById(android.R.id.text1);

        mLayout= (RelativeLayout) findViewById(R.id.layout);
        mLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
//                Log.d("RealTimeLocation", "--onTouch-------------");
                return true;
            }
        });

        EventBus.getDefault().register(this);

        int type = getIntent().getIntExtra("conversationType", 0);
        targetId = getIntent().getStringExtra("targetId");

        conversationType = Conversation.ConversationType.setValue(type);

        return mapView;
    }


    @Override
    protected void initData() {
        super.initData();

        final List<String> userIds = RongIMClient.getInstance().getRealTimeLocationParticipants(conversationType, targetId);

        if (userIds != null && userIds.size() > 0) {

            for (String userId : userIds) {
                addUserInfoToScrollView(userId);
            }

            setParticipantTextView(userIds.size());
        }

    }

    private void setParticipantTextView(int count) {

        if (count == -1) {
            final List<String> userIds = RongIMClient.getInstance().getRealTimeLocationParticipants(conversationType, targetId);

            if (userIds != null && userIds.size() > 0) {
                count = userIds.size();
            }
        }

        mParticipantTextView.setText(String.format(" %1$d人在共享位置", count));
    }

    @Override
    public void onClick(View v) {

        if (v == mExitImageView) {
            RongIMClient.getInstance().quitRealTimeLocation(conversationType, targetId);
//            Log.d("RealTimeLocationActivity", "--quitRealTimeLocation---");
            finish();
        } else if (v == mCloseImageView) {
            finish();
        }

    }


    public void onEventMainThread(final RongEvent.RealTimeLocationReceiveEvent event) {
        String userId = event.getUserId();

        DemoContext.getInstance().getDemoApi().getUserInfo(userId, new DemoApi.GetUserInfoListener() {

            @Override
            public void onSuccess(final UserInfo userInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        moveMarker(new LatLng(event.getLatitude(), event.getLongitude()), userInfo);
                    }
                });
            }

            @Override
            public void onError(String userId, BaseException e) {

            }
        });
    }


    public void onEventMainThread(RongEvent.RealTimeLocationQuitEvent event) {

        String userId = event.getUserId();

        removeMarker(userId);

        horizontalScrollView.removeUserFromView(userId);
        setParticipantTextView(-1);
    }

    public void onEventMainThread(RongEvent.RealTimeLocationJoinEvent event) {
        String userId = event.getUserId();

        addUserInfoToScrollView(userId);
    }

    @Override
    public boolean handleMessage(Message msg) {

        return false;
    }

    private void addUserInfoToScrollView(final String userId) {


        DemoContext.getInstance().getDemoApi().getUserInfo(userId, new DemoApi.GetUserInfoListener() {

            @Override
            public void onSuccess(final UserInfo userInfo) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        horizontalScrollView.addUserToView(userInfo);
                        setParticipantTextView(-1);
                    }
                });

            }

            @Override
            public void onError(String userId, BaseException e) {

            }
        });
    }


    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }
}
