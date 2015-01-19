package com.jpush.protocal.push;

public class PushMessageRequestBean {
	private int msgType;
	private long messageId;
	private String message;
	public PushMessageRequestBean(int msgType, long messageId, String message) {
		super();
		this.msgType = msgType;
		this.messageId = messageId;
		this.message = message;
	}
	public int getMsgType() {
		return msgType;
	}
	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}
	public long getMessageId() {
		return messageId;
	}
	public void setMessageId(long messageId) {
		this.messageId = messageId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
}
