package io.rong.imkit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.widget.adapter.BaseAdapter;
import io.rong.imkit.widget.provider.InputProvider;
import io.rong.imkit.widget.provider.VoiceInputProvider;

/**
 * Created by DragonJ on 15/2/9.
 */
public class InputView extends LinearLayout {


    InputProvider.MainInputProvider mMainProvider;
    InputProvider.MainInputProvider mSlaveProvider;
    InputProvider.MainInputProvider mMenuProvider;


    List<InputProvider> mProviderList;

    enum Style {
        SCE(0x123),
        ECS(0x321),
        CES(0x231),
        CSE(0x213),
        SC(0x120),
        CS(0x021),
        EC(0x320),
        CE(0x023),
        C(0x020);

        private int value = 0;

        Style(int value) {
            this.value = value;
        }
    }

    int mStyle;

    public enum Event {
        ACTION, INACTION, DESTROY;
    }

    RelativeLayout mInputLayout;
    LinearLayout mSwitcherLayout, mCustomMenuLayout;
    ImageView mMenuSwitcher1, mMenuSwitcher2;
    LinearLayout mInputMenuLayout, mInputMenuSwitchLayout;
    FrameLayout mCustomLayout;
    FrameLayout mWidgetLayout;
    FrameLayout mExtendLayout;
    FrameLayout mToggleLayout;
    ImageView mIcon1, mIcon2;
    GridView mPluginsLayout;
    View mView;

    int left, center, right;

