package com.jpush.webim.socketio.bean;

public class ChatObject {
	private String appKey;
	private String userName;
	private long uid;
	private String password;
	private String toUserName;
	private long toUid;
	private String message;
	private String msgType;
	public ChatObject(){};
	public ChatObject(String userName, String message){
		this.userName = userName;
		this.message = message;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getToUserName() {
		return toUserName;
	}
	public void setToUserName(String toUserName) {
		this.toUserName = toUserName;
	}
	public String getMsgType() {
		return msgType;
	}
	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public long getToUid() {
		return toUid;
	}
	public void setToUid(long toUid) {
		this.toUid = toUid;
	}
	
}	
