package com.jpush.protocal.im.bean;


public class UpdateGroupInfoRequestBean{
	private long gid;
	private String name;
	private String content;
	public UpdateGroupInfoRequestBean(){}
	public UpdateGroupInfoRequestBean(long gid, String name, String content){
		this.gid = gid;
		this.name = name;
		this.content = content;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
}
