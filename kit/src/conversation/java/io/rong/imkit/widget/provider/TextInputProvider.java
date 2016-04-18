package io.rong.imkit.widget.provider;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import io.rong.imkit.R;
import io.rong.imkit.RLog;
import io.rong.imkit.RongContext;
import io.rong.imkit.fragment.MessageInputFragment;
import io.rong.imkit.model.Draft;
import io.rong.imkit.model.Emoji;
import io.rong.imkit.util.AndroidEmoji;
import io.rong.imkit.widget.InputView;
import io.rong.imkit.widget.RCCircleFlowIndicator;
import io.rong.imkit.widget.RCViewFlow;
import io.rong.imkit.widget.adapter.EmojiPagerAdapter;
import io.rong.imlib.MessageTag;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Conversation;
import io.rong.message.TextMessage;

/**
 * Created by DragonJ on 15/2/11.
 */
public class TextInputProvider extends InputProvider.MainInputProvider implements TextWatcher, View.OnClickListener, View.OnFocusChangeListener, EmojiPagerAdapter.OnEmojiItemClickListener, View.OnLongClickListener {

    EditText mEdit;
    ImageView mSmile;
    Button mButton;
    InputView mInputView;
    LayoutInflater mInflater;
    FragmentManager mFragmentManager;
    RCViewFlow mFlow;
    Context mContext;
    Handler mHandler;
    TextWatcher mExtraTextWatcher;


    public TextInputProvider(RongContext context) {
        super(context);
        RLog.d(this, "TextInputProvider", "");

    }

    @Override
    public void onAttached(MessageInputFragment fragment, InputView view) {
        super.onAttached(fragment, view);

        mContext = fragment.getActivity();
        mFragmentManager = fragment.getActivity().getSupportFragmentManager();
        mHandler = new Handler();

        RLog.d(this, "onAttached", "");
    }

    @Override
    public void onDetached() {
        RLog.d(this, "Detached", "");

        if (mEdit != null && !TextUtils.isEmpty(mEdit.getText())) {
            RongContext.getInstance().executorBackground(new SaveDraftRunnable(getCurrentConversation(), mEdit.getText().toString()));
        } else {
            RongContext.getInstance().executorBackground(new CleanDraftRunnable(getCurrentConversation()));
        }

        mFlow = null;
        super.onDetached();
    }

    @Override
    public Drawable obtainSwitchDrawable(Context context) {
        return context.getResources().getDrawable(R.drawable.rc_ic_keyboard);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, InputView inputView) {
        RLog.d(this, "onCreateView", "");
        mInflater = inflater;

        View view = inflater.inflate(R.layout.rc_wi_txt_provider, parent);
        mEdit = (EditText) view.findViewById(android.R.id.edit);
        mSmile = (ImageView) view.findViewById(android.R.id.icon);

        if (inputView.getToggleLayout().getVisibility() == View.VISIBLE) {
            mButton = (Button) inflater.inflate(R.layout.rc_wi_text_btn, inputView.getToggleLayout(), false);
            inputView.getToggleLayout().addView(mButton);
        }

        if (inputView.getToggleLayout().getVisibility() != View.VISIBLE || mButton == null)
            mButton = (Button) view.findViewById(android.R.id.button1);

        mEdit.addTextChangedListener(this);
        mEdit.setOnFocusChangeListener(this);
        mSmile.setOnClickListener(this);
        mEdit.setOnClickListener(this);
        mEdit.setOnLongClickListener(this);
        mInputView = inputView;
        mButton.setOnClickListener(this);

        RongContext.getInstance().executorBackground(new DraftRenderRunnable(getCurrentConversation()));

        return view;
    }

    @Override
    public void setCurrentConversation(Conversation conversation) {
        super.setCurrentConversation(conversation);
        RongContext.getInstance().executorBackground(new DraftRenderRunnable(conversation));
    }

    class DraftRenderRunnable implements Runnable {
        Conversation conversation;

