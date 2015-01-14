package com.jpush.protocal.im.bean;

import java.util.List;


public class AddGroupMemberRequestBean{
	private long gid;
	private int member_count;
	private List<Long> member_uid_list;
	public AddGroupMemberRequestBean(){}
	public AddGroupMemberRequestBean(long gid, int member_count, List list){
		this.gid = gid;
		this.member_count = member_count;
		this.member_uid_list = list;
	}
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	public int getMember_count() {
		return member_count;
	}
	public void setMember_count(int member_count) {
		this.member_count = member_count;
	}
	public List getMember_uid_list() {
		return member_uid_list;
	}
	public void setMember_uid_list(List member_uid_list) {
		this.member_uid_list = member_uid_list;
	}
	
}
