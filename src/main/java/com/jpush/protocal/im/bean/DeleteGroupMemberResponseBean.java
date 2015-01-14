package com.jpush.protocal.im.bean;

public class DeleteGroupMemberResponseBean{
	private int code;
	private String message;
	private String gid;
	public DeleteGroupMemberResponseBean(){}
	public DeleteGroupMemberResponseBean(int code, String message, String gid){
		this.code = code;
		this.message = message;
		this.gid = gid;
	}
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getGid() {
		return gid;
	}
	public void setGid(String gid) {
		this.gid = gid;
	}
	
}
