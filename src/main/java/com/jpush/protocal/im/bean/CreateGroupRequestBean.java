package com.jpush.protocal.im.bean;


public class CreateGroupRequestBean{
	private String group_name;
	private String group_desc;
	private int group_level;
	private int flag;
	public CreateGroupRequestBean(){}
	public CreateGroupRequestBean(String group_name, String group_desc, int group_level, int flag){
		this.group_name = group_name;
		this.group_desc = group_desc;
		this.group_level = group_level;
		this.flag = flag;
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

	
}
