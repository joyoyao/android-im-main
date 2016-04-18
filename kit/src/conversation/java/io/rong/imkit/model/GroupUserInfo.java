package io.rong.imkit.model;

/**
 * Created by weiqinxiao on 15/10/28.
 */
public class GroupUserInfo {
    private String nickname;
    private String userId;
    private String groupId;

    public GroupUserInfo(String groupId, String userId, String nickname) {
        this.groupId = groupId;
        this.nickname = nickname;
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getUserId() {
        return userId;
    }
}
