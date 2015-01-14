package com.jpush.protocal.im.bean;


public class ExitGroupRequestBean{
	private long gid;
	public ExitGroupRequestBean(){}
	public ExitGroupRequestBean(long gid){
		this.gid = gid;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	
}
