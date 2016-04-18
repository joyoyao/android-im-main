package io.rong.imkit.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.rong.imlib.model.Conversation;
import io.rong.imlib.model.Message;

/**
 * Created by DragonJ on 15/3/18.
 */


public class ConversationTypeFilter {

    public static enum Level {
        ALL, CONVERSATION_TYPE, NONE
    }

    Level mLevel;

    List<Conversation.ConversationType> mTypes = new ArrayList<Conversation.ConversationType>();


    public static ConversationTypeFilter obtain(Conversation.ConversationType... conversationType) {
        return new ConversationTypeFilter(conversationType);
    }

    public static ConversationTypeFilter obtain(Level level) {
        return new ConversationTypeFilter(level);
    }

    public static ConversationTypeFilter obtain() {
        return new ConversationTypeFilter();
    }


    private ConversationTypeFilter(Conversation.ConversationType... type) {
        mTypes.addAll(Arrays.asList(type));
        mLevel = Level.CONVERSATION_TYPE;
    }

    private ConversationTypeFilter() {
        mLevel = Level.ALL;
    }

    private ConversationTypeFilter(Level level) {
        mLevel = level;
    }

    public Level getLevel() {
        return mLevel;
    }

    public List<Conversation.ConversationType> getConversationTypeList() {
        return mTypes;
    }


    public boolean hasFilter(Message message) {
        if (mLevel == Level.ALL)
            return true;

        if (mLevel == Level.CONVERSATION_TYPE) {
            if (mTypes.contains(message.getConversationType())) return true;
            return false;
        }

        return false;
    }

}
