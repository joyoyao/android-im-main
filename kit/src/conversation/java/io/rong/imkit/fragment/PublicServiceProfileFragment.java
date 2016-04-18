package io.rong.imkit.fragment;

import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import io.rong.imkit.R;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.cache.RongCache;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.widget.LoadingDialogFragment;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceProfile;
import com.sea_monster.resource.Resource;
import io.rong.imkit.widget.AsyncImageView;

/**
 * Created by zhjchen on 4/18/15.
 */

public class PublicServiceProfileFragment extends DispatchResultFragment {

    public static final String AGS_PUBLIC_ACCOUNT_INFO = "arg_public_account_info";
    PublicServiceProfile mPublicAccountInfo;

    private AsyncImageView mPortraitIV;
    private TextView mNameTV;
    private TextView mAccountTV;
    private TextView mDescriptionTV;
    private Button mEnterBtn;
    private Button mFollowBtn;
    private Button mUnfollowBtn;

    private String mTargetId;
    private Conversation.ConversationType mConversationType;
    private String name;
    private LoadingDialogFragment mLoadingDialogFragment;

    @Override
    protected void initFragment(Uri uri) {

        if (getActivity().getIntent() != null) {
            mPublicAccountInfo = getActivity().getIntent().getParcelableExtra(AGS_PUBLIC_ACCOUNT_INFO);
        }

        if (uri != null) {
            if (mPublicAccountInfo == null) {
                String typeStr = !TextUtils.isEmpty(uri.getLastPathSegment()) ? uri.getLastPathSegment().toUpperCase() : "";
                mConversationType = Conversation.ConversationType.valueOf(typeStr);
                mTargetId = uri.getQueryParameter("targetId");
                name = uri.getQueryParameter("name");
            } else {
                mConversationType = mPublicAccountInfo.getConversationType();
                mTargetId = mPublicAccountInfo.getTargetId();
                name = mPublicAccountInfo.getName();
            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_public_service_inf, container, false);

        mPortraitIV = (AsyncImageView) view.findViewById(R.id.portrait);
        mNameTV = (TextView) view.findViewById(R.id.name);
        mAccountTV = (TextView) view.findViewById(R.id.account);
        mDescriptionTV = (TextView) view.findViewById(R.id.description);
        mEnterBtn = (Button) view.findViewById(R.id.enter);
        mFollowBtn = (Button) view.findViewById(R.id.follow);
        mUnfollowBtn = (Button) view.findViewById(R.id.unfollow);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLoadingDialogFragment = LoadingDialogFragment.newInstance("", getResources().getString(R.string.rc_notice_data_is_loading));

        if (mPublicAccountInfo != null) {
            initData(mPublicAccountInfo);
        } else {
            if (!TextUtils.isEmpty(mTargetId) && RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                Conversation.PublicServiceType publicServiceType = null;
                if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                    publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                else
                    System.err.print("the public service type is error!!");

                RongIM.getInstance().getRongIMClient().getPublicServiceProfile(publicServiceType, mTargetId, new RongIMClient.ResultCallback<PublicServiceProfile>() {
                    @Override
                    public void onSuccess(PublicServiceProfile info) {
                        if (info != null) {
                            initData(info);
                            ConversationKey key = ConversationKey.obtain(mTargetId, mConversationType);
                            RongContext context = RongContext.getInstance();
                            if(key != null) {
                                context.getPublicServiceInfoCache().put(key.getKey(), info);
                            }
                        }
                    }

                    @Override
                    public void onError(RongIMClient.ErrorCode e) {
                        Log.e("PublicServiceProfileFragment", "Failure to get data!!!");
                    }
                });
            }
        }


    }


    private void initData(final PublicServiceProfile info) {

        if (info != null) {

            mPortraitIV.setResource(new Resource(info.getPortraitUri()));
            mNameTV.setText(info.getName());
            mAccountTV.setText(String.format(getResources().getString(R.string.rc_pub_service_info_account, info.getTargetId())));
            mDescriptionTV.setText(info.getIntroduction());

            boolean isFollow = info.isFollow();
            boolean isGlobal = info.isGlobal();

            if (isGlobal) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.add(R.id.rc_layout, SetConversationNotificationFragment.newInstance());
                ft.commitAllowingStateLoss();

                mFollowBtn.setVisibility(View.GONE);
                mEnterBtn.setVisibility(View.VISIBLE);
                mUnfollowBtn.setVisibility(View.GONE);
            } else {

                if (isFollow) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.add(R.id.rc_layout, SetConversationNotificationFragment.newInstance());
                    ft.commitAllowingStateLoss();

                    mFollowBtn.setVisibility(View.GONE);
                    mEnterBtn.setVisibility(View.VISIBLE);
                    mUnfollowBtn.setVisibility(View.VISIBLE);
                } else {
                    mFollowBtn.setVisibility(View.VISIBLE);
                    mEnterBtn.setVisibility(View.GONE);
                    mUnfollowBtn.setVisibility(View.GONE);
                }
            }


            mEnterBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                    if(listener != null && listener.onEnterConversationClick(v.getContext(), info)) {
                        return;
                    } else {
                        getActivity().finish();
                        RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
                    }
                }
            });

            mFollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIM.getInstance().getRongIMClient().subscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mLoadingDialogFragment.dismiss();
                            mFollowBtn.setVisibility(View.GONE);
                            mEnterBtn.setVisibility(View.VISIBLE);
                            mUnfollowBtn.setVisibility(View.VISIBLE);

                            RongCache<String, PublicServiceProfile> cache = RongContext.getInstance().getPublicServiceInfoCache();
                            ConversationKey key = ConversationKey.obtain(info.getTargetId(), info.getConversationType());
                            if(cache.get(key.getKey()) == null)
                                cache.put(key.getKey(), info);
                            RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), true));
                            RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                            if(listener != null && listener.onFollowClick(v.getContext(), info) == true) {
                                return;
                            } else {
                                getActivity().finish();
                                RongIM.getInstance().startConversation(getActivity(), info.getConversationType(), info.getTargetId(), info.getName());
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {
                            mLoadingDialogFragment.dismiss();
                        }
                    });
                    mLoadingDialogFragment.show(getFragmentManager());
                }
            });

            mUnfollowBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {

                    Conversation.PublicServiceType publicServiceType = null;
                    if (mConversationType == Conversation.ConversationType.APP_PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.APP_PUBLIC_SERVICE;
                    else if (mConversationType == Conversation.ConversationType.PUBLIC_SERVICE)
                        publicServiceType = Conversation.PublicServiceType.PUBLIC_SERVICE;
                    else
                        System.err.print("the public service type is error!!");

                    RongIM.getInstance().getRongIMClient().unsubscribePublicService(publicServiceType, info.getTargetId(), new RongIMClient.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            mFollowBtn.setVisibility(View.VISIBLE);
                            mEnterBtn.setVisibility(View.GONE);
                            mUnfollowBtn.setVisibility(View.GONE);

                            RongCache<String, PublicServiceProfile> cache = RongContext.getInstance().getPublicServiceInfoCache();
                            ConversationKey key = ConversationKey.obtain(info.getTargetId(), info.getConversationType());
                            if (cache.get(key.getKey()) != null)
                                cache.remove(key.getKey());

                            RongContext.getInstance().getEventBus().post(Event.PublicServiceFollowableEvent.obtain(info.getTargetId(), info.getConversationType(), false));
                            RongIM.PublicServiceBehaviorListener listener = RongContext.getInstance().getPublicServiceBehaviorListener();
                            if (listener != null && listener.onUnFollowClick(v.getContext(), info)) {
                                return;
                            } else {
                                getActivity().finish();
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode errorCode) {

                        }
                    });
                }
            });
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

}
