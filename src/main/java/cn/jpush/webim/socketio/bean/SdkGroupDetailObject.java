package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkGroupDetailObject {
	private long gid;
	private String owner_username;
	private String group_name;
	private String group_desc;
	private ArrayList<SdkUserInfoObject> members;
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
	public ArrayList<SdkUserInfoObject> getMembers() {
		return members;
	}
	public void setMembers(ArrayList<SdkUserInfoObject> members) {
		this.members = members;
	}
	
}
