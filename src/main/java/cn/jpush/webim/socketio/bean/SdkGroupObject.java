package cn.jpush.webim.socketio.bean;

public class SdkGroupObject {
	private String group_name;
	private String group_description;
	private int group_level;
	private int flag;
	private long gid;
	
	public String getGroup_name() {
		return group_name;
	}
	public void setGroup_name(String group_name) {
		this.group_name = group_name;
	}
	public String getGroup_description() {
		return group_description;
	}
	public void setGroup_description(String group_description) {
		this.group_description = group_description;
	}
	public int getGroup_level() {
		return group_level;
	}
	public void setGroup_level(int group_level) {
		this.group_level = group_level;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	
}
