package com.jpush.protocal.im.bean;

public class LogoutResponseBean{
	private int code;
	private String message;
	private String username;
	public LogoutResponseBean(){}
	public LogoutResponseBean(int code, String message, String username){
		this.code = code;
		this.message = message;
		this.username = username;
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
	
}
