package com.jpush.protocal.im.bean;


public class LoginRequestBean{
	private String username;
	private String password;
	private String appkey;
	public LoginRequestBean(){}
	public LoginRequestBean(String username, String password, String appkey){
		this.username = username;
		this.password = password;
		this.appkey = appkey;
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
	public String getAppkey() {
		return appkey;
	}
	public void setAppkey(String appkey) {
		this.appkey = appkey;
	}
	
}
