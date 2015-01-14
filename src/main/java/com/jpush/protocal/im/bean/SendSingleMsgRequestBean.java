package com.jpush.protocal.im.bean;

public class SendSingleMsgRequestBean {
	private long target_uid;
	private String msg_content;
	public SendSingleMsgRequestBean(){}
	public SendSingleMsgRequestBean(long target_uid, String msg_content){
		this.target_uid = target_uid;
		this.msg_content = msg_content;
	}
	public long getTarget_uid() {
		return target_uid;
	}
	public void setTarget_uid(long target_uid) {
		this.target_uid = target_uid;
	}
	public String getMsg_content() {
		return msg_content;
	}
	public void setMsg_content(String msg_content) {
		this.msg_content = msg_content;
	}
	
}
