package io.rong.imkit.widget.provider;

import android.content.Context;
import android.net.Uri;
import android.os.Parcelable;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;

import io.rong.imkit.model.UIMessage;
import io.rong.imlib.model.MessageContent;

public interface IContainerItemProvider<T extends Parcelable> {


    /**
     * 创建新View。
     *
     * @param context 当前上下文。
     * @param group   创建的新View所附属的父View。
     * @return 需要创建的新View。
     */
    public View newView(Context context, ViewGroup group);

    /**
     * 为View绑定数据。
     *
     * @param v        需要绑定数据的View。
     * @param position 绑定的数据位置。
     * @param data     绑定的数据。
     */
    public void bindView(View v, int position, T data);

    /**
     * 消息内容适配器。
     */
    public abstract class MessageProvider<K extends MessageContent> implements IContainerItemProvider<UIMessage> {

        /**
         * 为View绑定数据。
         *
         * @param v        需要绑定数据的View。
         * @param position 绑定的数据位置。
         * @param data     绑定的消息。
         */
        @Override
        public final void bindView(View v, int position, UIMessage data) {

            bindView(v, position, (K) data.getContent(), data);
        }

        /**
         * 为View绑定数据。
         *
         * @param v        需要绑定数据的View。
         * @param position 绑定的数据位置。
         * @param content  绑定的消息内容。
         * @param message  绑定的消息。
         */
        public abstract void bindView(View v, int position, K content, UIMessage message);

        /**
         * 当前数据的简单描述。
         *
         * @param data 当前需要绑定的数据
         * @return 数据的描述。
         */
        public final Spannable getSummary(UIMessage data) {
            return getContentSummary((K) data.getContent());
        }

        /**
         * 当前数据的简单描述。
         *
         * @param data 当前需要绑定的数据
         * @return 数据的描述。
         */
        public abstract Spannable getContentSummary(K data);

        /**
         * View的点击事件。
         *
         * @param view     所点击的View。
         * @param position 点击的位置。
         * @param content  点击的消息内容。
         * @param message  点击的消息。
         */
        public abstract void onItemClick(View view, int position, K content, UIMessage message);


        /**
         * View的长按事件。
         *
         * @param view     所长按的View。
         * @param position 长按的位置。
         * @param content  长按的消息内容。
         * @param message  长按的消息。
         */
        public abstract void onItemLongClick(final View view, int position, final K content, final UIMessage message);


    }


    /**
     * 会话适配器。
     */
    public interface ConversationProvider<T extends Parcelable> extends IContainerItemProvider<T> {
        /**
         * 绑定标题内容。
         *
         * @param id 需要绑定标题的Id。
         * @return 绑定标题内容。
         */
        public String getTitle(String id);

        /**
         * 绑定头像Uri。
         *
         * @param id 需要显示头像的Id。
         * @return 当前头像Uri。
         */
        public Uri getPortraitUri(String id);


    }


}
