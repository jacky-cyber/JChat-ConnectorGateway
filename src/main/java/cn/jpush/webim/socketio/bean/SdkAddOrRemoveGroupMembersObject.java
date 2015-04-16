package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkAddOrRemoveGroupMembersObject {
	private long groupId;
	private ArrayList<String> memberUsernames;
	public long getGroupId() {
		return groupId;
	}
	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}
	public ArrayList<String> getMemberUsernames() {
		return memberUsernames;
	}
	public void setMemberUsernames(ArrayList<String> memberUsernames) {
		this.memberUsernames = memberUsernames;
	}
	
	
}
