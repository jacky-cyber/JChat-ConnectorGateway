package cn.jpush.webim.socketio.bean;

public class ExitGroupBean {
	private String appKey;
	private long uid;
	private int sid;
	private long juid;
	private long gid;
	private long rid;
	public String getAppKey() {
		return appKey;
	}
	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}
	
	public long getRid() {
		return rid;
	}
	public void setRid(long rid) {
		this.rid = rid;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
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
	public long getGid() {
		return gid;
	}
	public void setGid(long gid) {
		this.gid = gid;
	}
	
}