    public InputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        View view = inflate(context, R.layout.rc_wi_input, this);
        mView = view;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.InputView);
        mStyle = a.getInt(R.styleable.InputView_RCStyle, 0x123);
        a.recycle();

        mProviderList = new ArrayList<>();

        mSwitcherLayout = (LinearLayout) view.findViewById(R.id.rc_switcher);
        mInputMenuSwitchLayout = (LinearLayout) view.findViewById(R.id.rc_menu_switch);
        mMenuSwitcher1 = (ImageView) view.findViewById(R.id.rc_switcher1);
        mMenuSwitcher2 = (ImageView) view.findViewById(R.id.rc_switcher2);
        mInputMenuLayout = (LinearLayout) view.findViewById(R.id.rc_input_menu);
        mInputLayout = (RelativeLayout) view.findViewById(android.R.id.input);
        mCustomLayout = (FrameLayout) view.findViewById(android.R.id.custom);
        mWidgetLayout = (FrameLayout) view.findViewById(android.R.id.widget_frame);
        mExtendLayout = (FrameLayout) view.findViewById(R.id.rc_ext);
        mToggleLayout = (FrameLayout) view.findViewById(android.R.id.toggle);

        mCustomMenuLayout = (LinearLayout) view.findViewById(R.id.rc_input_custom_menu);

        mIcon1 = (ImageView) view.findViewById(android.R.id.icon1);
        mIcon2 = (ImageView) view.findViewById(android.R.id.icon2);

        mPluginsLayout = (GridView) view.findViewById(R.id.rc_plugins);

        left = (mStyle >> 8) % 16;
        center = (mStyle >> 4) % 16;
        right = (mStyle) % 16;

        mIcon2.setImageDrawable(getResources().getDrawable(R.drawable.rc_ic_extend));
        mIcon2.setOnClickListener(new ExtendClickListener());
    }

    class ExtendClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mPluginsLayout.getVisibility() == View.GONE || mExtendLayout.getVisibility() == View.VISIBLE) {
                onExtendProviderActive(v.getContext());
                mPluginsLayout.setVisibility(View.VISIBLE);
                if (mMainProvider instanceof VoiceInputProvider) {
                    setInputProvider(mSlaveProvider, mMainProvider);
                }
            } else if (mPluginsLayout.getVisibility() == View.VISIBLE) {
                onProviderInactive(v.getContext());
            }
        }
    }

    public interface OnInfoButtonClick {
        public void onClick(View view);
    }

    private OnInfoButtonClick onInfoButtonClick;
    public void setOnInfoButtonClickListener(OnInfoButtonClick clickListener) {
        this.onInfoButtonClick = clickListener;
    }

    public void setExtendInputsVisibility(int visibility) {
        onProviderInactive(this.getContext());
        setPluginsLayoutVisibility(visibility);
    }

    public void setPluginsLayoutVisibility(int visibility) {
        mPluginsLayout.setVisibility(visibility);
    }

    public void setExtendLayoutVisibility(int visibility) {
        mExtendLayout.setVisibility(visibility);
    }

    public void setWidgetLayoutVisibility(int visibility) {
        mWidgetLayout.setVisibility(visibility);
    }

    private final void changeMainProvider(View view, InputProvider.MainInputProvider main, InputProvider.MainInputProvider slave) {
        mMainProvider.onSwitch(view.getContext());

        mPluginsLayout.setVisibility(View.GONE);
        mExtendLayout.setVisibility(View.GONE);

        setInputProvider(mSlaveProvider, mMainProvider);
    }

    private void setSCE() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(leftParams);

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }


    private void setECS() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(leftParams);

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCES() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mToggleLayout.setLayoutParams(centerParams);


        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(leftParams);
    }

    private void setCSE() {

        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(rightParams);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mSwitcherLayout.setLayoutParams(centerParams);


        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(leftParams);
    }

    private void setSC() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(leftParams);

        mToggleLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.RIGHT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCS() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mSwitcherLayout.setLayoutParams(rightParams);

        mToggleLayout.setVisibility(View.GONE);


        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());
        centerParams.addRule(RelativeLayout.LEFT_OF, mSwitcherLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setEC() {
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        leftParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        leftParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(leftParams);

        mSwitcherLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.RIGHT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setCE() {
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        mToggleLayout.setLayoutParams(rightParams);

        mSwitcherLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
        centerParams.addRule(RelativeLayout.LEFT_OF, mToggleLayout.getId());

        mCustomLayout.setLayoutParams(centerParams);
    }

    private void setC() {
        mSwitcherLayout.setVisibility(View.GONE);
        mToggleLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams centerParams = new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        centerParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

        mCustomLayout.setLayoutParams(centerParams);
    }


    public void setInputProvider(final InputProvider.MainInputProvider mainProvider, final InputProvider.MainInputProvider slaveProvider) {
        mMainProvider = mainProvider;
        mSlaveProvider = slaveProvider;

        if (mMenuProvider == null)
            mInputMenuSwitchLayout.setVisibility(View.GONE);

        mCustomLayout.removeAllViews();

        View leftView = null;
        View rightView = null;
        View centerView = null;


        switch (mStyle) {
            //SCE
            case (0x123):
                setSCE();
                break;
            //ECS
            case (0x321):
                setECS();
                break;
            //CES
            case (0x231):
                setCES();
                break;
            //CSE
            case (0x213):
                setCSE();
                break;
            //SC
            case (0x120):
                setSC();
                break;
            //CS
            case (0x021):
                setCS();
                break;
            //EC
            case (0x320):
                setEC();
                break;
            //CE
            case (0x023):
                setCE();
                break;
            //C
            case (0x020):
                setC();
                break;
        }

        if (mSlaveProvider != null) {
            mIcon1.setImageDrawable(mSlaveProvider.obtainSwitchDrawable(getContext()));
            mIcon1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(onInfoButtonClick != null)
                        onInfoButtonClick.onClick(v);
                    else
                        changeMainProvider(v, mSlaveProvider, mMainProvider);
                }
            });
        }

        mMainProvider.onCreateView(LayoutInflater.from(getContext()), mCustomLayout, this);
    }

    private Animation createPopupAnimIn(Context context) {
        AnimationSet animationSet = new AnimationSet(context, null);
        animationSet.setFillAfter(true);

        TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 150, 0);
        translateAnim.setDuration(300);
        animationSet.addAnimation(translateAnim);

        return animationSet;
    }

    private Animation createPopupAnimOut(Context context) {
        AnimationSet animationSet = new AnimationSet(context, null);
        animationSet.setFillAfter(true);

        TranslateAnimation translateAnim = new TranslateAnimation(0, 0, 0, 150);
        translateAnim.setDuration(300);
        animationSet.addAnimation(translateAnim);

        return animationSet;
    }

    public void setInputProviderEx(final InputProvider.MainInputProvider mainProvider,
                                   final InputProvider.MainInputProvider slaveProvider,
                                   final InputProvider.MainInputProvider menuProvider) {
        mMenuProvider = menuProvider;
        setInputProvider(mainProvider, slaveProvider);

        if (menuProvider != null && mMenuSwitcher1 != null) {
            mInputMenuSwitchLayout.setVisibility(View.VISIBLE);
            menuProvider.onCreateView(LayoutInflater.from(getContext()), mCustomMenuLayout, this);
            mInputMenuSwitchLayout.setOnClickListener(new InputClickListener());
            mMenuSwitcher2.setOnClickListener(new InputMenuClickListener());

            mMainProvider.onSwitch(getContext());
            mPluginsLayout.setVisibility(View.GONE);
            mExtendLayout.setVisibility(View.GONE);
            mInputLayout.setVisibility(View.GONE);
            mInputMenuLayout.setVisibility(View.VISIBLE);
        }
    }

    class InputClickListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            RLog.d(this, "InputClickListener", "change to input menu");

            mMainProvider.onSwitch(v.getContext());
            mPluginsLayout.setVisibility(View.GONE);
            mExtendLayout.setVisibility(View.GONE);

            mInputLayout.startAnimation(createPopupAnimOut(v.getContext()));
            mInputMenuLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInputLayout.clearAnimation();
                    mInputLayout.setVisibility(View.GONE);

                    mInputMenuLayout.startAnimation(createPopupAnimIn(v.getContext()));
                    mInputMenuLayout.setVisibility(View.VISIBLE);
                }
            }, 300 + 10);
        }
    }

    class InputMenuClickListener implements OnClickListener {

        @Override
        public void onClick(final View v) {
            RLog.d(this, "InputMenuClickListener", "change to input");

            mInputMenuLayout.startAnimation(createPopupAnimOut(v.getContext()));

            mInputLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInputMenuLayout.clearAnimation();
                    mInputMenuLayout.setVisibility(View.GONE);

                    mInputLayout.startAnimation(createPopupAnimIn(v.getContext()));
                    mInputLayout.setVisibility(View.VISIBLE);
                }
            }, 300 + 10);
        }
    }

    class PluginsAdapter extends BaseAdapter<InputProvider.ExtendProvider> {

        class ViewHolder {
            ImageView icon;
            TextView title;
        }

        public PluginsAdapter(Context context) {
            super(context);
        }

        @Override
        protected View newView(Context context, int position, ViewGroup group) {
            LayoutInflater inflater = LayoutInflater.from(context);

            View view = inflater.inflate(R.layout.rc_wi_plugins, null);

            ViewHolder holder = new ViewHolder();
            holder.icon = (ImageView) view.findViewById(android.R.id.icon);
            holder.title = (TextView) view.findViewById(android.R.id.title);
            view.setTag(holder);

            return view;
        }

        @Override
        protected void bindView(View v, int position, final InputProvider.ExtendProvider data) {
            ViewHolder holder = (ViewHolder) v.getTag();
            holder.icon.setImageDrawable(data.obtainPluginDrawable(v.getContext()));
            holder.title.setText(data.obtainPluginTitle(v.getContext()));

            holder.icon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.onPluginClick(v);
                }
            });

            if (position == 0) {
                int n;
                if (Build.VERSION.SDK_INT >= 11){
                    int columns = mPluginsLayout.getNumColumns();
                    n = mProviderList.size()%columns == 0 ? mProviderList.size()/columns : mProviderList.size()/columns + 1;
                }else {
                    n = mProviderList.size()%4 == 0 ? mProviderList.size()/4 : mProviderList.size()/4 + 1;
                }
                if (n > 2) {
                    int w = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    int h = View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                    v.measure(w, h);
                    int height = v.getMeasuredHeight();
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height * 2);
                    mPluginsLayout.setLayoutParams(layoutParams);
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }

    public void setExtendProvider(List<InputProvider.ExtendProvider> providers) {
        mProviderList.clear();
        for (InputProvider.ExtendProvider provider : providers) {
            mProviderList.add(provider);
        }

        PluginsAdapter adapter = new PluginsAdapter(getContext());
        adapter.addCollection(providers);

        int i = 1;
        for (InputProvider.ExtendProvider provider : providers) {
            provider.setIndex(++i);
        }

        mPluginsLayout.setAdapter(adapter);
    }


    public ViewGroup getExtendLayout() {
        return mExtendLayout;
    }

    public FrameLayout getToggleLayout() {
        return mToggleLayout;
    }
    public ImageView getIcon1() {
        return mIcon1;
    }

    public void onProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onActive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onActive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }

    public void onProviderInactive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.INACTION);
    }

    public void onExtendProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }

    public void onEmojiProviderActive(Context context) {
        if (mMainProvider != null)
            mMainProvider.onInactive(context);

        if (mSlaveProvider != null)
            mSlaveProvider.onInactive(context);

        if (mPluginsLayout.getVisibility() == View.VISIBLE) {
            mPluginsLayout.setVisibility(View.GONE);
        }

        if (mExtendLayout.getVisibility() == View.VISIBLE) {
            mExtendLayout.setVisibility(View.GONE);
        }

        RongContext.getInstance().getEventBus().post(Event.ACTION);
    }
}
