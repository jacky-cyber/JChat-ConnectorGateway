package cn.jpush.webim.socketio.bean;

import java.util.ArrayList;

/*{"uid":55,"juid":2911,"username":"p001","password":"6A79A5630C94E097520365217EA74CF0",
	"groups":[{"gid":197,"name":"group01","desc":"","level":0,"flag":0,"users":[]}]
			}*/
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
	private long uid;
	private long juid;
	private String username;
	private String password;
	private ArrayList<Object> groups;
	
}
