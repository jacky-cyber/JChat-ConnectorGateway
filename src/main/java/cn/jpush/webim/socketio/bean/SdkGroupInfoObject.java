package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkGroupInfoObject {
	private long gid;
	private String ownerUsername;
	private String groupName;
	private String groupDesc;
	private ArrayList<String> membersUsername;
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public String getOwnerUsername() {
		return ownerUsername;
	}
	public void setOwnerUsername(String ownerUsername) {
		this.ownerUsername = ownerUsername;
	}
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	public String getGroupDesc() {
		return groupDesc;
	}
	public void setGroupDesc(String groupDesc) {
		this.groupDesc = groupDesc;
	}
	public ArrayList<String> getMembersUsername() {
		return membersUsername;
	}
	public void setMembersUsername(ArrayList<String> membersUsername) {
		this.membersUsername = membersUsername;
	}
	
}
