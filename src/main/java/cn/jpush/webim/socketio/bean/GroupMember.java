package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

public class GroupMember {
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public long getJuid() {
		return juid;
	}
	public void setJuid(long juid) {
		this.juid = juid;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public ArrayList<Object> getGroups() {
		return groups;
	}
	public void setGroups(ArrayList<Object> groups) {
		this.groups = groups;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	private long uid;
	private long juid;
	private String username;
	private String password;
	private ArrayList<Object> groups;
	private int flag;
}
