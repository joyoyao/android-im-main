package io.rong.imkit.util;

import io.rong.imkit.model.UIConversation;
import io.rong.imkit.widget.adapter.BaseAdapter;

public class ConversationListUtils {
    public static int findPositionForNewConversation(UIConversation uiconversation, BaseAdapter<UIConversation> mAdapter) {
        int count = mAdapter.getCount();
        int i, position = 0;

        for (i = 0; i < count; i++) {
            if (uiconversation.isTop()) {
                if (mAdapter.getItem(i).isTop() && mAdapter.getItem(i).getUIConversationTime() > uiconversation.getUIConversationTime())
                    position++;
                else
                    break;
            } else {
                if (mAdapter.getItem(i).isTop() || mAdapter.getItem(i).getUIConversationTime() > uiconversation.getUIConversationTime())
                    position++;
                else
                    break;
            }
        }

        return position;
    }

    public static int findPositionForSetTop(UIConversation uiconversation, BaseAdapter<UIConversation> mAdapter) {
        int count = mAdapter.getCount();
        int i, position = 0;

        for (i = 0; i < count; i++) {
            if (mAdapter.getItem(i).isTop() && mAdapter.getItem(i).getUIConversationTime() > uiconversation.getUIConversationTime()) {
                position++;
            } else {
                break;
            }
        }
        return position;
    }

    /* function: 查找对某一会话取消置顶时，该会话的新位置
    * @Param index :该会话的原始位置
    * return: 该会话取消置顶后的新位置 */
    public static int findPositionForCancleTop(int index, BaseAdapter<UIConversation> mAdapter) {
        int count = mAdapter.getCount();
        int tap = 0;

        if (index > count) {
            throw new IllegalArgumentException("the index for the position is error!");
        }

        for (int i = index + 1; i < count; i++) {
            if (mAdapter.getItem(i).isTop()
                    || mAdapter.getItem(index).getUIConversationTime() < mAdapter.getItem(i).getUIConversationTime()) {
                tap++;
            } else {
                break;
            }
        }
        return index + tap;
    }
}
