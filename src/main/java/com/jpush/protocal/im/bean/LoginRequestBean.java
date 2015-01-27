package com.jpush.protocal.im.bean;


public class LoginRequestBean{
	private String username;
	private String password;
	public LoginRequestBean(){}
	public LoginRequestBean(String username, String password){
		this.username = username;
		this.password = password;
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
	
}
