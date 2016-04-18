package io.rong.imkit.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.RongIM;
import io.rong.imkit.model.ConversationKey;
import io.rong.imkit.model.Event;
import io.rong.imkit.widget.InputView;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.PublicServiceMenu;
import io.rong.imlib.model.PublicServiceProfile;

/**
 * Created by DragonJ on 14/10/23.
 */
public class MessageInputFragment extends UriFragment implements View.OnClickListener {

    private final static String IS_SHOW_EXTEND_INPUTS = "isShowExtendInputs";

    Conversation mConversation;
    InputView mInput;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rc_fr_messageinput, container, false);
        mInput = (InputView) view.findViewById(R.id.rc_input);
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (RongContext.getInstance().getPrimaryInputProvider() == null) {
            throw new RuntimeException("MainInputProvider must not be null.");
        }

        if (getUri() != null) {

            String isShowExtendInputs = getUri().getQueryParameter(IS_SHOW_EXTEND_INPUTS);

            if (isShowExtendInputs != null && ("true".equals(isShowExtendInputs) || "1".equals(isShowExtendInputs))) {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setExtendInputsVisibility(View.VISIBLE);
                    }
                }, 500);

            } else {
                getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mInput.setExtendInputsVisibility(View.GONE);
                    }
                }, 500);
            }
        }
    }

    public void setOnInfoButtonClick(InputView.OnInfoButtonClick onInfoButtonClick) {
        mInput.setOnInfoButtonClickListener(onInfoButtonClick);
    }

    private void setCurrentConversation(final Conversation conversation) {

        RongContext.getInstance().getPrimaryInputProvider().setCurrentConversation(conversation);

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().setCurrentConversation(conversation);
        }

        if (RongContext.getInstance().getMenuInputProvider() != null) {
            RongContext.getInstance().getMenuInputProvider().setCurrentConversation(conversation);
        }

        for (InputProvider provider : RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType())) {
            provider.setCurrentConversation(conversation);
        }

        mInput.setExtendProvider(RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()));

        for (InputProvider provider : RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType())) {
            provider.onAttached(this, mInput);
        }

        if (conversation.getConversationType().equals(Conversation.ConversationType.APP_PUBLIC_SERVICE) ||
                conversation.getConversationType().equals(Conversation.ConversationType.PUBLIC_SERVICE)) {

            ConversationKey key = ConversationKey.obtain(conversation.getTargetId(), conversation.getConversationType());
            PublicServiceProfile info = RongContext.getInstance().getPublicServiceInfoFromCache(key.getKey());
            if(info == null) {
                Conversation.PublicServiceType type = Conversation.PublicServiceType.setValue(conversation.getConversationType().getValue());
                if (RongIM.getInstance() != null && RongIM.getInstance().getRongIMClient() != null) {
                    RongIM.getInstance().getRongIMClient().getPublicServiceProfile(type, conversation.getTargetId(), new RongIMClient.ResultCallback<PublicServiceProfile>() {
                        @Override
                        public void onSuccess(PublicServiceProfile publicServiceProfile) {
                            ConversationKey key = ConversationKey.obtain(conversation.getTargetId(), conversation.getConversationType());
                            RongContext.getInstance().getPublicServiceInfoCache().put(key.getKey(), publicServiceProfile);
                            PublicServiceMenu menu = publicServiceProfile.getMenu();
                            if (menu != null && menu.getMenuItems() != null && menu.getMenuItems().size() > 0) {
                                mInput.setInputProviderEx(RongContext.getInstance().getPrimaryInputProvider(),
                                        RongContext.getInstance().getSecondaryInputProvider(),
                                        RongContext.getInstance().getMenuInputProvider());
                            } else {
                                mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                                        RongContext.getInstance().getSecondaryInputProvider());
                            }
                        }

                        @Override
                        public void onError(RongIMClient.ErrorCode e) {
                            mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                                    RongContext.getInstance().getSecondaryInputProvider());
                        }
                    });
                }
            } else {
                PublicServiceMenu menu = info.getMenu();
                if (menu != null && menu.getMenuItems() != null && menu.getMenuItems().size() > 0) {
                    mInput.setInputProviderEx(RongContext.getInstance().getPrimaryInputProvider(),
                            RongContext.getInstance().getSecondaryInputProvider(),
                            RongContext.getInstance().getMenuInputProvider());
                } else {
                    mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                            RongContext.getInstance().getSecondaryInputProvider());
                }
            }
        } else {
            mInput.setInputProvider(RongContext.getInstance().getPrimaryInputProvider(),
                    RongContext.getInstance().getSecondaryInputProvider());
        }

        RongContext.getInstance().getPrimaryInputProvider().onAttached(this, mInput);

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().onAttached(this, mInput);
        }
    }


    @Override
    protected void initFragment(Uri uri) {
        String typeStr = uri.getLastPathSegment().toUpperCase();
        Conversation.ConversationType type = Conversation.ConversationType.valueOf(typeStr);

        String targetId = uri.getQueryParameter("targetId");

        String title = uri.getQueryParameter("title");

        if (type == null)
            return;

        mConversation = Conversation.obtain(type, targetId, title);

        if (mConversation != null) {
            setCurrentConversation(mConversation);
        }

    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onDestroyView() {
        RLog.d(this, "onDestroyView", "the primary input provider is:" + RongContext.getInstance().getPrimaryInputProvider());

        RongContext.getInstance().getPrimaryInputProvider().onDetached();

        if (RongContext.getInstance().getSecondaryInputProvider() != null) {
            RongContext.getInstance().getSecondaryInputProvider().onDetached();
        }

        EventBus.getDefault().unregister(this);

        super.onDestroyView();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }


    private DispatchResultFragment getDispatchFragment(Fragment fragment) {
        if (fragment instanceof DispatchResultFragment)
            return (DispatchResultFragment) fragment;

        if (fragment.getParentFragment() == null)
            throw new RuntimeException(fragment.getClass().getName() + " must has a parent fragment instance of DispatchFragment.");

        return getDispatchFragment(fragment.getParentFragment());
    }

    @Override
    public boolean handleMessage(android.os.Message msg) {
        return false;
    }

    public void startActivityFromProvider(InputProvider provider, Intent intent, int requestCode) {
        if (requestCode == -1) {
            startActivityForResult(intent, -1);
            return;
        }
        if ((requestCode & 0xffffff80) != 0) {
            throw new IllegalArgumentException("Can only use lower 7 bits for requestCode");
        }

        getDispatchFragment(this).startActivityForResult(this, intent, ((provider.getIndex() + 1) << 7) + (requestCode & 0x7f));

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        int index = requestCode >> 7;
        if (index != 0) {
            index--;
            if (index > RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()).size() + 1) {
                RLog.w(this, "onActivityResult", "Activity result provider index out of range: 0x"
                        + Integer.toHexString(requestCode));
                return;
            }

            if (index == 0) {
                RongContext.getInstance().getPrimaryInputProvider().onActivityResult(requestCode & 0x7f, resultCode, data);
            } else if (index == 1) {
                RongContext.getInstance().getSecondaryInputProvider().onActivityResult(requestCode & 0x7f, resultCode, data);
            } else {
                RongContext.getInstance().getRegisteredExtendProviderList(mConversation.getConversationType()).get(index - 2).onActivityResult(requestCode & 0x7f, resultCode, data);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    public void onEventMainThread(Event.InputViewEvent event) {

        if (event.isVisibility()) {
            mInput.setExtendInputsVisibility(View.VISIBLE);
        } else {
            mInput.setExtendInputsVisibility(View.GONE);
        }
    }
}
