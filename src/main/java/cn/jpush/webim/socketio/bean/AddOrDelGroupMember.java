package cn.jpush.webim.socketio.bean;

public class AddOrDelGroupMember {
	private String appKey;
	private int sid;
	private long juid;
	private long uid;
	private long toUid;
	private long gid;
	private int member_count;
	private String username;
	
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	public int getSid() {
		return sid;
	}
	public void setSid(int sid) {
		this.sid = sid;
	}
	public long getJuid() {
		return juid;
	}
	public void setJuid(long juid) {
		this.juid = juid;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
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
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public long getToUid() {
		return toUid;
	}
	public void setToUid(long toUid) {
		this.toUid = toUid;
	}
	
}
