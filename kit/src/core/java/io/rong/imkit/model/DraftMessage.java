package io.rong.imkit.model;

import io.rong.imlib.model.Conversation;

public class DraftMessage {
    private String content;
    private Conversation.ConversationType conversationType;  //该draft所在会话的会话类型
    private String targetId;

    public DraftMessage(){}

    public static DraftMessage obtain(Conversation.ConversationType type,String id,String msgContent){
        DraftMessage obj = new DraftMessage();
        obj.content = msgContent;
        obj.conversationType = type;
        obj.targetId = id;
        return obj;
    }

    public void setContent(String msg){
        this.content = msg;
    }

    public String getContent(){
        return content;
    }

    public void setTargetId(String id){
        this.targetId = id;
    }
    public String getTargetId(){
        return targetId;
    }

    public void setConversationType(Conversation.ConversationType type){
        this.conversationType = type;
    }

    public Conversation.ConversationType getConversationType(){
        return conversationType;
    }
}
