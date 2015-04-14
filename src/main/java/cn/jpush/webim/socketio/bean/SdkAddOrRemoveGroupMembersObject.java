package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkAddOrRemoveGroupMembersObject {
	private long group_id;
	private ArrayList<String> member_usernames;
	public long getGroup_id() {
		return group_id;
	}
	public void setGroup_id(long group_id) {
		this.group_id = group_id;
	}
	public ArrayList<String> getMember_usernames() {
		return member_usernames;
	}
	public void setMember_usernames(ArrayList<String> member_usernames) {
		this.member_usernames = member_usernames;
	} 
	
}