        DraftRenderRunnable(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().getTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), new RongIMClient.ResultCallback<String>() {
                @Override
                public void onSuccess(String s) {
                    if(!TextUtils.isEmpty(s) && mEdit != null) {
                        mEdit.setText(s);
                        mEdit.setSelection(s.length());
                    }
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    class SaveDraftRunnable implements Runnable {
        String content;
        Conversation conversation;

        SaveDraftRunnable(Conversation conversation, String content) {
            this.conversation = conversation;
            this.content = content;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().saveTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), content, new RongIMClient.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    int type = conversation.getConversationType().getValue();
                    String targetId = conversation.getTargetId();
                    Draft draft = new Draft(targetId, type, content, null);
                    getContext().getEventBus().post(draft);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    class CleanDraftRunnable implements Runnable {
        Conversation conversation;

        CleanDraftRunnable(Conversation conversation) {
            this.conversation = conversation;
        }

        @Override
        public void run() {
            if (conversation == null || conversation.getTargetId() == null)
                return;

            RongIMClient.getInstance().clearTextMessageDraft(conversation.getConversationType(), conversation.getTargetId(), new RongIMClient.ResultCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    int type = conversation.getConversationType().getValue();
                    String targetId = conversation.getTargetId();
                    Draft draft = new Draft(targetId, type, null, null);
                    getContext().getEventBus().post(draft);
                }

                @Override
                public void onError(RongIMClient.ErrorCode e) {

                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (mSmile.equals(v)) {

            if (mFlow == null) {
                View view = mInflater.inflate(R.layout.rc_wi_ext_pager, mInputView.getExtendLayout());

                mFlow = (RCViewFlow) view.findViewById(R.id.rc_flow);

                RCCircleFlowIndicator indicator = (RCCircleFlowIndicator) view.findViewById(R.id.rc_indicator);
                mFlow.setFlowIndicator(indicator);

                EmojiPagerAdapter adapter = new EmojiPagerAdapter(mContext, mInputView.getExtendLayout(), AndroidEmoji.getEmojiList(), mFragmentManager);
                adapter.setEmojiItemClickListener(this);

                mFlow.setAdapter(adapter);
                if(mEdit != null){
                    mEdit.requestFocus();
                }
                mInputView.onEmojiProviderActive(getContext());
                mInputView.setExtendLayoutVisibility(View.VISIBLE);
            } else if (mInputView.getExtendLayout().getVisibility() == View.GONE) {
                mInputView.onEmojiProviderActive(getContext());
                mInputView.setExtendLayoutVisibility(View.VISIBLE);
            } else {
                mInputView.onProviderInactive(getContext());
            }
        } else if (v.equals(mButton)) {
            if (TextUtils.isEmpty(mEdit.getText().toString().trim())) {
                mEdit.getText().clear();
                mEdit.setText("");
                return;
            }

            publish(TextMessage.obtain(mEdit.getText().toString()));
            mEdit.getText().clear();
            mEdit.setText("");
        } else if (mEdit.equals(v)) {
            mInputView.onProviderActive(getContext());
        }
    }


    @Override
    public boolean onLongClick(View v) {
        if (mInputView != null && mInputView.getExtendLayout().getVisibility() == View.VISIBLE) {
            mInputView.onProviderInactive(getContext());
            mInputView.setExtendLayoutVisibility(View.GONE);
        }
        return false;
    }

    @Override
    public void onActive(Context context) {
        if (mEdit == null)
            return;

        mEdit.requestFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mEdit, 0);

    }

    @Override
    public void onInactive(Context context) {

        if (mEdit == null)
            return;

//        mEdit.clearFocus();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mEdit.getWindowToken(), 0);
    }

    @Override
    public void onSwitch(Context context) {
        mButton.setVisibility(View.GONE);
        onInactive(context);
        if (mEdit != null && !TextUtils.isEmpty(mEdit.getText())) {
            RongContext.getInstance().executorBackground(new SaveDraftRunnable(getCurrentConversation(), mEdit.getText().toString()));
        } else {
            RongContext.getInstance().executorBackground(new CleanDraftRunnable(getCurrentConversation()));
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (mEdit != null) {

            if (view.getTag() != null && view.getTag() instanceof Emoji) {
                Emoji emoji = (Emoji) view.getTag();

                for (char item : Character.toChars(emoji.getCode())) {
                    mEdit.getText().insert(mEdit.getSelectionStart(), Character.toString(item));
                }

            } else if (view.getTag().equals(-1)) {
                int keyCode = KeyEvent.KEYCODE_DEL;  //这里是退格键
                KeyEvent keyEventDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);

                KeyEvent keyEventUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                mEdit.onKeyDown(keyCode, keyEventDown);
                mEdit.onKeyUp(keyCode, keyEventUp);
            } else if (view.getTag().equals(0)) {

            }
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (mInputView != null && hasFocus)
            mInputView.setExtendInputsVisibility(View.GONE);
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if(mExtraTextWatcher != null)
        mExtraTextWatcher.beforeTextChanged(s,start,count,after);
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if(mExtraTextWatcher != null)

            mExtraTextWatcher.onTextChanged(s,start,before,count);

        if (mButton != null) {
            if (TextUtils.isEmpty(s)) {
                mButton.setVisibility(View.GONE);
            } else {
                mButton.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void afterTextChanged(final Editable s) {
        //isShowMessageTyping 是否显示输入状态
        if (s.toString().length() > 0) {
            boolean isShowMessageTyping = RongIMClient.getInstance().getTypingStatus();
            if (isShowMessageTyping == true) {
                MessageTag tag = TextMessage.class.getAnnotation(MessageTag.class);
                onTypingMessage(tag.value());
            }
        }

        if (AndroidEmoji.isEmoji(s.toString())) {
            int start = mEdit.getSelectionStart();
            int end = mEdit.getSelectionEnd();
            mEdit.removeTextChangedListener(this);
            mEdit.setText(AndroidEmoji.ensure(s.toString()));
            mEdit.addTextChangedListener(this);
            mEdit.setSelection(start, end);
        }
        if(mExtraTextWatcher != null)
            mExtraTextWatcher.afterTextChanged(s);

        RLog.d(this, "afterTextChanged", s.toString());
    }

    /**
     * 设置输入框
     *
     * @param content
     */
    public void setEditTextContent(CharSequence content) {

        if (mEdit != null && !TextUtils.isEmpty(content)) {
            mEdit.setText(content);
            mEdit.setSelection(content.length());
        }
    }

    public void setEditTextChangedListener(TextWatcher listener) {
        mExtraTextWatcher = listener;
    }
}