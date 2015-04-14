package cn.jpush.webim.socketio.bean;

public class SdkUpdateGroupInfoObject {
	private long group_id;
	private String group_name;
	private String group_description;
	public long getGroup_id() {
		return group_id;
	}
	public void setGroup_id(long group_id) {
		this.group_id = group_id;
	}

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
	
}
