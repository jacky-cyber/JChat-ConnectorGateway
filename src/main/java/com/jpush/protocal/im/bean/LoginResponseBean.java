package com.jpush.protocal.im.bean;


public class LoginResponseBean{
	private int code;
	private String message;
	private String username;
	private long uid;
	public LoginResponseBean(){}
	public LoginResponseBean(int code, String message, String username, long uid){
		this.code = code;
		this.username = username;
		this.message = message;
		this.uid = uid;
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	
}
