package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class SdkGroupDetailObject {
	private long gid;
	private String ownerUsername;
	private String groupName;
	private String groupDesc;
	private ArrayList<SdkUserInfoObject> members;
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
	public ArrayList<SdkUserInfoObject> getMembers() {
		return members;
	}
	public void setMembers(ArrayList<SdkUserInfoObject> members) {
		this.members = members;
	}
	
}
