package com.jpush.protocal.im.bean;

public class SendGroupMsgRequestBean {
	private long target_gid;
	private String msg_content;
	public SendGroupMsgRequestBean(){}
	public SendGroupMsgRequestBean(long target_gid, String msg_content){
		this.target_gid = target_gid;
		this.msg_content = msg_content;
	}
	
	public long getTarget_gid() {
		return target_gid;
	}
	public void setTarget_gid(long target_gid) {
		this.target_gid = target_gid;
	}
	public String getMsg_content() {
		return msg_content;
	}
	public void setMsg_content(String msg_content) {
		this.msg_content = msg_content;
	}
	
}
