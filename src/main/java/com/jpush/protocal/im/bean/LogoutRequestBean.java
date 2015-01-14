package com.jpush.protocal.im.bean;


public class LogoutRequestBean{	
	private String username;
	private String appkey;
	public LogoutRequestBean(){}
	public LogoutRequestBean(String username, String appkey){
		this.username = username;
		this.appkey = appkey;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getAppkey() {
		return appkey;
	}
	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}
	
}
