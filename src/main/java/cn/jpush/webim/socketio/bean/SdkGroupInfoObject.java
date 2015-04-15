package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkGroupInfoObject {
	private long gid;
	private String owner_username;
	private String group_name;
	private String group_desc;
	private ArrayList<String> members_username;
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public String getOwner_username() {
		return owner_username;
	}
	public void setOwner_username(String owner_username) {
		this.owner_username = owner_username;
	}
	public String getGroup_name() {
		return group_name;
	}
	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}
	public String getGroup_desc() {
		return group_desc;
	}
	public void setGroup_desc(String group_desc) {
		this.group_desc = group_desc;
	}
	public ArrayList<String> getMembers_username() {
		return members_username;
	}
	public void setMembers_username(ArrayList<String> members_username) {
		this.members_username = members_username;
	}
	
}
